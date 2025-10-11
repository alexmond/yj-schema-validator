package org.alexmond.yaml.validator.output;

import com.networknt.schema.output.OutputUnit;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * Marker interface for output formats.
 * <a href="https://json-schema.org/draft/2020-12/json-schema-core#name-output-formatting">Output Formatting</a>
 */
@Data
@Builder
public class FilesOutput {
    private Boolean valid;
    private Map<String, OutputUnit> files;
}
