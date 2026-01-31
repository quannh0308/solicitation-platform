# Design Document: Infrastructure Enhancement

## Overview

This design implements industry-standard AWS Step Functions orchestration patterns with S3-based intermediate storage for the CEAP infrastructure. The enhancement addresses two primary goals:

1. **Clean Lambda Naming**: Replace auto-generated CloudFormation suffixes with predictable, stable function names
2. **Scalable Orchestration**: Implement S3-based Lambda chaining to handle large datasets (>256KB), provide execution observability, and support optional Glue job integration for long-running ETL processes

The design follows AWS best practices used by Netflix, Airbnb, Capital One, and thousands of enterprises for large-scale data processing. It provides two deployment options:

- **Express Workflow**: For fast processing (<5 minutes), synchronous invocation, cost-optimized ($1 per million transitions)
- **Standard Workflow**: For long-running jobs (up to 1 year), asynchronous invocation, supports Glue/EMR integration ($25 per million transitions)

### Key Design Principles

- **Convention over Configuration**: Lambda functions use naming conventions to determine S3 paths, eliminating hardcoded dependencies
- **Loose Coupling**: Stages communicate via S3, enabling independent development, testing, and reordering
- **Observability First**: Built-in execution tracking, CloudWatch Logs, X-Ray tracing, and persistent intermediate outputs
- **Incremental Migration**: Backward compatible design allows gradual adoption without disrupting existing functionality

## Architecture

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Upstream Services                         │
│                   (Publish to SNS)                           │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│                      SQS Queue                               │
│  • Buffers messages                                          │
│  • 10 min visibility timeout                                 │
│  • Max 3 receive attempts                                    │
│  • DLQ for failures                                          │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│            EventBridge Pipe                                  │
│  • Batch size: 1                                             │
│  • Invocation type: REQUEST_RESPONSE (Express)               │
│                     or FIRE_AND_FORGET (Standard)            │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│         Step Functions Workflow                              │
│  ┌──────────────────────────────────────────────────────┐   │
│  │  ETL → Filter → Score → Store → Reactive            │   │
│  │   ↓      ↓       ↓       ↓        ↓                 │   │
│  │  S3     S3      S3      S3       S3                 │   │
│  └──────────────────────────────────────────────────────┘   │
│                                                               │
│  Optional Glue Job Integration (Standard Workflow):          │
│  ┌──────────────────────────────────────────────────────┐   │
│  │  ETL → Glue Job (2-4 hours) → Filter → Score...     │   │
│  │   ↓        ↓                     ↓       ↓           │   │
│  │  S3       S3                    S3      S3           │   │
│  └──────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

### S3 Data Organization

```
s3://ceap-workflow-bucket/
  executions/{executionId}/
    ├── ETLStage/output.json
    ├── FilterStage/output.json
    ├── ScoreStage/output.json
    ├── StoreStage/output.json
    └── ReactiveStage/output.json
```

### Workflow Execution Flow

1. **Trigger**: SQS message arrives from upstream SNS topic
2. **Pipe Invocation**: EventBridge Pipe invokes Step Function with execution context
3. **Stage Execution**: Each Lambda:
   - Receives execution context (executionId, currentStage, previousStage, workflowBucket)
   - Reads input from S3 (previous stage output) or uses initial SQS message body (first stage)
   - Processes data
   - Writes output to S3 at `executions/{executionId}/{currentStage}/output.json`
4. **Completion**: Final stage completes, execution marked as SUCCEEDED
5. **Cleanup**: S3 lifecycle policy deletes execution data after 7 days

## Components and Interfaces

### 1. Lambda Function Naming (CDK)

**Current Implementation Problem:**
```kotlin
// CloudFormation auto-generates suffix
val lambda = Function.Builder.create(this, "ReactiveLambdaFunction")
    .runtime(Runtime.JAVA_17)
    .handler("com.example.Handler")
    .code(Code.fromAsset("lambda.jar"))
    .build()

// Results in: CeapServingAPI-dev-ReactiveLambdaFunction89310B25-jLlIqkWt5O4x
```

**New Implementation:**
```kotlin
// Explicit function name prevents auto-generation
val lambda = Function.Builder.create(this, "ReactiveLambdaFunction")
    .functionName("${stackName}-${environment}-ReactiveLambdaFunction")
    .runtime(Runtime.JAVA_17)
    .handler("com.example.Handler")
    .code(Code.fromAsset("lambda.jar"))
    .build()

// Results in: CeapServingAPI-dev-ReactiveLambdaFunction
```

**Implementation Details:**
- Use `functionName` property in CDK Lambda construct
- Follow pattern: `{StackName}-{Environment}-{FunctionPurpose}`
- CloudWatch log groups automatically use clean name: `/aws/lambda/CeapServingAPI-dev-ReactiveLambdaFunction`
- Deployment will fail if name conflicts with existing function (prevents accidental overwrites)

### 2. S3 Workflow Bucket

**CDK Configuration:**
```kotlin
val workflowBucket = Bucket.Builder.create(this, "WorkflowBucket")
    .bucketName("ceap-workflow-${environment}-${accountId}")
    .versioned(false)
    .lifecycleRules(listOf(
        LifecycleRule.builder()
            .id("DeleteOldExecutions")
            .prefix("executions/")
            .expiration(Duration.days(7))
            .enabled(true)
            .build()
    ))
    .encryption(BucketEncryption.S3_MANAGED)
    .blockPublicAccess(BlockPublicAccess.BLOCK_ALL)
    .build()
```

**Key Features:**
- Lifecycle policy automatically deletes execution data after 7 days
- S3-managed encryption for data at rest
- Block all public access for security
- Organized by execution ID for easy correlation

### 3. Step Function Payload Structure

**Execution Context Passed to Each Lambda:**
```json
{
  "executionId": "abc-123-def-456",
  "currentStage": "FilterStage",
  "previousStage": "ETLStage",
  "workflowBucket": "ceap-workflow-dev-123456789",
  "initialData": {
    "messageId": "msg-789",
    "timestamp": "2024-01-15T10:30:00Z",
    "payload": { ... }
  }
}
```

