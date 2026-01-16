package com.solicitation.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.Instant;

/**
 * Records why a candidate was rejected by a filter.
 * 
 * Rejection records provide traceability for why candidates were filtered out,
 * including the filter that rejected them, the reason, and when it occurred.
 */
@Value
@Builder
public class RejectionRecord {
    
    /**
     * ID of the filter that rejected the candidate
     */
    @NotBlank
    @JsonProperty("filterId")
    String filterId;
    
    /**
     * Human-readable rejection reason
     */
    @NotBlank
    @JsonProperty("reason")
    String reason;
    
    /**
     * Machine-readable reason code
     */
    @NotBlank
    @JsonProperty("reasonCode")
    String reasonCode;
    
    /**
     * When the rejection occurred
     */
    @NotNull
    @JsonProperty("timestamp")
    Instant timestamp;
}
