package com.vulnbookstore.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
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

    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    /**
     * Derive a 32-byte AES-256 key from the ENCRYPTION_KEY environment variable.
     * The environment variable must be set before the application starts.
     */
    private static byte[] getKey() {
        String key = System.getenv("ENCRYPTION_KEY");
        if (key == null || key.isEmpty()) {
            throw new IllegalStateException("ENCRYPTION_KEY environment variable is not set");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(key.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new RuntimeException("Key derivation failed", e);
        }
    }

    /**
     * Hash a password using SHA-256.
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
            logger.error("Password hashing failed", e);
            throw new RuntimeException("Hashing failed", e);
        }
    }

    /**
     * Encrypt data using AES-256 in GCM mode.
     * The IV is randomly generated and prepended to the ciphertext.
     *
     * @param data the plaintext string to encrypt
     * @return Base64-encoded IV + ciphertext
     */
    public static String encrypt(String data) {
        try {
            SecretKeySpec secretKey = new SecretKeySpec(getKey(), "AES");

            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));

            byte[] encrypted = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));

            // Prepend IV to ciphertext so decrypt() can extract it
            byte[] combined = new byte[GCM_IV_LENGTH + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, GCM_IV_LENGTH);
            System.arraycopy(encrypted, 0, combined, GCM_IV_LENGTH, encrypted.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            logger.error("Encryption failed", e);
            throw new RuntimeException("Encryption failed", e);
        }
    }

    /**
     * Decrypt data that was encrypted with {@link #encrypt(String)}.
     * Expects Base64-encoded IV (12 bytes) followed by AES-256/GCM ciphertext.
     *
     * @param encryptedData Base64-encoded IV + ciphertext
     * @return decrypted plaintext string
     */
    public static String decrypt(String encryptedData) {
        try {
            SecretKeySpec secretKey = new SecretKeySpec(getKey(), "AES");

            byte[] combined = Base64.getDecoder().decode(encryptedData);
            byte[] iv = Arrays.copyOfRange(combined, 0, GCM_IV_LENGTH);
            byte[] ciphertext = Arrays.copyOfRange(combined, GCM_IV_LENGTH, combined.length);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));

            byte[] decrypted = cipher.doFinal(ciphertext);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            logger.error("Decryption failed", e);
            throw new RuntimeException("Decryption failed", e);
        }
    }

    /**
     * Generate a checksum for data integrity verification using SHA-256.
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
            logger.error("Checksum failed", e);
            throw new RuntimeException("Checksum failed", e);
        }
    }
}
