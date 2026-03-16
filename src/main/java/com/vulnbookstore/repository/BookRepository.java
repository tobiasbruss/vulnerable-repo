package com.vulnbookstore.repository;

import com.vulnbookstore.model.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Book entity.
 * Provides standard CRUD operations plus custom search queries.
 */
@Repository
public interface BookRepository extends JpaRepository<Book, Long> {

    Optional<Book> findByIsbn(String isbn);

    List<Book> findByCategory(String category);

    List<Book> findByAuthor(String author);

    // Parameterized query for category filtering
    @Query("SELECT b FROM Book b WHERE b.category = :category AND b.price <= :maxPrice")
    List<Book> findByCategoryAndMaxPrice(@Param("category") String category,
                                         @Param("maxPrice") java.math.BigDecimal maxPrice);

    @Query(value = "SELECT * FROM books WHERE title LIKE %:title%", nativeQuery = true)
    List<Book> findByTitleContaining(@Param("title") String title);
}
