package org.alexmond.yaml.validator.output.sarif;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

/**
 * Specifies the location of an artifact.
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ArtifactLocation {
    
    /**
     * A string containing a valid relative or absolute URI.
     */
    @JsonProperty("uri")
    private String uri;
    
    /**
     * A string which indirectly specifies the absolute URI.
     */
    @JsonProperty("uriBaseId")
    private String uriBaseId;
}
