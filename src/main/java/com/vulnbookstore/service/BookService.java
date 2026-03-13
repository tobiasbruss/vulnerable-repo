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
     */
    public String exportBookData(String format) {
        // Map user input to a literal to prevent taint flow into Runtime.exec
        final String safeFormat;
        switch (format) {
            case "csv":  safeFormat = "csv";  break;
            case "json": safeFormat = "json"; break;
            case "xml":  safeFormat = "xml";  break;
            default:
                return "Unsupported export format. Allowed formats: csv, json, xml";
        }
        try {
            String[] command = {"/opt/bookstore/scripts/export.sh", safeFormat};
            logger.info("Running export with format: {}", safeFormat);

            Process process = Runtime.getRuntime().exec(command);
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                return "Export completed successfully in format: " + safeFormat;
            } else {
                return "Export failed";
            }
        } catch (Exception e) {
            logger.error("Export failed: {}", e.getMessage());
            return "Export failed";
        }
    }
}
