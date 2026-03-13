package com.vulnbookstore.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
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

    // NOTE: The encryption key should be loaded from a secure environment variable or KMS.
    // Hardcoded keys are flagged by secret scanning tools.
    private static final String ENCRYPTION_KEY = "DES_KEY_";

    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

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
     * Encrypt data using AES-256 in GCM mode with a random IV.
     *
     * @param data the plaintext string to encrypt
     * @return Base64-encoded IV + ciphertext
     */
    public static String encrypt(String data) {
        try {
            // Derive a 256-bit AES key from ENCRYPTION_KEY using SHA-256
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            byte[] keyBytes = sha.digest(ENCRYPTION_KEY.getBytes(StandardCharsets.UTF_8));
            SecretKey secretKey = new SecretKeySpec(keyBytes, "AES");

            // Generate a random 12-byte IV for GCM
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec);

            byte[] encrypted = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));

            // Prepend IV to ciphertext for use during decryption
            byte[] ivAndEncrypted = new byte[GCM_IV_LENGTH + encrypted.length];
            System.arraycopy(iv, 0, ivAndEncrypted, 0, GCM_IV_LENGTH);
            System.arraycopy(encrypted, 0, ivAndEncrypted, GCM_IV_LENGTH, encrypted.length);

            return Base64.getEncoder().encodeToString(ivAndEncrypted);
        } catch (Exception e) {
            logger.error("Encryption failed: {}", e.getMessage());
            throw new RuntimeException("Encryption failed", e);
        }
    }

    /**
     * Decrypt data that was encrypted with {@link #encrypt(String)}.
     * Expects Base64-encoded IV + ciphertext as produced by encrypt().
     *
     * @param encryptedData Base64-encoded IV + ciphertext
     * @return decrypted plaintext string
     */
    public static String decrypt(String encryptedData) {
        try {
            byte[] ivAndEncrypted = Base64.getDecoder().decode(encryptedData);

            // Extract IV and ciphertext
            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] encrypted = new byte[ivAndEncrypted.length - GCM_IV_LENGTH];
            System.arraycopy(ivAndEncrypted, 0, iv, 0, GCM_IV_LENGTH);
            System.arraycopy(ivAndEncrypted, GCM_IV_LENGTH, encrypted, 0, encrypted.length);

            // Derive the same AES key
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            byte[] keyBytes = sha.digest(ENCRYPTION_KEY.getBytes(StandardCharsets.UTF_8));
            SecretKey secretKey = new SecretKeySpec(keyBytes, "AES");

            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec);

            byte[] decrypted = cipher.doFinal(encrypted);
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
