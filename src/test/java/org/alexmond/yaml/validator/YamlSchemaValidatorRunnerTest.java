// Test class file: YamlSchemaValidatorRunnerTest.java

package org.alexmond.yaml.validator;

import com.networknt.schema.output.OutputUnit;
import org.alexmond.yaml.validator.config.ReportType;
import org.alexmond.yaml.validator.config.YamlSchemaValidatorConfig;
import org.alexmond.yaml.validator.output.FilesOutput;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
class YamlSchemaValidatorRunnerTest {
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private String reportsDir="src/test/resources/testreport/";
    private String testDataDir="src/test/resources/testdata/";

    @Autowired
    private YamlSchemaValidator yamlSchemaValidatorReal;

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
     * Test to verify that Validate() method returns null and displays appropriate message
     * when no YAML/JSON files are provided as arguments.
     */
    @Test
    void testValidateNoFiles() {
        YamlSchemaValidatorConfig config = mock(YamlSchemaValidatorConfig.class);
        YamlSchemaValidator yamlSchemaValidator = mock(YamlSchemaValidator.class);
        Environment environment = mock(Environment.class);

        YamlSchemaValidatorRunner runner = new YamlSchemaValidatorRunner(config, yamlSchemaValidator, environment);

        ApplicationArguments args = mock(ApplicationArguments.class);

        FilesOutput result = runner.Validate(args);

        assertNull(result, "Expected result to be null when '--help' option is passed");
        verify(args, times(1)).containsOption("help");
        String expected = "At least one YAML/JSON file must be provided as a non-option argument";
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
        String expected = "Schema path must be provided when schemaPathOverride is enabled";
        assertTrue(outContent.toString().contains(expected), "Output should contain:" + expected);
    }

    /**
     * Test to verify that Validate() method processes validation for valid yaml files.
     */
    @Test
    void testValidateMethodWithValidFiles() {
        YamlSchemaValidatorConfig config = mock(YamlSchemaValidatorConfig.class);
        YamlSchemaValidator yamlSchemaValidator = mock(YamlSchemaValidator.class);
        Environment environment = mock(Environment.class);

        when(config.getSchema()).thenReturn("testdata/sample-schema.json");
        when(config.getReportType()).thenReturn(ReportType.JSON);
        when(config.isColor()).thenReturn(false);

        YamlSchemaValidatorRunner runner = new YamlSchemaValidatorRunner(config, yamlSchemaValidator, environment);

        ApplicationArguments args = mock(ApplicationArguments.class);
        when(args.getNonOptionArgs()).thenReturn(List.of("testdata/valid.yaml"));
        OutputUnit outputUnit = new OutputUnit();
        outputUnit.setValid(true);
        when(yamlSchemaValidator.validate("testdata/valid.yaml", "testdata/sample-schema.json")).thenReturn(Collections.singletonMap("testdata/valid.yaml", outputUnit));

        FilesOutput result = runner.Validate(args);

        assertNotNull(result, "Expected result not to be null with valid files");
        assertTrue(result.isValid(), "Expected result to be valid for the provided YAML file");
    }

    /**
     * Test validation of YAML files with different report types and expected outcomes.
     *
     * @param fileName       The name of the YAML file to validate
     * @param reportType     The type of report to generate (JSON, YAML, JUNIT, TEXT)
     * @param reportFile     The output report file name
     * @param expectedReport The expected report file to compare against
     * @param valid          Expected validation result ("true" or "false")
     */
    @ParameterizedTest
    @CsvSource({
            "valid.yaml,JSON,test1.json,validyaml.json,true",
            "valid.yaml,YAML,test1.yaml,validyaml.yaml,true",
            "valid.yaml,JUNIT,test1.xml,validyaml.xml,true",
            "valid.yaml,TEXT,test1.txt,validyaml.txt,true",
            "multi3invalid.yaml,JSON,test1.json,multi3invalidyaml.json,false",
            "multi3invalid.yaml,YAML,test1.yaml,multi3invalidyaml.yaml,false",
            "multi3invalid.yaml,JUNIT,test1.xml,multi3invalidyaml.xml,false",
            "multi3invalid.yaml,TEXT,test1.txt,multi3invalidyaml.txt,false",
            "invalid.yaml,JSON,test1.json,invalidyaml.json,false",
            "invalid.yaml,YAML,test1.yaml,invalidyaml.yaml,false",
            "invalid.yaml,JUNIT,test1.xml,invalidyaml.xml,false",
            "invalid.yaml,TEXT,test1.txt,invalidyaml.txt,false",
    })
    void fullTestWithReport(String fileName,String reportType,String reportFile,String expectedReport,String valid) {
        YamlSchemaValidatorConfig config = mock(YamlSchemaValidatorConfig.class);
        Environment environment = mock(Environment.class);

        when(config.getReportType()).thenReturn(ReportType.valueOf(reportType));
        when(config.getReportFileName()).thenReturn(reportFile);

        YamlSchemaValidatorRunner runner = new YamlSchemaValidatorRunner(config, yamlSchemaValidatorReal, environment);

        ApplicationArguments args = mock(ApplicationArguments.class);
        when(args.getNonOptionArgs()).thenReturn(List.of(testDataDir + fileName));
        FilesOutput result = runner.Validate(args);

        assertNotNull(result, "Expected result not to be null with valid files");
        if (valid.equals("true")) {
            assertTrue(result.isValid(), "Expected result to be valid for the provided file");
        }else{
            assertFalse(result.isValid(), "Expected result to be not valid for the provided file");
        }
        assertTrue(compareFiles(reportFile,reportsDir + expectedReport), "Reports should match");

    }

