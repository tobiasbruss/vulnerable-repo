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
import java.util.Arrays;
import java.util.Base64;

/**
 * Utility class for cryptographic operations.
 * Provides password hashing and data encryption for the bookstore application.
 */
public class CryptoUtil {

    private static final Logger logger = LoggerFactory.getLogger(CryptoUtil.class);

    // Key loaded from environment variable; padded/truncated to 32 bytes for AES-256.
    private static final byte[] AES_KEY = Arrays.copyOf(
            System.getenv().getOrDefault("ENCRYPTION_KEY", "changeme-encryption-key-placeholder")
                    .getBytes(StandardCharsets.UTF_8),
            32);

    private static final int GCM_IV_LENGTH  = 12;
    private static final int GCM_TAG_LENGTH = 128;

    /**
     * Hash a password using SHA-256.
     * For production use, prefer BCrypt/Argon2 with a random salt.
     *
     * @param password the plaintext password to hash
     * @return SHA-256 hex digest of the password
     */
    public static String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = md.digest(password.getBytes(StandardCharsets.UTF_8));

            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            logger.error("Password hashing failed");
            throw new RuntimeException("Hashing failed", e);
        }
    }

    /**
     * Encrypt data using AES-256/GCM with a random IV.
     * The IV is prepended to the ciphertext and Base64-encoded together.
     *
     * @param data the plaintext string to encrypt
     * @return Base64-encoded IV + ciphertext
     */
    public static String encrypt(String data) {
        try {
            SecretKey secretKey = new SecretKeySpec(AES_KEY, "AES");

            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

            byte[] encrypted = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));

            // Prepend IV so decrypt() can recover it
            byte[] combined = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            logger.error("Encryption failed");
            throw new RuntimeException("Encryption failed", e);
        }
    }

    /**
     * Decrypt data that was encrypted with {@link #encrypt(String)}.
     *
     * @param encryptedData Base64-encoded IV + ciphertext
     * @return decrypted plaintext string
     */
    public static String decrypt(String encryptedData) {
        try {
            byte[] combined = Base64.getDecoder().decode(encryptedData);

            byte[] iv = Arrays.copyOfRange(combined, 0, GCM_IV_LENGTH);
            byte[] ciphertext = Arrays.copyOfRange(combined, GCM_IV_LENGTH, combined.length);

            SecretKey secretKey = new SecretKeySpec(AES_KEY, "AES");
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);

            byte[] decrypted = cipher.doFinal(ciphertext);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            logger.error("Decryption failed");
            throw new RuntimeException("Decryption failed", e);
        }
    }

    /**
     * Generate a SHA-256 checksum for data integrity verification.
     *
     * @param data the data to checksum
     * @return SHA-256 hex digest
     */
    public static String checksum(String data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = md.digest(data.getBytes(StandardCharsets.UTF_8));

            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            logger.error("Checksum failed");
            throw new RuntimeException("Checksum failed", e);
        }
    }
}
