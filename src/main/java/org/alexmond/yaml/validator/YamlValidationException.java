package org.alexmond.yaml.validator;

import lombok.Getter;

import java.util.List;

@Getter
public class YamlValidationException extends RuntimeException {
    private final String yamlPath;
    private final String schemaPath;

    public YamlValidationException(String message, String yamlPath, String schemaPath) {
        super(message);
        this.yamlPath = yamlPath;
        this.schemaPath = schemaPath;
    }

    public YamlValidationException(Throwable cause, String yamlPath, String schemaPath) {
        super(cause);
        this.yamlPath = yamlPath;
        this.schemaPath = schemaPath;
    }
}
