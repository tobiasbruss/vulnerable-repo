package com.vulnbookstore;

import com.vulnbookstore.util.CryptoUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CryptoUtil.
 * Tests verify functional correctness of hashing and encryption operations.
 */
class CryptoUtilTest {

    // ----------------------------------------------------------------
    // hashPassword() — SHA-256
    // ----------------------------------------------------------------

    @Test
    @DisplayName("hashPassword() returns a non-null, non-empty hash")
    void hashPassword_returnsNonEmptyHash() {
        String hash = CryptoUtil.hashPassword("password123");

        assertNotNull(hash);
        assertThat(hash).isNotBlank();
    }

    @Test
    @DisplayName("hashPassword() returns a 64-character SHA-256 hex string")
    void hashPassword_returns64CharHexString() {
        String hash = CryptoUtil.hashPassword("password123");

        // SHA-256 produces a 256-bit (32-byte) hash → 64 hex characters
        assertThat(hash).hasSize(64);
        assertThat(hash).matches("[0-9a-f]{64}");
    }

    @Test
    @DisplayName("hashPassword() is deterministic — same input produces same hash")
    void hashPassword_isDeterministic() {
        String hash1 = CryptoUtil.hashPassword("mypassword");
        String hash2 = CryptoUtil.hashPassword("mypassword");

        assertEquals(hash1, hash2);
    }

    @Test
    @DisplayName("hashPassword() produces different hashes for different inputs")
    void hashPassword_producesDifferentHashesForDifferentInputs() {
        String hash1 = CryptoUtil.hashPassword("password1");
        String hash2 = CryptoUtil.hashPassword("password2");

        assertNotEquals(hash1, hash2);
    }

    @Test
    @DisplayName("hashPassword() is case-sensitive")
    void hashPassword_isCaseSensitive() {
        String hashLower = CryptoUtil.hashPassword("password");
        String hashUpper = CryptoUtil.hashPassword("PASSWORD");

        assertNotEquals(hashLower, hashUpper);
    }

    @Test
    @DisplayName("hashPassword() handles empty string input")
    void hashPassword_handlesEmptyString() {
        String hash = CryptoUtil.hashPassword("");

        // SHA-256 of empty string
        assertThat(hash).isEqualTo("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");
    }

    @Test
    @DisplayName("hashPassword() produces known SHA-256 hash for 'admin'")
    void hashPassword_producesKnownHashForAdmin() {
        String hash = CryptoUtil.hashPassword("admin");

        // Known SHA-256 hash of "admin"
        assertThat(hash).isEqualTo("8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918");
    }

    // ----------------------------------------------------------------
    // encrypt() and decrypt() — AES/GCM
    // ----------------------------------------------------------------

    @Test
    @DisplayName("encrypt() returns a non-null, non-empty Base64 string")
    void encrypt_returnsNonEmptyBase64String() {
        String encrypted = CryptoUtil.encrypt("Hello, World!");

        assertNotNull(encrypted);
        assertThat(encrypted).isNotBlank();
    }

    @Test
    @DisplayName("encrypt() produces different output than input")
    void encrypt_producesDifferentOutputThanInput() {
        String plaintext = "sensitive data";
        String encrypted = CryptoUtil.encrypt(plaintext);

        assertNotEquals(plaintext, encrypted);
    }

    @Test
    @DisplayName("encrypt() produces different ciphertext each call due to random IV (AES/GCM)")
    void encrypt_isNonDeterministic_dueToRandomIv() {
        // AES/GCM uses a random IV per encryption, so identical plaintexts
        // produce different ciphertexts — this is the secure behavior.
        String encrypted1 = CryptoUtil.encrypt("same data");
        String encrypted2 = CryptoUtil.encrypt("same data");

        assertNotEquals(encrypted1, encrypted2);
    }

    @Test
    @DisplayName("decrypt() correctly reverses encrypt() — round-trip test")
    void decrypt_correctlyReversesEncrypt() {
        String original = "Hello, VulnBookStore!";
        String encrypted = CryptoUtil.encrypt(original);
        String decrypted = CryptoUtil.decrypt(encrypted);

        assertEquals(original, decrypted);
    }

    @Test
    @DisplayName("encrypt/decrypt round-trip works for various inputs")
    void encryptDecrypt_roundTrip_variousInputs() {
        String[] testInputs = {
            "simple",
            "with spaces and punctuation!",
            "1234567890",
            "a",
            "The quick brown fox jumps over the lazy dog"
        };

        for (String input : testInputs) {
            String encrypted = CryptoUtil.encrypt(input);
            String decrypted = CryptoUtil.decrypt(encrypted);
            assertEquals(input, decrypted,
                    "Round-trip failed for input: " + input);
        }
    }

    @Test
    @DisplayName("encrypt() produces different ciphertext for different inputs")
    void encrypt_producesDifferentCiphertextForDifferentInputs() {
        String encrypted1 = CryptoUtil.encrypt("data1");
        String encrypted2 = CryptoUtil.encrypt("data2");

        assertNotEquals(encrypted1, encrypted2);
    }

    // ----------------------------------------------------------------
    // checksum() — SHA-256
    // ----------------------------------------------------------------

    @Test
    @DisplayName("checksum() returns a 64-character SHA-256 hex string")
    void checksum_returns64CharHexString() {
        String checksum = CryptoUtil.checksum("test data");

        // SHA-256 produces a 256-bit (32-byte) hash → 64 hex characters
        assertThat(checksum).hasSize(64);
        assertThat(checksum).matches("[0-9a-f]{64}");
    }

    @Test
    @DisplayName("checksum() is deterministic")
    void checksum_isDeterministic() {
        String cs1 = CryptoUtil.checksum("bookstore data");
        String cs2 = CryptoUtil.checksum("bookstore data");

        assertEquals(cs1, cs2);
    }

    @Test
    @DisplayName("checksum() produces different values for different inputs")
    void checksum_producesDifferentValuesForDifferentInputs() {
        String cs1 = CryptoUtil.checksum("data A");
        String cs2 = CryptoUtil.checksum("data B");

        assertNotEquals(cs1, cs2);
    }
}
