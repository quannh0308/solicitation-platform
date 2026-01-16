package com.solicitation.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

import javax.validation.constraints.NotBlank;

/**
 * Represents a dimension of context for a candidate.
 * Examples: marketplace, program, vertical
 * 
 * Context provides multi-dimensional metadata describing the solicitation scenario.
 * Each context has a type (e.g., "marketplace", "program") and an identifier.
 */
@Value
@Builder
public class Context {
    
    /**
     * Type of context (e.g., "marketplace", "program", "vertical")
     */
    @NotBlank
    @JsonProperty("type")
    String type;
    
    /**
     * Identifier for this context dimension
     */
    @NotBlank
    @JsonProperty("id")
    String id;
}
