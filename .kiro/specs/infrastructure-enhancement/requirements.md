# Requirements Document

## Introduction

This specification defines requirements for enhancing the CEAP infrastructure architecture to improve Lambda function naming conventions and implement industry-standard Step Functions orchestration patterns with S3-based intermediate storage. The enhancements will enable processing of large datasets (>256KB), provide execution observability, support long-running ETL jobs via Glue integration, and follow AWS best practices used by companies like Netflix, Airbnb, and Capital One.

## Glossary

- **CEAP**: The system being enhanced (Candidate Evaluation and Processing)
- **Lambda_Function**: AWS Lambda serverless compute function
- **Step_Function**: AWS Step Functions workflow orchestration service
- **S3_Bucket**: AWS Simple Storage Service object storage
- **Execution_ID**: Unique identifier for a Step Functions workflow execution
- **Stage**: A discrete processing step in the workflow (e.g., ETL, Filter, Score)
- **Convention_Based_Path**: S3 path following pattern `executions/{executionId}/{stageName}/output.json`
- **Express_Workflow**: Step Functions workflow type with 5-minute maximum duration, synchronous invocation
- **Standard_Workflow**: Step Functions workflow type with 1-year maximum duration, asynchronous invocation
- **Glue_Job**: AWS Glue ETL job for long-running data transformations
- **CDK**: AWS Cloud Development Kit for infrastructure as code
- **Intermediate_Output**: Data written to S3 between workflow stages for debugging and data lineage
- **EventBridge_Pipe**: AWS service connecting SQS queues to Step Functions
- **Payload_Limit**: Step Functions 256KB maximum for direct data passing between states

## Requirements

### Requirement 1: Clean Lambda Function Names

**User Story:** As a developer, I want Lambda functions to have clean, predictable names without auto-generated suffixes, so that I can easily reference them in scripts, monitoring tools, and the AWS Console.

#### Acceptance Criteria

1. THE CDK_Infrastructure SHALL generate Lambda function names following the pattern `{StackName}-{Environment}-{FunctionPurpose}` without random suffixes
2. WHEN a Lambda function is deployed, THE CDK_Infrastructure SHALL use explicit function name configuration to prevent CloudFormation auto-generation
3. THE Lambda_Function names SHALL remain stable across deployments unless explicitly renamed
4. THE CloudWatch_Log_Groups SHALL use the clean Lambda function names as their base path
5. WHERE a Lambda function name conflicts with an existing function, THE CDK_Infrastructure SHALL fail deployment with a clear error message

### Requirement 2: S3-Based Intermediate Storage

**User Story:** As a data engineer, I want workflow stages to communicate via S3 storage, so that I can process large datasets exceeding 256KB and debug data transformations at each stage.

#### Acceptance Criteria

1. THE Step_Function SHALL pass execution context (executionId, currentStage, previousStage, workflowBucket) to each Lambda instead of large data payloads
2. WHEN a Lambda_Function completes processing, THE Lambda_Function SHALL write output to S3 at path `executions/{executionId}/{currentStage}/output.json`
3. WHEN a Lambda_Function begins processing (except the first stage), THE Lambda_Function SHALL read input from S3 at path `executions/{executionId}/{previousStage}/output.json`
4. THE first Lambda_Function in the workflow SHALL use the initial SQS message body as input instead of reading from S3
5. THE S3_Bucket SHALL organize outputs by execution ID to enable correlation of all stages for a single workflow run
6. WHEN a workflow execution completes or fails, THE Intermediate_Output files SHALL remain in S3 for debugging and audit purposes
7. THE S3_Bucket SHALL implement lifecycle policies to automatically delete execution data after 7 days

### Requirement 3: Convention-Based Path Resolution

**User Story:** As a system architect, I want Lambda functions to determine their input/output paths using naming conventions, so that stages are loosely coupled and can be reordered without code changes.

#### Acceptance Criteria

1. THE Lambda_Function SHALL construct its input S3 path using the `previousStage` parameter from the Step Function payload
2. THE Lambda_Function SHALL construct its output S3 path using the `currentStage` parameter from the Step Function payload
3. THE Lambda_Function SHALL NOT contain hardcoded references to other Lambda functions or specific S3 paths
4. WHEN the Step Function stage order is modified, THE Lambda_Function code SHALL continue to work without modification
5. THE Lambda_Function SHALL be independently testable by providing mock execution context parameters

