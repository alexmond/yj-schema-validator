package org.alexmond.yaml.validator.output;

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
 * Test class for JUnit XML format output generation.
 * Tests the conversion of validation results to JUnit XML format.
 */
class FilesOutputToJunitTest {

    @Test
    @DisplayName("toJunitString: Should generate valid JUnit XML when all files are valid")
    void testToJunitString_AllFilesValid() {
        // Arrange
        OutputUnit outputUnit = new OutputUnit();
        outputUnit.setValid(true);

        Map<String, OutputUnit> files = Map.of("file1.yaml", outputUnit);
        FilesOutput filesOutput = new FilesOutput(files);

        // Act
        String junitString = filesOutput.toJunitString();

        // Assert
        assertThat(junitString).contains("failures=\"0\"")
                .contains("<testcase")
                .contains("classname=\"files\"")
                .contains("name=\"file1.yaml\"");
    }

    @Test
    @DisplayName("toJunitString: Should generate JUnit XML with failures for invalid files")
    void testToJunitString_SomeFilesInvalid() {
        // Arrange
        OutputUnit validOutputUnit = Mockito.mock(OutputUnit.class);
        Mockito.when(validOutputUnit.isValid()).thenReturn(true);

        OutputUnit invalidOutputUnit = Mockito.mock(OutputUnit.class);
        Mockito.when(invalidOutputUnit.isValid()).thenReturn(false);
        Mockito.when(invalidOutputUnit.getErrors()).thenReturn(Map.of("errorKey", "Error message"));

        Map<String, OutputUnit> files = Map.of(
                "file1.yaml", validOutputUnit,
                "file2.yaml", invalidOutputUnit
        );
        FilesOutput filesOutput = new FilesOutput(files);

        // Act
        String junitString = filesOutput.toJunitString();

        // Assert
        assertThat(junitString).contains("<testsuite")
                .contains("name=\"file1.yaml\"")
                .contains("name=\"file2.yaml\"")
                .contains("<failure message=\"Validation Failure\"");
    }

    @Test
    @DisplayName("toJunitString: Should include file details in JUnit XML for invalid files")
    void testToJunitString_InvalidFilesWithDetails() {
        // Arrange
        OutputUnit detailOutputUnit = Mockito.mock(OutputUnit.class);
        Mockito.when(detailOutputUnit.getInstanceLocation()).thenReturn("$.sample.boolean-sample");
        Mockito.when(detailOutputUnit.getSchemaLocation()).thenReturn("urn:example:10#/properties/sample/properties/boolean-sample");
        Mockito.when(detailOutputUnit.getErrors()).thenReturn(Map.of("type", "integer found, boolean expected"));

        OutputUnit invalidOutputUnit = Mockito.mock(OutputUnit.class);
        Mockito.when(invalidOutputUnit.isValid()).thenReturn(false);
        Mockito.when(invalidOutputUnit.getDetails()).thenReturn(Collections.singletonList(detailOutputUnit));

        Map<String, OutputUnit> files = Map.of("invalid.yaml", invalidOutputUnit);
        FilesOutput filesOutput = new FilesOutput(files);

        // Act
        String junitString = filesOutput.toJunitString();
        
        
        // Assert
        try {
            Files.writeString(Path.of("test1junit.xml"), junitString);
            assertTrue(compareFileToString(junitString, "src/test/resources/testreport/test1junit.xml"));
        } catch (IOException e) {
            throw new RuntimeException("Failed to write JUnit XML file", e);
        }
    }

    @Test
    @DisplayName("toJunitString: Should include testsuite element with correct attributes")
    void testToJunitString_TestsuiteAttributes() {
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
        String junitString = filesOutput.toJunitString();

        // Assert
        assertThat(junitString).contains("name=\"SchemaValidationSuite\"")
                .contains("tests=\"2\"")
                .contains("failures=\"1\"")
                .contains("errors=\"0\"");
    }

    @Test
    @DisplayName("toJunitString: Should categorize error types correctly")
    void testToJunitString_ErrorTypeCategorization() {
        // Arrange
        OutputUnit noSchemaUnit = Mockito.mock(OutputUnit.class);
        Mockito.when(noSchemaUnit.isValid()).thenReturn(false);
        Mockito.when(noSchemaUnit.getErrors()).thenReturn(Map.of("error", "No schema found"));

        Map<String, OutputUnit> files = Map.of("test.yaml", noSchemaUnit);
        FilesOutput filesOutput = new FilesOutput(files);

        // Act
        String junitString = filesOutput.toJunitString();

        // Assert
        assertThat(junitString).contains("<failure message=\"No Schema Error\"");
    }

    @Test
    @DisplayName("toJunitString: Should handle YAML parse errors")
    void testToJunitString_YamlParseError() {
        // Arrange
        OutputUnit parseErrorUnit = Mockito.mock(OutputUnit.class);
        Mockito.when(parseErrorUnit.isValid()).thenReturn(false);
        Mockito.when(parseErrorUnit.getErrors())
                .thenReturn(Map.of("error", "MarkedYAMLException: invalid YAML syntax"));

        Map<String, OutputUnit> files = Map.of("invalid.yaml", parseErrorUnit);
        FilesOutput filesOutput = new FilesOutput(files);

        // Act
        String junitString = filesOutput.toJunitString();

        // Assert
        assertThat(junitString).contains("<failure message=\"YAML Parse Error\"");
    }

