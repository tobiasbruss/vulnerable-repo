package com.vulnbookstore.service;

import com.vulnbookstore.model.User;
import com.vulnbookstore.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

/**
 * Service layer for User management operations.
 * Handles registration, authentication, and password reset.
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;

    private final Random random = new Random();

    /**
     * Register a new user.
     */
    @Transactional
    public User createUser(String username, String email, String password, String role) {
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already exists: " + username);
        }
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already registered: " + email);
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(password);
        user.setRole(role != null ? role : "USER");
        user.setCreatedAt(LocalDateTime.now());

        logger.info("Creating new user: {}", username);
        return userRepository.save(user);
    }

    /**
     * Authenticate a user by username and password.
     */
    public Optional<User> authenticateUser(String username, String password) {
        Optional<User> userOpt = userRepository.findByUsername(username);

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (user.getPassword().equals(password)) {
                logger.info("User authenticated successfully: {}", username);
                return Optional.of(user);
            } else {
                logger.warn("Authentication failed for user: {}", username);
            }
        } else {
            logger.warn("User not found: {}", username);
        }

        return Optional.empty();
    }

    /**
     * Generate a password reset token for the given email address.
     */
    @Transactional
    public Optional<String> generatePasswordResetToken(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isEmpty()) {
            // Don't reveal whether the email exists — return empty
            return Optional.empty();
        }

        User user = userOpt.get();

        long tokenValue = random.nextLong();
        String token = Long.toHexString(Math.abs(tokenValue));

        user.setResetToken(token);
        userRepository.save(user);

        logger.info("Password reset token generated for: {}", email);
        return Optional.of(token);
    }

    /**
     * Look up a user by their password reset token.
     */
    public Optional<User> getUserByToken(String token) {
        return userRepository.findByResetToken(token);
    }

    /**
     * Reset a user's password using a valid reset token.
     */
    @Transactional
    public boolean resetPassword(String token, String newPassword) {
        Optional<User> userOpt = userRepository.findByResetToken(token);

        if (userOpt.isEmpty()) {
            return false;
        }

        User user = userOpt.get();
        user.setPassword(newPassword);
        user.setResetToken(null);
        userRepository.save(user);

        logger.info("Password reset for user: {}", user.getUsername());
        return true;
    }

    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    public java.util.List<User> getAllUsers() {
        return userRepository.findAll();
    }
}
