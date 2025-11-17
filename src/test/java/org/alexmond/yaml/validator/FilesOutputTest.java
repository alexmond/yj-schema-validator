package org.alexmond.yaml.validator;

import com.networknt.schema.output.OutputUnit;
import org.alexmond.yaml.validator.output.FilesOutput;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.ansi.AnsiColor;
import org.springframework.boot.ansi.AnsiOutput;

import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test class for FilesOutput general functionality.
 * Tests colored string output and general validation behavior.
 */
class FilesOutputTest {

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

    @Test
    @DisplayName("FilesOutput: Should correctly determine valid status")
    void testFilesOutput_ValidStatus() {
        // Arrange
        OutputUnit validUnit = Mockito.mock(OutputUnit.class);
        Mockito.when(validUnit.isValid()).thenReturn(true);

        OutputUnit invalidUnit = Mockito.mock(OutputUnit.class);
        Mockito.when(invalidUnit.isValid()).thenReturn(false);

        // Act & Assert - All valid
        FilesOutput allValidOutput = new FilesOutput(Map.of("file1.yaml", validUnit));
        assertThat(allValidOutput.isValid()).isTrue();

        // Act & Assert - Some invalid
        FilesOutput someInvalidOutput = new FilesOutput(Map.of(
                "file1.yaml", validUnit,
                "file2.yaml", invalidUnit
        ));
        assertThat(someInvalidOutput.isValid()).isFalse();
    }

    @Test
    @DisplayName("toJsonString: Should generate valid JSON output")
    void testToJsonString_ValidJson() {
        // Arrange
        OutputUnit validUnit = new OutputUnit();
        validUnit.setValid(true);

        Map<String, OutputUnit> files = Map.of("file1.yaml", validUnit);
        FilesOutput filesOutput = new FilesOutput(files);

        // Act
        String jsonString = filesOutput.toJsonString();

        // Assert
        assertThat(jsonString).isNotEmpty()
                .contains("\"valid\"")
                .contains("\"files\"");
    }

    @Test
    @DisplayName("toYamlString: Should generate valid YAML output")
    void testToYamlString_ValidYaml() {
        // Arrange
        OutputUnit validUnit = new OutputUnit();
        validUnit.setValid(true);

        Map<String, OutputUnit> files = Map.of("file1.yaml", validUnit);
        FilesOutput filesOutput = new FilesOutput(files);

        // Act
        String yamlString = filesOutput.toYamlString();

        // Assert
        assertThat(yamlString).isNotEmpty()
                .contains("valid:")
                .contains("files:");
    }
}