### Requirement 4: Express Workflow Support

**User Story:** As a developer, I want to use Express workflows for fast processing pipelines, so that I can achieve low latency and cost-effective orchestration when all stages complete within 5 minutes.

#### Acceptance Criteria

1. THE CDK_Infrastructure SHALL support creating Express_Workflow Step Functions with 5-minute maximum duration
2. THE EventBridge_Pipe SHALL invoke Express_Workflow using REQUEST_RESPONSE (synchronous) invocation type
3. WHEN an Express_Workflow execution fails, THE EventBridge_Pipe SHALL return failure to SQS to enable message retry
4. THE Express_Workflow SHALL chain Lambda functions sequentially with each Lambda writing to and reading from S3
5. THE Express_Workflow SHALL include retry configuration with 2 attempts, 20-second initial interval, and 2x backoff rate
6. THE Express_Workflow SHALL use `resultPath: DISCARD` to prevent large Lambda responses from bloating execution history
7. THE Express_Workflow SHALL enable CloudWatch Logs with full execution data for debugging

### Requirement 5: Standard Workflow Support

**User Story:** As a data engineer, I want to use Standard workflows for long-running pipelines, so that I can integrate Glue jobs and other processes that exceed 5 minutes without timeout constraints.

#### Acceptance Criteria

1. THE CDK_Infrastructure SHALL support creating Standard_Workflow Step Functions with 1-year maximum duration
2. THE EventBridge_Pipe SHALL invoke Standard_Workflow using FIRE_AND_FORGET (asynchronous) invocation type
3. THE Standard_Workflow SHALL support mixing Lambda_Function steps and Glue_Job steps in the same workflow
4. WHEN a Glue_Job step executes, THE Step_Function SHALL wait for job completion using native integration pattern
5. THE Standard_Workflow SHALL pass execution context to Glue_Job via job arguments including executionId, input S3 path, and output S3 path
6. THE Glue_Job SHALL read input from the previous stage's S3 output path and write output to its own S3 path following the convention
7. THE Standard_Workflow SHALL include retry configuration for Glue_Job steps with 2 attempts, 5-minute initial interval, and 2x backoff rate

### Requirement 6: Workflow Type Selection

**User Story:** As a system architect, I want to choose between Express and Standard workflows based on processing time requirements, so that I can optimize for cost and latency when appropriate.

#### Acceptance Criteria

1. WHERE all workflow stages complete within 5 minutes, THE CDK_Infrastructure SHALL support Express_Workflow deployment
2. WHERE any workflow stage can exceed 5 minutes, THE CDK_Infrastructure SHALL require Standard_Workflow deployment
3. THE CDK_Infrastructure SHALL provide clear configuration options to specify workflow type (Express or Standard)
4. WHEN Express_Workflow is selected with stages exceeding 5 minutes, THE CDK_Infrastructure SHALL fail validation with a descriptive error
5. THE CDK_Infrastructure SHALL configure EventBridge_Pipe invocation type automatically based on workflow type (REQUEST_RESPONSE for Express, FIRE_AND_FORGET for Standard)

### Requirement 7: Glue Job Integration

**User Story:** As a data engineer, I want to integrate Glue jobs into workflows for long-running ETL processes, so that I can handle complex transformations that exceed Lambda's 15-minute timeout.

#### Acceptance Criteria

1. THE CDK_Infrastructure SHALL support adding Glue_Job steps to Standard_Workflow at any position in the stage sequence
2. THE Step_Function SHALL pass execution context to Glue_Job including `--execution-id`, `--input-bucket`, `--input-key`, `--output-bucket`, and `--output-key` arguments
3. THE Glue_Job SHALL read input data from S3 using the provided input path (previous stage output)
4. THE Glue_Job SHALL write output data to S3 using the provided output path following convention `executions/{executionId}/{currentStage}/output.json`
5. THE Step_Function SHALL use RUN_JOB integration pattern to wait for Glue_Job completion before proceeding to next stage
6. WHEN a Glue_Job fails after retries, THE Step_Function execution SHALL fail and record the error in execution history

### Requirement 8: Error Handling and Retry Logic

**User Story:** As a DevOps engineer, I want automatic retry logic for transient failures, so that temporary issues don't cause permanent workflow failures.

#### Acceptance Criteria