**Field Descriptions:**
- `executionId`: Step Functions execution name (unique per workflow run)
- `currentStage`: Name of the current Lambda stage (used for output S3 path)
- `previousStage`: Name of the previous stage (used for input S3 path), null for first stage
- `workflowBucket`: S3 bucket name for intermediate storage
- `initialData`: Original SQS message body (used by first stage only)

### 4. Lambda Handler Interface

**Base Handler Pattern (Kotlin):**
```kotlin
data class ExecutionContext(
    val executionId: String,
    val currentStage: String,
    val previousStage: String?,
    val workflowBucket: String,
    val initialData: JsonNode?
)

data class StageResult(
    val status: String,
    val stage: String,
    val recordsProcessed: Int
)

abstract class WorkflowLambdaHandler : RequestHandler<ExecutionContext, StageResult> {
    
    private val s3Client = S3Client.create()
    private val objectMapper = ObjectMapper()
    
    override fun handleRequest(context: ExecutionContext, lambdaContext: Context): StageResult {
        logger.info("Starting ${context.currentStage} for execution ${context.executionId}")
        
        // Read input
        val inputData = readInput(context)
        
        // Process data (implemented by subclass)
        val result = processData(inputData)
        
        // Write output
        writeOutput(context, result)
        
        return StageResult(
            status = "SUCCESS",
            stage = context.currentStage,
            recordsProcessed = result.size
        )
    }
    
    private fun readInput(context: ExecutionContext): JsonNode {
        return if (context.previousStage != null) {
            // Read from previous stage S3 output
            val inputKey = "executions/${context.executionId}/${context.previousStage}/output.json"
            logger.info("Reading input from s3://${context.workflowBucket}/$inputKey")
            
            val response = s3Client.getObject(
                GetObjectRequest.builder()
                    .bucket(context.workflowBucket)
                    .key(inputKey)
                    .build()
            )
            objectMapper.readTree(response.readAllBytes())
        } else {
            // First stage: use initial SQS message data
            logger.info("Using initial data from SQS message")
            context.initialData!!
        }
    }
    
    private fun writeOutput(context: ExecutionContext, data: JsonNode) {
        val outputKey = "executions/${context.executionId}/${context.currentStage}/output.json"
        logger.info("Writing output to s3://${context.workflowBucket}/$outputKey")
        
        s3Client.putObject(
            PutObjectRequest.builder()
                .bucket(context.workflowBucket)
                .key(outputKey)
                .contentType("application/json")
                .build(),
            RequestBody.fromBytes(objectMapper.writeValueAsBytes(data))
        )
    }
    
    // Implemented by each stage
    abstract fun processData(input: JsonNode): JsonNode
}
```

**Concrete Implementation Example:**
```kotlin
class FilterLambdaHandler : WorkflowLambdaHandler() {
    override fun processData(input: JsonNode): JsonNode {
        // Filter logic specific to this stage
        val candidates = input.get("candidates")
        val filtered = candidates.filter { candidate ->
            candidate.get("score").asInt() > 50
        }
        
        return objectMapper.createObjectNode().apply {
            set<JsonNode>("filteredCandidates", objectMapper.valueToTree(filtered))
            put("filterCount", filtered.size)
        }
    }
}
```

### 5. Workflow Type Selection

**Configuration Interface:**

The CDK infrastructure provides a clear choice between Express and Standard workflows through the `WorkflowType` enum:

```kotlin
enum class WorkflowType {
    EXPRESS,    // <5 minutes, synchronous, $1 per million transitions
    STANDARD    // Up to 1 year, asynchronous, $25 per million transitions, supports Glue
}

data class WorkflowConfiguration(
    val workflowName: String,
    val workflowType: WorkflowType,  // ← User chooses here
    val steps: List<WorkflowStepType>,
    val workflowBucket: String,
    val sourceQueue: String,
    val retryConfig: RetryConfiguration
)
```

**Decision Criteria:**
- Choose **EXPRESS** if: All stages are Lambdas completing in <5 minutes
- Choose **STANDARD** if: Any stage can exceed 5 minutes OR you need Glue job integration

**Automatic Configuration:**
Based on the selected `WorkflowType`, the infrastructure automatically configures:
- EventBridge Pipe invocation type (REQUEST_RESPONSE for Express, FIRE_AND_FORGET for Standard)
- Step Function type and timeout
- Failure detection mechanism (synchronous for Express, EventBridge rule for Standard)

### 6. Flexible Step Composition

**Step Type Definition:**

The infrastructure supports mixing Lambda and Glue steps in any order:

```kotlin
sealed class WorkflowStepType {
    data class Lambda(val step: LambdaStep) : WorkflowStepType()
    data class Glue(val step: GlueStep) : WorkflowStepType()
}

data class LambdaStep(
    val stateName: String,
    val lambdaFunctionKey: String
)

data class GlueStep(
    val stateName: String,
    val glueJobName: String
)
```

**Example Workflow Configurations:**

```kotlin
// Express Workflow: All Lambdas
val expressSteps = listOf(
    WorkflowStepType.Lambda(LambdaStep("ETLStage", "etlLambda")),
    WorkflowStepType.Lambda(LambdaStep("FilterStage", "filterLambda")),
    WorkflowStepType.Lambda(LambdaStep("ScoreStage", "scoreLambda")),
    WorkflowStepType.Lambda(LambdaStep("StoreStage", "storeLambda")),
    WorkflowStepType.Lambda(LambdaStep("ReactiveStage", "reactiveLambda"))
)

// Standard Workflow: Glue job inserted between Lambdas
val standardSteps = listOf(
    WorkflowStepType.Lambda(LambdaStep("ETLStage", "etlLambda")),
    WorkflowStepType.Lambda(LambdaStep("FilterStage", "filterLambda")),
    WorkflowStepType.Glue(GlueStep("HeavyETLStage", "heavy-etl-job")),  // ← Glue job inserted here
    WorkflowStepType.Lambda(LambdaStep("ScoreStage", "scoreLambda")),
    WorkflowStepType.Lambda(LambdaStep("StoreStage", "storeLambda")),
    WorkflowStepType.Lambda(LambdaStep("ReactiveStage", "reactiveLambda"))
)

// Standard Workflow: Multiple Glue jobs
val multiGlueSteps = listOf(
    WorkflowStepType.Glue(GlueStep("InitialETL", "initial-etl-job")),  // ← Start with Glue
    WorkflowStepType.Lambda(LambdaStep("FilterStage", "filterLambda")),
    WorkflowStepType.Glue(GlueStep("EnrichmentETL", "enrichment-job")),  // ← Another Glue job
    WorkflowStepType.Lambda(LambdaStep("StoreStage", "storeLambda"))
)
```

