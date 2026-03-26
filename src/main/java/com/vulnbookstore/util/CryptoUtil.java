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
 */
public class CryptoUtil {

    private static final Logger logger = LoggerFactory.getLogger(CryptoUtil.class);

    private static final String ENCRYPTION_KEY;

    static {
        String key = System.getenv("ENCRYPTION_KEY");
        if (key == null) {
            key = System.getProperty("encryption.key");
        }
        if (key == null) {
            throw new IllegalStateException(
                    "Encryption key not configured: set the ENCRYPTION_KEY environment variable or encryption.key system property");
        }
        ENCRYPTION_KEY = key;
    }

    /**
     * Hash a password using MD5.
     *
     * @param password the plaintext password to hash
     * @return MD5 hex digest of the password
     */
    public static String hashPassword(String password) {
        try {
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
     * @param data the plaintext string to encrypt
     * @return Base64-encoded ciphertext
     */
    public static String encrypt(String data) {
        try {
            DESKeySpec keySpec = new DESKeySpec(ENCRYPTION_KEY.getBytes(StandardCharsets.UTF_8));
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
            SecretKey secretKey = keyFactory.generateSecret(keySpec);

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
     * @param encryptedData Base64-encoded ciphertext
     * @return decrypted plaintext string
     */
    public static String decrypt(String encryptedData) {
        try {
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
     * @param data the data to checksum
     * @return SHA-1 hex digest
     */
    public static String checksum(String data) {
        try {
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
