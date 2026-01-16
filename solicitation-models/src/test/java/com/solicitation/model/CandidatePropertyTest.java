package com.solicitation.model;

import com.solicitation.model.arbitraries.CandidateArbitraries;
import net.jqwik.api.*;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based tests for Candidate model completeness.
 * 
 * Tests Property 2: Candidate model completeness
 * Validates: Requirements 2.1, 2.2
 */
class CandidatePropertyTest {
    
    private static final Validator validator = Validation.byDefaultProvider()
        .configure()
        .messageInterpolator(new ParameterMessageInterpolator())
        .buildValidatorFactory()
        .getValidator();
    
    /**
     * Property 2: Candidate model completeness
     * 
     * **Validates: Requirements 2.1, 2.2**
     * 
     * For any candidate stored in the system, it must contain all required fields:
     * - customerId
     * - context array (with at least one context)
     * - subject
     * - metadata with timestamps
     * - version number
     * - attributes with eventDate and channelEligibility
     */
    @Property(tries = 100)
    @Label("Property 2: All valid candidates must have all required fields present")
    void allValidCandidatesMustHaveRequiredFields(@ForAll("validCandidates") Candidate candidate) {
        // Verify all required fields are present
        assertThat(candidate.getCustomerId())
            .as("customerId must not be null")
            .isNotNull();
        
        assertThat(candidate.getContext())
            .as("context must not be null or empty")
            .isNotNull()
            .isNotEmpty();
        
        assertThat(candidate.getSubject())
            .as("subject must not be null")
            .isNotNull();
        
        assertThat(candidate.getAttributes())
            .as("attributes must not be null")
            .isNotNull();
        
        assertThat(candidate.getMetadata())
            .as("metadata must not be null")
            .isNotNull();
        
        // Verify nested required fields in attributes
        assertThat(candidate.getAttributes().getEventDate())
            .as("attributes.eventDate must not be null")
            .isNotNull();
        
        assertThat(candidate.getAttributes().getChannelEligibility())
            .as("attributes.channelEligibility must not be null")
            .isNotNull();
        
        // Verify nested required fields in metadata
        assertThat(candidate.getMetadata().getCreatedAt())
            .as("metadata.createdAt must not be null")
            .isNotNull();
        
        assertThat(candidate.getMetadata().getUpdatedAt())
            .as("metadata.updatedAt must not be null")
            .isNotNull();
        
        assertThat(candidate.getMetadata().getExpiresAt())
            .as("metadata.expiresAt must not be null")
            .isNotNull();
        
        assertThat(candidate.getMetadata().getVersion())
            .as("metadata.version must not be null and must be positive")
            .isNotNull()
            .isPositive();
        
        assertThat(candidate.getMetadata().getSourceConnectorId())
            .as("metadata.sourceConnectorId must not be null or blank")
            .isNotNull()
            .isNotBlank();
        
        assertThat(candidate.getMetadata().getWorkflowExecutionId())
            .as("metadata.workflowExecutionId must not be null or blank")
            .isNotNull()
            .isNotBlank();
        
        // Verify nested required fields in subject
        assertThat(candidate.getSubject().getType())
            .as("subject.type must not be null or blank")
            .isNotNull()
            .isNotBlank();
        
        assertThat(candidate.getSubject().getId())
            .as("subject.id must not be null or blank")
            .isNotNull()
            .isNotBlank();
        
        // Verify context elements have required fields
        for (Context context : candidate.getContext()) {
            assertThat(context.getType())
                .as("context.type must not be null or blank")
                .isNotNull()
                .isNotBlank();
            
            assertThat(context.getId())
                .as("context.id must not be null or blank")
                .isNotNull()
                .isNotBlank();
        }
    }
    
    /**
     * Property 2: Candidate model completeness - Validation enforcement
     * 
     * **Validates: Requirements 2.1, 2.2**
     * 
     * When a candidate is created with missing required fields,
     * the validation must catch and report the missing fields.
     */
    @Property(tries = 100)
    @Label("Property 2: Validation must catch candidates with missing required fields")
    void validationMustCatchMissingRequiredFields(@ForAll("candidatesWithMissingFields") Candidate candidate) {
        // Validate the candidate
        Set<ConstraintViolation<Candidate>> violations = validator.validate(candidate);
        
        // There must be at least one validation violation
        assertThat(violations)
            .as("Candidates with missing required fields must have validation violations")
            .isNotEmpty();
        
        // Verify that the violation is for a required field
        boolean hasRequiredFieldViolation = violations.stream()
            .anyMatch(violation -> {
                String propertyPath = violation.getPropertyPath().toString();
                String message = violation.getMessage();
                
                // Check if it's a violation for a required field
                return propertyPath.equals("customerId") ||
                       propertyPath.equals("context") ||
                       propertyPath.equals("subject") ||
                       propertyPath.equals("attributes") ||
                       propertyPath.equals("metadata") ||
                       message.contains("must not be null") ||
                       message.contains("must not be empty");
            });
        
        assertThat(hasRequiredFieldViolation)
            .as("Validation violations must include at least one required field violation")
            .isTrue();
    }
    
    /**
     * Property 2: Candidate model completeness - Valid candidates pass validation
     * 
     * **Validates: Requirements 2.1, 2.2**
     * 
     * All candidates with complete required fields must pass validation.
     */
    @Property(tries = 100)
    @Label("Property 2: Valid candidates with all required fields must pass validation")
    void validCandidatesPassValidation(@ForAll("validCandidates") Candidate candidate) {
        // Validate the candidate
        Set<ConstraintViolation<Candidate>> violations = validator.validate(candidate);
        
        // There should be no validation violations for valid candidates
        assertThat(violations)
            .as("Valid candidates with all required fields must have no validation violations")
            .isEmpty();
    }
    
    // Arbitrary providers
    
    @Provide
    Arbitrary<Candidate> validCandidates() {
        return CandidateArbitraries.validCandidates();
    }
    
    @Provide
    Arbitrary<Candidate> candidatesWithMissingFields() {
        return CandidateArbitraries.candidatesWithMissingFields();
    }
}