**Key Features:**
- **Position Flexibility**: Glue jobs can be inserted at any position (beginning, middle, end)
- **Multiple Glue Jobs**: Support for multiple Glue jobs in the same workflow
- **Automatic Chaining**: Infrastructure automatically chains steps via S3 intermediate storage
- **Convention-Based**: Each step (Lambda or Glue) follows the same S3 path convention

**Validation:**
- If `WorkflowType.EXPRESS` is selected with `WorkflowStepType.Glue` steps → CDK validation error
- If any Lambda can exceed 5 minutes with `WorkflowType.EXPRESS` → Warning (user responsibility)

### 7. Express Workflow CDK Definition

**For Fast Processing (<5 minutes, Lambda-only):**
```kotlin
fun createExpressWorkflow(
    scope: Construct,
    workflowName: String,
    steps: List<WorkflowStepType.Lambda>,  // Only Lambda steps allowed
    lambdaFunctions: Map<String, IFunction>,
    workflowBucket: IBucket,
    queue: IQueue
): StateMachine {
    
    // Validate: Express workflows cannot have Glue steps
    require(steps.all { it is WorkflowStepType.Lambda }) {
        "Express workflows only support Lambda steps. Use Standard workflow for Glue jobs."
    }
    
    // Extract Lambda steps
    val lambdaSteps = steps.map { (it as WorkflowStepType.Lambda).step }
    
    // Create Lambda invoke tasks
    val states = lambdaSteps.mapIndexed { index, step ->
        val lambdaTask = LambdaInvoke.Builder.create(scope, step.stateName)
            .lambdaFunction(lambdaFunctions[step.lambdaFunctionKey])
            .payload(TaskInput.fromObject(mapOf(
                "executionId" to JsonPath.stringAt("\$.Execution.Name"),
                "currentStage" to step.stateName,
                "previousStage" to if (index > 0) lambdaSteps[index - 1].stateName else null,
                "workflowBucket" to workflowBucket.bucketName,
                "initialData" to JsonPath.stringToJson(JsonPath.stringAt("\$[0].body"))
            )))
            .resultPath(JsonPath.DISCARD)  // Discard large responses
            .retryOnServiceExceptions(false)
            .build()
        
        // Add retry configuration
        lambdaTask.addRetry(
            RetryProps.builder()
                .errors(listOf(Errors.ALL))
                .interval(Duration.seconds(20))
                .maxAttempts(2)
                .backoffRate(2.0)
                .jitterStrategy(JitterType.NONE)
                .build()
        )
        
        lambdaTask
    }
    
    // Chain states sequentially
    states.forEachIndexed { index, state ->
        if (index > 0) {
            states[index - 1].next(state)
        }
    }
    
    // Create Express Step Function
    val stateMachine = StateMachine.Builder.create(scope, "ExpressWorkflow")
        .stateMachineName("$workflowName-Express")
        .definitionBody(DefinitionBody.fromChainable(states[0]))
        .stateMachineType(StateMachineType.EXPRESS)
        .tracingEnabled(true)
        .logs(LogOptions.builder()
            .destination(LogGroup.Builder.create(scope, "StateMachineLogs")
                .logGroupName("/aws/stepfunction/$workflowName")
                .retention(RetentionDays.TWO_WEEKS)
                .build())
            .includeExecutionData(true)
            .level(LogLevel.ALL)
            .build())
        .build()
    
    // Grant S3 permissions
    workflowBucket.grantReadWrite(stateMachine)
    
    // Create EventBridge Pipe with synchronous invocation
    Pipe.Builder.create(scope, "EventPipe")
        .pipeName("$workflowName-event-pipe")
        .source(SqsSource.Builder.create(queue)
            .batchSize(1)
            .build())
        .target(SfnStateMachine.Builder.create(stateMachine)
            .invocationType(StateMachineInvocationType.REQUEST_RESPONSE)
            .build())
        .desiredState(DesiredState.RUNNING)
        .build()
    
    return stateMachine
}
```

### 8. Standard Workflow CDK Definition

