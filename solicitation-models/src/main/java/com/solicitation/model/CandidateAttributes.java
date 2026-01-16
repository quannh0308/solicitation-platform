package com.solicitation.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

import javax.validation.constraints.NotNull;
import java.time.Instant;
import java.util.Map;

/**
 * Attributes describing the solicitation opportunity.
 * 
 * These attributes provide properties about the solicitation such as event date,
 * delivery date, channel eligibility, and other business-specific metadata.
 */
@Value
@Builder
public class CandidateAttributes {
    
    /**
     * When the triggering event occurred
     */
    @NotNull
    @JsonProperty("eventDate")
    Instant eventDate;
    
    /**
     * Preferred delivery date (optional)
     */
    @JsonProperty("deliveryDate")
    Instant deliveryDate;
    
    /**
     * Timing window for solicitation (optional)
     */
    @JsonProperty("timingWindow")
    String timingWindow;
    
    /**
     * Order value if applicable (optional)
     */
    @JsonProperty("orderValue")
    Double orderValue;
    
    /**
     * Whether media (images/video) is eligible
     */
    @JsonProperty("mediaEligible")
    Boolean mediaEligible;
    
    /**
     * Channel eligibility flags.
     * Key: channel name (e.g., "email", "in-app", "push")
     * Value: true if eligible for that channel
     */
    @NotNull
    @JsonProperty("channelEligibility")
    Map<String, Boolean> channelEligibility;
}
