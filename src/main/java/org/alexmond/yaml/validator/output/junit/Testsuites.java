package org.alexmond.yaml.validator.output.junit;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.Builder;
import lombok.Data;

/**
 * JUnit XML model representing a collection of test suites.
 * This class models the root element of JUnit XML report format, containing
 * attributes for test execution statistics and a nested test suite.
 */
@JacksonXmlRootElement(localName = "testsuites")
@Data
@Builder
public class Testsuites {
    /**
     * The name of the test suites collection.
     * Defaults to "SchemaValidationSuite".
     */
    @JacksonXmlProperty(isAttribute = true)
    @Builder.Default
    private String name = "SchemaValidationSuite";

    /**
     * The total number of tests executed across all test suites.
     */
    @JacksonXmlProperty(isAttribute = true)
    private int tests;

    /**
     * The total number of failed tests across all test suites.
     */
    @JacksonXmlProperty(isAttribute = true)
    private int failures;

    /**
     * The total number of errors encountered during test execution.
     * Defaults to 0.
     */
    @JacksonXmlProperty(isAttribute = true)
    @Builder.Default
    private int errors = 0;

    /**
     * The total number of skipped tests across all test suites.
     * Defaults to 0.
     */
    @JacksonXmlProperty(isAttribute = true)
    @Builder.Default
    private int skipped = 0;

    /**
     * The nested test suite containing individual test cases.
     */
    @JacksonXmlProperty(localName = "testsuite")
    private Testsuite testsuite;
}