**For Long-Running Jobs (with flexible Lambda/Glue composition):**
```kotlin
fun createStandardWorkflow(
    scope: Construct,
    workflowName: String,
    steps: List<WorkflowStepType>,  // Mix of Lambda and Glue steps
    lambdaFunctions: Map<String, IFunction>,
    workflowBucket: IBucket,
    queue: IQueue
): StateMachine {
    
    val chainableSteps = mutableListOf<IChainable>()
    
    steps.forEachIndexed { index, stepType ->
        val previousStage = if (index > 0) {
            when (val prev = steps[index - 1]) {
                is WorkflowStepType.Lambda -> prev.step.stateName
                is WorkflowStepType.Glue -> prev.step.stateName
            }
        } else null
        
        when (stepType) {
            is WorkflowStepType.Lambda -> {
                val step = stepType.step
                val lambdaTask = LambdaInvoke.Builder.create(scope, step.stateName)
                    .lambdaFunction(lambdaFunctions[step.lambdaFunctionKey])
                    .payload(TaskInput.fromObject(mapOf(
                        "executionId" to JsonPath.stringAt("\$.Execution.Name"),
                        "currentStage" to step.stateName,
                        "previousStage" to previousStage,
                        "workflowBucket" to workflowBucket.bucketName,
                        "initialData" to JsonPath.stringToJson(JsonPath.stringAt("\$[0].body"))
                    )))
                    .resultPath(JsonPath.DISCARD)
                    .build()
                
                lambdaTask.addRetry(
                    RetryProps.builder()
                        .errors(listOf(Errors.ALL))
                        .interval(Duration.seconds(20))
                        .maxAttempts(2)
                        .backoffRate(2.0)
                        .build()
                )
                
                chainableSteps.add(lambdaTask)
            }
            
            is WorkflowStepType.Glue -> {
                val step = stepType.step
                val glueTask = GlueStartJobRun.Builder.create(scope, step.stateName)
                    .glueJobName(step.glueJobName)
                    .integrationPattern(IntegrationPattern.RUN_JOB)  // Wait for completion
                    .arguments(TaskInput.fromObject(mapOf(
                        "--execution-id" to JsonPath.stringAt("\$.executionId"),
                        "--input-bucket" to workflowBucket.bucketName,
                        "--input-key" to JsonPath.format(
                            "executions/{}/{}}/output.json",
                            JsonPath.stringAt("\$.executionId"),
                            previousStage ?: "initial"
                        ),
                        "--output-bucket" to workflowBucket.bucketName,
                        "--output-key" to JsonPath.format(
                            "executions/{}/{}/output.json",
                            JsonPath.stringAt("\$.executionId"),
                            step.stateName
                        )
                    )))
                    .resultPath("\$.glueJobResult")
                    .build()
                
                glueTask.addRetry(
                    RetryProps.builder()
                        .errors(listOf("States.ALL"))
                        .interval(Duration.minutes(5))
                        .maxAttempts(2)
                        .backoffRate(2.0)
                        .build()
                )
                
                chainableSteps.add(glueTask)
            }
        }
    }
    
    // Chain all steps
    chainableSteps.forEachIndexed { index, step ->
        if (index > 0) {
            (chainableSteps[index - 1] as INextable).next(step)
        }
    }
    
    // Create Standard Step Function
    val stateMachine = StateMachine.Builder.create(scope, "StandardWorkflow")
        .stateMachineName("$workflowName-Standard")
        .definitionBody(DefinitionBody.fromChainable(chainableSteps[0]))
        .stateMachineType(StateMachineType.STANDARD)
        .tracingEnabled(true)
        .logs(LogOptions.builder()
            .destination(LogGroup.Builder.create(scope, "StateMachineLogs")
                .logGroupName("/aws/stepfunction/$workflowName")
                .retention(RetentionDays.TWO_WEEKS)
                .build())
            .includeExecutionData(true)
            .level(LogLevel.ALL)
            .build())
        .build()
    
    // Grant S3 permissions
    workflowBucket.grantReadWrite(stateMachine)
    
    // Create EventBridge Pipe with asynchronous invocation
    Pipe.Builder.create(scope, "EventPipe")
        .pipeName("$workflowName-event-pipe")
        .source(SqsSource.Builder.create(queue)
            .batchSize(1)
            .build())
        .target(SfnStateMachine.Builder.create(stateMachine)
            .invocationType(StateMachineInvocationType.FIRE_AND_FORGET)
            .build())
        .desiredState(DesiredState.RUNNING)
        .build()
    
    // Create failure detection EventBridge rule
    Rule.Builder.create(scope, "WorkflowFailureRule")
        .eventPattern(EventPattern.builder()
            .source(listOf("aws.states"))
            .detailType(listOf("Step Functions Execution Status Change"))
            .detail(mapOf(
                "status" to listOf("FAILED", "TIMED_OUT", "ABORTED"),
                "stateMachineArn" to listOf(stateMachine.stateMachineArn)
            ))
            .build())
        .targets(listOf(
            // Add SNS topic or Lambda for failure handling
        ))
        .build()
    
    return stateMachine
}
```

### 9. Glue Job Script Pattern

**PySpark Script for ETL Processing:**
```python
import sys
from awsglue.utils import getResolvedOptions
from pyspark.context import SparkContext
from awsglue.context import GlueContext
from awsglue.job import Job

# Get arguments from Step Functions
args = getResolvedOptions(sys.argv, [
    'JOB_NAME',
    'execution-id',
    'input-bucket',
    'input-key',
    'output-bucket',
    'output-key'
])

sc = SparkContext()
glueContext = GlueContext(sc)
spark = glueContext.spark_session
job = Job(glueContext)
job.init(args['JOB_NAME'], args)

# Read input from S3 (previous stage output)
input_path = f"s3://{args['input-bucket']}/{args['input-key']}"
print(f"Reading input from {input_path}")
df = spark.read.json(input_path)

# Perform ETL transformations
# Example: Complex data enrichment, joins, aggregations
transformed_df = df.transform(...)  # Your ETL logic here

# Write output to S3 (for next stage)
output_path = f"s3://{args['output-bucket']}/{args['output-key']}"
print(f"Writing output to {output_path}")
transformed_df.write.mode('overwrite').json(output_path)

job.commit()
```

## Data Models

### Execution Context Model

```kotlin
data class ExecutionContext(
    val executionId: String,        // Step Functions execution name
    val currentStage: String,        // Current Lambda stage name
    val previousStage: String?,      // Previous stage name (null for first)
    val workflowBucket: String,      // S3 bucket for intermediate storage
    val initialData: JsonNode?       // SQS message body (first stage only)
)
```

### Stage Result Model

```kotlin
data class StageResult(
    val status: String,              // "SUCCESS" or "FAILED"
    val stage: String,               // Stage name that produced this result
    val recordsProcessed: Int,       // Number of records processed
    val errorMessage: String? = null // Error details if failed
)
```

### S3 Output Model

Each stage writes a JSON document to S3 with stage-specific structure. Example:

```json
{
  "executionId": "abc-123-def-456",
  "stage": "FilterStage",
  "timestamp": "2024-01-15T10:35:00Z",
  "data": {
    "filteredCandidates": [...],
    "filterCount": 42,
    "filterCriteria": {...}
  },
  "metadata": {
    "processingTimeMs": 1234,
    "inputRecordCount": 100,
    "outputRecordCount": 42
  }
}
```

