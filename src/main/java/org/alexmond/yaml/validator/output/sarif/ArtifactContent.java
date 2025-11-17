package org.alexmond.yaml.validator.output.sarif;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

/**
 * Represents content from an artifact.
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ArtifactContent {
    
    /**
     * UTF-8-encoded content from an artifact.
     */
    @JsonProperty("text")
    private String text;
}
