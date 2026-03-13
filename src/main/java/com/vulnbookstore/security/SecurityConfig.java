package com.vulnbookstore.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security configuration for VulnBookStore.
 *
 * ⚠️ WARNING: This configuration is INTENTIONALLY INSECURE for educational purposes.
 * Multiple security misconfigurations are present below.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // ⚠️ VULNERABILITY: Hardcoded admin password in source code.
    // Secret scanning tools (e.g., GitHub Secret Scanning, truffleHog) should flag this.
    // Should be loaded from environment variables or a secrets manager.
    private static final String ADMIN_PASSWORD = "admin@Bookstore2024!";

    // ⚠️ VULNERABILITY: Hardcoded user password in source code.
    private static final String USER_PASSWORD = "userpass123";

    /**
     * Security filter chain configuration.
     *
     * ⚠️ VULNERABILITY 1: CSRF protection is disabled.
     * This makes the application vulnerable to Cross-Site Request Forgery attacks.
     * Comment says "disabled for simplicity" — a common but dangerous shortcut.
     *
     * ⚠️ VULNERABILITY 2: All requests are permitted without authentication.
     * This is overly permissive — broken access control at the framework level.
     * Even "protected" endpoints are accessible to unauthenticated users.
     *
     * ⚠️ VULNERABILITY 3: H2 console is accessible to all, and frame options
     * are disabled to allow the H2 console iframe to render.
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // ⚠️ VULNERABILITY: All endpoints are publicly accessible
            .authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll()
            )

            // Allow H2 console to be embedded in iframes (disables X-Frame-Options)
            .headers(headers -> headers
                .frameOptions(frame -> frame.disable())
            );

        return http.build();
    }

    /**
     * In-memory user store with hardcoded credentials.
     *
     * ⚠️ VULNERABILITY: Hardcoded credentials in source code.
     * Passwords are stored using {noop} (plaintext) — no hashing applied.
     * Should use BCryptPasswordEncoder and load credentials from secure storage.
     */
    @Bean
    public UserDetailsService userDetailsService() {
        // ⚠️ VULNERABILITY: {noop} means plaintext password — no encoding
        UserDetails admin = User.withUsername("admin")
                .password("{noop}" + ADMIN_PASSWORD)
                .roles("ADMIN")
                .build();

        UserDetails regularUser = User.withUsername("user")
                .password("{noop}" + USER_PASSWORD)
                .roles("USER")
                .build();

        return new InMemoryUserDetailsManager(admin, regularUser);
    }
}
