package com.solicitation.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.time.Instant;

/**
 * System-level metadata for candidate tracking.
 * 
 * This metadata provides system-level tracking information including timestamps,
 * version number for optimistic locking, source information, and workflow traceability.
 */
@Value
@Builder(toBuilder = true)
public class CandidateMetadata {
    
    /**
     * When the candidate was created
     */
    @NotNull
    @JsonProperty("createdAt")
    Instant createdAt;
    
    /**
     * When the candidate was last updated
     */
    @NotNull
    @JsonProperty("updatedAt")
    Instant updatedAt;
    
    /**
     * When the candidate expires (for TTL)
     */
    @NotNull
    @JsonProperty("expiresAt")
    Instant expiresAt;
    
    /**
     * Version number for optimistic locking
     */
    @Positive
    @JsonProperty("version")
    Long version;
    
    /**
     * ID of the data connector that created this candidate
     */
    @NotBlank
    @JsonProperty("sourceConnectorId")
    String sourceConnectorId;
    
    /**
     * Workflow execution ID for traceability
     */
    @NotBlank
    @JsonProperty("workflowExecutionId")
    String workflowExecutionId;
}
