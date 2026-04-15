package org.alexmond.yaml.validator.output;

import com.networknt.schema.output.OutputUnit;
import lombok.RequiredArgsConstructor;
import org.alexmond.yaml.validator.output.sarif.*;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Converts validation output files to SARIF (Static Analysis Results Interchange Format) format.
 * This class takes validation results and transforms them into a SARIF-compatible JSON structure
 * that can be consumed by CI/CD tools, security scanners, and code analysis systems.
 */
@RequiredArgsConstructor
public class FilesOutputToSarif {

    private final Map<String, OutputUnit> files;

    /**
     * Converts this FilesOutput to SARIF JSON format.
     *
     * @return SARIF JSON string
     */
    public String toSarifString() {
        SarifReport sarifReport = SarifReport.builder()
                .version("2.1.0")
                .schema("https://json.schemastore.org/sarif-2.1.0.json")
                .run(buildRun())
                .build();

        try {
            JsonMapper jsonMapper = JsonMapper.builder()
                    .configure(SerializationFeature.INDENT_OUTPUT, true)
                    .build();
            return jsonMapper.writeValueAsString(sarifReport);
        } catch (Exception e) {
            throw new RuntimeException("Error converting to SARIF JSON", e);
        }
    }

    /**
     * Builds a SARIF run from the validation results.
     * Creates the tool information, results, and invocation details.
     *
     * @return A Run object containing all analysis results
     */
    private Run buildRun() {
        String startTime = Instant.now().toString();
        List<Result> results = buildResults();
        boolean executionSuccessful = files.values().stream().allMatch(unit -> unit != null && unit.isValid());

        return Run.builder()
                .tool(buildTool())
                .results(results)
                .invocation(buildInvocation(executionSuccessful, startTime))
                .build();
    }

    /**
     * Builds the tool information for the SARIF report.
     *
     * @return A Tool object with driver information
     */
    private Tool buildTool() {
        ToolComponent driver = ToolComponent.builder()
                .name("YAML Schema Validator")
                .version("1.0.0")
                .informationUri("https://github.com/alexmond/yj-schema-validator")
                .semanticVersion("1.0.0")
                .rule(buildRule())
                .build();

        return Tool.builder()
                .driver(driver)
                .build();
    }

    /**
     * Builds the reporting descriptor (rule) for schema validation.
     *
     * @return A ReportingDescriptor object defining the validation rule
     */
    private ReportingDescriptor buildRule() {
        return ReportingDescriptor.builder()
                .id("schema-validation")
                .shortDescription(MultiformatMessageString.builder()
                        .text("Schema validation error")
                        .build())
                .fullDescription(MultiformatMessageString.builder()
                        .text("The file does not conform to the specified JSON/YAML schema")
                        .build())
                .help(MultiformatMessageString.builder()
                        .text("Ensure that the file content matches the schema definition")
                        .build())
                .defaultConfiguration(ReportingConfiguration.builder()
                        .level("error")
                        .build())
                .build();
    }

    /**
     * Builds the list of results from the validation output.
     * Each invalid file generates one or more SARIF results.
     *
     * @return List of Result objects
     */
    private List<Result> buildResults() {
        List<Result> results = new ArrayList<>();

        for (Map.Entry<String, OutputUnit> entry : files.entrySet()) {
            String filename = entry.getKey();
            OutputUnit unit = entry.getValue();

            if (unit != null && !unit.isValid()) {
                results.addAll(buildResultsForFile(filename, unit));
            }
        }

        return results;
    }

    /**
     * Builds SARIF results for a single file.
     * Creates detailed results for each validation error found in the file.
     *
     * @param filename The name of the file being validated
     * @param unit     The OutputUnit containing validation results
     * @return List of Result objects for this file
     */
    private List<Result> buildResultsForFile(String filename, OutputUnit unit) {
        List<Result> results = new ArrayList<>();

        // Handle top-level errors
        if (unit.getErrors() != null && !unit.getErrors().isEmpty()) {
            String errorMessage = extractErrorMessage(unit);
            results.add(buildResult(filename, errorMessage, null, null));
        }

        // Handle detailed errors
        if (unit.getDetails() != null && !unit.getDetails().isEmpty()) {
            for (OutputUnit detail : unit.getDetails()) {
                if (detail != null && !detail.isValid()) {
                    String message = extractDetailErrorMessage(detail);
                    String instanceLocation = detail.getInstanceLocation();
                    Integer lineNumber = extractLineNumber(detail);
                    
                    results.add(buildResult(filename, message, instanceLocation, lineNumber));
                }
            }
        }

        return results;
    }

