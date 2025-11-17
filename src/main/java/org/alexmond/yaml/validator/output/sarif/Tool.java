package org.alexmond.yaml.validator.output.sarif;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

/**
 * The analysis tool that was run.
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Tool {
    
    /**
     * The analysis tool driver (core analysis tool).
     */
    @JsonProperty("driver")
    private ToolComponent driver;
}
