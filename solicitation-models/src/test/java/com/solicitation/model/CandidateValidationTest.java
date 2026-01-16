package com.solicitation.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests validation constraints on the Candidate model and related classes.
 */
class CandidateValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void testValidCandidate() {
        // Given
        Instant now = Instant.now();
        
        Candidate candidate = createValidCandidate(now);
        
        // When
        Set<ConstraintViolation<Candidate>> violations = validator.validate(candidate);
        
        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    void testCandidateWithNullCustomerId() {
        // Given
        Instant now = Instant.now();
        
        Candidate candidate = createValidCandidate(now).toBuilder()
                .customerId(null)
                .build();
        
        // When
        Set<ConstraintViolation<Candidate>> violations = validator.validate(candidate);
        
        // Then
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("customerId"));
    }

    @Test
    void testCandidateWithEmptyContext() {
        // Given
        Instant now = Instant.now();
        
        Candidate candidate = createValidCandidate(now).toBuilder()
                .context(List.of())
                .build();
        
        // When
        Set<ConstraintViolation<Candidate>> violations = validator.validate(candidate);
        
        // Then
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("context"));
    }

    @Test
    void testCandidateWithNullSubject() {
        // Given
        Instant now = Instant.now();
        
        Candidate candidate = createValidCandidate(now).toBuilder()
                .subject(null)
                .build();
        
        // When
        Set<ConstraintViolation<Candidate>> violations = validator.validate(candidate);
        
        // Then
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("subject"));
    }

    @Test
    void testContextWithBlankType() {
        // Given
        Context context = Context.builder()
                .type("")
                .id("US")
                .build();
        
        // When
        Set<ConstraintViolation<Context>> violations = validator.validate(context);
        
        // Then
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("type"));
    }

    @Test
    void testContextWithBlankId() {
        // Given
        Context context = Context.builder()
                .type("marketplace")
                .id("")
                .build();
        
        // When
        Set<ConstraintViolation<Context>> violations = validator.validate(context);
        
        // Then
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("id"));
    }

    @Test
    void testSubjectWithBlankType() {
        // Given
        Subject subject = Subject.builder()
                .type("")
                .id("ASIN123")
                .build();
        
        // When
        Set<ConstraintViolation<Subject>> violations = validator.validate(subject);
        
        // Then
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("type"));
    }

    @Test
    void testScoreWithNullValue() {
        // Given
        Score score = Score.builder()
                .modelId("model-1")
                .value(null)
                .confidence(0.9)
                .timestamp(Instant.now())
                .build();
        
        // When
        Set<ConstraintViolation<Score>> violations = validator.validate(score);
        
        // Then
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("value"));
    }

    @Test
    void testScoreWithInvalidConfidence() {
        // Given - confidence > 1.0
        Score score = Score.builder()
                .modelId("model-1")
                .value(0.85)
                .confidence(1.5)
                .timestamp(Instant.now())
                .build();
        
        // When
        Set<ConstraintViolation<Score>> violations = validator.validate(score);
        
        // Then
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("confidence"));
    }

    @Test
    void testScoreWithNegativeConfidence() {
        // Given - confidence < 0
        Score score = Score.builder()
                .modelId("model-1")
                .value(0.85)
                .confidence(-0.1)
                .timestamp(Instant.now())
                .build();
        
        // When
        Set<ConstraintViolation<Score>> violations = validator.validate(score);
        
        // Then
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("confidence"));
    }

    @Test
    void testCandidateMetadataWithNullVersion() {
        // Given
        Instant now = Instant.now();
        
        CandidateMetadata metadata = CandidateMetadata.builder()
                .createdAt(now)
                .updatedAt(now)
                .expiresAt(now.plusSeconds(86400))
                .version(null)
                .sourceConnectorId("connector")
                .workflowExecutionId("exec")
                .build();
        
        // When
        Set<ConstraintViolation<CandidateMetadata>> violations = validator.validate(metadata);
        
        // Then
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("version"));
    }

    @Test
    void testCandidateMetadataWithZeroVersion() {
        // Given - version must be positive (> 0)
        Instant now = Instant.now();
        
        CandidateMetadata metadata = CandidateMetadata.builder()
                .createdAt(now)
                .updatedAt(now)
                .expiresAt(now.plusSeconds(86400))
                .version(0L)
                .sourceConnectorId("connector")
                .workflowExecutionId("exec")
                .build();
        
        // When
        Set<ConstraintViolation<CandidateMetadata>> violations = validator.validate(metadata);
        
        // Then
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("version"));
    }

    @Test
    void testRejectionRecordWithBlankFilterId() {
        // Given
        RejectionRecord rejection = RejectionRecord.builder()
                .filterId("")
                .reason("Test reason")
                .reasonCode("TEST")
                .timestamp(Instant.now())
                .build();
        
        // When
        Set<ConstraintViolation<RejectionRecord>> violations = validator.validate(rejection);
        
        // Then
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("filterId"));
    }

    @Test
    void testCandidateAttributesWithNullEventDate() {
        // Given
        CandidateAttributes attributes = CandidateAttributes.builder()
                .eventDate(null)
                .channelEligibility(Map.of("email", true))
                .build();
        
        // When
        Set<ConstraintViolation<CandidateAttributes>> violations = validator.validate(attributes);
        
        // Then
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("eventDate"));
    }

    @Test
    void testCandidateAttributesWithNullChannelEligibility() {
        // Given
        CandidateAttributes attributes = CandidateAttributes.builder()
                .eventDate(Instant.now())
                .channelEligibility(null)
                .build();
        
        // When
        Set<ConstraintViolation<CandidateAttributes>> violations = validator.validate(attributes);
        
        // Then
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("channelEligibility"));
    }

    // Helper method to create a valid candidate
    private Candidate createValidCandidate(Instant now) {
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
        
        return Candidate.builder()
                .customerId("customer-123")
                .context(List.of(context))
                .subject(subject)
                .attributes(attributes)
                .metadata(metadata)
                .build();
    }
}
