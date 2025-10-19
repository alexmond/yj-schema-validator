package org.alexmond.yaml.validator.output;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.networknt.schema.output.OutputUnit;
import lombok.RequiredArgsConstructor;
import org.alexmond.yaml.validator.output.junit.Failure;
import org.alexmond.yaml.validator.output.junit.Testcase;
import org.alexmond.yaml.validator.output.junit.Testsuite;
import org.alexmond.yaml.validator.output.junit.Testsuites;

import java.util.Map;

/**
 * Converts validation output files to JUnit XML format for test reporting.
 * This class takes validation results and transforms them into a JUnit-compatible XML structure
 * that can be consumed by CI/CD tools and test reporting systems.
 */
@RequiredArgsConstructor
public class FilesOutputToJunit {

    private final Map<String, OutputUnit> files;

    /**
     * Converts this FilesOutput to JUnit XML format.
     *
     * @return JUnit XML string
     */
    public String toJunitString() {
        int totalTests = files.size();
        long failureCount = files.values().stream().filter(unit -> unit != null && !unit.isValid()).count();

        Testsuites testsuites = Testsuites.builder()
                .name("SchemaValidationSuite")
                .tests(totalTests)
                .failures((int) failureCount)
                .testsuite(buildTestsuite())
                .build();

        try {
            ObjectMapper xmlMapper = new XmlMapper();
            xmlMapper.enable(com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT);
            return xmlMapper.writeValueAsString(testsuites);
        } catch (Exception e) {
            throw new RuntimeException("Error converting to JUnit XML", e);
        }
    }

    /**
     * Builds a test suite from the validation results.
     * Creates individual test cases for each validated file and includes failure details
     * for files that failed validation.
     *
     * @return A Testsuite object containing all test cases
     */
    private Testsuite buildTestsuite() {
        Testsuite testsuite = Testsuite.builder()
                .name("SchemaValidationSuite")
                .tests(files.size())
                .failures((int) files.values().stream().filter(unit -> unit != null && !unit.isValid()).count())
                .testcases(new java.util.ArrayList<>())
                .build();

        java.util.List<Testcase> testcases = new java.util.ArrayList<>();
        for (Map.Entry<String, OutputUnit> entry : files.entrySet()) {
            String filename = entry.getKey();
            OutputUnit unit = entry.getValue();

            Testcase testcase = Testcase.builder()
                    .classname("files")
                    .name(filename)
                    .time(0.0)
                    .build();

            if (unit != null && !unit.isValid()) {
                String fullError = extractFullErrorMessage(unit);
                String message = extractFailureMessage(unit, fullError);

                Failure failure = Failure.builder()
                        .message(message != null ? message : "Validation Failure")
                        .value(fullError != null ? fullError : "")
                        .build();

                testcase.setFailure(failure);
            }

            testcases.add(testcase);
        }

        testsuite.setTestcases(testcases);
        return testsuite;
    }

    /**
     * Extracts the complete error message from an OutputUnit.
     * Combines both direct errors and nested detail errors into a single string.
     *
     * @param unit The OutputUnit containing validation results
     * @return A string containing the full error message
     */
    private String extractFullErrorMessage(OutputUnit unit) {
        StringBuilder sb = new StringBuilder();
        if (unit.getErrors() != null && !unit.getErrors().isEmpty()) {
            unit.getErrors().forEach((key, value) -> {
                if ("error".equals(key) && value != null) {
                    sb.append(value);
                }
            });
        }
        if (unit.getDetails() != null && !unit.getDetails().isEmpty()) {
            for (OutputUnit detail : unit.getDetails()) {
                if (detail != null && detail.getErrors() != null && !detail.getErrors().isEmpty()) {
                    detail.getErrors().forEach((key, value) -> {
                        if (value != null) {
                            sb.append(value).append("\n");
                        }
                    });
                }
            }
        }
        return sb.toString().trim();
    }

    /**
     * Extracts a concise failure message from an OutputUnit.
     * Categorizes the failure type based on the error content (schema error, YAML parse error, etc.).
     *
     * @param unit      The OutputUnit containing validation results
     * @param fullError The complete error message (not used in current implementation)
     * @return A categorized failure message
     */
    private String extractFailureMessage(OutputUnit unit, String fullError) {
        if (unit.getErrors() != null && unit.getErrors().containsKey("error")) {
            String errorMsg = (String) unit.getErrors().get("error");
            if (errorMsg != null) {
                if (errorMsg.startsWith("No schema")) {
                    return "No Schema Error";
                } else if (errorMsg.contains("MarkedYAMLException") || errorMsg.contains("YAMLException")) {
                    return "YAML Parse Error";
                }
                return "Validation Error";
            }
        } else if (unit.getDetails() != null && !unit.getDetails().isEmpty()) {
            OutputUnit firstDetail = unit.getDetails().get(0);
            if (firstDetail != null && firstDetail.getInstanceLocation() != null && firstDetail.getErrors() != null) {
                return "Type Mismatch at " + firstDetail.getInstanceLocation();
            }
        }
        return "Validation Failure";
    }
}
