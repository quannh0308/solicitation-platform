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
        // Validate: Express workflows cannot contain Glue steps
        if (workflowType == WorkflowType.EXPRESS) {
            val hasGlueSteps = steps.any { it is WorkflowStepType.Glue }
            require(!hasGlueSteps) {
                "Express workflows only support Lambda steps. Use Standard workflow for Glue jobs. " +
                "Found ${steps.count { it is WorkflowStepType.Glue }} Glue step(s) in configuration."
            }
        }
        
        // Validate: All Lambda function keys must exist in the functions map
        steps.filterIsInstance<WorkflowStepType.Lambda>().forEach { lambdaStep ->
            require(lambdaFunctions.containsKey(lambdaStep.step.lambdaFunctionKey)) {
                "Lambda function key '${lambdaStep.step.lambdaFunctionKey}' not found in functions map. " +
                "Available keys: ${lambdaFunctions.keys.joinToString(", ")}"
            }
        }
        
        // Validate: At least one step is required
        require(steps.isNotEmpty()) {
            "Workflow must contain at least one step. Received empty steps list."
        }
    }
}
