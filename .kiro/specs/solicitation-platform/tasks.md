# Implementation Tasks - Current Cycle

## Current Focus: Task 3 & 4 - DynamoDB Storage Layer

This task list shows the current 2-task implementation cycle. After completing these tasks, the next cycle will be loaded from FOUNDATION.

## Task Status Legend
- `[ ]` - Not started
- `[~]` - In progress  
- `[x]` - Complete
- `[*]` - Property-based test task

---

## Completed Tasks

### Task 1: Set up project structure and core infrastructure ✅
- ✅ Created Gradle multi-module project with Kotlin 1.9.21
- ✅ Migrated from Maven to Gradle 8.5 with Kotlin DSL
- ✅ Migrated from Java to Kotlin for all modules
- ✅ Set up AWS CDK (Kotlin) for infrastructure as code
- ✅ Created DynamoDB table definitions (CDK)
- ✅ Created Lambda function stacks (CDK)
- ✅ Created reusable SolicitationLambda construct
- ✅ Configured AWS Lambda runtime (Java 17)
- ✅ Set up deployment pipeline (deploy-cdk.sh)
- ✅ Set up logging framework (SLF4J + Logback + kotlin-logging)
- ✅ Configured 13 modules: 8 libraries + 5 Lambda workflows
- **Status**: COMPLETE
- **Technology Stack**: Kotlin 1.9.21, Gradle 8.5, AWS CDK 2.167.1
- **Architecture**: Multi-module with plug-and-play CDK infrastructure

### Task 2: Implement core data models ✅
- ✅ Created 7 Kotlin data classes for Candidate model (Task 2.1)
  - Candidate, Context, Subject, Score, CandidateAttributes, CandidateMetadata, RejectionRecord
  - Added Jackson annotations for JSON serialization
  - Implemented Bean Validation (JSR 380) with @field: prefix
  - Created unit tests (4 tests passing)
- ✅ Created 6 configuration model classes (Task 2.4)
  - ProgramConfig, FilterConfig, FilterChainConfig, ChannelConfig, DataConnectorConfig, ScoringModelConfig
  - Added validation logic for all required fields
  - Created unit tests (8 tests passing)
- ✅ Implemented property-based tests using jqwik (Tasks 2.2, 2.3, 2.5)
  - CandidatePropertyTest: 7 properties (700 test cases)
  - ContextPropertyTest: 6 properties (600 test cases)
  - ProgramConfigPropertyTest: 12 properties (1,200 test cases)
- **Status**: COMPLETE
- **Test Results**: All 37 tests passing (12 unit + 25 property tests)
- **Validates**: Requirements 1.3, 2.1, 2.2, 2.3, 2.4, 2.5, 10.1, 10.2

### Task 3: Implement DynamoDB storage layer ✅
- ✅ Created DynamoDB repository interface and implementation (Task 3.1)
  - Implemented CRUD operations (create, read, update, delete)
  - Added batch write support with DynamoDB batch limits (25 items)
  - Implemented query operations using primary key and GSIs
  - Added optimistic locking using version numbers
- ✅ Implemented property-based tests (Tasks 3.2-3.7)
  - StorageRoundTripPropertyTest: Validates storage consistency (Property 12)
  - QueryFilteringPropertyTest: Validates query correctness (Property 13)
  - OptimisticLockingPropertyTest: Validates conflict detection (Property 14)
  - BatchWritePropertyTest: Validates batch atomicity (Property 15)
  - TTLConfigurationPropertyTest: Validates TTL calculation (Property 51)
- ✅ Implemented TTL configuration logic (Task 3.6)
  - TTLCalculator for computing expiration timestamps
  - Integration with program configuration
- **Status**: COMPLETE
- **Test Results**: All 5 property tests passing (100+ iterations each)
- **Validates**: Requirements 5.1, 5.2, 5.3, 5.5, 17.1

---

## Current Task Cycle

- [ ] Task 5: Implement data connector framework
- [ ] Task 6: Implement scoring engine layer

---

## Task 5 Details: Implement data connector framework

Implement the framework for extracting data from various sources and transforming it into unified candidate models.

