package com.vulnbookstore.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

/**
 * Utility class for cryptographic operations.
 * Provides password hashing and data encryption for the bookstore application.
 *
 * ⚠️ WARNING: This class uses intentionally weak cryptographic algorithms
 * for educational/demonstration purposes. DO NOT use in production.
 */
public class CryptoUtil {

    private static final Logger logger = LoggerFactory.getLogger(CryptoUtil.class);

    // ⚠️ VULNERABILITY: Hardcoded encryption key in source code.
    // GitHub Secret Scanning and CodeQL should flag this.
    // Keys must never be hardcoded — use a KMS or environment variable.
    private static final String ENCRYPTION_KEY = "DES_KEY_";  // exactly 8 bytes for DES

    /**
     * Hash a password using MD5.
     *
     * ⚠️ VULNERABILITY: MD5 is a cryptographically broken hash function.
     * It is fast (enabling brute-force/rainbow table attacks), collision-prone,
     * and has no salt — identical passwords produce identical hashes.
     *
     * Should use BCrypt, Argon2, or PBKDF2 with a random salt instead.
     *
     * CWE-327: Use of a Broken or Risky Cryptographic Algorithm
     *
     * @param password the plaintext password to hash
     * @return MD5 hex digest of the password
     */
    public static String hashPassword(String password) {
        try {
            // ⚠️ VULNERABILITY: MD5 — broken for security use
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hashBytes = md.digest(password.getBytes(StandardCharsets.UTF_8));

            // Convert bytes to hex string
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            logger.error("Password hashing failed: {}", e.getMessage());
            throw new RuntimeException("Hashing failed", e);
        }
    }

    /**
     * Encrypt data using DES in ECB mode.
     *
     * ⚠️ VULNERABILITY 1: DES (Data Encryption Standard) uses a 56-bit key,
     * which is trivially brute-forceable with modern hardware.
     * Should use AES-256 at minimum.
     *
     * ⚠️ VULNERABILITY 2: ECB (Electronic Codebook) mode is deterministic —
     * identical plaintext blocks produce identical ciphertext blocks, leaking
     * patterns in the data. A famous example is the "ECB penguin" image.
     * Should use AES-GCM or AES-CBC with a random IV.
     *
     * ⚠️ VULNERABILITY 3: Hardcoded key (ENCRYPTION_KEY constant above).
     *
     * CWE-327: Use of a Broken or Risky Cryptographic Algorithm
     *
     * @param data the plaintext string to encrypt
     * @return Base64-encoded ciphertext
     */
    public static String encrypt(String data) {
        try {
            // ⚠️ VULNERABILITY: DES/ECB — weak cipher and mode
            DESKeySpec keySpec = new DESKeySpec(ENCRYPTION_KEY.getBytes(StandardCharsets.UTF_8));
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
            SecretKey secretKey = keyFactory.generateSecret(keySpec);

            // ⚠️ VULNERABILITY: ECB mode — no IV, deterministic output
            Cipher cipher = Cipher.getInstance("DES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);

            byte[] encrypted = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            logger.error("Encryption failed: {}", e.getMessage());
            throw new RuntimeException("Encryption failed", e);
        }
    }

    /**
     * Decrypt data that was encrypted with {@link #encrypt(String)}.
     *
     * ⚠️ VULNERABILITY: Same issues as encrypt() — DES/ECB with hardcoded key.
     *
     * @param encryptedData Base64-encoded ciphertext
     * @return decrypted plaintext string
     */
    public static String decrypt(String encryptedData) {
        try {
            // ⚠️ VULNERABILITY: DES/ECB — weak cipher and mode
            DESKeySpec keySpec = new DESKeySpec(ENCRYPTION_KEY.getBytes(StandardCharsets.UTF_8));
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
            SecretKey secretKey = keyFactory.generateSecret(keySpec);

            Cipher cipher = Cipher.getInstance("DES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey);

            byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(encryptedData));
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            logger.error("Decryption failed: {}", e.getMessage());
            throw new RuntimeException("Decryption failed", e);
        }
    }

    /**
     * Generate a simple checksum for data integrity verification.
     *
     * ⚠️ VULNERABILITY: SHA-1 is deprecated for security use (collision attacks
     * demonstrated since 2017). Should use SHA-256 or SHA-3.
     *
     * @param data the data to checksum
     * @return SHA-1 hex digest
     */
    public static String checksum(String data) {
        try {
            // ⚠️ VULNERABILITY: SHA-1 — deprecated for security use
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] hashBytes = md.digest(data.getBytes(StandardCharsets.UTF_8));

            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            logger.error("Checksum failed: {}", e.getMessage());
            throw new RuntimeException("Checksum failed", e);
        }
    }
}
