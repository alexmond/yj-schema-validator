package org.alexmond.yaml.validator.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

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
     * Path to the YAML file that needs to be validated.
     */
    private String file;

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
    private Boolean schemaPathOverride = false;

    private ReportType reportType = ReportType.TEXT;

    private boolean isIgnoreSslErrors = true;
}
