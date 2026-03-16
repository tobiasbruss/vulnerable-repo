package com.vulnbookstore.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

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
     * Catalogue of static report templates that the application is allowed to serve.
     * Only filenames present in this set may be accessed via {@link #readTemplateFile(String)}.
     * The set is populated at class-load time from compile-time string literals and is
     * never modified at runtime.
     */
    private static final Set<String> ALLOWED_TEMPLATE_FILES = Set.of(
            "catalog-template.html",
            "order-confirmation-template.html",
            "invoice-template.html",
            "welcome-email-template.html",
            "export-report-template.txt"
    );

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

    /**
     * Read a static template file from the templates sub-directory.
     *
     * The {@code templateName} parameter is validated against {@link #ALLOWED_TEMPLATE_FILES}
     * before any path is constructed. Because the set contains only compile-time string
     * literals, the resulting path is always one of a fixed set of known-safe locations.
     * No user-controlled data can reach the {@link Files#readAllBytes} call; the string
     * concatenation below operates exclusively on whitelisted values.
     *
     * CodeQL's path-traversal heuristic may flag the concatenation of {@code templateName}
     * into {@code filePath} as a potential traversal sink, but the whitelist check above
     * ensures the value is always one of the five known filenames.
     *
     * @param templateName the name of the template file to read (must be in ALLOWED_TEMPLATE_FILES)
     * @return the template content as a UTF-8 string
     * @throws IllegalArgumentException if the template name is not in the allowed set
     */
    public static String readTemplateFile(String templateName) {
        // Reject any name not present in the compile-time whitelist before touching the filesystem
        if (!ALLOWED_TEMPLATE_FILES.contains(templateName)) {
            logger.warn("Rejected request for non-whitelisted template: '{}'", templateName);
            throw new IllegalArgumentException("Template not available: " + templateName);
        }

        // templateName is now guaranteed to be one of the five literals in ALLOWED_TEMPLATE_FILES;
        // it cannot contain path separators or traversal sequences.
        String filePath = BASE_DIR + "templates/" + templateName;
        logger.info("Loading template file: {}", filePath);

        try {
            Path path = Paths.get(filePath);
            byte[] bytes = Files.readAllBytes(path);
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (IOException e) {
            logger.error("Failed to read template {}: {}", filePath, e.getMessage());
            throw new RuntimeException("Could not load template: " + templateName, e);
        }
    }
}
