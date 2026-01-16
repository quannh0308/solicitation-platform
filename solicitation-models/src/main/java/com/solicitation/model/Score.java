package com.solicitation.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.Instant;
import java.util.Map;

/**
 * Represents a score from an ML model evaluating candidate quality.
 * 
 * Scores provide evaluation of candidate quality with value, confidence, and timestamp.
 * Multiple scores from different models can be associated with a single candidate.
 */
@Value
@Builder
public class Score {
    
    /**
     * Identifier of the model that produced this score
     */
    @NotBlank
    @JsonProperty("modelId")
    String modelId;
    
    /**
     * Score value (typically 0.0 to 1.0)
     */
    @NotNull
    @JsonProperty("value")
    Double value;
    
    /**
     * Confidence in the score (0.0 to 1.0)
     */
    @Min(0)
    @Max(1)
    @JsonProperty("confidence")
    Double confidence;
    
    /**
     * When the score was computed
     */
    @NotNull
    @JsonProperty("timestamp")
    Instant timestamp;
    
    /**
     * Optional metadata about the scoring
     */
    @JsonProperty("metadata")
    Map<String, Object> metadata;
}
