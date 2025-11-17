package org.alexmond.yaml.validator.output.sarif;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

/**
 * A message string or message format string in multiple formats.
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MultiformatMessageString {
    
    /**
     * A plain text message string.
     */
    @JsonProperty("text")
    private String text;
    
    /**
     * A Markdown message string.
     */
    @JsonProperty("markdown")
    private String markdown;
}