### Workflow Configuration Model

```kotlin
data class WorkflowConfiguration(
    val workflowName: String,
    val workflowType: WorkflowType,  // EXPRESS or STANDARD
    val steps: List<WorkflowStepType>,
    val workflowBucket: String,
    val sourceQueue: String,
    val retryConfig: RetryConfiguration
)

enum class WorkflowType {
    EXPRESS,    // <5 minutes, synchronous
    STANDARD    // Up to 1 year, asynchronous
}

data class RetryConfiguration(
    val lambdaMaxAttempts: Int = 2,
    val lambdaIntervalSeconds: Int = 20,
    val lambdaBackoffRate: Double = 2.0,
    val glueMaxAttempts: Int = 2,
    val glueIntervalMinutes: Int = 5,
    val glueBackoffRate: Double = 2.0
)
```

## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system—essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*


### Property Reflection

After analyzing all acceptance criteria, I identified several redundant properties that can be consolidated:

**Redundancies Identified:**
1. Requirements 1.2, 11.1 both test explicit function name configuration → Combine into single property
2. Requirements 2.5, 9.7 both test S3 organization by executionId → Duplicate, keep 2.5
3. Requirements 4.5, 8.1, 11.5 all test Lambda retry configuration → Combine into single property
4. Requirements 5.7, 8.2, 11.6 all test Glue retry configuration → Combine into single property
5. Requirements 6.3, 11.3 both test workflow type configuration → Duplicate, keep 6.3
6. Requirements 9.1, 9.2, 11.7 all test observability configuration → Combine into single property
7. Requirements 2.7, 11.8 both test S3 lifecycle policy → Duplicate, keep 2.7
8. Requirements 2.2 and 3.2 both test output path construction → Can be combined (output path uses currentStage)
9. Requirements 2.3 and 3.1 both test input path construction → Can be combined (input path uses previousStage)

**Properties After Consolidation:**
- Lambda naming: 1 property (covers 1.1, 1.2, 1.3, 1.4)
- S3 path conventions: 2 properties (input path, output path)
- S3 organization: 1 property (by executionId)
- Workflow configuration: 3 properties (Express, Standard, type selection)
- Retry configuration: 2 properties (Lambda, Glue)
- Observability: 2 properties (tracing/logging, Lambda logging)
- Error handling: 3 properties (failure states, DLQ, failure detection)
- IAM permissions: 1 property (S3 access)
- Glue integration: 3 properties (arguments, I/O, integration pattern)
- Backward compatibility: 2 properties (naming patterns, incremental migration)

**Total: ~20 unique properties** (down from 60+ testable criteria)

### Correctness Properties

Property 1: Lambda Function Name Format
*For any* Lambda function created by CDK infrastructure, the function name SHALL match the pattern `{StackName}-{Environment}-{FunctionPurpose}` without random suffixes, and the CloudWatch log group SHALL be `/aws/lambda/{FunctionName}`
**Validates: Requirements 1.1, 1.2, 1.3, 1.4**

Property 2: S3 Output Path Convention
*For any* Lambda function execution, the output SHALL be written to S3 at path `executions/{executionId}/{currentStage}/output.json` where executionId and currentStage come from the execution context
**Validates: Requirements 2.2, 3.2**

Property 3: S3 Input Path Convention
*For any* non-first Lambda function execution, the input SHALL be read from S3 at path `executions/{executionId}/{previousStage}/output.json` where executionId and previousStage come from the execution context
**Validates: Requirements 2.3, 3.1**

Property 4: First Stage Input Source
*For any* workflow execution, the first Lambda function SHALL use the initialData field from the execution context instead of reading from S3
**Validates: Requirements 2.4**

Property 5: S3 Organization by Execution ID
*For any* workflow execution, all stage outputs SHALL be stored under the same S3 prefix `executions/{executionId}/` to enable correlation
**Validates: Requirements 2.5**

Property 6: S3 Output Persistence
*For any* workflow execution (completed or failed), the intermediate output files SHALL remain in S3 after execution completes
**Validates: Requirements 2.6**

Property 7: S3 Lifecycle Policy
*For any* S3 workflow bucket, a lifecycle policy SHALL be configured to automatically delete objects under `executions/` prefix after 7 days
**Validates: Requirements 2.7**

Property 8: Execution Context Payload Structure
*For any* Lambda invocation in a Step Functions workflow, the payload SHALL contain executionId, currentStage, previousStage (or null), workflowBucket, and initialData (or null) fields, and SHALL NOT contain large data payloads
**Validates: Requirements 2.1**

Property 9: No Hardcoded Dependencies
*For any* Lambda function implementation, the code SHALL NOT contain hardcoded references to other Lambda functions, specific S3 bucket names, or specific S3 paths
**Validates: Requirements 3.3**

Property 10: Stage Reordering Independence
*For any* Lambda function, if the Step Function stage order is modified (stages reordered, added, or removed), the Lambda code SHALL continue to work without modification
**Validates: Requirements 3.4**

Property 11: Independent Testability
*For any* Lambda function, it SHALL be testable in isolation by providing mock execution context parameters without requiring actual Step Functions execution
**Validates: Requirements 3.5**

Property 12: Express Workflow Configuration
*For any* Express workflow created by CDK, the workflow type SHALL be EXPRESS, maximum duration SHALL be 5 minutes, CloudWatch Logs SHALL be enabled with includeExecutionData=true, and X-Ray tracing SHALL be enabled
**Validates: Requirements 4.1, 4.7**

Property 13: Express Workflow Invocation Type
*For any* Express workflow, the EventBridge Pipe SHALL use REQUEST_RESPONSE (synchronous) invocation type
**Validates: Requirements 4.2**

Property 14: Express Workflow Failure Handling
*For any* Express workflow execution that fails, the EventBridge Pipe SHALL return failure to SQS, causing the message to become visible again for retry
**Validates: Requirements 4.3**

