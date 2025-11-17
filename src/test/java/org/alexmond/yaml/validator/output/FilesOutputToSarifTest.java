package org.alexmond.yaml.validator.output;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.output.OutputUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class for SARIF format output generation.
 * Tests the conversion of validation results to SARIF format.
 */
class FilesOutputToSarifTest {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("toSarifString: Should generate valid SARIF JSON when all files are valid")
    void testToSarifString_AllFilesValid() throws IOException {
        // Arrange
        OutputUnit outputUnit = new OutputUnit();
        outputUnit.setValid(true);

        Map<String, OutputUnit> files = Map.of("file1.yaml", outputUnit);
        FilesOutput filesOutput = new FilesOutput(files);

        // Act
        String sarifString = filesOutput.toSarifString();

        // Assert
        JsonNode sarifJson = objectMapper.readTree(sarifString);
        assertThat(sarifJson.get("version").asText()).isEqualTo("2.1.0");
        assertThat(sarifJson.get("$schema").asText()).isEqualTo("https://json.schemastore.org/sarif-2.1.0.json");
        assertThat(sarifJson.get("runs")).isNotNull();
        assertThat(sarifJson.get("runs").isArray()).isTrue();
        
        JsonNode run = sarifJson.get("runs").get(0);
        assertThat(run.get("tool").get("driver").get("name").asText()).isEqualTo("YAML Schema Validator");
        assertThat(run.get("results").isEmpty()).isTrue();
        assertThat(run.get("invocations").get(0).get("executionSuccessful").asBoolean()).isTrue();
        assertThat(run.get("invocations").get(0).get("exitCode").asInt()).isEqualTo(0);
    }

    @Test
    @DisplayName("toSarifString: Should generate SARIF JSON with results for invalid files")
    void testToSarifString_SomeFilesInvalid() throws IOException {
        // Arrange
        OutputUnit validOutputUnit = Mockito.mock(OutputUnit.class);
        Mockito.when(validOutputUnit.isValid()).thenReturn(true);

        OutputUnit invalidOutputUnit = Mockito.mock(OutputUnit.class);
        Mockito.when(invalidOutputUnit.isValid()).thenReturn(false);
        Mockito.when(invalidOutputUnit.getErrors()).thenReturn(Map.of("error", "Validation failed"));

        Map<String, OutputUnit> files = Map.of(
            "file1.yaml", validOutputUnit,
            "file2.yaml", invalidOutputUnit
        );
        FilesOutput filesOutput = new FilesOutput(files);

        // Act
        String sarifString = filesOutput.toSarifString();

        // Assert
        JsonNode sarifJson = objectMapper.readTree(sarifString);
        JsonNode results = sarifJson.get("runs").get(0).get("results");
        
        assertThat(results.isArray()).isTrue();
        assertThat(results.size()).isEqualTo(1);
        
        JsonNode result = results.get(0);
        assertThat(result.get("ruleId").asText()).isEqualTo("schema-validation");
        assertThat(result.get("level").asText()).isEqualTo("error");
        assertThat(result.get("message").get("text").asText()).contains("Validation failed");
        assertThat(result.get("locations").get(0).get("physicalLocation")
                .get("artifactLocation").get("uri").asText()).isEqualTo("file2.yaml");
    }

    @Test
    @DisplayName("toSarifString: Should include detailed validation errors in SARIF format")
    void testToSarifString_InvalidFilesWithDetails() throws IOException {
        // Arrange
        OutputUnit detailOutputUnit = Mockito.mock(OutputUnit.class);
        Mockito.when(detailOutputUnit.getInstanceLocation()).thenReturn("$.sample.boolean-sample");
        Mockito.when(detailOutputUnit.getSchemaLocation()).thenReturn("urn:example:10#/properties/sample/properties/boolean-sample");
        Mockito.when(detailOutputUnit.getErrors()).thenReturn(Map.of("type", "integer found, boolean expected"));
        Mockito.when(detailOutputUnit.isValid()).thenReturn(false);

        OutputUnit invalidOutputUnit = Mockito.mock(OutputUnit.class);
        Mockito.when(invalidOutputUnit.isValid()).thenReturn(false);
        Mockito.when(invalidOutputUnit.getDetails()).thenReturn(Collections.singletonList(detailOutputUnit));

        Map<String, OutputUnit> files = Map.of("invalid.yaml", invalidOutputUnit);
        FilesOutput filesOutput = new FilesOutput(files);

        // Act
        String sarifString = filesOutput.toSarifString();
        
        // Assert - Compare with expected file
        assertTrue(compareSarifToFile(sarifString, "src/test/resources/testreport/test1sarif.sarif"));
    }

