package org.alexmond.yaml.validator.output.sarif;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

/**
 * A location within a programming artifact.
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Location {
    
    /**
     * Identifies the artifact and region.
     */
    @JsonProperty("physicalLocation")
    private PhysicalLocation physicalLocation;
}
