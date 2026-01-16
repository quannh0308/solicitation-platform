package com.solicitation.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

import javax.validation.constraints.NotBlank;
import java.util.Map;

/**
 * Represents the subject being solicited for response.
 * Examples: product, video, music track, service, event, experience, content piece
 * 
 * The subject is the item for which customer feedback or response is being solicited.
 */
@Value
@Builder
public class Subject {
    
    /**
     * Type of subject (e.g., "product", "video", "track", "service", "event")
     */
    @NotBlank
    @JsonProperty("type")
    String type;
    
    /**
     * Unique identifier for the subject
     */
    @NotBlank
    @JsonProperty("id")
    String id;
    
    /**
     * Optional metadata about the subject
     */
    @JsonProperty("metadata")
    Map<String, Object> metadata;
}
