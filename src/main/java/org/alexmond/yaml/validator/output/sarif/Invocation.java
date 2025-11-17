package org.alexmond.yaml.validator.output.sarif;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

/**
 * Describes the invocation of the analysis tool.
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Invocation {
    
    /**
     * Specifies whether the tool's execution completed successfully.
     */
    @JsonProperty("executionSuccessful")
    private Boolean executionSuccessful;
    
    /**
     * The start time of the invocation.
     */
    @JsonProperty("startTimeUtc")
    private String startTimeUtc;
    
    /**
     * The end time of the invocation.
     */
    @JsonProperty("endTimeUtc")
    private String endTimeUtc;
    
    /**
     * The process exit code.
     */
    @JsonProperty("exitCode")
    private Integer exitCode;
}