1. THE Lambda_Function steps SHALL retry on all errors with maximum 2 attempts, 20-second initial interval, and 2x backoff rate
2. THE Glue_Job steps SHALL retry on all errors with maximum 2 attempts, 5-minute initial interval, and 2x backoff rate
3. WHEN a Lambda_Function or Glue_Job fails after all retries, THE Step_Function execution SHALL transition to FAILED state
4. THE Step_Function SHALL record detailed error information including error type, error message, and failed stage name in execution history
5. THE SQS_Queue SHALL implement Dead Letter Queue (DLQ) for messages that fail after 3 receive attempts
6. WHERE Express_Workflow is used, WHEN execution fails, THE EventBridge_Pipe SHALL return failure to SQS to enable message retry
7. WHERE Standard_Workflow is used, THE CDK_Infrastructure SHALL create EventBridge rules to detect failed executions and trigger alerts

### Requirement 9: Execution Observability

**User Story:** As a DevOps engineer, I want comprehensive execution tracking and logging, so that I can monitor workflow health, debug failures, and analyze performance.

#### Acceptance Criteria

1. THE Step_Function SHALL enable X-Ray tracing for end-to-end execution visibility
2. THE Step_Function SHALL enable CloudWatch Logs with full execution data including input, output, and error details
3. THE CloudWatch_Log_Groups SHALL retain logs for 14 days for debugging and audit purposes
4. THE Step_Function execution history SHALL show current stage, completed stages, failed stages, and execution duration
5. THE Lambda_Function SHALL log execution context (executionId, currentStage) at the start of each invocation
6. THE Lambda_Function SHALL log S3 paths used for input and output for debugging data flow
7. THE S3_Bucket SHALL organize intermediate outputs by executionId to enable correlation of all stages for a single workflow

### Requirement 10: Backward Compatibility

**User Story:** As a developer, I want to maintain existing functionality during the migration, so that current workflows continue operating while new patterns are adopted.

#### Acceptance Criteria

1. THE existing Lambda_Function implementations SHALL continue to work during the migration period
2. THE CDK_Infrastructure SHALL support deploying both old and new Lambda function naming patterns simultaneously
3. WHEN migrating to S3-based orchestration, THE CDK_Infrastructure SHALL allow incremental stage migration
4. THE existing DynamoDB storage layer SHALL remain unchanged and continue to function
5. THE existing API endpoints SHALL continue to work without modification during infrastructure changes

### Requirement 11: CDK Infrastructure Implementation

**User Story:** As a DevOps engineer, I want infrastructure defined as code using CDK with Kotlin, so that deployments are repeatable, version-controlled, and follow infrastructure-as-code best practices.

#### Acceptance Criteria

1. THE CDK_Infrastructure SHALL define Lambda functions with explicit function names using `functionName` property
2. THE CDK_Infrastructure SHALL create S3_Bucket with appropriate IAM permissions for Lambda and Glue job access
3. THE CDK_Infrastructure SHALL define Step_Function with configurable workflow type (Express or Standard)
4. THE CDK_Infrastructure SHALL create EventBridge_Pipe connecting SQS queue to Step_Function with appropriate invocation type
5. THE CDK_Infrastructure SHALL configure Lambda retry policies declaratively in Step Function definition
6. THE CDK_Infrastructure SHALL configure Glue_Job retry policies declaratively in Step Function definition
7. THE CDK_Infrastructure SHALL enable CloudWatch Logs and X-Ray tracing for Step Functions
8. THE CDK_Infrastructure SHALL implement S3 lifecycle policies to delete execution data after 7 days
9. THE CDK_Infrastructure SHALL create IAM roles with least-privilege permissions for Lambda, Glue, and Step Functions

### Requirement 12: Testing and Validation

**User Story:** As a developer, I want comprehensive testing including property-based tests, so that I can verify correctness across a wide range of inputs and execution scenarios.

#### Acceptance Criteria

1. THE Lambda_Function implementations SHALL include unit tests for S3 read/write operations
2. THE Lambda_Function implementations SHALL include property-based tests verifying convention-based path construction
3. THE Step_Function definitions SHALL include integration tests verifying end-to-end workflow execution
4. THE CDK_Infrastructure SHALL include tests verifying correct IAM permissions for S3 access
5. THE testing suite SHALL verify S3 intermediate outputs are written correctly at each stage
6. THE testing suite SHALL verify workflow execution tracking and observability features
7. THE testing suite SHALL include tests for error handling and retry logic
