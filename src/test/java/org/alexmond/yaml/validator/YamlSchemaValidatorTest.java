package org.alexmond.yaml.validator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.SpecVersion;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.PropertySourcesPropertyResolver;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import java.io.FileInputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Slf4j
public class YamlSchemaValidatorTest {

    @Autowired
    private YamlSchemaValidator yamlSchemaValidator;


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
//    public static final String OPENAPI_3_0 = "https://spec.openapis.org/oas/3.0/dialect";
//
//    /**
//     * OpenAPI 3.1
//     */
//    public static final String OPENAPI_3_1 = "https://spec.openapis.org/oas/3.1/dialect/base";
    
    @ParameterizedTest
    @CsvSource({
            "'{\"$schema\": \"https://json-schema.org/draft-04/schema#\"}', V4",
            "'{\"$schema\": \"https://json-schema.org/draft-06/schema#\"}', V6",
            "'{\"$schema\": \"https://json-schema.org/draft-07/schema#\"}', V7",
            "'{\"$schema\": \"https://json-schema.org/draft/2019-09/schema\"}', V201909",
            "'{\"$schema\": \"https://json-schema.org/draft/2020-12/schema\"}', V202012",
//            "'{\"$schema\": \"https://spec.openapis.org/oas/3.0/dialect\"}', OPENAPI_3_0",
//            "'{\"$schema\": \"https://spec.openapis.org/oas/3.1/dialect/base\"}', OPENAPI_3_1",
            "'{}', V202012", // Default case when $schema is missing
            "'{\"$schema\": \"https://unsupported-schema.org\"}', V202012" // Unsupported schema defaults to V202012
    })
    void testGetSchemaVersion(String schemaJson, String expectedVersion) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode schemaNode = objectMapper.readTree(schemaJson);

        SpecVersion.VersionFlag version = yamlSchemaValidator.getSchemaVersion(schemaNode);

        assertEquals(SpecVersion.VersionFlag.valueOf(expectedVersion), version);
    }

}