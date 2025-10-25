package org.alexmond.yaml.validator.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.List;

/**
 * Configuration properties for YAML schema validation.
 * Properties are bound to the 'validator' prefix in application properties.
 * Contains settings for schema validation, report generation, and HTTP client configuration.
 *
 * @since 1.0
 */
@Component
@Data
@ConfigurationProperties
public class YamlSchemaValidatorConfig {

    /**
     * Array of file paths to validate against the schema.
     * Can contain both YAML and JSON files.
     *
     * @since 1.0
     */
    private List<String> files;
    /**
     * Path to the JSON schema in either JSON or YAML file format used for validation.
     * Can be either a local file path or URL.
     */
    private String schema;

    /**
     * HTTP timeout in seconds for fetching remote schemas.
     *
     * @value 10 seconds
     * @since 1.0
     */
    private Duration httpTimeout = Duration.ofSeconds(10);

    /**
     * Flag to override a schema path specified in the YAML / JSON file.
     * If true, uses schemaPath property instead of $schema from YAML.
     *
     * @value false
     * @since 1.0
     */
    private boolean schemaOverride = false;

    /**
     * Type of validation report to generate.
     *
     * @value TEXT
     * @since 1.0
     */
    private ReportType reportType = ReportType.TEXT;

    /**
     * Name of the file to write the validation report to.
     * Only used when reportType is not TEXT.
     *
     * @since 1.0
     */
    private String reportFileName;

    /**
     * Flag to control whether SSL certificate validation errors should be ignored.
     * When true, SSL certificate validation errors will be ignored during HTTP requests.
     *
     * @value true
     * @since 1.0
     */
    private boolean isIgnoreSslErrors = true;

    /**
     * Flag to control whether to use colored output in the console.
     * When true, validation results will be displayed with ANSI color codes.
     *
     * @value true
     * @since 1.0
     */
    private boolean color = true;
}