    @Test
    @DisplayName("toJunitString: Should include failure message for type mismatch")
    void testToJunitString_TypeMismatchError() {
        // Arrange
        OutputUnit detailOutputUnit = Mockito.mock(OutputUnit.class);
        Mockito.when(detailOutputUnit.getInstanceLocation()).thenReturn("$.data.field");
        Mockito.when(detailOutputUnit.getErrors()).thenReturn(Map.of("type", "string expected"));

        OutputUnit invalidOutputUnit = Mockito.mock(OutputUnit.class);
        Mockito.when(invalidOutputUnit.isValid()).thenReturn(false);
        Mockito.when(invalidOutputUnit.getDetails()).thenReturn(Collections.singletonList(detailOutputUnit));

        Map<String, OutputUnit> files = Map.of("test.yaml", invalidOutputUnit);
        FilesOutput filesOutput = new FilesOutput(files);

        // Act
        String junitString = filesOutput.toJunitString();

        // Assert
        assertThat(junitString).contains("<failure message=\"Type Mismatch at $.data.field\"");
    }

    @Test
    @DisplayName("toJunitString: Should handle multiple files with mixed results")
    void testToJunitString_MultipleFilesMixedResults() {
        // Arrange
        OutputUnit valid1 = Mockito.mock(OutputUnit.class);
        Mockito.when(valid1.isValid()).thenReturn(true);

        OutputUnit valid2 = Mockito.mock(OutputUnit.class);
        Mockito.when(valid2.isValid()).thenReturn(true);

        OutputUnit invalid1 = Mockito.mock(OutputUnit.class);
        Mockito.when(invalid1.isValid()).thenReturn(false);
        Mockito.when(invalid1.getErrors()).thenReturn(Map.of("error", "Error 1"));

        OutputUnit invalid2 = Mockito.mock(OutputUnit.class);
        Mockito.when(invalid2.isValid()).thenReturn(false);
        Mockito.when(invalid2.getErrors()).thenReturn(Map.of("error", "Error 2"));

        Map<String, OutputUnit> files = Map.of(
                "valid1.yaml", valid1,
                "valid2.yaml", valid2,
                "invalid1.yaml", invalid1,
                "invalid2.yaml", invalid2
        );
        FilesOutput filesOutput = new FilesOutput(files);

        // Act
        String junitString = filesOutput.toJunitString();

        // Assert
        assertThat(junitString).contains("tests=\"4\"")
                .contains("failures=\"2\"")
                .contains("name=\"valid1.yaml\"")
                .contains("name=\"valid2.yaml\"")
                .contains("name=\"invalid1.yaml\"")
                .contains("name=\"invalid2.yaml\"");
    }

    @Test
    @DisplayName("toJunitString: Should be valid XML")
    void testToJunitString_ValidXml() {
        // Arrange
        OutputUnit outputUnit = new OutputUnit();
        outputUnit.setValid(true);

        Map<String, OutputUnit> files = Map.of("file1.yaml", outputUnit);
        FilesOutput filesOutput = new FilesOutput(files);

        // Act
        String junitString = filesOutput.toJunitString();

        // Assert
        assertThat(junitString).isNotEmpty()
                .startsWith("<?xml")
                .contains("</testsuites>");
    }

    @Test
    @DisplayName("toJunitString: Should include testsuites root element")
    void testToJunitString_RootElement() {
        // Arrange
        OutputUnit outputUnit = new OutputUnit();
        outputUnit.setValid(true);

        Map<String, OutputUnit> files = Map.of("file1.yaml", outputUnit);
        FilesOutput filesOutput = new FilesOutput(files);

        // Act
        String junitString = filesOutput.toJunitString();

        // Assert
        assertThat(junitString).contains("<testsuites")
                .contains("</testsuites>")
                .contains("<testsuite")
                .contains("</testsuite>");
    }

    @Test
    @DisplayName("toJunitString: Should set time attribute to 0.0 for all testcases")
    void testToJunitString_TimeAttribute() {
        // Arrange
        OutputUnit outputUnit = new OutputUnit();
        outputUnit.setValid(true);

        Map<String, OutputUnit> files = Map.of("file1.yaml", outputUnit);
        FilesOutput filesOutput = new FilesOutput(files);

        // Act
        String junitString = filesOutput.toJunitString();

        // Assert
        assertThat(junitString).contains("time=\"0.0\"");
    }

    @Test
    @DisplayName("toJunitString: Should include full error message in failure value")
    void testToJunitString_FullErrorMessage() {
        // Arrange
        OutputUnit detailUnit1 = Mockito.mock(OutputUnit.class);
        Mockito.when(detailUnit1.getErrors()).thenReturn(Map.of("type", "First error"));

        OutputUnit detailUnit2 = Mockito.mock(OutputUnit.class);
        Mockito.when(detailUnit2.getErrors()).thenReturn(Map.of("required", "Second error"));

        OutputUnit invalidOutputUnit = Mockito.mock(OutputUnit.class);
        Mockito.when(invalidOutputUnit.isValid()).thenReturn(false);
        Mockito.when(invalidOutputUnit.getDetails())
                .thenReturn(java.util.List.of(detailUnit1, detailUnit2));

        Map<String, OutputUnit> files = Map.of("test.yaml", invalidOutputUnit);
        FilesOutput filesOutput = new FilesOutput(files);

        // Act
        String junitString = filesOutput.toJunitString();

        // Assert
        assertThat(junitString).contains("First error")
                .contains("Second error");
    }

    /**
     * Compares JUnit XML output with expected file content.
     *
     * @param junitString Generated JUnit XML string
     * @param fileName    Path to expected JUnit XML file
     * @return true if content matches, false otherwise
     */
    private boolean compareFileToString(String junitString, String fileName) {
        try {
            String fileContent = Files.readString(Path.of(fileName));
            return junitString.trim().equals(fileContent.trim());
        } catch (IOException e) {
            throw new RuntimeException("Failed to read JUnit XML file", e);
        }
    }
}
