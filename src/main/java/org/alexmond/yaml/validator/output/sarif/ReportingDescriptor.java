package org.alexmond.yaml.validator.output.sarif;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

/**
 * Metadata that describes a specific report produced by the tool.
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReportingDescriptor {
    
    /**
     * A stable, opaque identifier for the report.
     */
    @JsonProperty("id")
    private String id;
    
    /**
     * A concise description of the report.
     */
    @JsonProperty("shortDescription")
    private MultiformatMessageString shortDescription;
    
    /**
     * A comprehensive description of the report.
     */
    @JsonProperty("fullDescription")
    private MultiformatMessageString fullDescription;
    
    /**
     * Help text for the report.
     */
    @JsonProperty("help")
    private MultiformatMessageString help;
    
    /**
     * Default severity level.
     */
    @JsonProperty("defaultConfiguration")
    private ReportingConfiguration defaultConfiguration;
}
