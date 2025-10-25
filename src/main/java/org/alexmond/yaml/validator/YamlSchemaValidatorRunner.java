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
import java.util.List;
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
        FilesOutput filesOutput = Validate(args);

        if (filesOutput == null || filesOutput.isValid()) {
            System.exit(0);
        } else {
            System.exit(1);
        }

    }

    public FilesOutput Validate(ApplicationArguments args) {
        log.warn(args.toString());
        if (args.containsOption("help")) {
            printHelp();
            return null;
        }
        String configError = validateConfig(args);
        if (configError != null) {
            System.out.println("Configuration error:" + configError);
            printHelp();
            return null;
        }
        Map<String, OutputUnit> allResultsl = new LinkedHashMap<>();
        List<String> files = args.getNonOptionArgs();
        if(config.getFiles() != null && !config.getFiles().isEmpty()) {
            files = config.getFiles();
        }
        files.forEach(file -> {
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
        return filesOutput;
    }

    /**
     * Displays usage instructions and available command line options.
     * Exits the application with status code 0 after printing the help message.
     */
    private void printHelp() {
        String helpText = """
                Usage: java -jar yaml-schema-validator.jar [options] <file1> <file2> ...
                
                Options:
                  --help                               Show this help message
                  --schema=<path>                      Path to the JSON schema file (required unless schema-override is false)
                  --schema-override=<true|false>       If set, uses --schema instead of $schema from YAML/JSON
                  --report-type=<type>                 Output format: text (default), json, yaml, junit
                  --report-file-name=<name>            Write report to the given file (prints to stdout if not set)
                  --http-timeout=<dur>                 HTTP timeout for fetching remote schemas (e.g., 10s, 2m). Default: 10s
                  --ignore-ssl-errors=<true|false>     Ignore SSL certificate validation errors when fetching schemas
                  --color=<true|false>                 Use ANSI colors in text output (default: enabled)
                """;
        System.out.println(helpText);
    }

    /**
     * Validates the application configuration based on provided arguments.
     *
     * @param args Application arguments to validate
     * @return Error message if validation fails, null if validation succeeds
     */
    private String validateConfig(ApplicationArguments args) {
        if (args.getNonOptionArgs().isEmpty() && config.getFiles().isEmpty())
            return "At least one YAML/JSON file must be provided as a non-option argument";
        if (config.isSchemaOverride() && config.getSchema() == null) {
            return "Schema path must be provided when schemaPathOverride is enabled";
        }
        return null;
    }
}