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
     * Search books by title or author keyword using a parameterized JPQL query.
     */
    public List<Book> searchBooks(String query) {
        String jpql = "SELECT b FROM Book b WHERE b.title LIKE :pattern OR b.author LIKE :pattern";
        return entityManager.createQuery(jpql, Book.class)
                .setParameter("pattern", "%" + query + "%")
                .getResultList();
    }

    private static final Set<String> VALID_EXPORT_FORMATS = Set.of("csv", "json", "xml");

    /**
     * Export book data in the specified format.
     * Supported formats: csv, json, xml
     */
    public String exportBookData(String format) {
        if (!VALID_EXPORT_FORMATS.contains(format)) {
            logger.warn("Invalid export format requested");
            return "Invalid export format. Supported formats: csv, json, xml";
        }
        try {
            logger.info("Running export with format: {}", format);
            // Use array form of exec to prevent shell metacharacter injection
            Process process = Runtime.getRuntime().exec(
                    new String[]{"/opt/bookstore/scripts/export.sh", format});
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                return "Export completed successfully in format: " + format;
            } else {
                return "Export failed with exit code: " + exitCode;
            }
        } catch (Exception e) {
            logger.error("Export failed", e);
            return "Export failed due to an internal error";
        }
    }
}
