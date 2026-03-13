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
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

/**
 * Spring Security configuration for VulnBookStore.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String ADMIN_PASSWORD = System.getenv().getOrDefault("ADMIN_PASSWORD", "admin@Bookstore2024!");
    private static final String USER_PASSWORD = System.getenv().getOrDefault("USER_PASSWORD", "userpass123");

    /**
     * Security filter chain configuration.
     * CSRF protection is enabled using a cookie-based token repository so
     * JavaScript clients can read and send the CSRF token. The H2 console
     * path is excluded from CSRF enforcement since it is a dev-only tool.
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .ignoringRequestMatchers("/h2-console/**")
            )

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
     * In-memory user store. Passwords are encoded with BCrypt.
     * Credentials are loaded from environment variables with fallback defaults
     * for local development only.
     */
    @Bean
    public UserDetailsService userDetailsService() {
        UserDetails admin = User.withUsername("admin")
                .password("{bcrypt}" + org.springframework.security.crypto.bcrypt.BCrypt.hashpw(ADMIN_PASSWORD, org.springframework.security.crypto.bcrypt.BCrypt.gensalt()))
                .roles("ADMIN")
                .build();

        UserDetails regularUser = User.withUsername("user")
                .password("{bcrypt}" + org.springframework.security.crypto.bcrypt.BCrypt.hashpw(USER_PASSWORD, org.springframework.security.crypto.bcrypt.BCrypt.gensalt()))
                .roles("USER")
                .build();

        return new InMemoryUserDetailsManager(admin, regularUser);
    }
}
