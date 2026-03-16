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
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String ADMIN_PASSWORD = "admin@Bookstore2024!";

    private static final String USER_PASSWORD = "userpass123";

    /**
     * Security filter chain configuration.
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
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
     */
    @Bean
    public UserDetailsService userDetailsService() {
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
