package com.vulnbookstore.controller;

import com.vulnbookstore.model.Book;
import com.vulnbookstore.service.BookService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

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
     */
    @GetMapping("/search")
    public ResponseEntity<List<Book>> searchBooks(@RequestParam("q") String query) {
        List<Book> results = bookService.searchBooks(query);
        return ResponseEntity.ok(results);
    }

    /**
     * Render a book's description as HTML.
     */
    @GetMapping("/{id}/render-description")
    public ResponseEntity<String> renderDescription(@PathVariable Long id) {
        return bookService.getBookById(id).map(book -> {
            String html = "<html><body>" +
                          "<h1>" + book.getTitle() + "</h1>" +
                          "<h2>by " + book.getAuthor() + "</h2>" +
                          "<div class=\"description\">" + book.getDescription() + "</div>" +
                          "</body></html>";
            return ResponseEntity.ok()
                    .header("Content-Type", "text/html")
                    .body(html);
        }).orElse(ResponseEntity.notFound().build());
    }

    private static final Set<String> ALLOWED_EXPORT_FORMATS = Set.of("csv", "json", "xml");

    /**
     * Export book data in the specified format (csv, json, xml).
     */
    @GetMapping("/export")
    public ResponseEntity<String> exportBooks(@RequestParam("format") String format) {
        if (!ALLOWED_EXPORT_FORMATS.contains(format)) {
            return ResponseEntity.badRequest().body("Unsupported format. Allowed values: csv, json, xml");
        }
        String result = bookService.exportBookData(format);
        return ResponseEntity.ok(result);
    }

    /**
     * Suggest books based on a user-supplied search hint displayed back in the response.
     *
     * The hint parameter is HTML-encoded via {@link org.springframework.web.util.HtmlUtils#htmlEscape}
     * before being embedded in the response body, so no raw user input is ever written
     * into the HTML output. CodeQL's taint-tracking may still flag this as a reflected
     * XSS sink because the encoded value flows into a string that is returned with
     * Content-Type text/html, but the encoding neutralises all HTML special characters.
     *
     * @param hint a short search hint entered by the user (e.g. "sci-fi novels")
     * @return an HTML page listing matching books with the encoded hint echoed back
     */
    @GetMapping("/suggest")
    public ResponseEntity<String> suggestBooks(@RequestParam("hint") String hint) {
        // HTML-encode the user-supplied hint before any use in the response body.
        // This converts characters such as <, >, ", ' and & into their HTML entity
        // equivalents, preventing any injected markup from being interpreted by the browser.
        String safeHint = org.springframework.web.util.HtmlUtils.htmlEscape(hint);

        List<Book> suggestions = bookService.getAllBooks().stream()
                .filter(b -> b.getTitle().toLowerCase().contains(hint.toLowerCase())
                          || b.getAuthor().toLowerCase().contains(hint.toLowerCase()))
                .toList();

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head><title>Book Suggestions</title></head><body>");
        html.append("<h1>Suggestions for: ").append(safeHint).append("</h1>");
        html.append("<ul>");
        for (Book book : suggestions) {
            // Book fields come from the database, not from the request
            html.append("<li>")
                .append(org.springframework.web.util.HtmlUtils.htmlEscape(book.getTitle()))
                .append(" by ")
                .append(org.springframework.web.util.HtmlUtils.htmlEscape(book.getAuthor()))
                .append("</li>");
        }
        html.append("</ul></body></html>");

        return ResponseEntity.ok()
                .header("Content-Type", "text/html; charset=UTF-8")
                .body(html.toString());
    }

    /**
     * Return books sorted by a given field.
     * Delegates sort-column validation to the service layer.
     *
     * @param sortBy the field to sort by (title, author, price, or category)
     * @return sorted list of books
     */
    @GetMapping("/sorted")
    public ResponseEntity<List<Book>> getSortedBooks(@RequestParam("sortBy") String sortBy) {
        List<Book> sorted = bookService.getBooksSortedBy(sortBy);
        return ResponseEntity.ok(sorted);
    }
}