    @Test
    @DisplayName("toSarifString: Should include tool information in SARIF format")
    void testToSarifString_ToolInformation() throws IOException {
        // Arrange
        OutputUnit outputUnit = new OutputUnit();
        outputUnit.setValid(true);

        Map<String, OutputUnit> files = Map.of("file1.yaml", outputUnit);
        FilesOutput filesOutput = new FilesOutput(files);

        // Act
        String sarifString = filesOutput.toSarifString();

        // Assert
        JsonNode sarifJson = objectMapper.readTree(sarifString);
        JsonNode driver = sarifJson.get("runs").get(0).get("tool").get("driver");
        
        assertThat(driver.get("name").asText()).isEqualTo("YAML Schema Validator");
        assertThat(driver.get("version").asText()).isEqualTo("1.0.0");
        assertThat(driver.get("informationUri").asText()).isEqualTo("https://github.com/alexmond/yj-schema-validator");
        assertThat(driver.get("semanticVersion").asText()).isEqualTo("1.0.0");
        
        JsonNode rules = driver.get("rules");
        assertThat(rules.isArray()).isTrue();
        assertThat(rules.get(0).get("id").asText()).isEqualTo("schema-validation");
    }

    @Test
    @DisplayName("toSarifString: Should include rule metadata in SARIF format")
    void testToSarifString_RuleMetadata() throws IOException {
        // Arrange
        OutputUnit outputUnit = new OutputUnit();
        outputUnit.setValid(true);

        Map<String, OutputUnit> files = Map.of("file1.yaml", outputUnit);
        FilesOutput filesOutput = new FilesOutput(files);

        // Act
        String sarifString = filesOutput.toSarifString();

        // Assert
        JsonNode sarifJson = objectMapper.readTree(sarifString);
        JsonNode rule = sarifJson.get("runs").get(0).get("tool").get("driver").get("rules").get(0);
        
        assertThat(rule.get("id").asText()).isEqualTo("schema-validation");
        assertThat(rule.get("shortDescription").get("text").asText()).isEqualTo("Schema validation error");
        assertThat(rule.get("fullDescription").get("text").asText())
                .isEqualTo("The file does not conform to the specified JSON/YAML schema");
        assertThat(rule.get("help").get("text").asText())
                .isEqualTo("Ensure that the file content matches the schema definition");
        assertThat(rule.get("defaultConfiguration").get("level").asText()).isEqualTo("error");
    }

    @Test
    @DisplayName("toSarifString: Should include invocation information in SARIF format")
    void testToSarifString_InvocationInformation() throws IOException {
        // Arrange
        OutputUnit invalidOutputUnit = Mockito.mock(OutputUnit.class);
        Mockito.when(invalidOutputUnit.isValid()).thenReturn(false);
        Mockito.when(invalidOutputUnit.getErrors()).thenReturn(Map.of("error", "Validation failed"));

        Map<String, OutputUnit> files = Map.of("file1.yaml", invalidOutputUnit);
        FilesOutput filesOutput = new FilesOutput(files);

        // Act
        String sarifString = filesOutput.toSarifString();

        // Assert
        JsonNode sarifJson = objectMapper.readTree(sarifString);
        JsonNode invocation = sarifJson.get("runs").get(0).get("invocations").get(0);
        
        assertThat(invocation.get("executionSuccessful").asBoolean()).isFalse();
        assertThat(invocation.get("exitCode").asInt()).isEqualTo(1);
        assertThat(invocation.get("startTimeUtc").asText()).isNotEmpty();
        assertThat(invocation.get("endTimeUtc").asText()).isNotEmpty();
    }

    @Test
    @DisplayName("toSarifString: Should include location information for errors")
    void testToSarifString_LocationInformation() throws IOException {
        // Arrange
        OutputUnit detailOutputUnit = Mockito.mock(OutputUnit.class);
        Mockito.when(detailOutputUnit.getInstanceLocation()).thenReturn("$.data.items[0].name");
        Mockito.when(detailOutputUnit.getErrors()).thenReturn(Map.of("type", "string expected, number found"));
        Mockito.when(detailOutputUnit.isValid()).thenReturn(false);

        OutputUnit invalidOutputUnit = Mockito.mock(OutputUnit.class);
        Mockito.when(invalidOutputUnit.isValid()).thenReturn(false);
        Mockito.when(invalidOutputUnit.getDetails()).thenReturn(Collections.singletonList(detailOutputUnit));

        Map<String, OutputUnit> files = Map.of("test.yaml", invalidOutputUnit);
        FilesOutput filesOutput = new FilesOutput(files);

        // Act
        String sarifString = filesOutput.toSarifString();

        // Assert
        JsonNode sarifJson = objectMapper.readTree(sarifString);
        JsonNode location = sarifJson.get("runs").get(0).get("results").get(0).get("locations").get(0);
        JsonNode physicalLocation = location.get("physicalLocation");
        
        assertThat(physicalLocation.get("artifactLocation").get("uri").asText()).isEqualTo("test.yaml");
        assertThat(physicalLocation.get("region").get("snippet").get("text").asText())
                .contains("$.data.items[0].name");
    }