    /**
     * Test validation of YAML files using configuration files instead of command line arguments.
     *
     * @param fileName       The name of the YAML file to validate
     * @param reportType     The type of report to generate (JSON, YAML, JUNIT, TEXT)
     * @param reportFile     The output report file name
     * @param expectedReport The expected report file to compare against
     * @param valid          Expected validation result ("true" or "false")
     */
    @ParameterizedTest
    @CsvSource({
            "valid.yaml,JSON,test1.json,validyaml.json,true",
    })
    void fullTestWithReportConfigFiles(String fileName,String reportType,String reportFile,String expectedReport,String valid) {
        YamlSchemaValidatorConfig config = mock(YamlSchemaValidatorConfig.class);
        Environment environment = mock(Environment.class);

        when(config.getReportType()).thenReturn(ReportType.valueOf(reportType));
        when(config.getReportFileName()).thenReturn(reportFile);

        YamlSchemaValidatorRunner runner = new YamlSchemaValidatorRunner(config, yamlSchemaValidatorReal, environment);

        ApplicationArguments args = mock(ApplicationArguments.class);
        when(args.getNonOptionArgs()).thenReturn(List.of("none.yaml"));
        when(config.getFiles()).thenReturn(List.of(testDataDir + fileName));
        FilesOutput result = runner.Validate(args);

        assertNotNull(result, "Expected result not to be null with valid files");
        if (valid.equals("true")) {
            assertTrue(result.isValid(), "Expected result to be valid for the provided file");
        }else{
            assertFalse(result.isValid(), "Expected result to be not valid for the provided file");
        }
        assertTrue(compareFiles(reportFile,reportsDir + expectedReport), "Reports should match");

    }

    /**
     * Compares two files byte by byte to check if they are identical.
     *
     * @param path1 Path to the first file
     * @param path2 Path to the second file
     * @return true if files are identical, false otherwise or if files cannot be read
     */
    public boolean compareFiles(String path1, String path2) {
        try {
            byte[] bytes1 = Files.readAllBytes(Paths.get(path1));
            byte[] bytes2 = Files.readAllBytes(Paths.get(path2));
            return Arrays.equals(bytes1, bytes2);
        } catch (IOException e) {
            // Handles cases where files don't exist or can't be read
            return false;
        }
    }
    

    /**
     * Test to verify that Validate() method processes invalid YAML files and returns invalid output.
     */
    @Test
    void testValidateMethodWithInvalidFiles() {
        YamlSchemaValidatorConfig config = mock(YamlSchemaValidatorConfig.class);
        YamlSchemaValidator yamlSchemaValidator = mock(YamlSchemaValidator.class);
        Environment environment = mock(Environment.class);

        when(config.getSchema()).thenReturn("testdata/sample-schema.json");
        when(config.getReportType()).thenReturn(ReportType.JSON);
        when(config.isColor()).thenReturn(false);

        YamlSchemaValidatorRunner runner = new YamlSchemaValidatorRunner(config, yamlSchemaValidator, environment);

        ApplicationArguments args = mock(ApplicationArguments.class);
        when(args.getNonOptionArgs()).thenReturn(List.of("testdata/invalid.yaml"));

        OutputUnit invalidOutputUnit = new OutputUnit();
        invalidOutputUnit.setValid(false);

        when(yamlSchemaValidator.validate("testdata/invalid.yaml", "testdata/sample-schema.json")).thenReturn(Collections.singletonMap("testdata/invalid.yaml", invalidOutputUnit));

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

        when(config.getSchema()).thenReturn("testdata/sample-schema.json");
        when(config.getReportType()).thenReturn(ReportType.JSON);
        when(config.isColor()).thenReturn(false);

        YamlSchemaValidatorRunner runner = new YamlSchemaValidatorRunner(config, yamlSchemaValidator, environment);

        ApplicationArguments args = mock(ApplicationArguments.class);
        when(args.getNonOptionArgs()).thenReturn(List.of("error-prone.yaml"));

        doThrow(new RuntimeException("Simulated exception")).when(yamlSchemaValidator).validate("error-prone.yaml", "testdata/sample-schema.json");

        FilesOutput result = runner.Validate(args);

        assertNotNull(result, "Expected result not to be null even if an exception is thrown during validation");
        assertFalse(result.isValid(), "Expected result to be invalid when an exception occurs");
    }
}