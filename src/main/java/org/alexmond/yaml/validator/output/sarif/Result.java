package org.alexmond.yaml.validator.output.sarif;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;

import java.util.List;

/**
 * A result produced by an analysis tool.
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Result {
    
    /**
     * The stable, unique identifier of the rule.
     */
    @JsonProperty("ruleId")
    private String ruleId;
    
    /**
     * The severity level of the result.
     * Possible values: "none", "note", "warning", "error"
     */
    @JsonProperty("level")
    @Builder.Default
    private String level = "warning";
    
    /**
     * A message that describes the result.
     */
    @JsonProperty("message")
    private Message message;
    
    /**
     * The set of locations where the result occurred.
     */
    @JsonProperty("locations")
    @Singular
    private List<Location> locations;
}