Property 15: Express Workflow Structure
*For any* Express workflow, Lambda functions SHALL be chained sequentially, each Lambda SHALL write output to S3, and each non-first Lambda SHALL read input from S3
**Validates: Requirements 4.4**

Property 16: Lambda Retry Configuration
*For any* Lambda step in a Step Functions workflow, the retry configuration SHALL specify: errors=[ALL], maxAttempts=2, interval=20 seconds, backoffRate=2.0
**Validates: Requirements 4.5, 8.1**

Property 17: Result Path Discard
*For any* Lambda step in a Step Functions workflow, the resultPath SHALL be set to DISCARD to prevent large responses from bloating execution history
**Validates: Requirements 4.6**

Property 18: Standard Workflow Configuration
*For any* Standard workflow created by CDK, the workflow type SHALL be STANDARD, CloudWatch Logs SHALL be enabled with includeExecutionData=true, and X-Ray tracing SHALL be enabled
**Validates: Requirements 5.1**

Property 19: Standard Workflow Invocation Type
*For any* Standard workflow, the EventBridge Pipe SHALL use FIRE_AND_FORGET (asynchronous) invocation type
**Validates: Requirements 5.2**

Property 20: Mixed Step Type Support
*For any* Standard workflow, the workflow definition SHALL support including both Lambda function steps and Glue job steps in any order
**Validates: Requirements 5.3**

Property 21: Glue Job Integration Pattern
*For any* Glue job step in a Standard workflow, the integration pattern SHALL be RUN_JOB (wait for completion) before proceeding to the next stage
**Validates: Requirements 5.4, 7.5**

Property 22: Glue Job Arguments
*For any* Glue job step in a Standard workflow, the job arguments SHALL include --execution-id, --input-bucket, --input-key, --output-bucket, and --output-key
**Validates: Requirements 5.5, 7.2**

Property 23: Glue Job I/O Convention
*For any* Glue job execution, the job SHALL read input from S3 using the --input-bucket and --input-key arguments, and SHALL write output to S3 using the --output-bucket and --output-key arguments following the convention `executions/{executionId}/{stageName}/output.json`
**Validates: Requirements 5.6, 7.3, 7.4**

Property 24: Glue Job Retry Configuration
*For any* Glue job step in a Step Functions workflow, the retry configuration SHALL specify: errors=[States.ALL], maxAttempts=2, interval=5 minutes, backoffRate=2.0
**Validates: Requirements 5.7, 8.2**

Property 25: Workflow Type Selection Validation
*For any* workflow configuration, if all stages complete within 5 minutes, Express workflow SHALL be supported; if any stage can exceed 5 minutes, Standard workflow SHALL be required
**Validates: Requirements 6.1, 6.2**

Property 26: Workflow Type Configuration Interface
*For any* CDK infrastructure code, there SHALL be a clear configuration option to specify workflow type (Express or Standard)
**Validates: Requirements 6.3**

Property 27: Automatic Invocation Type Configuration
*For any* workflow, the EventBridge Pipe invocation type SHALL be automatically configured based on workflow type: REQUEST_RESPONSE for Express, FIRE_AND_FORGET for Standard
**Validates: Requirements 6.5**

Property 28: Glue Job Positioning Flexibility
*For any* Standard workflow, Glue job steps SHALL be insertable at any position in the stage sequence (beginning, middle, or end)
**Validates: Requirements 7.1**

Property 29: Glue Job Failure Handling
*For any* Glue job step that fails after all retries, the Step Functions execution SHALL transition to FAILED state and record error details in execution history
**Validates: Requirements 7.6**

Property 30: Step Failure State Transition
*For any* Lambda or Glue job step that fails after exhausting all retry attempts, the Step Functions execution SHALL transition to FAILED state
**Validates: Requirements 8.3**

Property 31: Error Information Recording
*For any* failed Step Functions execution, the execution history SHALL contain error type, error message, and the name of the failed stage
**Validates: Requirements 8.4**

Property 32: SQS Dead Letter Queue Configuration
*For any* SQS queue used as workflow trigger, a Dead Letter Queue SHALL be configured with maxReceiveCount=3
**Validates: Requirements 8.5**

Property 33: Express Workflow SQS Retry
*For any* Express workflow execution that fails, the SQS message SHALL be returned to the queue for retry (up to maxReceiveCount)
**Validates: Requirements 8.6**

Property 34: Standard Workflow Failure Detection
*For any* Standard workflow, an EventBridge rule SHALL be configured to detect execution status changes to FAILED, TIMED_OUT, or ABORTED and trigger alerts
**Validates: Requirements 8.7**

Property 35: Observability Configuration
*For any* Step Functions workflow, X-Ray tracing SHALL be enabled (tracingEnabled=true), CloudWatch Logs SHALL be enabled with includeExecutionData=true and level=ALL, and log retention SHALL be 14 days
**Validates: Requirements 9.1, 9.2, 9.3**

Property 36: Lambda Execution Context Logging
*For any* Lambda function execution, the function SHALL log the executionId and currentStage at the start of invocation
**Validates: Requirements 9.5**

Property 37: Lambda S3 Path Logging
*For any* Lambda function execution, the function SHALL log the S3 input path (if applicable) and output path used for data I/O
**Validates: Requirements 9.6**

Property 38: Dual Naming Pattern Support
*For any* CDK deployment during migration, the infrastructure SHALL support deploying Lambda functions with both old (auto-generated) and new (explicit) naming patterns simultaneously
**Validates: Requirements 10.2**

Property 39: Incremental Stage Migration
*For any* workflow migration to S3-based orchestration, the CDK infrastructure SHALL allow migrating stages incrementally (one at a time) rather than requiring all-at-once migration
**Validates: Requirements 10.3**

Property 40: S3 IAM Permissions
*For any* workflow, the S3 bucket SHALL grant read and write permissions to all Lambda functions and Glue jobs in the workflow via IAM roles
**Validates: Requirements 11.2**