    @Test
    @DisplayName("toSarifString: Should handle multiple validation errors for single file")
    void testToSarifString_MultipleErrorsPerFile() throws IOException {
        // Arrange
        OutputUnit detail1 = Mockito.mock(OutputUnit.class);
        Mockito.when(detail1.getInstanceLocation()).thenReturn("$.field1");
        Mockito.when(detail1.getErrors()).thenReturn(Map.of("type", "Error 1"));
        Mockito.when(detail1.isValid()).thenReturn(false);

        OutputUnit detail2 = Mockito.mock(OutputUnit.class);
        Mockito.when(detail2.getInstanceLocation()).thenReturn("$.field2");
        Mockito.when(detail2.getErrors()).thenReturn(Map.of("type", "Error 2"));
        Mockito.when(detail2.isValid()).thenReturn(false);

        OutputUnit invalidOutputUnit = Mockito.mock(OutputUnit.class);
        Mockito.when(invalidOutputUnit.isValid()).thenReturn(false);
        Mockito.when(invalidOutputUnit.getDetails()).thenReturn(java.util.List.of(detail1, detail2));

        Map<String, OutputUnit> files = Map.of("test.yaml", invalidOutputUnit);
        FilesOutput filesOutput = new FilesOutput(files);

        // Act
        String sarifString = filesOutput.toSarifString();

        // Assert
        JsonNode sarifJson = objectMapper.readTree(sarifString);
        JsonNode results = sarifJson.get("runs").get(0).get("results");
        
        assertThat(results.size()).isEqualTo(2);
        assertThat(results.get(0).get("message").get("text").asText()).contains("$.field1");
        assertThat(results.get(1).get("message").get("text").asText()).contains("$.field2");
    }

    @Test
    @DisplayName("toSarifString: Should be valid JSON")
    void testToSarifString_ValidJson() {
        // Arrange
        OutputUnit outputUnit = new OutputUnit();
        outputUnit.setValid(true);

        Map<String, OutputUnit> files = Map.of("file1.yaml", outputUnit);
        FilesOutput filesOutput = new FilesOutput(files);

        // Act & Assert - Should not throw exception
        String sarifString = filesOutput.toSarifString();
        assertThat(sarifString).isNotEmpty();
        
        // Verify it's valid JSON by parsing it
        try {
            JsonNode jsonNode = objectMapper.readTree(sarifString);
            assertThat(jsonNode).isNotNull();
        } catch (IOException e) {
            throw new AssertionError("Generated SARIF is not valid JSON", e);
        }
    }

    /**
     * Compares SARIF output with expected file, ignoring timestamp fields.
     *
     * @param sarifString Generated SARIF JSON string
     * @param fileName    Path to expected SARIF file
     * @return true if SARIF matches (ignoring timestamps), false otherwise
     */
    private boolean compareSarifToFile(String sarifString, String fileName) {
        try {
            String fileContent = Files.readString(Path.of(fileName));
            
            JsonNode generated = objectMapper.readTree(sarifString);
            JsonNode expected = objectMapper.readTree(fileContent);
            
            // Remove timestamp fields for comparison
            removeTimestampFields(generated);
            removeTimestampFields(expected);
            
            return generated.equals(expected);
        } catch (IOException e) {
            throw new RuntimeException("Failed to compare SARIF files", e);
        }
    }

    /**
     * Removes timestamp fields from SARIF JSON for comparison purposes.
     *
     * @param jsonNode SARIF JSON node
     */
    private void removeTimestampFields(JsonNode jsonNode) {
        if (jsonNode.isObject()) {
            ((com.fasterxml.jackson.databind.node.ObjectNode) jsonNode).remove("startTimeUtc");
            ((com.fasterxml.jackson.databind.node.ObjectNode) jsonNode).remove("endTimeUtc");
        }
        jsonNode.forEach(this::removeTimestampFields);
    }
}
