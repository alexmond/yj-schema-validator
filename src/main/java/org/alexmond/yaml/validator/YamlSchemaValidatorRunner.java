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
    private final YamlSchemaValidator  yamlSchemaValidator;

    @Override
    public void run(ApplicationArguments args) {
        // Parse command-line arguments
//        if (yamlPath == null || schemaPath == null) {
        log.warn(args.toString());
//        if (config.getFile() == null) {
//            log.error("Usage: java -jar app.jar --file=<yaml/json_file> --schema=<schema_file>");
//            throw new RuntimeException("Missing required arguments");
//        }
        if(!args.getNonOptionArgs().isEmpty())
            args.getNonOptionArgs().forEach(file -> {
                try {
                    yamlSchemaValidator.validate(file, config.getSchema());
                } catch (RuntimeException e) {
                    log.error("Runtime error during validation", e);
                }
            });
    }
}