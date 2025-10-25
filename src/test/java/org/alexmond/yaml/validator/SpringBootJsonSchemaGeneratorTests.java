package org.alexmond.yaml.validator;

import lombok.extern.slf4j.Slf4j;
import org.alexmond.config.json.schema.service.JsonSchemaService;
import org.alexmond.config.json.schema.service.MissingTypeCollector;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

@ActiveProfiles("test")
@SpringBootTest
@Slf4j
class
SpringBootJsonSchemaGeneratorTests {

    @Autowired
    private JsonSchemaService jsonSchemaService;

    @Test
    void generateJsonSchema() {

        var jsonConfigSchemaJson = jsonSchemaService.generateFullSchemaJson();
        var jsonConfigSchemaYaml = jsonSchemaService.generateFullSchemaYaml();
        try {
            log.info("Writing json schema");
            Files.writeString(Paths.get("docs//modules/ROOT/attachments/yj-schema-validator-config.json"), jsonConfigSchemaJson, StandardCharsets.UTF_8);
            log.info("Writing yaml schema");
            Files.writeString(Paths.get("docs/modules/ROOT/attachments/yj-schema-validator-config.yaml"), jsonConfigSchemaYaml, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
