package org.alexmond.yaml.validator.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Utility class for comparing XML, JSON, and SARIF files.
 * Handles Jackson 3 attribute/property ordering differences.
 */
public class XmlCompareUtil {

    /**
     * Compares two files with normalization for XML/JSON attribute ordering.
     *
     * @param path1 Path to the first file
     * @param path2 Path to the second file
     * @return true if files are identical after normalization, false otherwise
     */
    public static boolean compareFiles(String path1, String path2) {
        try {
            String content1 = Files.readString(Paths.get(path1));
            String content2 = Files.readString(Paths.get(path2));

            // Remove timestamps before comparison in sarif files
            content1 = content1.replaceAll("\"startTimeUtc\"\\s*:\\s*\"[^\"]*\"", "\"startTimeUtc\":\"\"");
            content1 = content1.replaceAll("\"endTimeUtc\"\\s*:\\s*\"[^\"]*\"", "\"endTimeUtc\":\"\"");
            content2 = content2.replaceAll("\"startTimeUtc\"\\s*:\\s*\"[^\"]*\"", "\"startTimeUtc\":\"\"");
            content2 = content2.replaceAll("\"endTimeUtc\"\\s*:\\s*\"[^\"]*\"", "\"endTimeUtc\":\"\"");

            // Normalize XML/JSON attribute ordering for Jackson 3 compatibility
            if (path1.endsWith(".xml") || path1.endsWith(".json") || path1.endsWith(".sarif")) {
                content1 = normalizeStructuredContent(content1);
                content2 = normalizeStructuredContent(content2);
            }

            boolean matches = content1.equals(content2);
            if (!matches) {
                System.out.println("Files differ: " + path1 + " vs " + path2);
                String[] lines1 = content1.split("\n");
                String[] lines2 = content2.split("\n");
                int maxLines = Math.max(lines1.length, lines2.length);
                for (int i = 0; i < Math.min(10, maxLines); i++) {
                    String line1 = i < lines1.length ? lines1[i] : "";
                    String line2 = i < lines2.length ? lines2[i] : "";
                    if (!line1.equals(line2)) {
                        System.out.println("Line " + (i + 1) + " differs:");
                        System.out.println("  Actual:   " + line1);
                        System.out.println("  Expected: " + line2);
                    }
                }
            }
            return matches;
        } catch (IOException e) {
            System.err.println("Error comparing files: " + e.getMessage());
            return false;
        }
    }

    /**
     * Compares file content with a string.
     *
     * @param fileString The string to compare
     * @param fileName   Path to the file
     * @return true if content matches after normalization, false otherwise
     */
    public static boolean compareFileToString(String fileString, String fileName) {
        try {
            String fileContent = Files.readString(Path.of(fileName));
            String[] actualLines = fileString.trim().split("\n");
            String[] expectedLines = fileContent.trim().split("\n");

            boolean matches = true;
            int maxLines = Math.max(actualLines.length, expectedLines.length);

            for (int i = 0; i < maxLines; i++) {
                String actualLine = i < actualLines.length ? actualLines[i] : "";
                String expectedLine = i < expectedLines.length ? expectedLines[i] : "";

                if (!normalizeXmlLine(actualLine).equals(normalizeXmlLine(expectedLine))) {
                    matches = false;
                    System.out.println("Line " + (i + 1) + " differs:");
                    System.out.println("  Expected: " + expectedLine);
                    System.out.println("  Actual:   " + actualLine);
                }
            }

            return matches;
        } catch (IOException e) {
            throw new RuntimeException("Failed to read file: " + fileName, e);
        }
    }

    /**
     * Normalizes XML attributes and JSON properties to handle different ordering from Jackson 3.
     */
    private static String normalizeStructuredContent(String content) {
        String[] lines = content.split("\n");
        StringBuilder normalized = new StringBuilder();

        for (String line : lines) {
            normalized.append(normalizeXmlLine(line.trim())).append("\n");
        }

        return normalized.toString();
    }

    /**
     * Normalizes XML line by sorting attributes to handle different serialization orders.
     */
    public static String normalizeXmlLine(String line) {
        String trimmed = line.trim();

        // Check if line contains XML element with attributes
        if (!trimmed.startsWith("<") || !trimmed.contains("=")) {
            return trimmed;
        }

        // Extract tag name
        int firstSpace = trimmed.indexOf(' ');
        int firstClose = trimmed.indexOf('>');

        if (firstSpace == -1 || firstClose == -1 || firstSpace > firstClose) {
            return trimmed;
        }

        String tagStart = trimmed.substring(0, firstSpace);
        String tagEnd = trimmed.substring(firstClose);
        String attributes = trimmed.substring(firstSpace + 1, firstClose).trim();

        // Sort attributes alphabetically
        String[] attrs = attributes.split("\\s+(?=\\w+=)");
        java.util.Arrays.sort(attrs);

        return tagStart + " " + String.join(" ", attrs) + tagEnd;
    }
}
