package com.ceap.infrastructure.workflow

import software.amazon.awscdk.Duration
import software.amazon.awscdk.services.logs.LogGroup
import software.amazon.awscdk.services.logs.RetentionDays
import software.amazon.awscdk.services.pipes.CfnPipe
import software.amazon.awscdk.services.stepfunctions.*
import software.amazon.awscdk.services.stepfunctions.tasks.LambdaInvoke
import software.constructs.Construct

/**
 * Factory for creating Step Functions workflows with S3-based orchestration.
 * 
 * This factory provides methods to create Express and Standard workflows
 * following AWS best practices for large-scale data processing.
 * 
 * Key features:
 * - Convention-based S3 path resolution
 * - Automatic retry configuration
 * - CloudWatch Logs and X-Ray tracing
 * - Support for Lambda and Glue steps
 */
object WorkflowFactory {
    
    /**
     * Creates an Express workflow for fast processing pipelines.
     * 
     * Express workflows are designed for:
     * - All stages complete within 5 minutes
     * - Lambda-only processing (no Glue jobs)
     * - Synchronous invocation (REQUEST_RESPONSE)
     * - Cost-optimized ($1 per million transitions)
     * 
     * Features:
     * - Sequential Lambda chaining with S3 intermediate storage
     * - Automatic retry logic (2 attempts, 20s interval, 2x backoff)
     * - Result path set to DISCARD to prevent large responses
     * - CloudWatch Logs with full execution data
     * - X-Ray tracing enabled
     * 
     * @param scope CDK construct scope
     * @param config Workflow configuration
     * @return Created StateMachine
     * 
     * @throws IllegalArgumentException if config contains Glue steps
     * 
     * Validates: Requirements 4.1, 4.4, 4.5, 4.6, 4.7
     */
    fun createExpressWorkflow(
        scope: Construct,
        config: WorkflowConfiguration
    ): StateMachine {
        // Validate: Express workflows only support Lambda steps
        require(config.workflowType == WorkflowType.EXPRESS) {
            "createExpressWorkflow requires WorkflowType.EXPRESS. " +
            "Received: ${config.workflowType}"
        }
        
        val hasGlueSteps = config.steps.any { it is WorkflowStepType.Glue }
        require(!hasGlueSteps) {
            "Express workflows only support Lambda steps. Use createStandardWorkflow for Glue jobs. " +
            "Found ${config.steps.count { it is WorkflowStepType.Glue }} Glue step(s)."
        }
        
        // Extract Lambda steps
        val lambdaSteps = config.steps.map { 
            (it as WorkflowStepType.Lambda).step 
        }
        
        // Create Lambda invoke tasks with execution context payload
        val states = lambdaSteps.mapIndexed { index, step ->
            val lambdaFunction = config.lambdaFunctions[step.lambdaFunctionKey]
                ?: error("Lambda function not found for key: ${step.lambdaFunctionKey}")
            
            // Determine previous stage name (null for first stage)
            val previousStage = if (index > 0) {
                lambdaSteps[index - 1].stateName
            } else {
                null
            }
            
            // Create Lambda invoke task with execution context payload
            // Payload structure:
            // {
            //   "executionId": "$.Execution.Name",
            //   "currentStage": "ETLStage",
            //   "previousStage": null | "PreviousStage",
            //   "workflowBucket": "ceap-workflow-dev-123456789",
            //   "initialData": "$.body" (parsed from SQS message)
            // }
            val lambdaTask = LambdaInvoke.Builder.create(scope, step.stateName)
                .lambdaFunction(lambdaFunction)
                .payload(TaskInput.fromObject(mapOf(
                    "executionId" to JsonPath.stringAt("\$.Execution.Name"),
                    "currentStage" to step.stateName,
                    "previousStage" to previousStage,
                    "workflowBucket" to config.workflowBucket.bucketName,
                    "initialData" to JsonPath.stringToJson(JsonPath.stringAt("\$[0].body"))
                )))
                .resultPath(JsonPath.DISCARD)  // Discard large responses (Requirement 4.6)
                .retryOnServiceExceptions(false)  // We configure retries manually
                .build()
            
            // Add retry configuration (Requirement 4.5)
            // - Maximum 2 attempts
            // - Initial interval: 20 seconds
            // - Backoff rate: 2x (20s, 40s)
            // - No jitter (prevents rapid consecutive retries)
            lambdaTask.addRetry(
                RetryProps.builder()
                    .errors(listOf(Errors.ALL))
                    .interval(Duration.seconds(config.retryConfig.lambdaIntervalSeconds.toLong()))
                    .maxAttempts(config.retryConfig.lambdaMaxAttempts)
                    .backoffRate(config.retryConfig.lambdaBackoffRate)
                    .jitterStrategy(JitterType.NONE)
                    .build()
            )
            
            lambdaTask
        }
        
        // Chain states sequentially (Requirement 4.4)
        // ETL → Filter → Score → Store → Reactive
        states.forEachIndexed { index, state ->
            if (index > 0) {
                states[index - 1].next(state)
            }
        }
        
        // Create CloudWatch Logs log group for Step Functions
        // Retention: 14 days (Requirement 9.3)
        val logGroup = LogGroup.Builder.create(scope, "${config.workflowName}Logs")
            .logGroupName("/aws/stepfunctions/${config.workflowName}")
            .retention(RetentionDays.TWO_WEEKS)
            .build()
        
        // Create Express Step Function (Requirement 4.1)
        val stateMachine = StateMachine.Builder.create(scope, "${config.workflowName}StateMachine")
            .stateMachineName("${config.workflowName}-Express")
            .definitionBody(DefinitionBody.fromChainable(states[0]))
            .stateMachineType(StateMachineType.EXPRESS)  // Express workflow type
            .timeout(Duration.minutes(5))  // 5-minute maximum duration
            .tracingEnabled(true)  // Enable X-Ray tracing (Requirement 4.7, 9.1)
            .logs(LogOptions.builder()
                .destination(logGroup)
                .includeExecutionData(true)  // Include full execution data (Requirement 4.7, 9.2)
                .level(LogLevel.ALL)  // Log all events (Requirement 9.2)
                .build())
            .build()
        
        // Grant S3 permissions to Step Functions execution role
        // Allows Lambda functions to read/write to workflow bucket
        config.workflowBucket.grantReadWrite(stateMachine)
        
        // Create EventBridge Pipe to connect SQS queue to Step Function
        // Uses REQUEST_RESPONSE (synchronous) invocation type (Requirement 4.2)
        createEventBridgePipe(
            scope = scope,
            pipeName = "${config.workflowName}-pipe",
            sourceQueue = config.sourceQueue,
            targetStateMachine = stateMachine,
            invocationType = "REQUEST_RESPONSE"  // Synchronous for Express workflows
        )
        
        return stateMachine
    }
    
