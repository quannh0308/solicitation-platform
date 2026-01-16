# Implementation Tasks - Current Cycle

## Current Focus: Task 2 - Implement Core Data Models

This task list shows the current 2-task implementation cycle. After completing these tasks, the next cycle will be loaded from FOUNDATION.

## Task Status Legend
- `[ ]` - Not started
- `[~]` - In progress  
- `[x]` - Complete
- `[*]` - Property-based test task

---

## Completed Tasks

### Task 1: Set up project structure and core infrastructure ✅
- Created Maven multi-module project with AWS SDK dependencies
- Set up DynamoDB table definitions (CloudFormation)
- Configured AWS Lambda runtime and deployment pipeline
- Set up logging framework (SLF4J + CloudWatch with PII redaction)
- **Status**: COMPLETE

---

## Current Task Cycle

### Task 2: Implement core data models

Create the unified candidate model and configuration models that serve as the canonical representation throughout the platform.

#### Subtasks:

- [ ] 2.1 Create Candidate model with all fields
  - Implement Context, Subject, Score, CandidateAttributes, CandidateMetadata classes
  - Add JSON serialization/deserialization annotations
  - Implement validation for required fields
  - _Requirements: 2.1, 2.2, 2.3, 2.4_
  - _Files to create_:
    - `solicitation-models/src/main/java/com/solicitation/model/Candidate.java`
    - `solicitation-models/src/main/java/com/solicitation/model/Context.java`
    - `solicitation-models/src/main/java/com/solicitation/model/Subject.java`
    - `solicitation-models/src/main/java/com/solicitation/model/Score.java`
    - `solicitation-models/src/main/java/com/solicitation/model/CandidateAttributes.java`
    - `solicitation-models/src/main/java/com/solicitation/model/CandidateMetadata.java`
    - `solicitation-models/src/main/java/com/solicitation/model/RejectionRecord.java`

- [ ]* 2.2 Write property test for candidate model completeness
  - **Property 2: Candidate model completeness**
  - **Validates: Requirements 2.1, 2.2**
  - Verify all required fields are present in any candidate
  - Verify validation catches missing fields
  - _Files to create_:
    - `solicitation-models/src/test/java/com/solicitation/model/CandidatePropertyTest.java`
    - `solicitation-models/src/test/java/com/solicitation/model/arbitraries/CandidateArbitraries.java`

- [ ]* 2.3 Write property test for context extensibility
  - **Property 3: Context extensibility**
  - **Validates: Requirements 1.3, 2.3**
  - Verify any valid context type/id can be stored without data loss
  - Verify JSON round-trip preserves context values
  - _Files to create_:
    - `solicitation-models/src/test/java/com/solicitation/model/ContextPropertyTest.java`

- [ ] 2.4 Create configuration models (ProgramConfig, FilterConfig, ChannelConfig)
  - Implement program registry data structures
  - Add validation logic for configuration fields
  - _Requirements: 10.1, 10.2_
  - _Files to create_:
    - `solicitation-models/src/main/java/com/solicitation/model/config/ProgramConfig.java`
    - `solicitation-models/src/main/java/com/solicitation/model/config/FilterConfig.java`
    - `solicitation-models/src/main/java/com/solicitation/model/config/ChannelConfig.java`
    - `solicitation-models/src/main/java/com/solicitation/model/config/DataConnectorConfig.java`
    - `solicitation-models/src/main/java/com/solicitation/model/config/ScoringModelConfig.java`
    - `solicitation-models/src/main/java/com/solicitation/model/config/FilterChainConfig.java`

- [ ]* 2.5 Write property test for program configuration validation
  - **Property 30: Program configuration validation**
  - **Validates: Requirements 10.1**
  - Verify all required fields must be present
  - Verify validation rejects invalid configurations
  - _Files to create_:
    - `solicitation-models/src/test/java/com/solicitation/model/config/ProgramConfigPropertyTest.java`
    - `solicitation-models/src/test/java/com/solicitation/model/arbitraries/ConfigArbitraries.java`

---

## Testing Requirements

### Property-Based Testing
- Use **jqwik** framework for Java property-based tests
- Minimum **100 iterations** per property test
- Each property test must reference its design document property number
- Tag format: `@Property` annotation with comment `// Property {number}: {description}`

### Test Organization
- Unit tests in `src/test/java` matching source package structure
- Property tests in same location with `PropertyTest` suffix
- Arbitrary generators in `arbitraries` subpackage for reuse

### Validation Testing
- Test valid object construction
- Test validation failures for missing/invalid fields
- Test JSON serialization/deserialization round-trip
- Test builder pattern functionality

---

## Success Criteria

Task 2 is complete when:
1. ✅ All model classes are implemented with proper annotations
2. ✅ JSON serialization/deserialization works correctly
3. ✅ Field validation catches missing/invalid data
4. ✅ All property tests pass with 100+ iterations
5. ✅ Unit tests cover edge cases and error conditions
6. ✅ Code follows Java best practices and is well-documented
7. ✅ Maven build succeeds with no warnings

---

## Next Cycle Preview

After Task 2 completion, the next cycle will focus on:
- **Task 3**: Implement DynamoDB storage layer using these models
- **Task 4**: Checkpoint - Ensure storage layer tests pass

---

## Notes

- Property tests marked with `*` are required for correctness validation
- Each task references specific requirements for traceability
- Use the design document for detailed implementation guidance
- Refer to FOUNDATION/tasks.md for the complete task list
