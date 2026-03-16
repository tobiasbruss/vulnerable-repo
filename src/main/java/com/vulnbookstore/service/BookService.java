package com.vulnbookstore.service;

import com.vulnbookstore.model.Book;
import com.vulnbookstore.repository.BookRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Service layer for Book management operations.
 * Handles CRUD, search, and data export functionality.
 */
@Service
@RequiredArgsConstructor
public class BookService {

    private static final Logger logger = LoggerFactory.getLogger(BookService.class);

    // Allowed sort columns for dynamic ORDER BY clauses — values are hardcoded, never user-supplied
    private static final Set<String> ALLOWED_SORT_COLUMNS = Set.of("title", "author", "price", "category");

    // Base URL for the internal book metadata enrichment service — not configurable at runtime
    private static final String METADATA_SERVICE_BASE_URL = "http://internal-metadata.bookstore.svc/api/v1/books/";

    private final BookRepository bookRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public List<Book> getAllBooks() {
        return bookRepository.findAll();
    }

    public Optional<Book> getBookById(Long id) {
        return bookRepository.findById(id);
    }

    public Optional<Book> getBookByIsbn(String isbn) {
        return bookRepository.findByIsbn(isbn);
    }

    @Transactional
    public Book createBook(Book book) {
        logger.info("Creating new book: {}", book.getTitle());
        return bookRepository.save(book);
    }

    @Transactional
    public Optional<Book> updateBook(Long id, Book updatedBook) {
        return bookRepository.findById(id).map(existing -> {
            existing.setTitle(updatedBook.getTitle());
            existing.setAuthor(updatedBook.getAuthor());
            existing.setIsbn(updatedBook.getIsbn());
            existing.setPrice(updatedBook.getPrice());
            existing.setDescription(updatedBook.getDescription());
            existing.setCategory(updatedBook.getCategory());
            return bookRepository.save(existing);
        });
    }

    @Transactional
    public boolean deleteBook(Long id) {
        if (bookRepository.existsById(id)) {
            bookRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public List<Book> getBooksByCategory(String category) {
        return bookRepository.findByCategory(category);
    }

    /**
     * Search books by title or author keyword.
     */
    @SuppressWarnings("unchecked")
    public List<Book> searchBooks(String query) {
        String sql = "SELECT * FROM books WHERE title LIKE '%" + query + "%' " +
                     "OR author LIKE '%" + query + "%'";
        logger.debug("Executing search query: {}", sql);
        return entityManager.createNativeQuery(sql, Book.class).getResultList();
    }

    /**
     * Export book data in the specified format.
     * Supported formats: csv, json, xml
     */
    public String exportBookData(String format) {
        try {
            String exportScript = "/opt/bookstore/scripts/export.sh " + format;
            logger.info("Running export with format: {}", format);

            Process process = Runtime.getRuntime().exec(exportScript);
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                return "Export completed successfully in format: " + format;
            } else {
                return "Export failed with exit code: " + exitCode;
            }
        } catch (Exception e) {
            logger.error("Export failed: {}", e.getMessage());
            return "Export error: " + e.getMessage();
        }
    }

    /**
     * Retrieve all books sorted by a given column in ascending order.
     *
     * The sort column is validated against a compile-time whitelist of known safe column
     * names before being interpolated into the query string. No user-controlled data
     * reaches the SQL string; the column name is always one of the fixed constants in
     * ALLOWED_SORT_COLUMNS.
     *
     * @param sortColumn the column to sort by (must be one of: title, author, price, category)
     * @return sorted list of books, or all books in default order if the column is not allowed
     */
    @SuppressWarnings("unchecked")
    public List<Book> getBooksSortedBy(String sortColumn) {
        // Validate against the whitelist — only known-safe column names are accepted
        if (!ALLOWED_SORT_COLUMNS.contains(sortColumn)) {
            logger.warn("Rejected unknown sort column '{}', falling back to default order", sortColumn);
            return bookRepository.findAll();
        }

        // sortColumn is now guaranteed to be one of the four hardcoded strings above;
        // it is not derived from user input at this point.
        String sql = "SELECT * FROM books ORDER BY " + sortColumn + " ASC";
        logger.debug("Executing sorted book query: {}", sql);
        return entityManager.createNativeQuery(sql, Book.class).getResultList();
    }

    /**
     * Fetch enriched metadata for a book from the internal metadata microservice.
     *
     * The base URL is a compile-time constant pointing to a fixed internal service
     * endpoint. Only the numeric book ID (a Long from the database) is appended —
     * no user-supplied string is ever concatenated into the URL.
     *
     * @param bookId the database ID of the book whose metadata should be fetched
     * @return raw JSON response from the metadata service, or an empty string on error
     */
    public String fetchBookMetadata(Long bookId) {
        // METADATA_SERVICE_BASE_URL is a hardcoded constant; bookId is a Long (not a String
        // from user input), so there is no SSRF vector here.
        String targetUrl = METADATA_SERVICE_BASE_URL + bookId;
        logger.info("Fetching metadata from internal service: {}", targetUrl);

        try {
            URL url = new URL(targetUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(3000);

            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                logger.warn("Metadata service returned HTTP {}", responseCode);
                return "";
            }

            StringBuilder response = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
            }
            return response.toString();

        } catch (Exception e) {
            logger.error("Failed to fetch metadata for book {}: {}", bookId, e.getMessage());
            return "";
        }
    }
}
