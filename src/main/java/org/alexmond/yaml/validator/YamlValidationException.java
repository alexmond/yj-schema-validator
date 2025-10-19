package org.alexmond.yaml.validator;

import lombok.Getter;

/**
 * Exception thrown when YAML validation against a JSON schema fails.
 * This exception contains information about both the YAML file and schema file paths
 * involved in the validation process.
 */
@Getter
public class YamlValidationException extends RuntimeException {
    private final String yamlPath;
    private final String schemaPath;

    /**
     * Constructs a new YamlValidationException with a specified error message and file paths.
     *
     * @param message    the detailed error message describing the validation failure
     * @param yamlPath   the path to the YAML file that failed validation
     * @param schemaPath the path to the JSON schema file used for validation
     */
    public YamlValidationException(String message, String yamlPath, String schemaPath) {
        super(message);
        this.yamlPath = yamlPath;
        this.schemaPath = schemaPath;
    }

    /**
     * Constructs a new YamlValidationException with a specified cause and file paths.
     *
     * @param cause      the underlying cause of the validation failure
     * @param yamlPath   the path to the YAML file that failed validation
     * @param schemaPath the path to the JSON schema file used for validation
     */
    public YamlValidationException(Throwable cause, String yamlPath, String schemaPath) {
        super(cause);
        this.yamlPath = yamlPath;
        this.schemaPath = schemaPath;
    }
}
