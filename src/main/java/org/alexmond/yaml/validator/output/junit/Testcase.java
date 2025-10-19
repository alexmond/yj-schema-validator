package org.alexmond.yaml.validator.output.junit;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.Builder;
import lombok.Data;


/**
 * JUnit XML model for Testcase.
 * Represents a single test case in JUnit XML report format.
 * This class is used for serialization of test results into XML format
 * compatible with JUnit report specifications.
 */
@JacksonXmlRootElement(localName = "testcase")
@Data
@Builder
public class Testcase {
    /**
     * The name of the class containing the test case.
     * Defaults to "files" if not specified.
     */
    @JacksonXmlProperty(isAttribute = true)
    @Builder.Default
    private String classname = "files";

    /**
     * The name of the test case.
     */
    @JacksonXmlProperty(isAttribute = true)
    private String name;

    /**
     * The execution time of the test case in seconds.
     * Defaults to 0.0 if not specified.
     */
    @JacksonXmlProperty(isAttribute = true)
    @Builder.Default
    private double time = 0.0;

    /**
     * The failure details if the test case failed.
     * Null if the test case passed.
     */
    @JacksonXmlProperty(localName = "failure")
    private Failure failure;
}
