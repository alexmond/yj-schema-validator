package org.alexmond.yaml.validator;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

public class YamlSchemaValidatorTest {

    private final YamlSchemaValidator yamlSchemaValidator = new YamlSchemaValidator();


    @ParameterizedTest
    @CsvSource({
            "src/test/resources/valid.yaml,src/test/resources/sample-schema.json,true",
            "src/test/resources/valid.yaml,src/test/resources/sample-schema.yaml,true",
            "src/test/resources/valid.yaml,src/test/resources/missing-schema.yaml,false",
            "src/test/resources/valid.yaml,https://alexmond.github.io/spring-boot-config-json-schema-starter/current/_attachments/boot-generic-config.json,true",
            "src/test/resources/valid.yaml,https://alexmond.github.io/spring-boot-config-json-schema-starter/current/_attachments/missing.json,false",
            "src/test/resources/valid.yaml,,true",
            "src/test/resources/invalid.yaml,,false",
            "src/test/resources/remoteValid.yaml,,true",
            "src/test/resources/missingfile.yaml,,false"
    })
    void testYamlValidation(String yamlPath, String schemaPath, boolean valid) {

        if (valid) {
            assertTrue(yamlSchemaValidator.validate(yamlPath,schemaPath));
        } else {
            assertFalse(yamlSchemaValidator.validate(yamlPath,schemaPath));
        }
    }
}