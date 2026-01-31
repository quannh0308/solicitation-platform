package com.ceap.infrastructure.workflow

import software.amazon.awscdk.services.lambda.IFunction
import software.amazon.awscdk.services.s3.IBucket
import software.amazon.awscdk.services.sqs.IQueue

/**
 * Workflow type selection for Step Functions orchestration.
 * 
 * EXPRESS: Fast processing (<5 minutes), synchronous invocation, $1 per million transitions
 * STANDARD: Long-running jobs (up to 1 year), asynchronous invocation, $25 per million transitions
 * 
 * Validates: Requirement 6.3
 */
enum class WorkflowType {
    /**
     * Express workflow for fast processing pipelines.
     * - Maximum duration: 5 minutes
     * - Invocation type: REQUEST_RESPONSE (synchronous)
     * - Cost: $1 per million state transitions
     * - Use case: All stages complete within 5 minutes, Lambda-only
     */
    EXPRESS,
    
    /**
     * Standard workflow for long-running pipelines.
     * - Maximum duration: 1 year
     * - Invocation type: FIRE_AND_FORGET (asynchronous)
     * - Cost: $25 per million state transitions
     * - Use case: Any stage can exceed 5 minutes, supports Glue jobs
     */
    STANDARD
}

/**
 * Workflow step type - supports Lambda functions and Glue jobs.
 * 
 * Validates: Requirement 6.3
 */
sealed class WorkflowStepType {
    /**
     * Lambda function step in the workflow.
     */
    data class Lambda(val step: LambdaStep) : WorkflowStepType()
    
    /**
     * Glue job step in the workflow (Standard workflow only).
     */
    data class Glue(val step: GlueStep) : WorkflowStepType()
}

/**
 * Lambda step configuration.
 * 
 * @property stateName Name of the Step Functions state (e.g., "ETLStage", "FilterStage")
 * @property lambdaFunctionKey Key to look up the Lambda function in the functions map
 */
data class LambdaStep(
    val stateName: String,
    val lambdaFunctionKey: String
)

/**
 * Glue job step configuration.
 * 
 * @property stateName Name of the Step Functions state (e.g., "HeavyETLStage")
 * @property glueJobName Name of the Glue job to execute
 */
data class GlueStep(
    val stateName: String,
    val glueJobName: String
)

/**
 * Retry configuration for workflow steps.
 * 
 * @property lambdaMaxAttempts Maximum retry attempts for Lambda steps (default: 2)
 * @property lambdaIntervalSeconds Initial retry interval for Lambda steps in seconds (default: 20)
 * @property lambdaBackoffRate Backoff rate for Lambda retries (default: 2.0)
 * @property glueMaxAttempts Maximum retry attempts for Glue steps (default: 2)
 * @property glueIntervalMinutes Initial retry interval for Glue steps in minutes (default: 5)
 * @property glueBackoffRate Backoff rate for Glue retries (default: 2.0)
 * 
 * Validates: Requirements 4.5, 5.7, 8.1, 8.2
 */
data class RetryConfiguration(
    val lambdaMaxAttempts: Int = 2,
    val lambdaIntervalSeconds: Int = 20,
    val lambdaBackoffRate: Double = 2.0,
    val glueMaxAttempts: Int = 2,
    val glueIntervalMinutes: Int = 5,
    val glueBackoffRate: Double = 2.0
)

/**
 * Complete workflow configuration for Step Functions orchestration.
 * 
 * @property workflowName Name of the workflow (used for resource naming)
 * @property workflowType Type of workflow (EXPRESS or STANDARD)
 * @property steps List of workflow steps (Lambda or Glue)
 * @property lambdaFunctions Map of Lambda functions by key
 * @property workflowBucket S3 bucket for intermediate storage
 * @property sourceQueue SQS queue that triggers the workflow
 * @property retryConfig Retry configuration for workflow steps
 * 
 * Validates: Requirement 6.3
 */
