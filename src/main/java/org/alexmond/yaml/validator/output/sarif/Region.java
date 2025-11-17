package org.alexmond.yaml.validator.output.sarif;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

/**
 * A region within an artifact where a result was detected.
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Region {
    
    /**
     * The line number of the first character in the region (1-based).
     */
    @JsonProperty("startLine")
    private Integer startLine;
    
    /**
     * The column number of the first character in the region (1-based).
     */
    @JsonProperty("startColumn")
    private Integer startColumn;
    
    /**
     * The line number of the last character in the region (1-based).
     */
    @JsonProperty("endLine")
    private Integer endLine;
    
    /**
     * The column number of the last character in the region (1-based).
     */
    @JsonProperty("endColumn")
    private Integer endColumn;
    
    /**
     * The portion of the artifact contents within the specified region.
     */
    @JsonProperty("snippet")
    private ArtifactContent snippet;
}
