package com.vulnbookstore;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * VulnBookStore - A BookStore REST API
 *
 * ⚠️ WARNING: This application is INTENTIONALLY VULNERABLE.
 * It is designed for educational purposes to demonstrate GitHub Advanced Security (GHAS).
 * DO NOT USE IN PRODUCTION.
 */
@SpringBootApplication
public class VulnBookStoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(VulnBookStoreApplication.class, args);
    }
}
