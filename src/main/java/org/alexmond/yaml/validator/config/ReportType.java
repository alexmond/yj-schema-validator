package org.alexmond.yaml.validator.config;

/**
 * Represents different types of output report formats for validation results.
 */
public enum ReportType {
    /**
     * Plain text format report
     */
    TEXT,
    /**
     * YAML format report
     */
    YAML,
    /**
     * JSON format report
     */
    JSON,
    /**
     * JUnit XML format report
     */
    JUNIT,
    /**
     * Sarif format report
     */
    SARIF
}
