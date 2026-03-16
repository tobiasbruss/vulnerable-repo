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
     * @param filename the name of the file to read (relative to BASE_DIR)
     * @return file contents as a String
     */
    public static String readFile(String filename) {
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
     * @param filename the name of the report file (relative to BASE_DIR)
     * @param content  the content to write
     */
    public static void writeReport(String filename, String content) {
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
     */
    public static boolean fileExists(String filename) {
        String filePath = BASE_DIR + filename;
        return Files.exists(Paths.get(filePath));
    }
}
