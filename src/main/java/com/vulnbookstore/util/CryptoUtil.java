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

    private static final int AES_KEY_SIZE = 32;   // 256-bit AES key
    private static final int GCM_IV_SIZE  = 12;   // 96-bit IV recommended for GCM
    private static final int GCM_TAG_BITS = 128;  // 128-bit authentication tag

    /**
     * Returns the 256-bit AES key bytes derived from the APP_ENCRYPTION_KEY
     * environment variable. Falls back to a development default — never use
     * the fallback in production.
     */
    private static byte[] getKeyBytes() {
        String keyStr = System.getenv().getOrDefault(
                "APP_ENCRYPTION_KEY", "DefaultDev32ByteKeyNotForProd!!");
        return Arrays.copyOf(keyStr.getBytes(StandardCharsets.UTF_8), AES_KEY_SIZE);
    }

    /**
     * Hash a value using SHA-256.
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
            logger.error("Password hashing failed: {}", e.getMessage());
            throw new RuntimeException("Hashing failed", e);
        }
    }

    /**
     * Encrypt data using AES-256-GCM with a random IV.
     * The output is Base64(iv || ciphertext) so {@link #decrypt(String)} can
     * recover the IV automatically.
     *
     * @param data the plaintext string to encrypt
     * @return Base64-encoded IV + ciphertext
     */
    public static String encrypt(String data) {
        try {
            byte[] iv = new byte[GCM_IV_SIZE];
            new SecureRandom().nextBytes(iv);

            SecretKeySpec keySpec = new SecretKeySpec(getKeyBytes(), "AES");
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, new GCMParameterSpec(GCM_TAG_BITS, iv));

            byte[] encrypted = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));

            // Prepend IV to ciphertext so decrypt() can recover it
            byte[] combined = new byte[GCM_IV_SIZE + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, GCM_IV_SIZE);
            System.arraycopy(encrypted, 0, combined, GCM_IV_SIZE, encrypted.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            logger.error("Encryption failed: {}", e.getMessage());
            throw new RuntimeException("Encryption failed", e);
        }
    }

    /**
     * Decrypt data that was encrypted with {@link #encrypt(String)}.
     *
     * @param encryptedData Base64-encoded IV + ciphertext produced by encrypt()
     * @return decrypted plaintext string
     */
    public static String decrypt(String encryptedData) {
        try {
            byte[] combined = Base64.getDecoder().decode(encryptedData);
            byte[] iv = Arrays.copyOfRange(combined, 0, GCM_IV_SIZE);
            byte[] ciphertext = Arrays.copyOfRange(combined, GCM_IV_SIZE, combined.length);

            SecretKeySpec keySpec = new SecretKeySpec(getKeyBytes(), "AES");
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, keySpec, new GCMParameterSpec(GCM_TAG_BITS, iv));

            byte[] decrypted = cipher.doFinal(ciphertext);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            logger.error("Decryption failed: {}", e.getMessage());
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
            logger.error("Checksum failed: {}", e.getMessage());
            throw new RuntimeException("Checksum failed", e);
        }
    }
}
