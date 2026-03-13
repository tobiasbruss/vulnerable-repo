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

    private static final String ADMIN_PASSWORD = System.getenv().getOrDefault("ADMIN_PASSWORD", "changeme-admin");
    private static final String USER_PASSWORD  = System.getenv().getOrDefault("USER_PASSWORD",  "changeme-user");

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll()
            )
            .headers(headers -> headers
                .frameOptions(frame -> frame.sameOrigin())
            );

        return http.build();
    }

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
