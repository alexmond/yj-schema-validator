package org.alexmond.yaml.validator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class YamlSchemaValidatorRunner implements ApplicationRunner {

    private final YamlSchemaValidator  yamlSchemaValidator = new YamlSchemaValidator();

    @Override
    public void run(ApplicationArguments args) {
        // Parse command-line arguments
        String yamlPath = args.getOptionValues("yaml").get(0);
        String schemaPath = args.getOptionValues("schema").get(0);

//        if (yamlPath == null || schemaPath == null) {
        if (yamlPath == null) {
            log.error("Usage: java -jar app.jar --yaml=<yaml_file> --schema=<schema_file>");
            throw new RuntimeException("Missing required arguments");
        }

        try {
            yamlSchemaValidator.validate(yamlPath, schemaPath);
        } catch (RuntimeException e) {
            log.error("Runtime error during validation", e);
        }
    }
}