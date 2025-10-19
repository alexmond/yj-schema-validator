package org.alexmond.yaml.validator;

import com.networknt.schema.output.OutputUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.alexmond.yaml.validator.config.YamlSchemaValidatorConfig;
import org.alexmond.yaml.validator.output.FilesOutput;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Spring Boot Application Runner that handles YAML/JSON schema validation.
 * This runner processes command line arguments, validates input files against JSON schemas,
 * and outputs validation results in various formats (JSON, YAML, JUnit, or colored text).
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class YamlSchemaValidatorRunner implements ApplicationRunner {

    private final YamlSchemaValidatorConfig config;
    private final YamlSchemaValidator yamlSchemaValidator;
    private final Environment environment;

    /**
     * Executes the validation process when the application starts.
     * Handles command line arguments, validates configuration, processes input files,
     * and outputs results in the specified format.
     *
     * @param args Application arguments containing validation options and file paths
     */
    @Override
    public void run(ApplicationArguments args) {
        if (environment.matchesProfiles("test")) {
            return;
        }

        log.warn(args.toString());
        if (args.containsOption("help")) {
            printHelp();
            System.exit(0);
            return;
        }
        String configError = validateConfig(args);
        if (configError != null) {
            System.out.println("Configuration error:" + configError);
            printHelp();
            System.exit(1);
            return;
        }
        Map<String, OutputUnit> allResultsl = new LinkedHashMap<>();
        args.getNonOptionArgs().forEach(file -> {
            try {
                var result = yamlSchemaValidator.validate(file, config.getSchema());
                allResultsl.putAll(result);
            } catch (RuntimeException e) {
                log.error("Unexpected error during validation", e);
            }
        });
        FilesOutput filesOutput = new FilesOutput(allResultsl);

        String reportContent = switch (config.getReportType()) {
            case JSON -> filesOutput.toJsonString();
            case YAML -> filesOutput.toYamlString();
            case JUNIT -> filesOutput.toJunitString();
            default -> filesOutput.toColoredString(config.isColor());
        };

        if (config.getReportFileName() != null) {
            try {
                Files.writeString(Path.of(config.getReportFileName()), reportContent);
            } catch (Exception e) {
                log.error("Failed to write report to file: {}", config.getReportFileName(), e);
            }
        } else {
            System.out.println(reportContent);
        }

        if (filesOutput.isValid()) {
            System.exit(0);
        } else {
            System.exit(1);
        }

    }

    /**
     * Displays usage instructions and available command line options.
     * Exits the application with status code 0 after printing the help message.
     */
    private void printHelp() {
        String helpText = """
                Usage: java -jar yaml-schema-validator.jar [options] <file1> <file2> ...
                
                Options:
                  --schema=<path>          Path to the JSON schema file (required unless schemaPathOverride is false)
                  --schemaPathOverride     If set, the schema path must be provided via --schema option
                  --help                   Show this help message
                """;
        System.out.println(helpText);
        System.exit(0);
    }

    /**
     * Validates the application configuration based on provided arguments.
     *
     * @param args Application arguments to validate
     * @return Error message if validation fails, null if validation succeeds
     */
    private String validateConfig(ApplicationArguments args) {
        if (args.getNonOptionArgs().isEmpty())
            return "At least one YAML/JSON file must be provided as a non-option argument";
        if (config.isSchemaOverride() && config.getSchema() == null) {
            return "Schema path must be provided when schemaPathOverride is enabled";
        }
        return null;
    }
}