Property 41: EventBridge Pipe Configuration
*For any* workflow, an EventBridge Pipe SHALL be created connecting the SQS queue to the Step Functions workflow with the appropriate invocation type
**Validates: Requirements 11.4**

Property 42: Least-Privilege IAM Roles
*For any* workflow, the IAM roles for Lambda functions, Glue jobs, and Step Functions SHALL follow least-privilege principles, granting only the minimum permissions required for operation
**Validates: Requirements 11.9**

## Error Handling

### Lambda Function Errors

**Transient Errors (Retryable):**
- S3 throttling (503 SlowDown)
- S3 temporary unavailability (500 InternalError)
- Network timeouts
- Lambda cold start timeouts

**Retry Strategy:**
- Maximum 2 retry attempts
- Initial interval: 20 seconds
- Backoff rate: 2x (20s, 40s)
- No jitter (prevents rapid consecutive retries)

**Permanent Errors (Non-Retryable):**
- S3 access denied (403 Forbidden) → IAM permission issue
- S3 object not found (404 NoSuchKey) → Previous stage failed or path incorrect
- Invalid JSON in S3 object → Data corruption
- Lambda timeout (15 minutes) → Processing too slow, consider Glue job

**Error Response:**
```kotlin
data class StageResult(
    val status: String = "FAILED",
    val stage: String,
    val recordsProcessed: Int = 0,
    val errorMessage: String
)
```

### Glue Job Errors

**Transient Errors (Retryable):**
- Glue capacity unavailable
- S3 throttling
- Network issues
- Spark executor failures (partial)

**Retry Strategy:**
- Maximum 2 retry attempts
- Initial interval: 5 minutes
- Backoff rate: 2x (5min, 10min)
- Longer intervals due to Glue job cost

**Permanent Errors (Non-Retryable):**
- Invalid PySpark script syntax
- S3 access denied
- Out of memory (need more DPUs)
- Data schema mismatch

### Step Functions Errors

**Execution Failures:**
- Lambda/Glue step fails after retries → Execution transitions to FAILED
- Execution timeout (5 min for Express, configurable for Standard)
- Invalid state machine definition

**Failure Detection:**
- **Express Workflow**: EventBridge Pipe returns failure synchronously to SQS
- **Standard Workflow**: EventBridge rule detects execution status change and triggers alerts

**Failure Recovery:**
1. Check Step Functions execution history to identify failed stage
2. Check CloudWatch Logs for detailed error messages
3. Download S3 intermediate outputs to inspect data at failure point
4. Fix issue (code bug, IAM permissions, data quality)
5. Redrive execution:
   - Option A: Retry entire workflow from beginning
   - Option B: Resume from failed stage using existing S3 data

### SQS Error Handling

**Message Retry:**
- Visibility timeout: 10 minutes (allows workflow to complete)
- Max receive count: 3 attempts
- After 3 failures → Message moved to Dead Letter Queue

**DLQ Processing:**
- Manual inspection of failed messages
- Identify root cause (data quality, system issue, bug)
- Fix issue and manually reprocess or discard

### Monitoring and Alerting

**CloudWatch Alarms:**
1. **High Failure Rate**: ExecutionsFailed > threshold
2. **Long Execution Time**: ExecutionTime > expected duration
3. **DLQ Messages**: ApproximateNumberOfMessagesVisible in DLQ > 0
4. **Lambda Errors**: Errors metric > threshold for any stage
5. **Glue Job Failures**: Glue job status = FAILED

**Alert Actions:**
- Send SNS notification to operations team
- Trigger Lambda for automated remediation (if applicable)
- Create incident ticket in tracking system

## Testing Strategy

### Dual Testing Approach

This feature requires both **unit tests** and **property-based tests** for comprehensive coverage:

**Unit Tests** focus on:
- Specific examples demonstrating correct behavior
- Edge cases (empty data, malformed JSON, missing fields)
- Error conditions (S3 access denied, network timeout)
- Integration points (S3 client, Step Functions SDK)

**Property-Based Tests** focus on:
- Universal properties that hold for all inputs
- Convention-based path construction across random execution contexts
- S3 organization invariants across random workflow executions
- Configuration validation across random workflow definitions

Together, these approaches provide:
- Unit tests catch concrete bugs in specific scenarios
- Property tests verify general correctness across wide input ranges
- Comprehensive coverage without excessive test maintenance

### Property-Based Testing Configuration

**Framework**: Use Kotest Property Testing for Kotlin
- Minimum 100 iterations per property test (due to randomization)
- Each property test references its design document property
- Tag format: `@Tag("Feature: infrastructure-enhancement, Property {number}: {property_text}")`

**Example Property Test:**
```kotlin
@Test
@Tag("Feature: infrastructure-enhancement, Property 2: S3 Output Path Convention")
fun `output path follows convention for all execution contexts`() = runTest {
    checkAll(100, Arb.executionContext()) { context ->
        val handler = TestLambdaHandler()
        val result = handler.handleRequest(context, mockLambdaContext)
        
        val expectedPath = "executions/${context.executionId}/${context.currentStage}/output.json"
        val actualPath = captureS3PutRequest().key
        
        actualPath shouldBe expectedPath
    }
}
```

### Unit Testing Strategy

**Lambda Handler Tests:**
```kotlin
class FilterLambdaHandlerTest {
    @Test
    fun `filters candidates with score above threshold`() {
        val context = ExecutionContext(
            executionId = "test-123",
            currentStage = "FilterStage",
            previousStage = "ETLStage",
            workflowBucket = "test-bucket",
            initialData = null
        )
        
        // Mock S3 input
        mockS3GetObject("executions/test-123/ETLStage/output.json", """
            {"candidates": [
                {"id": 1, "score": 60},
                {"id": 2, "score": 40},
                {"id": 3, "score": 80}
            ]}
        """)
        
        val handler = FilterLambdaHandler()
        val result = handler.handleRequest(context, mockLambdaContext)
        
        result.status shouldBe "SUCCESS"
        result.recordsProcessed shouldBe 2
        
        // Verify S3 output
        val output = captureS3PutRequest()
        output.key shouldBe "executions/test-123/FilterStage/output.json"
        val data = parseJson(output.body)
        data["filteredCandidates"].size shouldBe 2
    }
    
    @Test
    fun `handles empty candidate list`() {
        // Edge case: empty input
        mockS3GetObject("executions/test-123/ETLStage/output.json", """
            {"candidates": []}
        """)
        
        val result = handler.handleRequest(context, mockLambdaContext)
        
        result.status shouldBe "SUCCESS"
        result.recordsProcessed shouldBe 0
    }
    
    @Test
    fun `handles S3 access denied error`() {
        // Error condition
        mockS3GetObjectThrows(AccessDeniedException("Access denied"))
        
        shouldThrow<AccessDeniedException> {
            handler.handleRequest(context, mockLambdaContext)
        }
    }
}
```

