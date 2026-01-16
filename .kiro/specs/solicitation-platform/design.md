# Design Document - Task 2: Implement Core Data Models

## Task Context

**Current Task**: Task 2 - Implement core data models
**Focus**: Create Java classes for Candidate, Context, Subject, Score, and configuration models

## Overview

Task 2 implements the foundational data models that represent solicitation candidates and program configurations. These models serve as the canonical representation used throughout the platform.

### Design Principles for Task 2

1. **Immutability**: Use immutable objects where possible to prevent accidental modifications
2. **Validation**: Validate all required fields at construction time
3. **Extensibility**: Support arbitrary context dimensions and score types
4. **Serialization**: Ensure proper JSON serialization for storage and API responses
5. **Type Safety**: Use strong typing to prevent errors at compile time

## Data Models

### Candidate Model

The Candidate model is the core data structure representing a solicitation opportunity.

```java
package com.solicitation.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import lombok.Builder;
import lombok.Value;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.util.List;
import java.util.Map;

/**
 * Represents a solicitation candidate - a potential opportunity to solicit
 * customer response for a specific subject.
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
     * Multi-dimensional context describing the solicitation scenario
     * Must contain at least one context (e.g., marketplace, program, vertical)
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
     * Scores from various ML models
     * Key: modelId, Value: Score object
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
     * History of rejections (if any)
     */
    @JsonProperty("rejectionHistory")
    List<RejectionRecord> rejectionHistory;
    
    @JsonPOJOBuilder(withPrefix = "")
    public static class CandidateBuilder {
    }
}
```

### Context Model

```java
package com.solicitation.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

import javax.validation.constraints.NotBlank;

/**
 * Represents a dimension of context for a candidate
 * Examples: marketplace, program, vertical
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
```

### Subject Model

```java
package com.solicitation.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

import javax.validation.constraints.NotBlank;
import java.util.Map;

/**
 * Represents the subject being solicited for response
 * Examples: product, video, music track, service, event
 */
@Value
@Builder
public class Subject {
    
    /**
     * Type of subject (e.g., "product", "video", "track", "service")
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
```

### Score Model

```java
package com.solicitation.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.Instant;

/**
 * Represents a score from an ML model
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
```

### CandidateAttributes Model

```java
package com.solicitation.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

import javax.validation.constraints.NotNull;
import java.time.Instant;
import java.util.Map;

/**
 * Attributes describing the solicitation opportunity
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
     * Channel eligibility flags
     * Key: channel name (e.g., "email", "in-app", "push")
     * Value: true if eligible for that channel
     */
    @NotNull
    @JsonProperty("channelEligibility")
    Map<String, Boolean> channelEligibility;
}
```

### CandidateMetadata Model

```java
package com.solicitation.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.time.Instant;

/**
 * System-level metadata for candidate tracking
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
```

### RejectionRecord Model

```java
package com.solicitation.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.Instant;

/**
 * Records why a candidate was rejected by a filter
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
```

## Configuration Models

### ProgramConfig Model

```java
package com.solicitation.model.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.util.List;

/**
 * Configuration for a solicitation program
 */
@Value
@Builder
public class ProgramConfig {
    
    /**
     * Unique program identifier
     */
    @NotBlank
    @JsonProperty("programId")
    String programId;
    
    /**
     * Human-readable program name
     */
    @NotBlank
    @JsonProperty("programName")
    String programName;
    
    /**
     * Whether the program is enabled
     */
    @NotNull
    @JsonProperty("enabled")
    Boolean enabled;
    
    /**
     * Marketplaces where this program operates
     */
    @NotEmpty
    @JsonProperty("marketplaces")
    List<String> marketplaces;
    
    /**
     * Data connector configurations
     */
    @NotEmpty
    @Valid
    @JsonProperty("dataConnectors")
    List<DataConnectorConfig> dataConnectors;
    
    /**
     * Scoring model configurations
     */
    @NotEmpty
    @Valid
    @JsonProperty("scoringModels")
    List<ScoringModelConfig> scoringModels;
    
    /**
     * Filter chain configuration
     */
    @NotNull
    @Valid
    @JsonProperty("filterChain")
    FilterChainConfig filterChain;
    
    /**
     * Channel configurations
     */
    @NotEmpty
    @Valid
    @JsonProperty("channels")
    List<ChannelConfig> channels;
    
    /**
     * Batch schedule (cron expression, optional)
     */
    @JsonProperty("batchSchedule")
    String batchSchedule;
    
    /**
     * Whether reactive mode is enabled
     */
    @NotNull
    @JsonProperty("reactiveEnabled")
    Boolean reactiveEnabled;
    
    /**
     * Candidate TTL in days
     */
    @Positive
    @JsonProperty("candidateTTLDays")
    Integer candidateTTLDays;
    
    /**
     * Timing window in days (optional)
     */
    @JsonProperty("timingWindowDays")
    Integer timingWindowDays;
}
```

### FilterConfig Model

```java
package com.solicitation.model.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.PositiveOrZero;
import java.util.Map;

/**
 * Configuration for a single filter
 */
@Value
@Builder
public class FilterConfig {
    
    /**
     * Unique filter identifier
     */
    @NotBlank
    @JsonProperty("filterId")
    String filterId;
    
    /**
     * Type of filter (TRUST, ELIGIBILITY, BUSINESS_RULE, QUALITY, CAPACITY)
     */
    @NotBlank
    @JsonProperty("filterType")
    String filterType;
    
    /**
     * Whether the filter is enabled
     */
    @NotNull
    @JsonProperty("enabled")
    Boolean enabled;
    
    /**
     * Filter-specific parameters
     */
    @JsonProperty("parameters")
    Map<String, Object> parameters;
    
    /**
     * Execution order (lower numbers execute first)
     */
    @PositiveOrZero
    @JsonProperty("order")
    Integer order;
}
```

