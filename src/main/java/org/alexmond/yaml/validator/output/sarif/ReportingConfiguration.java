package org.alexmond.yaml.validator.output.sarif;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

/**
 * Information about the configuration of a reporting descriptor.
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReportingConfiguration {
    
    /**
     * The reporting level ("warning", "error", "note", "none").
     */
    @JsonProperty("level")
    private String level;
}
