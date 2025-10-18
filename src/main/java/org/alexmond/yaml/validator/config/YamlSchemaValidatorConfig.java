package org.alexmond.yaml.validator.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Configuration properties for YAML schema validation.
 * Properties are bound to the 'validator' prefix in application properties.
 */
@Configuration
@Data
@ConfigurationProperties("validator")
public class YamlSchemaValidatorConfig {
    /**
     * Path to the JSON schema in either JSON or YAML file format used for validation.
     * Can be either a local file path or URL.
     */
    private String schema;

    /**
     * HTTP timeout in seconds for fetching remote schemas.
     * Default: 10 sec
     */
    private Duration httpTimeout = Duration.ofSeconds(10);

    /**
     * Flag to override a schema path specified in the YAML / JSON file.
     * If true, uses schemaPath property instead of $schema from YAML.
     */
    private boolean schemaPathOverride = false;

    /**
     * Type of validation report to generate.
     * Default: TEXT
     */
    private ReportType reportType = ReportType.TEXT;

    /**
     * Flag to control whether SSL certificate validation errors should be ignored.
     * When true, SSL certificate validation errors will be ignored during HTTP requests.
     * Default: true
     */
    private boolean isIgnoreSslErrors = true;

    /**
     * Flag to control whether to use colored output in the console.
     * When true, validation results will be displayed with ANSI color codes.
     * Default: true
     */
    private boolean colorOutput = true;
}