**CDK Infrastructure Tests:**
```kotlin
class WorkflowStackTest {
    @Test
    fun `creates Express workflow with correct configuration`() {
        val app = App()
        val stack = WorkflowStack(app, "TestStack", WorkflowConfiguration(
            workflowName = "TestWorkflow",
            workflowType = WorkflowType.EXPRESS,
            steps = listOf(/* ... */),
            workflowBucket = "test-bucket",
            sourceQueue = "test-queue",
            retryConfig = RetryConfiguration()
        ))
        
        val template = Template.fromStack(stack)
        
        // Verify Step Function configuration
        template.hasResourceProperties("AWS::StepFunctions::StateMachine", mapOf(
            "StateMachineType" to "EXPRESS",
            "TracingConfiguration" to mapOf("Enabled" to true),
            "LoggingConfiguration" to mapOf(
                "IncludeExecutionData" to true,
                "Level" to "ALL"
            )
        ))
        
        // Verify EventBridge Pipe invocation type
        template.hasResourceProperties("AWS::Pipes::Pipe", mapOf(
            "Target" to mapOf(
                "StepFunctionStateMachineParameters" to mapOf(
                    "InvocationType" to "REQUEST_RESPONSE"
                )
            )
        ))
    }
    
    @Test
    fun `creates Lambda with explicit function name`() {
        val template = Template.fromStack(stack)
        
        template.hasResourceProperties("AWS::Lambda::Function", mapOf(
            "FunctionName" to Match.stringLikeRegexp(".*-dev-FilterLambdaFunction")
        ))
    }
    
    @Test
    fun `grants S3 permissions to Lambda functions`() {
        val template = Template.fromStack(stack)
        
        // Verify IAM policy grants S3 read/write
        template.hasResourceProperties("AWS::IAM::Policy", mapOf(
            "PolicyDocument" to mapOf(
                "Statement" to Match.arrayWith(listOf(
                    mapOf(
                        "Action" to listOf("s3:GetObject", "s3:PutObject"),
                        "Effect" to "Allow",
                        "Resource" to Match.stringLikeRegexp("arn:aws:s3:::.*-workflow-bucket/executions/*")
                    )
                ))
            )
        ))
    }
}
```

**Integration Tests:**
```kotlin
@IntegrationTest
class WorkflowIntegrationTest {
    @Test
    fun `executes complete workflow end-to-end`() = runTest {
        // Deploy test stack
        val stack = deployTestStack()
        
        // Send test message to SQS
        val messageId = sendSqsMessage(stack.queueUrl, """
            {"testData": "sample"}
        """)
        
        // Wait for workflow completion
        val execution = waitForExecution(stack.stateMachineArn, timeout = 5.minutes)
        
        execution.status shouldBe "SUCCEEDED"
        
        // Verify S3 outputs exist for all stages
        val stages = listOf("ETLStage", "FilterStage", "ScoreStage", "StoreStage", "ReactiveStage")
        stages.forEach { stage ->
            val output = s3Client.getObject(
                stack.workflowBucket,
                "executions/${execution.executionId}/$stage/output.json"
            )
            output shouldNotBe null
        }
        
        // Verify final output correctness
        val finalOutput = parseJson(s3Client.getObject(
            stack.workflowBucket,
            "executions/${execution.executionId}/ReactiveStage/output.json"
        ))
        finalOutput["status"] shouldBe "completed"
    }
    
    @Test
    fun `handles Lambda failure with retry`() = runTest {
        // Inject failure in FilterStage (first attempt)
        injectFailure(stack.filterLambda, attempts = 1)
        
        val execution = waitForExecution(stack.stateMachineArn)
        
        // Should succeed after retry
        execution.status shouldBe "SUCCEEDED"
        execution.retryCount shouldBe 1
    }
    
    @Test
    fun `moves message to DLQ after max retries`() = runTest {
        // Inject permanent failure
        injectFailure(stack.filterLambda, attempts = 10)
        
        // Send message
        sendSqsMessage(stack.queueUrl, """{"testData": "sample"}""")
        
        // Wait for DLQ message
        val dlqMessage = waitForDlqMessage(stack.dlqUrl, timeout = 15.minutes)
        
        dlqMessage shouldNotBe null
        dlqMessage.body shouldContain "testData"
    }
}
```

### Test Coverage Goals

**Unit Tests:**
- 100% coverage of Lambda handler logic
- 100% coverage of CDK construct creation
- All error conditions tested
- All edge cases tested

**Property-Based Tests:**
- All 42 correctness properties implemented
- Minimum 100 iterations per property
- Random input generation for execution contexts, workflow configurations, and data payloads

**Integration Tests:**
- End-to-end workflow execution (happy path)
- Failure and retry scenarios
- DLQ handling
- S3 lifecycle policy verification

### Continuous Testing

**Pre-Deployment:**
- Run all unit tests and property tests in CI/CD pipeline
- Fail deployment if any test fails
- Generate code coverage report (target: >90%)

**Post-Deployment:**
- Run integration tests against deployed stack
- Monitor CloudWatch metrics for anomalies
- Verify S3 lifecycle policy is working (check after 7 days)

**Ongoing:**
- Synthetic monitoring: Send test messages hourly
- Alert on test failures
- Track execution time trends
