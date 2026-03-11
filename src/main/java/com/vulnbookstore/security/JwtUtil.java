package com.vulnbookstore.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for generating and validating JWT tokens.
 *
 * ⚠️ WARNING: This class contains intentional security vulnerabilities
 * for educational/demonstration purposes.
 */
@Component
public class JwtUtil {

    private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);

    // ⚠️ VULNERABILITY: Hardcoded JWT secret key in source code.
    // GitHub Secret Scanning and CodeQL should flag this.
    // A short, guessable key like this can be brute-forced offline.
    // Should be loaded from environment variables: System.getenv("JWT_SECRET")
    private static final String SECRET = "mySecretKey123";

    // ⚠️ VULNERABILITY: Token expiration is set to a very long duration (30 days).
    // Combined with no server-side revocation, stolen tokens remain valid for weeks.
    private static final long EXPIRATION_MS = 30L * 24 * 60 * 60 * 1000; // 30 days

    /**
     * Generate a JWT token for the given username and role.
     *
     * ⚠️ VULNERABILITY: Uses HS256 with a short, hardcoded key.
     * HS256 is symmetric — the same key signs and verifies, so any party
     * with the key can forge tokens. The key "mySecretKey123" is too short
     * and easily brute-forced.
     */
    public String generateToken(String username, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_MS))
                .signWith(SignatureAlgorithm.HS256, SECRET) // ⚠️ weak key
                .compact();
    }

    /**
     * Extract the username (subject) from a JWT token.
     */
    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    /**
     * Extract the role claim from a JWT token.
     */
    public String extractRole(String token) {
        return (String) extractAllClaims(token).get("role");
    }

    /**
     * Validate a JWT token.
     *
     * ⚠️ VULNERABILITY: Token expiration is checked by the JJWT library when
     * parsing, but this method catches ALL exceptions and returns false — meaning
     * a malformed or tampered token is silently rejected rather than logged/alerted.
     * More critically, if expiration validation were disabled (e.g., by using
     * a custom clock), expired tokens would be accepted indefinitely.
     *
     * Additionally, there is no check for token revocation — once issued,
     * a token is valid until expiry regardless of logout or password change.
     */
    public boolean validateToken(String token) {
        try {
            extractAllClaims(token);
            return true;
        } catch (Exception e) {
            // ⚠️ VULNERABILITY: silently swallowing all JWT exceptions
            // An attacker probing for weaknesses gets no useful error feedback,
            // but legitimate security monitoring also loses visibility.
            logger.debug("Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Check whether a token is expired.
     *
     * ⚠️ VULNERABILITY: This method is defined but never called in validateToken().
     * Expiration is only checked implicitly by JJWT during parsing.
     * If the library behavior changes or is misconfigured, expiration could be skipped.
     */
    public boolean isTokenExpired(String token) {
        Date expiration = extractAllClaims(token).getExpiration();
        return expiration.before(new Date());
    }

    private Claims extractAllClaims(String token) {
        // ⚠️ VULNERABILITY: uses the hardcoded SECRET string directly as the signing key
        return Jwts.parser()
                .setSigningKey(SECRET)
                .parseClaimsJws(token)
                .getBody();
    }
}
