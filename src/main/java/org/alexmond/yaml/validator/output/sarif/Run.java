package org.alexmond.yaml.validator.output.sarif;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;

import java.util.List;

/**
 * Represents a single run of an analysis tool.
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Run {
    
    /**
     * Information about the analysis tool.
     */
    @JsonProperty("tool")
    private Tool tool;
    
    /**
     * The set of results contained in this run.
     */
    @JsonProperty("results")
    @Singular
    private List<Result> results;
    
    /**
     * Describes the invocation of the analysis tool.
     */
    @JsonProperty("invocations")
    @Singular
    private List<Invocation> invocations;
}
