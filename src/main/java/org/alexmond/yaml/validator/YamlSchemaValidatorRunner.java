package org.alexmond.yaml.validator;

import com.networknt.schema.output.OutputUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.alexmond.yaml.validator.config.ReportType;
import org.alexmond.yaml.validator.config.YamlSchemaValidatorConfig;
import org.alexmond.yaml.validator.output.FilesOutput;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class YamlSchemaValidatorRunner implements ApplicationRunner {

    private final YamlSchemaValidatorConfig config;
    private final YamlSchemaValidator yamlSchemaValidator;
    private final Environment environment;

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

        if (config.getReportType() == ReportType.TEXT){
            System.out.println(filesOutput.toColoredString(config.isColorOutput()));
        }else{
            System.out.println(filesOutput.toJsonString());
        }

        if (filesOutput.isValid()) {
            System.exit(0);
        } else {
            System.exit(1);
        }

    }

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

    private String validateConfig(ApplicationArguments args) {
        if (args.getNonOptionArgs().isEmpty())
            return "At least one YAML/JSON file must be provided as a non-option argument";
        if (config.isSchemaPathOverride() && config.getSchema() == null) {
            return "Schema path must be provided when schemaPathOverride is enabled";
        }
        return null;
    }
}