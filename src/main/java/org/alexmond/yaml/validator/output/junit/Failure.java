package org.alexmond.yaml.validator.output.junit;


import lombok.Builder;
import lombok.Data;
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import tools.jackson.dataformat.xml.annotation.JacksonXmlText;


/**
 * JUnit XML model for Failure.
 * Represents a test failure in JUnit XML reports.
 * This class is used for XML serialization/deserialization of test failures
 * using Jackson XML annotations.
 */
@Data
@Builder
public class Failure {
    /**
     * The failure message describing what went wrong.
     * Maps to the 'message' attribute in XML.
     */
    @JacksonXmlProperty(isAttribute = true)
    private String message;

    /**
     * The detailed failure content or stack trace.
     * Maps to the text content of the failure element in XML.
     */
    @JacksonXmlText
    private String value;
}
