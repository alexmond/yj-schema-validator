package org.alexmond.yaml.validator.output.sarif;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;

import java.util.List;

/**
 * A component of the analysis tool, such as the driver or an extension.
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ToolComponent {
    
    /**
     * The name of the tool component.
     */
    @JsonProperty("name")
    private String name;
    
    /**
     * The tool component version.
     */
    @JsonProperty("version")
    private String version;
    
    /**
     * A brief description of the tool component.
     */
    @JsonProperty("informationUri")
    private String informationUri;
    
    /**
     * The semantic version of the tool component.
     */
    @JsonProperty("semanticVersion")
    private String semanticVersion;
    
    /**
     * An array of reportingDescriptor objects relevant to the analysis performed by this run.
     */
    @JsonProperty("rules")
    @Singular
    private List<ReportingDescriptor> rules;
}
