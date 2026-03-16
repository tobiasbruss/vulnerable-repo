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

    private static final String ENCRYPTION_KEY = "AES_KEY_16BYTES!";  // 16 bytes for AES-128

    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

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
     * Encrypt data using AES-GCM.
     *
     * @param data the plaintext string to encrypt
     * @return Base64-encoded IV + ciphertext
     */
    public static String encrypt(String data) {
        try {
            SecretKey secretKey = new SecretKeySpec(
                    ENCRYPTION_KEY.getBytes(StandardCharsets.UTF_8), "AES");

            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);
            GCMParameterSpec paramSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, paramSpec);

            byte[] encrypted = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));

            // Prepend IV to ciphertext so decrypt can extract it
            byte[] ivAndCiphertext = new byte[GCM_IV_LENGTH + encrypted.length];
            System.arraycopy(iv, 0, ivAndCiphertext, 0, GCM_IV_LENGTH);
            System.arraycopy(encrypted, 0, ivAndCiphertext, GCM_IV_LENGTH, encrypted.length);

            return Base64.getEncoder().encodeToString(ivAndCiphertext);
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
            byte[] ivAndCiphertext = Base64.getDecoder().decode(encryptedData);
            byte[] iv = Arrays.copyOfRange(ivAndCiphertext, 0, GCM_IV_LENGTH);
            byte[] ciphertext = Arrays.copyOfRange(ivAndCiphertext, GCM_IV_LENGTH, ivAndCiphertext.length);

            SecretKey secretKey = new SecretKeySpec(
                    ENCRYPTION_KEY.getBytes(StandardCharsets.UTF_8), "AES");
            GCMParameterSpec paramSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey, paramSpec);

            byte[] decrypted = cipher.doFinal(ciphertext);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            logger.error("Decryption failed");
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
