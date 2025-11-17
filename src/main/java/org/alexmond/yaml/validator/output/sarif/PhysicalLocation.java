package org.alexmond.yaml.validator.output.sarif;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

/**
 * A physical location relevant to a result.
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PhysicalLocation {
    
    /**
     * The location of the artifact.
     */
    @JsonProperty("artifactLocation")
    private ArtifactLocation artifactLocation;
    
    /**
     * Specifies a portion of the artifact.
     */
    @JsonProperty("region")
    private Region region;
}