### Subtasks:

- [ ] 5.1 Create DataConnector interface
  - Define interface methods (getName, validateConfig, extractData, transformToCandidate)
  - Create base abstract class with common validation logic
  - _Requirements: 1.1, 1.2_
  - _Files to create_:
    - `solicitation-connectors/src/main/kotlin/com/solicitation/connectors/DataConnector.kt`
    - `solicitation-connectors/src/main/kotlin/com/solicitation/connectors/BaseDataConnector.kt`

- [ ] 5.2 Implement data warehouse connector
  - Implement Athena/Glue integration for data warehouse queries
  - Add field mapping configuration support
  - Implement transformation to unified candidate model
  - _Requirements: 1.2, 1.3_
  - _Files to create_:
    - `solicitation-connectors/src/main/kotlin/com/solicitation/connectors/DataWarehouseConnector.kt`
    - `solicitation-connectors/src/main/kotlin/com/solicitation/connectors/FieldMapper.kt`

- [ ]* 5.3 Write property test for transformation preserves semantics
  - **Property 1: Data connector transformation preserves semantics**
  - **Validates: Requirements 1.2**
  - Verify source data is correctly transformed to candidate model
  - Verify no data loss during transformation
  - _Files to create_:
    - `solicitation-connectors/src/test/kotlin/com/solicitation/connectors/TransformationPropertyTest.kt`

- [ ] 5.4 Add schema validation logic
  - Implement JSON Schema validation for source data
  - Add detailed error logging for validation failures
  - _Requirements: 1.4, 16.1, 16.2, 16.3_
  - _Files to create_:
    - `solicitation-connectors/src/main/kotlin/com/solicitation/connectors/SchemaValidator.kt`

- [ ]* 5.5 Write property test for required field validation
  - **Property 49: Required field validation**
  - **Validates: Requirements 16.1, 16.3**
  - Verify missing required fields are detected
  - Verify validation errors are descriptive
  - _Files to create_:
    - `solicitation-connectors/src/test/kotlin/com/solicitation/connectors/RequiredFieldPropertyTest.kt`

- [ ]* 5.6 Write property test for date format validation
  - **Property 50: Date format validation**
  - **Validates: Requirements 16.2, 16.3**
  - Verify date fields are validated correctly
  - Verify invalid date formats are rejected
  - _Files to create_:
    - `solicitation-connectors/src/test/kotlin/com/solicitation/connectors/DateFormatPropertyTest.kt`

---

## Task 6 Details: Implement scoring engine layer

Implement the scoring engine that evaluates candidates using ML models and caches results.

### Subtasks:

- [ ] 6.1 Create ScoringProvider interface
  - Define interface methods (getModelId, scoreCandidate, scoreBatch, healthCheck)
  - Add fallback score support
  - _Requirements: 3.1, 3.4_
  - _Files to create_:
    - `solicitation-scoring/src/main/kotlin/com/solicitation/scoring/ScoringProvider.kt`
    - `solicitation-scoring/src/main/kotlin/com/solicitation/scoring/BaseScoringProvider.kt`

- [ ] 6.2 Implement score caching in DynamoDB
  - Create score cache table schema
  - Implement cache read/write with TTL
  - Add cache invalidation logic
  - _Requirements: 3.5_
  - _Files to create_:
    - `solicitation-scoring/src/main/kotlin/com/solicitation/scoring/ScoreCache.kt`
    - `solicitation-scoring/src/main/kotlin/com/solicitation/scoring/DynamoDBScoreCache.kt`

- [ ]* 6.3 Write property test for score caching consistency
  - **Property 6: Score caching consistency**
  - **Validates: Requirements 3.5**
  - Verify cached scores match computed scores
  - Verify cache TTL is respected
  - _Files to create_:
    - `solicitation-scoring/src/test/kotlin/com/solicitation/scoring/ScoreCachingPropertyTest.kt`

- [ ] 6.4 Implement feature store integration
  - Create feature retrieval client
  - Add feature validation against required features
  - _Requirements: 3.2_
  - _Files to create_:
    - `solicitation-scoring/src/main/kotlin/com/solicitation/scoring/FeatureStoreClient.kt`