data class WorkflowConfiguration(
    val workflowName: String,
    val workflowType: WorkflowType,
    val steps: List<WorkflowStepType>,
    val lambdaFunctions: Map<String, IFunction>,
    val workflowBucket: IBucket,
    val sourceQueue: IQueue,
    val retryConfig: RetryConfiguration = RetryConfiguration()
) {
    init {
        // Validate configuration on construction
        validate()
    }
    
    /**
     * Validates the workflow configuration.
     * 
     * This method performs comprehensive validation including:
     * - Express workflows cannot contain Glue steps
     * - All Lambda function keys must exist in the functions map
     * - At least one step is required
     * - Workflow name must not be empty
     * 
     * @throws IllegalArgumentException if validation fails with descriptive error message
     * 
     * Validates: Requirements 6.1, 6.2, 6.4
     */
    private fun validate() {
        // Validate: Workflow name must not be empty
        require(workflowName.isNotBlank()) {
            "Workflow name cannot be empty or blank."
        }
        
        // Validate: At least one step is required
        require(steps.isNotEmpty()) {
            "Workflow must contain at least one step. Received empty steps list."
        }
        
        // Validate: Express workflows cannot contain Glue steps (Requirement 6.1, 6.2)
        if (workflowType == WorkflowType.EXPRESS) {
            val glueSteps = steps.filterIsInstance<WorkflowStepType.Glue>()
            require(glueSteps.isEmpty()) {
                buildString {
                    appendLine("Express workflows only support Lambda steps. Use Standard workflow for Glue jobs.")
                    appendLine()
                    appendLine("Reason: Express workflows have a 5-minute maximum duration and use synchronous")
                    appendLine("invocation (REQUEST_RESPONSE). Glue jobs typically run longer than 5 minutes and")
                    appendLine("require asynchronous invocation (FIRE_AND_FORGET) available in Standard workflows.")
                    appendLine()
                    appendLine("Found ${glueSteps.size} Glue step(s) in configuration:")
                    glueSteps.forEach { glueStep ->
                        appendLine("  - ${glueStep.step.stateName} (Glue job: ${glueStep.step.glueJobName})")
                    }
                    appendLine()
                    appendLine("Solution: Change workflowType to WorkflowType.STANDARD to use Glue jobs.")
                }
            }
        }
        
        // Validate: All Lambda function keys must exist in the functions map
        val lambdaSteps = steps.filterIsInstance<WorkflowStepType.Lambda>()
        lambdaSteps.forEach { lambdaStep ->
            val functionKey = lambdaStep.step.lambdaFunctionKey
            require(lambdaFunctions.containsKey(functionKey)) {
                buildString {
                    appendLine("Lambda function key '$functionKey' not found in functions map.")
                    appendLine()
                    appendLine("Step: ${lambdaStep.step.stateName}")
                    appendLine("Missing key: $functionKey")
                    appendLine()
                    appendLine("Available function keys:")
                    if (lambdaFunctions.isEmpty()) {
                        appendLine("  (none - functions map is empty)")
                    } else {
                        lambdaFunctions.keys.sorted().forEach { key ->
                            appendLine("  - $key")
                        }
                    }
                    appendLine()
                    appendLine("Solution: Add the Lambda function to the lambdaFunctions map with key '$functionKey'")
                    appendLine("or update the step configuration to use an existing function key.")
                }
            }
        }
        
        // Validate: Step names must be unique
        val stepNames = steps.map { step ->
            when (step) {
                is WorkflowStepType.Lambda -> step.step.stateName
                is WorkflowStepType.Glue -> step.step.stateName
            }
        }
        val duplicateNames = stepNames.groupingBy { it }.eachCount().filter { it.value > 1 }
        require(duplicateNames.isEmpty()) {
            buildString {
                appendLine("Step names must be unique within a workflow.")
                appendLine()
                appendLine("Duplicate step names found:")
                duplicateNames.forEach { (name, count) ->
                    appendLine("  - '$name' appears $count times")
                }
                appendLine()
                appendLine("Solution: Rename duplicate steps to have unique names.")
            }
        }
    }
}
