package org.alexmond.yaml.validator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.output.OutputUnit;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.converter.ConvertWith;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Slf4j
public class YamlSchemaValidatorTest {

    @Autowired
    private YamlSchemaValidator yamlSchemaValidator;

    @ParameterizedTest(name = "Validate {0} against schema {1}")
    @CsvSource({
            "src/test/resources/valid.yaml,src/test/resources/sample-schema.json",
            "src/test/resources/valid.yaml,src/test/resources/sample-schema.yaml",
            "src/test/resources/valid.yaml,https://alexmond.github.io/spring-boot-config-json-schema-starter/current/_attachments/boot-generic-config.json",
            "src/test/resources/valid.yaml,",
            "src/test/resources/valid.json,",
            "src/test/resources/remoteValid.yaml,"
    })
    void shouldValidateYamlSuccessfully(String yamlPath, String schemaPath) {
        Map<String, OutputUnit> outputUnitMap;
        outputUnitMap = yamlSchemaValidator.validate(yamlPath, schemaPath);
        outputUnitMap.values().stream().findFirst().ifPresent(outputUnit -> assertTrue(outputUnit::isValid, "YAML validation failed: " + outputUnit));
    }



    @ParameterizedTest
    @CsvSource({
            "src/test/resources/validNoSchema.yaml,src/test/resources/missing-schema.yaml,NoSuchFileException",
            "src/test/resources/validNoSchema.yaml,https://alexmond.github.io/spring-boot-config-json-schema-starter/current/_attachments/missing.json,HTTP request failed with status code 404",
            "src/test/resources/missingfile.yaml,,NoSuchFileException",
            "src/test/resources/empty.yaml,,No schema found in YAML file or provided as parameter",
            "src/test/resources/badformat.yaml,,MarkedYAMLException",
    })
    void testYamlValidationError(String yamlPath, String schemaPath, String error) {
        Map<String, OutputUnit> outputUnitMap = yamlSchemaValidator.validate(yamlPath, schemaPath);
        log.error("Yaml validation error: {}", outputUnitMap.toString());
        OutputUnit outputUnit = outputUnitMap.values().iterator().next();
        assertFalse(outputUnit.isValid());
        log.error("Yaml validation error: {}", outputUnit.getErrors());
        assertTrue(outputUnitMap.containsKey(yamlPath) && ((String) outputUnit.getErrors().get("error")).contains(error));
    }

    @ParameterizedTest
    @CsvSource(delimiter = ':', value = {
            "src/test/resources/invalid.yaml::integer found, boolean expected",
    })
    void testYamlValidationInvalid(String yamlPath, String schemaPath, String error) {
        Map<String, OutputUnit> outputUnitMap = yamlSchemaValidator.validate(yamlPath, schemaPath);
        OutputUnit outputUnit = outputUnitMap.values().iterator().next();
        assertFalse(outputUnit.isValid());
        assertNotNull(outputUnit.getDetails());
    }

    static Stream<Arguments> multiDocProvider() {
        return Stream.of(
                Arguments.of(
                        "src/test/resources/multi3valid.yaml", 3, new boolean[]{true, true, true}),
                Arguments.of(
                        "src/test/resources/multi3invalid.yaml", 3, new boolean[]{true, false, true})
        );
    }

    @ParameterizedTest
    @MethodSource("multiDocProvider")
    void testMultiDocumentYaml(String yamlPath,int numberOfDocuments,boolean[] valid) {
        Map<String, OutputUnit> sortedOutputUnitMap = new TreeMap<>();
        sortedOutputUnitMap.putAll(yamlSchemaValidator.validate(yamlPath, null));
        assertEquals(sortedOutputUnitMap.size(), numberOfDocuments);
        int index = 0;
        for (Map.Entry<String, OutputUnit> entry : sortedOutputUnitMap.entrySet()) {
            OutputUnit outputUnit = entry.getValue();
            assertEquals(outputUnit.isValid(), valid[index]);
            index++;
        }
    }

    @ParameterizedTest
    @CsvSource({
            "'{\"$schema\": \"https://json-schema.org/draft-04/schema#\"}', V4",
            "'{\"$schema\": \"https://json-schema.org/draft-06/schema#\"}', V6",
            "'{\"$schema\": \"https://json-schema.org/draft-07/schema#\"}', V7",
            "'{\"$schema\": \"https://json-schema.org/draft/2019-09/schema\"}', V201909",
            "'{\"$schema\": \"https://json-schema.org/draft/2020-12/schema\"}', V202012",
            "'{}', V202012",
            "'{\"$schema\": \"https://unsupported-schema.org\"}', V202012"
    })
    void testGetSchemaVersion(String schemaJson, String expectedVersion) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode schemaNode = objectMapper.readTree(schemaJson);

        SpecVersion.VersionFlag version = yamlSchemaValidator.getSchemaVersion(schemaNode);

        assertEquals(SpecVersion.VersionFlag.valueOf(expectedVersion), version);
    }
}