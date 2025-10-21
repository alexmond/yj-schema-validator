package org.alexmond.yaml.validator.output;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.networknt.schema.output.OutputUnit;
import lombok.Data;

import org.springframework.boot.ansi.AnsiColor;
import org.springframework.boot.ansi.AnsiOutput;

import java.util.Map;

/**
 * Represents the output format for file validation results.
 * This class handles the structured output of validation results for multiple files,
 * providing various output formats including colored console output, JSON, YAML, and JUnit XML.
 *
 * @see <a href="https://json-schema.org/draft/2020-12/json-schema-core#name-output-formatting">Output Formatting</a>
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonRootName("")
@JsonPropertyOrder({"valid", "files"})
@JsonIgnoreProperties(ignoreUnknown = true)
public class FilesOutput {

    /**
     * Indicates whether all files in the validation set are valid.
     */
    private boolean valid;
    /**
     * Maps filenames to their corresponding validation results.
     */
    private Map<String, OutputUnit> files;

    /**
     * Constructs a new FilesOutput instance with the given validation results.
     *
     * @param files Map of filename to validation results
     */
    public FilesOutput(Map<String, OutputUnit> files) {
        this.files = files;
        this.valid = files.values().stream().allMatch(OutputUnit::isValid);
    }

    // Existing toColoredString, toJsonString, toYamlString methods unchanged...

    /**
     * Converts the validation results to a human-readable string with optional ANSI color formatting.
     *
     * @param color true to enable ANSI color output, false for plain text
     * @return formatted string representation of the validation results
     */

    public String toColoredString(boolean color) {
        AnsiOutput.Enabled previous = AnsiOutput.getEnabled();
        AnsiOutput.setEnabled(color ? AnsiOutput.Enabled.ALWAYS : AnsiOutput.Enabled.NEVER);
        try {
            StringBuilder result = new StringBuilder();
            result.append("Validation Result: ");
            if (valid) {
                result.append(AnsiOutput.toString(AnsiColor.GREEN, "ok", AnsiColor.DEFAULT));
            } else {
                result.append(AnsiOutput.toString(AnsiColor.RED, "invalid", AnsiColor.DEFAULT));
            }
            result.append("\n");

            files.forEach((filename, output) -> {
                result.append(filename).append(": ");
                if (output.isValid()) {
                    result.append(AnsiOutput.toString(AnsiColor.GREEN, "ok", AnsiColor.DEFAULT));
                } else {
                    result.append(AnsiOutput.toString(AnsiColor.RED, "invalid", AnsiColor.DEFAULT));
                }
                result.append("\n");

                if (!output.isValid() && output.getErrors() != null) {
                    output.getErrors().forEach((label, message) -> {
                        result.append(" " + label + ": ").append(message).append("\n");
                    });
                }

                if (!output.isValid() && output.getDetails() != null) {
                    output.getDetails().forEach(detail -> {
                        result.append(" Details:\n");
                        result.append(" Path: ").append(detail.getInstanceLocation()).append("\n");
                        result.append(" Schema: ").append(detail.getSchemaLocation()).append("\n");
                        if (detail.getErrors() != null) {
                            detail.getErrors().forEach((label, message) -> {
                                result.append(" ").append(label).append(": ").append(message).append("\n");
                            });
                        }
                    });
                }
            });

            return result.toString();
        } finally {
            AnsiOutput.setEnabled(previous);
        }
    }

    /**
     * Converts the validation results to a JSON string representation.
     *
     * @return JSON string of the validation results
     * @throws RuntimeException if JSON conversion fails
     */
    public String toJsonString() {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(this);
        } catch (Exception e) {
            throw new RuntimeException("Error converting to JSON", e);
        }
    }

    /**
     * Converts the validation results to a YAML string representation.
     *
     * @return YAML string of the validation results
     * @throws RuntimeException if YAML conversion fails
     */
    public String toYamlString() {
        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(this);
        } catch (Exception e) {
            throw new RuntimeException("Error converting to YAML", e);
        }
    }

    /**
     * Converts the validation results to JUnit XML format.
     *
     * @return JUnit XML string representation of the validation results
     */
    public String toJunitString() {
        FilesOutputToJunit junitOutput = new FilesOutputToJunit(files);
        return junitOutput.toJunitString();
    }

}
