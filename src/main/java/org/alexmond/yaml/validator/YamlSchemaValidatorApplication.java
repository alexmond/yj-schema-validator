package org.alexmond.yaml.validator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot application for validating YAML and JSON files against JSON schema definitions.
 * This command-line utility ensures configuration files comply with predefined schemas.
 */
@SpringBootApplication
public class YamlSchemaValidatorApplication {
    /**
     * The main entry point of the application.
     * Starts the Spring Boot application context and initializes the validator.
     *
     * @param args command line arguments including file paths and options:
     *             --schema: Path to the JSON schema file
     *             --schemaPathOverride: Override schema path requirement
     *             --help: Show help message
     */
    public static void main(String[] args) {
        System.setProperty("spring.config.name", "yj-schema-validator");
        SpringApplication.run(YamlSchemaValidatorApplication.class, args);
    }
}
