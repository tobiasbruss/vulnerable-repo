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
     * Uses a parameterized JPQL query to prevent SQL injection.
     */
    public List<Book> searchBooks(String query) {
        String jpql = "SELECT b FROM Book b WHERE b.title LIKE :pattern OR b.author LIKE :pattern";
        return entityManager.createQuery(jpql, Book.class)
                .setParameter("pattern", "%" + query + "%")
                .getResultList();
    }

    /**
     * Export book data in the specified format.
     * Supported formats: csv, json, xml
     */
    public String exportBookData(String format) {
        // Validate format against an allowlist before use
        if (!java.util.Set.of("csv", "json", "xml").contains(format)) {
            return "Unsupported export format";
        }
        try {
            logger.info("Running export");
            // Use array form of exec() to prevent shell interpretation
            String[] command = {"/opt/bookstore/scripts/export.sh", format};
            Process process = Runtime.getRuntime().exec(command);
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                return "Export completed successfully";
            } else {
                return "Export failed";
            }
        } catch (Exception e) {
            logger.error("Export failed");
            return "Export error occurred";
        }
    }
}
