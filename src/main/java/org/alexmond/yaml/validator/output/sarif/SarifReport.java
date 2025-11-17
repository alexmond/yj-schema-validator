package org.alexmond.yaml.validator.output.sarif;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;

import java.util.List;

/**
 * SARIF (Static Analysis Results Interchange Format) root model.
 * Represents a SARIF log file conforming to SARIF v2.1.0 specification.
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SarifReport {
    
    /**
     * The SARIF schema version.
     */
    @JsonProperty("version")
    @Builder.Default
    private String version = "2.1.0";
    
    /**
     * The URI of the JSON schema corresponding to the version.
     */
    @JsonProperty("$schema")
    @Builder.Default
    private String schema = "https://json.schemastore.org/sarif-2.1.0.json";
    
    /**
     * The set of runs contained in this log file.
     */
    @JsonProperty("runs")
    @Singular
    private List<Run> runs;
}
