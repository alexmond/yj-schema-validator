package org.alexmond.yaml.validator.output;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.output.OutputUnit;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * Marker interface for output formats.
 * <a href="https://json-schema.org/draft/2020-12/json-schema-core#name-output-formatting">Output Formatting</a>
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonRootName("")
@JsonPropertyOrder({"valid", "files"})
@JsonIgnoreProperties(ignoreUnknown = true)
public class FilesOutput {

    public FilesOutput(Map<String, OutputUnit> files) {
        this.files = files;
        this.valid = files.values().stream().allMatch(OutputUnit::isValid);
    }

    private boolean valid;
    private Map<String, OutputUnit> files;

    public String toColoredString(boolean color) {
        String ansiGreen;
        String ansiRed;
        String ansiReset;
        
        if (!color) {
            ansiGreen = "";
            ansiRed = "";
            ansiReset = "";
        } else {
            ansiReset = "\u001B[0m";
            ansiRed = "\u001B[31m";
            ansiGreen = "\u001B[32m";
        }

        StringBuilder result = new StringBuilder();
        result.append("Validation Result: ");
        result.append(valid ?
                ansiGreen + "ok" :
                ansiRed + "invalid").append(ansiReset).append("\n");

        files.forEach((filename, output) -> {
            result.append(filename).append(": ");
            result.append(output.isValid() ?
                    ansiGreen + "ok" :
                    ansiRed + "invalid").append(ansiReset).append("\n");
            if (!output.isValid() && output.getErrors() != null) {
                output.getErrors().forEach((label,message) -> {
                    result.append("  " + label + ": ").append(message).append("\n");
                });
            }
            if (!output.isValid() && output.getDetails() != null) {
                output.getDetails().forEach(detail -> {
                    result.append("  Details:\n");
                    result.append("    Path: ").append(detail.getInstanceLocation()).append("\n");
                    result.append("    Schema: ").append(detail.getSchemaLocation()).append("\n");
                    if (detail.getErrors() != null) {
                        detail.getErrors().forEach((label,message) -> {
                            result.append("    ").append(label).append(": ").append(message).append("\n");
                        });
                    }
                });
            }
        });
        return result.toString();
    }

    public String toJsonString() {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(this);
        } catch (Exception e) {
            throw new RuntimeException("Error converting to JSON", e);
        }
    }

}
