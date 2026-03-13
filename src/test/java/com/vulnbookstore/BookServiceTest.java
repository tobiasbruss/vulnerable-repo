package com.vulnbookstore;

import com.vulnbookstore.model.Book;
import com.vulnbookstore.repository.BookRepository;
import com.vulnbookstore.service.BookService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for BookService.
 * Tests cover standard CRUD operations and search functionality.
 *
 * Note: These tests verify functional correctness, not security properties.
 * Security vulnerabilities (SQL injection, command injection) are intentionally
 * present in the service but are not tested here.
 */
@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private EntityManager entityManager;

    @Mock
    private TypedQuery<Book> typedQuery;

    @InjectMocks
    private BookService bookService;

    private Book sampleBook1;
    private Book sampleBook2;

    @BeforeEach
    void setUp() {
        sampleBook1 = new Book(1L, "Clean Code", "Robert C. Martin",
                "978-0132350884", new BigDecimal("44.99"),
                "A handbook of agile software craftsmanship.", "Programming");

        sampleBook2 = new Book(2L, "The Pragmatic Programmer", "David Thomas",
                "978-0135957059", new BigDecimal("49.99"),
                "Your journey to mastery.", "Programming");

        // @PersistenceContext-injected fields are not handled by Mockito's @InjectMocks
        // (Mockito only injects via constructor/setter/field for its own annotations).
        // Manually inject the mock EntityManager so searchBooks() tests work correctly.
        ReflectionTestUtils.setField(bookService, "entityManager", entityManager);
    }

    // ----------------------------------------------------------------
    // getAllBooks()
    // ----------------------------------------------------------------

    @Test
    @DisplayName("getAllBooks() returns all books from repository")
    void getAllBooks_returnsAllBooks() {
        List<Book> expected = Arrays.asList(sampleBook1, sampleBook2);
        when(bookRepository.findAll()).thenReturn(expected);

        List<Book> result = bookService.getAllBooks();

        assertThat(result).hasSize(2);
        assertThat(result).containsExactlyInAnyOrder(sampleBook1, sampleBook2);
        verify(bookRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("getAllBooks() returns empty list when no books exist")
    void getAllBooks_returnsEmptyList_whenNoBooksExist() {
        when(bookRepository.findAll()).thenReturn(Collections.emptyList());

        List<Book> result = bookService.getAllBooks();

        assertThat(result).isEmpty();
        verify(bookRepository, times(1)).findAll();
    }

    // ----------------------------------------------------------------
    // getBookById()
    // ----------------------------------------------------------------

    @Test
    @DisplayName("getBookById() returns book when found")
    void getBookById_returnsBook_whenFound() {
        when(bookRepository.findById(1L)).thenReturn(Optional.of(sampleBook1));

        Optional<Book> result = bookService.getBookById(1L);

        assertTrue(result.isPresent());
        assertEquals("Clean Code", result.get().getTitle());
        verify(bookRepository).findById(1L);
    }

    @Test
    @DisplayName("getBookById() returns empty when not found")
    void getBookById_returnsEmpty_whenNotFound() {
        when(bookRepository.findById(99L)).thenReturn(Optional.empty());

        Optional<Book> result = bookService.getBookById(99L);

        assertFalse(result.isPresent());
    }

    // ----------------------------------------------------------------
    // createBook()
    // ----------------------------------------------------------------

    @Test
    @DisplayName("createBook() saves and returns the new book")
    void createBook_savesAndReturnsBook() {
        Book newBook = new Book(null, "Spring in Action", "Craig Walls",
                "978-1617294945", new BigDecimal("59.99"), "Spring framework guide.", "Java");
        Book savedBook = new Book(3L, "Spring in Action", "Craig Walls",
                "978-1617294945", new BigDecimal("59.99"), "Spring framework guide.", "Java");

        when(bookRepository.save(newBook)).thenReturn(savedBook);

        Book result = bookService.createBook(newBook);

        assertNotNull(result.getId());
        assertEquals(3L, result.getId());
        assertEquals("Spring in Action", result.getTitle());
        verify(bookRepository).save(newBook);
    }

    @Test
    @DisplayName("createBook() delegates to repository save")
    void createBook_delegatesToRepository() {
        when(bookRepository.save(any(Book.class))).thenReturn(sampleBook1);

        bookService.createBook(sampleBook1);

        verify(bookRepository, times(1)).save(sampleBook1);
    }

    // ----------------------------------------------------------------
    // updateBook()
    // ----------------------------------------------------------------

    @Test
    @DisplayName("updateBook() updates existing book fields")
    void updateBook_updatesExistingBook() {
        Book updatedData = new Book(null, "Clean Code 2nd Ed.", "Robert C. Martin",
                "978-0132350884", new BigDecimal("49.99"), "Updated edition.", "Programming");

        when(bookRepository.findById(1L)).thenReturn(Optional.of(sampleBook1));
        when(bookRepository.save(any(Book.class))).thenAnswer(inv -> inv.getArgument(0));

        Optional<Book> result = bookService.updateBook(1L, updatedData);

        assertTrue(result.isPresent());
        assertEquals("Clean Code 2nd Ed.", result.get().getTitle());
        assertEquals(new BigDecimal("49.99"), result.get().getPrice());
    }

    @Test
    @DisplayName("updateBook() returns empty when book not found")
    void updateBook_returnsEmpty_whenNotFound() {
        when(bookRepository.findById(99L)).thenReturn(Optional.empty());

        Optional<Book> result = bookService.updateBook(99L, sampleBook1);

        assertFalse(result.isPresent());
        verify(bookRepository, never()).save(any());
    }

    // ----------------------------------------------------------------
    // deleteBook()
    // ----------------------------------------------------------------

    @Test
    @DisplayName("deleteBook() returns true when book exists and is deleted")
    void deleteBook_returnsTrue_whenBookExists() {
        when(bookRepository.existsById(1L)).thenReturn(true);
        doNothing().when(bookRepository).deleteById(1L);

        boolean result = bookService.deleteBook(1L);

        assertTrue(result);
        verify(bookRepository).deleteById(1L);
    }

    @Test
    @DisplayName("deleteBook() returns false when book does not exist")
    void deleteBook_returnsFalse_whenBookNotFound() {
        when(bookRepository.existsById(99L)).thenReturn(false);

        boolean result = bookService.deleteBook(99L);

        assertFalse(result);
        verify(bookRepository, never()).deleteById(any());
    }

    // ----------------------------------------------------------------
    // searchBooks()
    // Note: This test verifies functional behavior only.
    // The SQL injection vulnerability in searchBooks() is intentional
    // and is NOT tested here — it is meant to be detected by CodeQL.
    // ----------------------------------------------------------------

    @Test
    @DisplayName("searchBooks() returns matching books for a normal query")
    void searchBooks_returnsMatchingBooks() {
        List<Book> expected = List.of(sampleBook1);

        when(entityManager.createQuery(anyString(), eq(Book.class)))
                .thenReturn(typedQuery);
        when(typedQuery.setParameter(anyString(), anyString())).thenReturn(typedQuery);
        when(typedQuery.getResultList()).thenReturn(expected);

        List<Book> result = bookService.searchBooks("Clean");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Clean Code");
    }

    @Test
    @DisplayName("searchBooks() returns empty list when no matches found")
    void searchBooks_returnsEmptyList_whenNoMatches() {
        when(entityManager.createQuery(anyString(), eq(Book.class)))
                .thenReturn(typedQuery);
        when(typedQuery.setParameter(anyString(), anyString())).thenReturn(typedQuery);
        when(typedQuery.getResultList()).thenReturn(Collections.emptyList());

        List<Book> result = bookService.searchBooks("nonexistent");

        assertThat(result).isEmpty();
    }
}