    /**
     * Creates an EventBridge Pipe to connect SQS queue to Step Functions.
     * 
     * EventBridge Pipes provide a managed integration between SQS and Step Functions:
     * - Automatic polling of SQS messages
     * - Batch size configuration
     * - Invocation type selection (REQUEST_RESPONSE or FIRE_AND_FORGET)
     * - Built-in error handling and retry logic
     * 
     * For Express workflows:
     * - Uses REQUEST_RESPONSE (synchronous) invocation
     * - Returns failure to SQS on execution failure (enables message retry)
     * 
     * For Standard workflows:
     * - Uses FIRE_AND_FORGET (asynchronous) invocation
     * - Does not wait for execution completion
     * 
     * @param scope CDK construct scope
     * @param pipeName Name of the EventBridge Pipe
     * @param sourceQueue SQS queue to poll messages from
     * @param targetStateMachine Step Functions state machine to invoke
     * @param invocationType Invocation type: "REQUEST_RESPONSE" or "FIRE_AND_FORGET"
     * 
     * Validates: Requirements 4.2, 11.4
     */
    private fun createEventBridgePipe(
        scope: Construct,
        pipeName: String,
        sourceQueue: software.amazon.awscdk.services.sqs.IQueue,
        targetStateMachine: StateMachine,
        invocationType: String
    ) {
        // Create IAM role for EventBridge Pipe
        val pipeRole = software.amazon.awscdk.services.iam.Role.Builder.create(scope, "${pipeName}Role")
            .assumedBy(software.amazon.awscdk.services.iam.ServicePrincipal("pipes.amazonaws.com"))
            .build()
        
        // Grant SQS permissions to Pipe role
        sourceQueue.grantConsumeMessages(pipeRole)
        
        // Grant Step Functions permissions to Pipe role
        targetStateMachine.grantStartExecution(pipeRole)
        
        // Create EventBridge Pipe
        CfnPipe.Builder.create(scope, pipeName)
            .name(pipeName)
            .roleArn(pipeRole.roleArn)
            .source(sourceQueue.queueArn)
            .sourceParameters(CfnPipe.PipeSourceParametersProperty.builder()
                .sqsQueueParameters(CfnPipe.PipeSourceSqsQueueParametersProperty.builder()
                    .batchSize(1)  // Process one message at a time (Requirement 4.2)
                    .build())
                .build())
            .target(targetStateMachine.stateMachineArn)
            .targetParameters(CfnPipe.PipeTargetParametersProperty.builder()
                .stepFunctionStateMachineParameters(
                    CfnPipe.PipeTargetStateMachineParametersProperty.builder()
                        .invocationType(invocationType)  // REQUEST_RESPONSE or FIRE_AND_FORGET
                        .build()
                )
                .build())
            .desiredState("RUNNING")  // Start the pipe immediately
            .build()
    }
}
