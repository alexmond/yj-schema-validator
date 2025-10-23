package org.alexmond.yaml.validator;

import com.networknt.schema.output.OutputUnit;
import org.alexmond.yaml.validator.output.FilesOutput;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.ansi.AnsiColor;
import org.springframework.boot.ansi.AnsiOutput;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;

import static java.nio.file.Files.writeString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FilesOutputTest {

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
                               .contains("<testcase").contains("classname=\"files\"").contains("name=\"file1.yaml\"");
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
        assertTrue(compareFileToString(junitString,"src/test/resources/testreport/test1junit.xml"));
    }

    private boolean compareFileToString(String junitString, String fileName) {
        try {
            String fileContent = Files.readString(Path.of(fileName));
            return junitString.trim().equals(fileContent.trim());
        } catch (IOException e) {
            throw new RuntimeException("Failed to read JUnit XML file", e);
        }
    }


    @Test
    @DisplayName("toColoredString: Should print 'ok' in green color when all files are valid and color is enabled")
    void testToColoredString_AllFilesValid_ColorEnabled() {
        // Arrange
        OutputUnit mockOutputUnit = Mockito.mock(OutputUnit.class);
        Mockito.when(mockOutputUnit.isValid()).thenReturn(true);

        Map<String, OutputUnit> files = Collections.singletonMap("file1.yaml", mockOutputUnit);
        FilesOutput filesOutput = new FilesOutput(files);

        // Act
        String result = filesOutput.toColoredString(true);

        // Assert
        assertThat(result).contains(AnsiOutput.toString(AnsiColor.GREEN, "ok", AnsiColor.DEFAULT));
        assertThat(result).doesNotContain(AnsiOutput.toString(AnsiColor.RED, "invalid", AnsiColor.DEFAULT));
    }

    @Test
    @DisplayName("toColoredString: Should print 'invalid' in red color when some files are invalid and color is enabled")
    void testToColoredString_SomeFilesInvalid_ColorEnabled() {
        // Arrange
        OutputUnit validOutputUnit = Mockito.mock(OutputUnit.class);
        Mockito.when(validOutputUnit.isValid()).thenReturn(true);

        OutputUnit invalidOutputUnit = Mockito.mock(OutputUnit.class);
        Mockito.when(invalidOutputUnit.isValid()).thenReturn(false);

        Map<String, OutputUnit> files = Map.of(
                "file1.yaml", validOutputUnit,
                "file2.yaml", invalidOutputUnit
        );
        FilesOutput filesOutput = new FilesOutput(files);

        // Act
        String result = filesOutput.toColoredString(true);

        // Assert
        assertThat(result).contains(AnsiOutput.toString(AnsiColor.GREEN, "ok", AnsiColor.DEFAULT));
        assertThat(result).contains(AnsiOutput.toString(AnsiColor.RED, "invalid", AnsiColor.DEFAULT));
    }

    @Test
    @DisplayName("toColoredString: Should not include ANSI colors when color is disabled")
    void testToColoredString_ColorDisabled() {
        // Arrange
        OutputUnit validOutputUnit = Mockito.mock(OutputUnit.class);
        Mockito.when(validOutputUnit.isValid()).thenReturn(true);

        OutputUnit invalidOutputUnit = Mockito.mock(OutputUnit.class);
        Mockito.when(invalidOutputUnit.isValid()).thenReturn(false);

        Map<String, OutputUnit> files = Map.of(
                "file1.yaml", validOutputUnit,
                "file2.yaml", invalidOutputUnit
        );
        FilesOutput filesOutput = new FilesOutput(files);

        // Act
        String result = filesOutput.toColoredString(false);
        AnsiOutput.setEnabled(AnsiOutput.Enabled.ALWAYS);
        // Assert
        assertThat(result).contains("file1.yaml: ok");
        assertThat(result).contains("file2.yaml: invalid");
        assertThat(result).doesNotContain(AnsiOutput.toString(AnsiColor.GREEN));
        assertThat(result).doesNotContain(AnsiOutput.toString(AnsiColor.RED));
    }

    @Test
    @DisplayName("toColoredString: Should include error details for invalid files")
    void testToColoredString_InvalidFilesWithErrors() {
        // Arrange
        OutputUnit invalidOutputUnit = Mockito.mock(OutputUnit.class);
        Mockito.when(invalidOutputUnit.isValid()).thenReturn(false);
        Mockito.when(invalidOutputUnit.getErrors())
                .thenReturn(Map.of("errorKey", "Error message"));

        Map<String, OutputUnit> files = Map.of("file2.yaml", invalidOutputUnit);
        FilesOutput filesOutput = new FilesOutput(files);

        // Act
        String result = filesOutput.toColoredString(true);

        // Assert
        assertThat(result).contains("file2.yaml");
        assertThat(result).contains("errorKey: Error message");
    }

    @Test
    @DisplayName("toColoredString: Should include validation details for invalid files")
    void testToColoredString_InvalidFilesWithDetails() {
        // Arrange
        OutputUnit detailOutputUnit = Mockito.mock(OutputUnit.class);
        Mockito.when(detailOutputUnit.getInstanceLocation()).thenReturn("/path/to/instance");
        Mockito.when(detailOutputUnit.getSchemaLocation()).thenReturn("/path/to/schema");
        Mockito.when(detailOutputUnit.getErrors()).thenReturn(Map.of("detailError", "Detail error message"));

        OutputUnit invalidOutputUnit = Mockito.mock(OutputUnit.class);
        Mockito.when(invalidOutputUnit.isValid()).thenReturn(false);
        Mockito.when(invalidOutputUnit.getDetails()).thenReturn(Collections.singletonList(detailOutputUnit));

        Map<String, OutputUnit> files = Map.of("file2.yaml", invalidOutputUnit);
        FilesOutput filesOutput = new FilesOutput(files);

        // Act
        String result = filesOutput.toColoredString(true);

        // Assert
        assertThat(result).contains("Details:");
        assertThat(result).contains("Path: /path/to/instance");
        assertThat(result).contains("Schema: /path/to/schema");
        assertThat(result).contains("detailError: Detail error message");
    }
    
    private void writeTestFile(String content, String filename) {
        try {
            writeString(java.nio.file.Path.of(filename), content);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write JUnit XML file", e);

        }
    }
}