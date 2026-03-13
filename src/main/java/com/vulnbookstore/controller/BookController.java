package com.vulnbookstore.controller;

import com.vulnbookstore.model.Book;
import com.vulnbookstore.service.BookService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.web.util.HtmlUtils;

import java.util.List;

/**
 * REST controller for Book management endpoints.
 */
@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;

    @GetMapping
    public ResponseEntity<List<Book>> getAllBooks() {
        return ResponseEntity.ok(bookService.getAllBooks());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Book> getBookById(@PathVariable Long id) {
        return bookService.getBookById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Book> createBook(@RequestBody Book book) {
        Book created = bookService.createBook(book);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Book> updateBook(@PathVariable Long id, @RequestBody Book book) {
        return bookService.updateBook(id, book)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBook(@PathVariable Long id) {
        if (bookService.deleteBook(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<List<Book>> getBooksByCategory(@PathVariable String category) {
        return ResponseEntity.ok(bookService.getBooksByCategory(category));
    }

    /**
     * Search books by title or author.
     *
     * ⚠️ VULNERABILITY: The 'q' parameter is passed directly to BookService.searchBooks()
     * which concatenates it into a raw SQL query — SQL Injection.
     *
     * Example exploit:
     *   GET /api/books/search?q=' OR '1'='1
     *   GET /api/books/search?q='; DROP TABLE books; --
     */
    @GetMapping("/search")
    public ResponseEntity<List<Book>> searchBooks(@RequestParam("q") String query) {
        // TODO: add input validation/sanitization
        List<Book> results = bookService.searchBooks(query);
        return ResponseEntity.ok(results);
    }

    /**
     * Render a book's description as HTML.
     * All user-controlled fields are HTML-escaped before embedding in the response.
     */
    @GetMapping("/{id}/render-description")
    public ResponseEntity<String> renderDescription(@PathVariable Long id) {
        return bookService.getBookById(id).map(book -> {
            String html = "<html><body>" +
                          "<h1>" + HtmlUtils.htmlEscape(book.getTitle()) + "</h1>" +
                          "<h2>by " + HtmlUtils.htmlEscape(book.getAuthor()) + "</h2>" +
                          "<div class=\"description\">" + HtmlUtils.htmlEscape(book.getDescription()) + "</div>" +
                          "</body></html>";
            return ResponseEntity.ok()
                    .header("Content-Type", "text/html")
                    .body(html);
        }).orElse(ResponseEntity.notFound().build());
    }

    /**
     * Export book data in the specified format (csv, json, xml).
     * Format validation is enforced in the service layer.
     */
    @GetMapping("/export")
    public ResponseEntity<String> exportBooks(@RequestParam("format") String format) {
        String result = bookService.exportBookData(format);
        return ResponseEntity.ok(result);
    }
}
