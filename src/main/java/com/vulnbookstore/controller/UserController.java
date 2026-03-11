package com.vulnbookstore.controller;

import com.vulnbookstore.model.User;
import com.vulnbookstore.service.UserService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * REST controller for User management endpoints.
 * Handles registration, authentication, and user administration.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;

    /**
     * Register a new user account.
     */
    @PostMapping("/users/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> request) {
        try {
            String username = request.get("username");
            String email    = request.get("email");
            String password = request.get("password");
            String role     = request.getOrDefault("role", "USER");

            User user = userService.createUser(username, email, password, role);

            // ⚠️ VULNERABILITY: Sensitive data exposure — the full User object is
            // returned in the response, including the plaintext password field.
            // Should return a DTO that excludes the password.
            return ResponseEntity.status(HttpStatus.CREATED).body(user);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Authenticate a user and return their profile.
     *
     * ⚠️ VULNERABILITY: Sensitive data exposure — returns the full User object
     * including the plaintext password on successful login.
     */
    @PostMapping("/users/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String password = request.get("password");

        Optional<User> userOpt = userService.authenticateUser(username, password);

        if (userOpt.isPresent()) {
            // ⚠️ VULNERABILITY: returns full user object including password
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Login successful");
            response.put("user", userOpt.get()); // exposes password field
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid username or password"));
        }
    }

    /**
     * Get a user by ID.
     *
     * ⚠️ VULNERABILITY: Sensitive data exposure — returns the full User object
     * including the plaintext password. No authorization check — any caller
     * can retrieve any user's details including their password.
     */
    @GetMapping("/users/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        // TODO: add authorization check — users should only see their own profile
        return userService.getUserById(id)
                .map(user -> ResponseEntity.ok((Object) user)) // exposes password
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Request a password reset token.
     */
    @PostMapping("/users/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        Optional<String> token = userService.generatePasswordResetToken(email);

        // Always return success to avoid email enumeration
        return ResponseEntity.ok(Map.of("message",
                "If that email is registered, a reset link has been sent."));
    }

    /**
     * Reset password using a token.
     */
    @PostMapping("/users/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> request) {
        String token       = request.get("token");
        String newPassword = request.get("newPassword");

        if (userService.resetPassword(token, newPassword)) {
            return ResponseEntity.ok(Map.of("message", "Password reset successfully"));
        }
        return ResponseEntity.badRequest().body(Map.of("error", "Invalid or expired token"));
    }

    /**
     * Admin endpoint: list all users.
     *
     * ⚠️ VULNERABILITY: Broken Access Control — authorization is based solely on
     * a custom HTTP header value ("X-Admin-Token: admin-secret"). This is trivially
     * bypassable — any client that knows (or guesses) the header value gets full
     * admin access, regardless of their actual identity or role.
     *
     * Should use Spring Security's @PreAuthorize("hasRole('ADMIN')") or similar.
     */
    @GetMapping("/admin/users")
    public ResponseEntity<?> getAllUsers(
            @RequestHeader(value = "X-Admin-Token", required = false) String adminToken) {

        // ⚠️ VULNERABILITY: header-based "authentication" — easily bypassed
        if (!"admin-secret".equals(adminToken)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Access denied"));
        }

        // ⚠️ VULNERABILITY: returns all users including their plaintext passwords
        List<User> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }
}