### ChannelConfig Model

```java
package com.solicitation.model.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Map;

/**
 * Configuration for a delivery channel
 */
@Value
@Builder
public class ChannelConfig {
    
    /**
     * Unique channel identifier
     */
    @NotBlank
    @JsonProperty("channelId")
    String channelId;
    
    /**
     * Type of channel (EMAIL, IN_APP, PUSH, VOICE, SMS)
     */
    @NotBlank
    @JsonProperty("channelType")
    String channelType;
    
    /**
     * Whether the channel is enabled
     */
    @NotNull
    @JsonProperty("enabled")
    Boolean enabled;
    
    /**
     * Whether shadow mode is enabled
     */
    @NotNull
    @JsonProperty("shadowMode")
    Boolean shadowMode;
    
    /**
     * Channel-specific configuration
     */
    @JsonProperty("config")
    Map<String, Object> config;
}
```

## Validation Strategy

### Field Validation

Use Java Bean Validation (JSR 380) annotations:
- `@NotNull`: Field must not be null
- `@NotBlank`: String must not be null or empty
- `@NotEmpty`: Collection must not be null or empty
- `@Valid`: Cascade validation to nested objects
- `@Positive`: Number must be positive
- `@Min/@Max`: Number range validation

### Custom Validation

For complex validation logic, implement custom validators:

```java
package com.solicitation.model.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class ContextValidator implements ConstraintValidator<ValidContext, List<Context>> {
    
    @Override
    public boolean isValid(List<Context> contexts, ConstraintValidatorContext context) {
        if (contexts == null || contexts.isEmpty()) {
            return false;
        }
        
        // Ensure at least one context is present
        // Validate context types are known
        // Check for duplicate context types
        
        return true;
    }
}
```

## Testing Strategy

### Unit Tests

Test each model class:
- Valid construction with all required fields
- Validation failures for missing/invalid fields
- JSON serialization/deserialization
- Builder pattern functionality
- Immutability (where applicable)

### Property-Based Tests

Implement property tests for:

**Property 2: Candidate model completeness**
- Generate random candidates
- Verify all required fields are present
- Verify validation catches missing fields

**Property 3: Context extensibility**
- Generate random context types and IDs
- Verify they can be stored without data loss
- Verify JSON round-trip preserves values

**Property 4: Version monotonicity**
- Generate candidate with version N
- Update to version N+1
- Verify version increases and timestamp updates

**Property 30: Program configuration validation**
- Generate random program configs
- Verify validation catches missing required fields
- Verify valid configs pass validation

## Implementation Checklist

### Task 2.1: Create Candidate model with all fields
- [ ] Create Candidate class with all fields
- [ ] Create Context class
- [ ] Create Subject class
- [ ] Create Score class
- [ ] Create CandidateAttributes class
- [ ] Create CandidateMetadata class
- [ ] Create RejectionRecord class
- [ ] Add Jackson annotations for JSON serialization
- [ ] Add validation annotations
- [ ] Implement builder pattern
- [ ] Add JavaDoc documentation

### Task 2.2: Write property test for candidate model completeness
- [ ] Set up property testing framework (jqwik or QuickTheories)
- [ ] Create arbitrary generators for all model classes
- [ ] Implement Property 2 test
- [ ] Verify test catches validation failures
- [ ] Run test with 100+ iterations

### Task 2.3: Write property test for context extensibility
- [ ] Create arbitrary generators for Context
- [ ] Implement Property 3 test
- [ ] Verify JSON round-trip preserves data
- [ ] Run test with 100+ iterations

### Task 2.4: Create configuration models
- [ ] Create ProgramConfig class
- [ ] Create FilterConfig class
- [ ] Create ChannelConfig class
- [ ] Create DataConnectorConfig class
- [ ] Create ScoringModelConfig class
- [ ] Create FilterChainConfig class
- [ ] Add validation logic
- [ ] Add JavaDoc documentation

### Task 2.5: Write property test for program configuration validation
- [ ] Create arbitrary generators for config classes
- [ ] Implement Property 30 test
- [ ] Verify validation catches missing fields
- [ ] Run test with 100+ iterations

## Dependencies

```kotlin
// solicitation-models/build.gradle.kts additions for Task 2
dependencies {
    // Internal dependencies
    implementation(project(":solicitation-common"))

    // JSON Processing
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.2")
    implementation("com.fasterxml.jackson.core:jackson-annotations:2.15.2")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.15.2")

    // Bean Validation
    implementation("javax.validation:validation-api:2.0.1.Final")
    implementation("org.hibernate.validator:hibernate-validator:7.0.5.Final")
    implementation("org.glassfish:jakarta.el:4.0.2") // Required for Hibernate Validator
    
    // Lombok is applied via plugin in root build.gradle.kts
    // Property-based testing (jqwik) is included in root build.gradle.kts
}
```

## Next Steps

After Task 2 completion:
- Task 3 will implement DynamoDB storage layer using these models
- Task 5 will implement data connectors that create these models
- Task 6 will implement scoring engine that populates score fields
