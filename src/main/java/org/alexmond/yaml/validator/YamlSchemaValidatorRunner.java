package org.alexmond.yaml.validator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Set;

@Component
@Slf4j
@RequiredArgsConstructor
public class YamlSchemaValidatorRunner implements ApplicationRunner {

    private final YamlSchemaValidator  yamlSchemaValidator = new YamlSchemaValidator();

    @Override
    public void run(ApplicationArguments args) throws Exception {
        // Parse command-line arguments
        String yamlPath = null;
        String schemaPath = null;

        yamlPath = args.getOptionValues("yaml").get(0);
        schemaPath = args.getOptionValues("schema").get(0);

//        if (yamlPath == null || schemaPath == null) {
        if (yamlPath == null) {
            log.error("Usage: java -jar app.jar --yaml=<yaml_file> --schema=<schema_file>");
            throw new RuntimeException("Missing required arguments");
        }

        yamlSchemaValidator.validate(yamlPath, schemaPath);
    }

}