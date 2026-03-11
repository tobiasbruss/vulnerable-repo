-- VulnBookStore Sample Data
-- Populates the H2 in-memory database with books and users for demonstration.
--
-- ⚠️ NOTE: User passwords are stored in PLAINTEXT — intentional vulnerability.
-- In a real application, passwords must be hashed with BCrypt or Argon2.

-- ============================================================
-- Books
-- ============================================================
INSERT INTO books (title, author, isbn, price, description, category) VALUES
    ('The Pragmatic Programmer', 'David Thomas, Andrew Hunt', '978-0135957059', 49.99,
     'Your journey to mastery. A classic guide to software craftsmanship.',
     'Programming'),

    ('Clean Code', 'Robert C. Martin', '978-0132350884', 44.99,
     'A Handbook of Agile Software Craftsmanship. Learn to write readable, maintainable code.',
     'Programming'),

    ('Design Patterns', 'Gang of Four', '978-0201633610', 54.99,
     'Elements of Reusable Object-Oriented Software. The seminal patterns reference.',
     'Software Architecture'),

    ('The Mythical Man-Month', 'Frederick P. Brooks Jr.', '978-0201835953', 39.99,
     'Essays on Software Engineering. Why adding manpower to a late project makes it later.',
     'Software Engineering'),

    ('Introduction to Algorithms', 'Cormen, Leiserson, Rivest, Stein', '978-0262046305', 89.99,
     'Comprehensive introduction to algorithms and data structures. The definitive textbook.',
     'Computer Science'),

    ('You Don''t Know JS', 'Kyle Simpson', '978-1491924464', 34.99,
     'A deep dive into the core mechanisms of the JavaScript language.',
     'JavaScript'),

    ('Spring in Action', 'Craig Walls', '978-1617294945', 59.99,
     'Covers Spring framework fundamentals, Spring Boot, Spring MVC, and Spring Security.',
     'Java'),

    ('Java: The Complete Reference', 'Herbert Schildt', '978-1260440232', 69.99,
     'Comprehensive coverage of the Java programming language and its libraries.',
     'Java'),

    ('The Web Application Hacker''s Handbook', 'Stuttard & Pinto', '978-1118026472', 64.99,
     '<script>alert("XSS Demo: This description contains a stored XSS payload")</script>Finding and Exploiting Security Flaws.',
     'Security'),

    ('OWASP Testing Guide', 'OWASP Foundation', '978-0000000001', 0.00,
     'The definitive guide to web application security testing methodologies.',
     'Security');

-- ============================================================
-- Users
-- ⚠️ VULNERABILITY: Passwords stored in plaintext
-- ============================================================
INSERT INTO users (username, email, password, role, created_at) VALUES
    ('admin', 'admin@vulnbookstore.com', 'admin@Bookstore2024!', 'ADMIN', CURRENT_TIMESTAMP),
    ('alice', 'alice@example.com', 'password123', 'USER', CURRENT_TIMESTAMP),
    ('bob', 'bob@example.com', 'qwerty456', 'USER', CURRENT_TIMESTAMP),
    ('charlie', 'charlie@example.com', 'letmein', 'USER', CURRENT_TIMESTAMP),
    ('diana', 'diana@example.com', 'diana2024', 'USER', CURRENT_TIMESTAMP);

-- ============================================================
-- Orders
-- ============================================================
INSERT INTO orders (user_id, book_id, quantity, total_price, status, created_at) VALUES
    (2, 1, 1, 49.99, 'CONFIRMED', CURRENT_TIMESTAMP),
    (2, 2, 2, 89.98, 'SHIPPED',   CURRENT_TIMESTAMP),
    (3, 5, 1, 89.99, 'PENDING',   CURRENT_TIMESTAMP),
    (4, 3, 1, 54.99, 'CONFIRMED', CURRENT_TIMESTAMP),
    (5, 7, 1, 59.99, 'PENDING',   CURRENT_TIMESTAMP);