- [ ]* 6.5 Write property test for feature retrieval completeness
  - **Property 8: Feature retrieval completeness**
  - **Validates: Requirements 3.2**
  - Verify all required features are retrieved
  - Verify feature values are valid
  - _Files to create_:
    - `solicitation-scoring/src/test/kotlin/com/solicitation/scoring/FeatureRetrievalPropertyTest.kt`

- [ ] 6.6 Implement multi-model scoring support
  - Add logic to execute multiple scoring models per candidate
  - Store scores with modelId, value, confidence, timestamp
  - _Requirements: 3.3_
  - _Files to create_:
    - `solicitation-scoring/src/main/kotlin/com/solicitation/scoring/MultiModelScorer.kt`

- [ ]* 6.7 Write property test for multi-model scoring independence
  - **Property 5: Multi-model scoring independence**
  - **Validates: Requirements 3.3**
  - Verify models execute independently
  - Verify one model failure doesn't affect others
  - _Files to create_:
    - `solicitation-scoring/src/test/kotlin/com/solicitation/scoring/MultiModelPropertyTest.kt`

- [ ] 6.8 Add scoring fallback logic with circuit breaker
  - Implement circuit breaker pattern for model endpoints
  - Add fallback to cached scores or default values
  - Add failure logging
  - _Requirements: 3.4, 9.3_
  - _Files to create_:
    - `solicitation-scoring/src/main/kotlin/com/solicitation/scoring/ScoringCircuitBreaker.kt`

- [ ]* 6.9 Write property test for scoring fallback correctness
  - **Property 7: Scoring fallback correctness**
  - **Validates: Requirements 3.4, 9.3**
  - Verify fallback is triggered on failures
  - Verify fallback scores are valid
  - _Files to create_:
    - `solicitation-scoring/src/test/kotlin/com/solicitation/scoring/ScoringFallbackPropertyTest.kt`

---

## Testing Requirements

### Property-Based Testing
- Use **jqwik** framework for Kotlin property-based tests
- Minimum **100 iterations** per property test
- Each property test must reference its design document property number
- Tag format: `@Property` annotation with comment `// Property {number}: {description}`

### Test Organization
- Unit tests in `src/test/kotlin` matching source package structure
- Property tests in same location with `PropertyTest` suffix
- Arbitrary generators in `arbitraries` subpackage for reuse

### Data Connector Testing
- Test data extraction from various sources
- Test field mapping with different configurations
- Test schema validation with valid and invalid data
- Test transformation to candidate model
- Test error handling and logging

### Scoring Engine Testing
- Test score computation with various models
- Test score caching with TTL
- Test feature retrieval from feature store
- Test multi-model execution
- Test circuit breaker and fallback logic

---

## Success Criteria

Task 5 is complete when:
1. ✅ DataConnector interface and base class created
2. ✅ Data warehouse connector implemented
3. ✅ Field mapping configuration working
4. ✅ Schema validation logic implemented
5. ✅ All property tests pass with 100+ iterations
6. ✅ Unit tests cover edge cases and error conditions
7. ✅ Gradle build succeeds with no warnings

Task 6 is complete when:
1. ✅ ScoringProvider interface created
2. ✅ Score caching in DynamoDB working
3. ✅ Feature store integration implemented
4. ✅ Multi-model scoring support working
5. ✅ Circuit breaker and fallback logic implemented
6. ✅ All property tests pass with 100+ iterations
7. ✅ Unit tests cover edge cases and error conditions
8. ✅ Gradle build succeeds with no warnings

---

## Next Cycle Preview

After Task 5 & 6 completion, the next cycle will focus on:
- **Task 7**: Implement filtering and eligibility pipeline
- **Task 8**: Checkpoint - Ensure scoring and filtering tests pass

---

## Notes

- Property tests marked with `*` are required for correctness validation
- Each task references specific requirements for traceability
- Use the design document for detailed implementation guidance
- Refer to FOUNDATION/tasks.md for the complete task list
- DynamoDB local can be used for testing without AWS credentials
