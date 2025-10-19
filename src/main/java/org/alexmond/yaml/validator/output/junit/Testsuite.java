package org.alexmond.yaml.validator.output.junit;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;


/**
 * Represents a test suite in JUnit XML format.
 * This class models the XML structure for a collection of test cases,
 * including overall test execution statistics and results.
 */
@Data
@Builder
public class Testsuite {
    /**
     * Name of the test suite, defaults to "SchemaValidationSuite"
     */
    @JacksonXmlProperty(isAttribute = true)
    @Builder.Default
    private String name = "SchemaValidationSuite";

    /**
     * Base directory or file path for the test suite, defaults to "src/test/resources"
     */
    @JacksonXmlProperty(isAttribute = true)
    @Builder.Default
    private String file = "src/test/resources";

    /**
     * Total execution time of the test suite in seconds
     */
    @JacksonXmlProperty(isAttribute = true)
    @Builder.Default
    private double time = 0.0;

    /**
     * Total number of tests executed in this suite
     */
    @JacksonXmlProperty(isAttribute = true)
    private int tests;

    /**
     * Number of failed tests in this suite
     */
    @JacksonXmlProperty(isAttribute = true)
    private int failures;

    /**
     * Number of tests that resulted in errors, defaults to 0
     */
    @JacksonXmlProperty(isAttribute = true)
    @Builder.Default
    private int errors = 0;

    /**
     * Number of skipped tests in this suite, defaults to 0
     */
    @JacksonXmlProperty(isAttribute = true)
    @Builder.Default
    private int skipped = 0;

    /**
     * List of individual test cases contained in this suite
     */
    @JacksonXmlProperty(localName = "testcase")
    @Builder.Default
    private List<Testcase> testcases = new java.util.ArrayList<>();
}
