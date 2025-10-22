// Test class file: YamlSchemaValidatorRunnerTest.java

package org.alexmond.yaml.validator;

import com.networknt.schema.output.OutputUnit;
import org.alexmond.yaml.validator.config.ReportType;
import org.alexmond.yaml.validator.config.YamlSchemaValidatorConfig;
import org.alexmond.yaml.validator.output.FilesOutput;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationArguments;
import org.springframework.core.env.Environment;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class YamlSchemaValidatorRunnerTest {
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;

    @BeforeEach
    void setUp() {
        System.setOut(new PrintStream(outContent));
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
        outContent.reset();  // Clear for next test
    }

    /**
     * Test to verify that Validate() method returns null and doesn't proceed with validation
     * if the "--help" option is provided.
     */
    @Test
    void testValidateMethodWithHelpOption() {
        YamlSchemaValidatorConfig config = mock(YamlSchemaValidatorConfig.class);
        YamlSchemaValidator yamlSchemaValidator = mock(YamlSchemaValidator.class);
        Environment environment = mock(Environment.class);

        YamlSchemaValidatorRunner runner = new YamlSchemaValidatorRunner(config, yamlSchemaValidator, environment);

        ApplicationArguments args = mock(ApplicationArguments.class);
        when(args.containsOption("help")).thenReturn(true);

        FilesOutput result = runner.Validate(args);

        assertNull(result, "Expected result to be null when '--help' option is passed");
        verify(args, times(1)).containsOption("help");
        String expected = "Usage: java -jar yaml-schema-validator.jar";
        assertTrue(outContent.toString().contains(expected), "Output should contain:" + expected);
    }

    /**
     * Test to verify that Validate() method returns null when configuration validation fails.
     */
    @Test
    void testValidateMethodWithInvalidConfig() {
        YamlSchemaValidatorConfig config = mock(YamlSchemaValidatorConfig.class);
        YamlSchemaValidator yamlSchemaValidator = mock(YamlSchemaValidator.class);
        Environment environment = mock(Environment.class);

        when(config.isSchemaOverride()).thenReturn(true);
        when(config.getSchema()).thenReturn(null);

        YamlSchemaValidatorRunner runner = new YamlSchemaValidatorRunner(config, yamlSchemaValidator, environment);

        ApplicationArguments args = mock(ApplicationArguments.class);
        when(args.getNonOptionArgs()).thenReturn(List.of("file1.yaml"));

        FilesOutput result = runner.Validate(args);

        assertNull(result, "Expected result to be null when configuration is invalid");
    }

    /**
     * Test to verify that Validate() method processes validation for valid yaml files.
     */
    @Test
    void testValidateMethodWithValidFiles() {
        YamlSchemaValidatorConfig config = mock(YamlSchemaValidatorConfig.class);
        YamlSchemaValidator yamlSchemaValidator = mock(YamlSchemaValidator.class);
        Environment environment = mock(Environment.class);

        when(config.getSchema()).thenReturn("sample-schema.json");
        when(config.getReportType()).thenReturn(ReportType.JSON);
        when(config.isColor()).thenReturn(false);

        YamlSchemaValidatorRunner runner = new YamlSchemaValidatorRunner(config, yamlSchemaValidator, environment);

        ApplicationArguments args = mock(ApplicationArguments.class);
        when(args.getNonOptionArgs()).thenReturn(List.of("valid.yaml"));
        OutputUnit outputUnit = new OutputUnit();
        outputUnit.setValid(true);
        when(yamlSchemaValidator.validate("valid.yaml", "sample-schema.json")).thenReturn(Collections.singletonMap("valid.yaml", outputUnit));

        FilesOutput result = runner.Validate(args);

        assertNotNull(result, "Expected result not to be null with valid files");
        assertTrue(result.isValid(), "Expected result to be valid for the provided YAML file");
    }

    /**
     * Test to verify that Validate() method processes invalid YAML files and returns invalid output.
     */
    @Test
    void testValidateMethodWithInvalidFiles() {
        YamlSchemaValidatorConfig config = mock(YamlSchemaValidatorConfig.class);
        YamlSchemaValidator yamlSchemaValidator = mock(YamlSchemaValidator.class);
        Environment environment = mock(Environment.class);

        when(config.getSchema()).thenReturn("sample-schema.json");
        when(config.getReportType()).thenReturn(ReportType.JSON);
        when(config.isColor()).thenReturn(false);

        YamlSchemaValidatorRunner runner = new YamlSchemaValidatorRunner(config, yamlSchemaValidator, environment);

        ApplicationArguments args = mock(ApplicationArguments.class);
        when(args.getNonOptionArgs()).thenReturn(List.of("invalid.yaml"));

        OutputUnit invalidOutputUnit = mock(OutputUnit.class);
        when(invalidOutputUnit.isValid()).thenReturn(false);

        when(yamlSchemaValidator.validate("invalid.yaml", "sample-schema.json")).thenReturn(Collections.singletonMap("invalid.yaml", invalidOutputUnit));

        FilesOutput result = runner.Validate(args);

        assertNotNull(result, "Expected result not to be null with invalid files");
        assertFalse(result.isValid(), "Expected result to be invalid for the provided YAML file");
    }

    /**
     * Test to verify that Validate() method handles runtime exceptions during file processing.
     */
//    @Test
    void testValidateMethodHandlesRuntimeException() {
        YamlSchemaValidatorConfig config = mock(YamlSchemaValidatorConfig.class);
        YamlSchemaValidator yamlSchemaValidator = mock(YamlSchemaValidator.class);
        Environment environment = mock(Environment.class);

        when(config.getSchema()).thenReturn("sample-schema.json");
        when(config.getReportType()).thenReturn(ReportType.JSON);
        when(config.isColor()).thenReturn(false);

        YamlSchemaValidatorRunner runner = new YamlSchemaValidatorRunner(config, yamlSchemaValidator, environment);

        ApplicationArguments args = mock(ApplicationArguments.class);
        when(args.getNonOptionArgs()).thenReturn(List.of("error-prone.yaml"));

        doThrow(new RuntimeException("Simulated exception")).when(yamlSchemaValidator).validate("error-prone.yaml", "sample-schema.json");

        FilesOutput result = runner.Validate(args);

        assertNotNull(result, "Expected result not to be null even if an exception is thrown during validation");
        assertFalse(result.isValid(), "Expected result to be invalid when an exception occurs");
    }
}