    /**
     * Builds a single SARIF result.
     *
     * @param filename         The file where the issue was found
     * @param message          The error message
     * @param instanceLocation The JSON path where the error occurred
     * @param lineNumber       The line number (if available)
     * @return A Result object
     */
    private Result buildResult(String filename, String message, String instanceLocation, Integer lineNumber) {
        return Result.builder()
                .ruleId("schema-validation")
                .level("error")
                .message(Message.builder()
                        .text(message)
                        .build())
                .location(buildLocation(filename, instanceLocation, lineNumber))
                .build();
    }

    /**
     * Builds a SARIF location for a result.
     *
     * @param filename         The file path
     * @param instanceLocation The JSON path (optional)
     * @param lineNumber       The line number (optional)
     * @return A Location object
     */
    private Location buildLocation(String filename, String instanceLocation, Integer lineNumber) {
        Region.RegionBuilder regionBuilder = Region.builder();
        
        if (lineNumber != null) {
            regionBuilder.startLine(lineNumber);
        }
        
        if (instanceLocation != null) {
            regionBuilder.snippet(ArtifactContent.builder()
                    .text("Path: " + instanceLocation)
                    .build());
        }

        return Location.builder()
                .physicalLocation(PhysicalLocation.builder()
                        .artifactLocation(ArtifactLocation.builder()
                                .uri(filename)
                                .build())
                        .region(regionBuilder.build())
                        .build())
                .build();
    }

    /**
     * Builds the invocation information for the SARIF report.
     *
     * @param executionSuccessful Whether the validation execution completed successfully
     * @param startTime           The start time of the validation
     * @return An Invocation object
     */
    private Invocation buildInvocation(boolean executionSuccessful, String startTime) {
        return Invocation.builder()
                .executionSuccessful(executionSuccessful)
                .startTimeUtc(startTime)
                .endTimeUtc(Instant.now().toString())
                .exitCode(executionSuccessful ? 0 : 1)
                .build();
    }

    /**
     * Extracts the error message from an OutputUnit.
     *
     * @param unit The OutputUnit containing validation results
     * @return The error message
     */
    private String extractErrorMessage(OutputUnit unit) {
        if (unit.getErrors() != null && unit.getErrors().containsKey("error")) {
            Object errorObj = unit.getErrors().get("error");
            return errorObj != null ? errorObj.toString() : "Validation error";
        }
        return "Validation error";
    }

    /**
     * Extracts the detailed error message from an OutputUnit.
     *
     * @param detail The OutputUnit detail containing specific validation error
     * @return The detailed error message
     */
    private String extractDetailErrorMessage(OutputUnit detail) {
        StringBuilder message = new StringBuilder();
        
        if (detail.getInstanceLocation() != null) {
            message.append("At path '").append(detail.getInstanceLocation()).append("': ");
        }
        
        if (detail.getErrors() != null && !detail.getErrors().isEmpty()) {
            detail.getErrors().forEach((key, value) -> {
                if (value != null) {
                    message.append(value).append(" ");
                }
            });
        } else {
            message.append("Validation error");
        }
        
        return message.toString().trim();
    }

    /**
     * Attempts to extract line number information from an OutputUnit.
     * Note: Line numbers may not always be available depending on the validation library.
     *
     * @param detail The OutputUnit detail
     * @return The line number if available, null otherwise
     */
    private Integer extractLineNumber(OutputUnit detail) {
        // Line number extraction depends on the schema validator implementation
        // This is a placeholder - adjust based on actual data availability
        return null;
    }
}
