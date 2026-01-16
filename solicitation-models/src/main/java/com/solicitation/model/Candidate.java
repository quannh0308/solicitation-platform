package com.solicitation.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import lombok.Builder;
import lombok.Value;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

/**
 * Represents a solicitation candidate - a potential opportunity to solicit
 * customer response for a specific subject.
 * 
 * A candidate is the core data structure representing a solicitation opportunity,
 * containing all necessary information about the customer, subject, context,
 * scores, attributes, and metadata.
 * 
 * This is the canonical representation used throughout the platform for all
 * solicitation candidates across different programs and verticals.
 */
@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = Candidate.CandidateBuilder.class)
public class Candidate {
    
    /**
     * Unique identifier for the customer
     */
    @NotNull
    @JsonProperty("customerId")
    String customerId;
    
    /**
     * Multi-dimensional context describing the solicitation scenario.
     * Must contain at least one context (e.g., marketplace, program, vertical).
     * 
     * Context provides extensible dimensions for categorizing and filtering candidates.
     */
    @NotEmpty
    @Valid
    @JsonProperty("context")
    List<Context> context;
    
    /**
     * The subject being solicited for response
     */
    @NotNull
    @Valid
    @JsonProperty("subject")
    Subject subject;
    
    /**
     * Scores from various ML models.
     * Key: modelId, Value: Score object
     * 
     * Multiple scoring models can evaluate the same candidate, each producing
     * a score with value, confidence, and timestamp.
     */
    @JsonProperty("scores")
    Map<String, Score> scores;
    
    /**
     * Attributes describing the solicitation opportunity
     */
    @NotNull
    @Valid
    @JsonProperty("attributes")
    CandidateAttributes attributes;
    
    /**
     * System-level metadata for tracking
     */
    @NotNull
    @Valid
    @JsonProperty("metadata")
    CandidateMetadata metadata;
    
    /**
     * History of rejections (if any).
     * Records why this candidate was rejected by filters, providing traceability.
     */
    @JsonProperty("rejectionHistory")
    List<RejectionRecord> rejectionHistory;
    
    /**
     * Builder class for Candidate with Jackson deserialization support
     */
    @JsonPOJOBuilder(withPrefix = "")
    public static class CandidateBuilder {
    }
}
