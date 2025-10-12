package org.alexmond.yaml.validator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.alexmond.yaml.validator.config.YamlSchemaValidatorConfig;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class YamlSchemaValidatorRunner implements ApplicationRunner {

    private final YamlSchemaValidatorConfig config;
    private final YamlSchemaValidator yamlSchemaValidator;

    @Override
    public void run(ApplicationArguments args) {
        log.warn(args.toString());
        validateConfig(args);

        args.getNonOptionArgs().forEach(file -> {
            try {
                yamlSchemaValidator.validate(file, config.getSchema());
            } catch (RuntimeException e) {
                log.error("Unexpected error during validation", e);
            }
        });
    }

    private void validateConfig(ApplicationArguments args) {
        if (args.getNonOptionArgs().isEmpty())
            throw new IllegalArgumentException("At least one YAML/JSON file must be provided as a non-option argument");
        if (config.getSchemaPathOverride() && config.getSchema() == null) {
            throw new IllegalArgumentException("Schema path must be provided when schemaPathOverride is enabled");
        }
    }
}