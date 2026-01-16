package com.solicitation.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the Candidate model and related classes.
 * Tests JSON serialization/deserialization and validation.
 */
class CandidateTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    void testCandidateBuilder() {
        // Given
        Instant now = Instant.now();
        
        Context context = Context.builder()
                .type("marketplace")
                .id("US")
                .build();
        
        Subject subject = Subject.builder()
                .type("product")
                .id("ASIN123")
                .build();
        
        Map<String, Boolean> channelEligibility = new HashMap<>();
        channelEligibility.put("email", true);
        channelEligibility.put("in-app", false);
        
        CandidateAttributes attributes = CandidateAttributes.builder()
                .eventDate(now)
                .channelEligibility(channelEligibility)
                .build();
        
        CandidateMetadata metadata = CandidateMetadata.builder()
                .createdAt(now)
                .updatedAt(now)
                .expiresAt(now.plusSeconds(86400))
                .version(1L)
                .sourceConnectorId("order-connector")
                .workflowExecutionId("exec-123")
                .build();
        
        // When
        Candidate candidate = Candidate.builder()
                .customerId("customer-123")
                .context(List.of(context))
                .subject(subject)
                .attributes(attributes)
                .metadata(metadata)
                .build();
        
        // Then
        assertThat(candidate).isNotNull();
        assertThat(candidate.getCustomerId()).isEqualTo("customer-123");
        assertThat(candidate.getContext()).hasSize(1);
        assertThat(candidate.getContext().get(0).getType()).isEqualTo("marketplace");
        assertThat(candidate.getSubject().getType()).isEqualTo("product");
        assertThat(candidate.getAttributes().getChannelEligibility()).containsEntry("email", true);
        assertThat(candidate.getMetadata().getVersion()).isEqualTo(1L);
    }

    @Test
    void testCandidateJsonSerialization() throws Exception {
        // Given
        Instant now = Instant.parse("2024-01-01T00:00:00Z");
        
        Context context = Context.builder()
                .type("marketplace")
                .id("US")
                .build();
        
        Subject subject = Subject.builder()
                .type("product")
                .id("ASIN123")
                .build();
        
        Map<String, Boolean> channelEligibility = new HashMap<>();
        channelEligibility.put("email", true);
        
        CandidateAttributes attributes = CandidateAttributes.builder()
                .eventDate(now)
                .channelEligibility(channelEligibility)
                .build();
        
        CandidateMetadata metadata = CandidateMetadata.builder()
                .createdAt(now)
                .updatedAt(now)
                .expiresAt(now.plusSeconds(86400))
                .version(1L)
                .sourceConnectorId("order-connector")
                .workflowExecutionId("exec-123")
                .build();
        
        Candidate candidate = Candidate.builder()
                .customerId("customer-123")
                .context(List.of(context))
                .subject(subject)
                .attributes(attributes)
                .metadata(metadata)
                .build();
        
        // When
        String json = objectMapper.writeValueAsString(candidate);
        Candidate deserialized = objectMapper.readValue(json, Candidate.class);
        
        // Then
        assertThat(deserialized).isNotNull();
        assertThat(deserialized.getCustomerId()).isEqualTo(candidate.getCustomerId());
        assertThat(deserialized.getContext()).hasSize(1);
        assertThat(deserialized.getContext().get(0).getType()).isEqualTo("marketplace");
        assertThat(deserialized.getSubject().getId()).isEqualTo("ASIN123");
        assertThat(deserialized.getMetadata().getVersion()).isEqualTo(1L);
    }

    @Test
    void testScoreModel() {
        // Given
        Instant now = Instant.now();
        
        // When
        Score score = Score.builder()
                .modelId("quality-model-v1")
                .value(0.85)
                .confidence(0.92)
                .timestamp(now)
                .build();
        
        // Then
        assertThat(score).isNotNull();
        assertThat(score.getModelId()).isEqualTo("quality-model-v1");
        assertThat(score.getValue()).isEqualTo(0.85);
        assertThat(score.getConfidence()).isEqualTo(0.92);
        assertThat(score.getTimestamp()).isEqualTo(now);
    }

    @Test
    void testRejectionRecord() {
        // Given
        Instant now = Instant.now();
        
        // When
        RejectionRecord rejection = RejectionRecord.builder()
                .filterId("trust-filter")
                .reason("Customer has opted out")
                .reasonCode("OPT_OUT")
                .timestamp(now)
                .build();
        
        // Then
        assertThat(rejection).isNotNull();
        assertThat(rejection.getFilterId()).isEqualTo("trust-filter");
        assertThat(rejection.getReason()).isEqualTo("Customer has opted out");
        assertThat(rejection.getReasonCode()).isEqualTo("OPT_OUT");
    }

    @Test
    void testContextExtensibility() {
        // Given - multiple context dimensions
        Context marketplace = Context.builder()
                .type("marketplace")
                .id("US")
                .build();
        
        Context program = Context.builder()
                .type("program")
                .id("review-solicitation")
                .build();
        
        Context vertical = Context.builder()
                .type("vertical")
                .id("retail")
                .build();
        
        // When
        List<Context> contexts = List.of(marketplace, program, vertical);
        
        // Then - all contexts should be preserved
        assertThat(contexts).hasSize(3);
        assertThat(contexts).extracting(Context::getType)
                .containsExactly("marketplace", "program", "vertical");
    }

    @Test
    void testCandidateWithScores() {
        // Given
        Instant now = Instant.now();
        
        Score qualityScore = Score.builder()
                .modelId("quality-model")
                .value(0.85)
                .confidence(0.9)
                .timestamp(now)
                .build();
        
        Score engagementScore = Score.builder()
                .modelId("engagement-model")
                .value(0.72)
                .confidence(0.88)
                .timestamp(now)
                .build();
        
        Map<String, Score> scores = new HashMap<>();
        scores.put("quality-model", qualityScore);
        scores.put("engagement-model", engagementScore);
        
        Context context = Context.builder()
                .type("marketplace")
                .id("US")
                .build();
        
        Subject subject = Subject.builder()
                .type("product")
                .id("ASIN123")
                .build();
        
        CandidateAttributes attributes = CandidateAttributes.builder()
                .eventDate(now)
                .channelEligibility(Map.of("email", true))
                .build();
        
        CandidateMetadata metadata = CandidateMetadata.builder()
                .createdAt(now)
                .updatedAt(now)
                .expiresAt(now.plusSeconds(86400))
                .version(1L)
                .sourceConnectorId("connector")
                .workflowExecutionId("exec")
                .build();
        
        // When
        Candidate candidate = Candidate.builder()
                .customerId("customer-123")
                .context(List.of(context))
                .subject(subject)
                .scores(scores)
                .attributes(attributes)
                .metadata(metadata)
                .build();
        
        // Then
        assertThat(candidate.getScores()).hasSize(2);
        assertThat(candidate.getScores()).containsKeys("quality-model", "engagement-model");
        assertThat(candidate.getScores().get("quality-model").getValue()).isEqualTo(0.85);
    }

    @Test
    void testCandidateToBuilder() {
        // Given
        Instant now = Instant.now();
        
        Context context = Context.builder()
                .type("marketplace")
                .id("US")
                .build();
        
        Subject subject = Subject.builder()
                .type("product")
                .id("ASIN123")
                .build();
        
        CandidateAttributes attributes = CandidateAttributes.builder()
                .eventDate(now)
                .channelEligibility(Map.of("email", true))
                .build();
        
        CandidateMetadata metadata = CandidateMetadata.builder()
                .createdAt(now)
                .updatedAt(now)
                .expiresAt(now.plusSeconds(86400))
                .version(1L)
                .sourceConnectorId("connector")
                .workflowExecutionId("exec")
                .build();
        
        Candidate original = Candidate.builder()
                .customerId("customer-123")
                .context(List.of(context))
                .subject(subject)
                .attributes(attributes)
                .metadata(metadata)
                .build();
        
        // When - update metadata with new version
        CandidateMetadata updatedMetadata = metadata.toBuilder()
                .version(2L)
                .updatedAt(now.plusSeconds(60))
                .build();
        
        Candidate updated = original.toBuilder()
                .metadata(updatedMetadata)
                .build();
        
        // Then
        assertThat(updated.getMetadata().getVersion()).isEqualTo(2L);
        assertThat(updated.getMetadata().getUpdatedAt()).isAfter(original.getMetadata().getUpdatedAt());
        assertThat(updated.getCustomerId()).isEqualTo(original.getCustomerId());
    }
}
