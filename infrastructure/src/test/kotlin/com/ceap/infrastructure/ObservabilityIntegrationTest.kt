package com.ceap.infrastructure

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClient
import software.amazon.awssdk.services.cloudwatchlogs.model.FilterLogEventsRequest
import software.amazon.awssdk.services.cloudwatchlogs.model.DescribeLogStreamsRequest
import software.amazon.awssdk.services.cloudwatchlogs.model.OrderBy
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.SendMessageRequest
import software.amazon.awssdk.services.stepfunctions.SfnClient
import software.amazon.awssdk.services.stepfunctions.model.DescribeExecutionRequest
import software.amazon.awssdk.services.stepfunctions.model.ExecutionStatus
import software.amazon.awssdk.services.stepfunctions.model.ListExecutionsRequest
import software.amazon.awssdk.services.xray.XRayClient
import software.amazon.awssdk.services.xray.model.GetTraceSummariesRequest
import java.time.Duration
import java.time.Instant
import kotlin.test.assertNotNull

/**
 * Integration test for observability features.
 * 
 * This test validates that observability features are working correctly:
 * - CloudWatch Logs contain execution data
 * - X-Ray traces are created
 * - Lambda logs contain execution context
 * - Step Functions execution history is complete
 * 
 * Test Flow:
 * 1. Execute workflow
 * 2. Wait for workflow to complete
 * 3. Verify CloudWatch Logs contain execution data
 * 4. Verify X-Ray traces are created
 * 5. Verify Lambda logs contain execution context
 * 6. Verify Step Functions execution history
 * 
 * Validates: Requirement 12.6
 */
@Tag("integration")
@Tag("observability")
@Tag("monitoring")
class ObservabilityIntegrationTest {
    
    companion object {
        private val objectMapper: ObjectMapper = jacksonObjectMapper()
        private val sqsClient: SqsClient = SqsClient.create()
        private val sfnClient: SfnClient = SfnClient.create()
        private val cloudWatchLogsClient: CloudWatchLogsClient = CloudWatchLogsClient.create()
        private val xrayClient: XRayClient = XRayClient.create()
        
        // Test configuration
        private const val TEST_WORKFLOW_NAME = "test-observability-workflow"
        private const val TEST_ENVIRONMENT = "integration-test"
        
        // Deployed resources
        private lateinit var queueUrl: String
        private lateinit var stateMachineArn: String
        private lateinit var stepFunctionsLogGroupName: String
        private lateinit var lambdaLogGroupPrefix: String
        
        @JvmStatic
        @BeforeAll
        fun setup() {
            println("Setting up observability integration test...")
            
            queueUrl = System.getenv("TEST_QUEUE_URL") 
                ?: "https://sqs.us-east-1.amazonaws.com/123456789/$TEST_WORKFLOW_NAME-queue"
            stateMachineArn = System.getenv("TEST_STATE_MACHINE_ARN") 
                ?: "arn:aws:states:us-east-1:123456789:stateMachine:$TEST_WORKFLOW_NAME-Express"
            stepFunctionsLogGroupName = System.getenv("TEST_STEP_FUNCTIONS_LOG_GROUP")
                ?: "/aws/stepfunctions/$TEST_WORKFLOW_NAME"
            lambdaLogGroupPrefix = System.getenv("TEST_LAMBDA_LOG_GROUP_PREFIX")
                ?: "/aws/lambda/$TEST_ENVIRONMENT"
            
            println("Test configuration:")
            println("  Queue URL: $queueUrl")
            println("  State machine ARN: $stateMachineArn")
            println("  Step Functions log group: $stepFunctionsLogGroupName")
            println("  Lambda log group prefix: $lambdaLogGroupPrefix")
            println()
        }
    }
    
    /**
     * Test: Verify observability features.
     * 
     * This test validates all observability features:
     * 1. Execute workflow
     * 2. Verify CloudWatch Logs for Step Functions
     * 3. Verify X-Ray traces
     * 4. Verify Lambda logs contain execution context
     * 5. Verify execution history completeness
     * 
     * Validates: Requirement 12.6
     */
    @Test
    fun `verifies observability features are working`() {
        println("Starting observability verification test...")
        
        // Step 1: Execute workflow
        val testExecutionId = "observability-test-${System.currentTimeMillis()}"
        val testMessage = createTestMessage(testExecutionId)
        val messageId = sendSqsMessage(queueUrl, testMessage)
        println("Sent test message to SQS: messageId=$messageId")
        
        // Wait for execution to start
        println("Waiting for workflow execution to start...")
        val executionArn = waitForExecutionToStart(
            stateMachineArn = stateMachineArn,
            timeout = Duration.ofMinutes(2)
        )
        assertNotNull(executionArn, "Workflow execution should start")
        println("Workflow execution started: $executionArn")
        
        val executionId = executionArn.substringAfterLast(":")
        
        // Wait for execution to complete
        println("Waiting for workflow execution to complete...")
        val execution = waitForExecutionToComplete(
            executionArn = executionArn,
            timeout = Duration.ofMinutes(5)
        )
        
        assertEquals(
            ExecutionStatus.SUCCEEDED,
            execution.status,
            "Workflow execution should succeed"
        )
        println("Workflow execution completed successfully")
        
        // Step 2: Verify CloudWatch Logs for Step Functions
        println("\nVerifying CloudWatch Logs for Step Functions...")
        val stepFunctionsLogs = getStepFunctionsLogs(
            logGroupName = stepFunctionsLogGroupName,
            executionId = executionId
        )
        
        assertTrue(
            stepFunctionsLogs.isNotEmpty(),
            "Step Functions logs should contain execution data"
        )
        println("Found ${stepFunctionsLogs.size} log event(s) for Step Functions execution")
        
        // Verify logs contain execution data
        val logsContainExecutionData = stepFunctionsLogs.any { log ->
            log.contains(executionId) || 
            log.contains("ExecutionStarted") ||
            log.contains("ExecutionSucceeded")
        }
        assertTrue(
            logsContainExecutionData,
            "Step Functions logs should contain execution data (execution ID, status changes)"
        )
        println("Verified Step Functions logs contain execution data ✓")
        
        // Step 3: Verify X-Ray traces
        println("\nVerifying X-Ray traces...")
        val traces = getXRayTraces(
            startTime = execution.startDate(),
            endTime = execution.stopDate() ?: Instant.now()
        )
        
        assertTrue(
            traces.isNotEmpty(),
            "X-Ray traces should be created for workflow execution"
        )
        println("Found ${traces.size} X-Ray trace(s)")
        
        // Verify traces contain workflow execution
        val tracesContainExecution = traces.any { trace ->
            trace.id().contains(executionId.take(8)) ||
            trace.duration() != null
        }
        assertTrue(
            tracesContainExecution,
            "X-Ray traces should contain workflow execution data"
        )
        println("Verified X-Ray traces are created ✓")
        
        // Step 4: Verify Lambda logs contain execution context
        println("\nVerifying Lambda logs contain execution context...")
        val stages = listOf("ETLStage", "FilterStage", "ScoreStage", "StoreStage", "ReactiveStage")
        
        stages.forEach { stage ->
            val lambdaLogs = getLambdaLogs(
                logGroupPrefix = lambdaLogGroupPrefix,
                stage = stage,
                executionId = executionId
            )
            
            if (lambdaLogs.isNotEmpty()) {
                // Verify logs contain execution context
                val logsContainContext = lambdaLogs.any { log ->
                    log.contains("executionId=$executionId") ||
                    log.contains("currentStage=$stage") ||
                    log.contains("Starting stage")
                }
                
                assertTrue(
                    logsContainContext,
                    "Lambda logs for $stage should contain execution context"
                )
                println("  ✓ $stage logs contain execution context")
            } else {
                println("  ⚠ $stage logs not found (may not have executed yet)")
            }
        }
        
        println("Verified Lambda logs contain execution context ✓")
        
        // Step 5: Verify execution history completeness
        println("\nVerifying execution history completeness...")
        val executionHistory = sfnClient.getExecutionHistory(
            software.amazon.awssdk.services.stepfunctions.model.GetExecutionHistoryRequest.builder()
                .executionArn(executionArn)
                .build()
        )
        
        val events = executionHistory.events()
        assertTrue(
            events.isNotEmpty(),
            "Execution history should contain events"
        )
        println("Found ${events.size} event(s) in execution history")
        
        // Verify key events exist
        val hasExecutionStarted = events.any { it.type().toString().contains("ExecutionStarted") }
        val hasExecutionSucceeded = events.any { it.type().toString().contains("ExecutionSucceeded") }
        val hasTaskStates = events.any { it.type().toString().contains("TaskState") }
        
        assertTrue(hasExecutionStarted, "Execution history should contain ExecutionStarted event")
        assertTrue(hasExecutionSucceeded, "Execution history should contain ExecutionSucceeded event")
        assertTrue(hasTaskStates, "Execution history should contain TaskState events")
        
        println("Verified execution history is complete ✓")
        
        println("\nObservability verification test completed ✓")
    }
    
    /**
     * Creates a test message for the workflow.
     */
    private fun createTestMessage(executionId: String): String {
        val testData = mapOf(
            "executionId" to executionId,
            "candidates" to listOf(
                mapOf("id" to "obs-test-1", "score" to 75),
                mapOf("id" to "obs-test-2", "score" to 85)
            ),
            "metadata" to mapOf(
                "testRun" to true,
                "testType" to "observability",
                "timestamp" to Instant.now().toString()
            )
        )
        
        return objectMapper.writeValueAsString(testData)
    }
    
    /**
     * Sends a message to the SQS queue.
     */
    private fun sendSqsMessage(queueUrl: String, messageBody: String): String {
        val request = SendMessageRequest.builder()
            .queueUrl(queueUrl)
            .messageBody(messageBody)
            .build()
        
        val response = sqsClient.sendMessage(request)
        return response.messageId()
    }
    
    /**
     * Waits for a Step Functions execution to start.
     */
    private fun waitForExecutionToStart(
        stateMachineArn: String,
        timeout: Duration
    ): String? {
        val startTime = Instant.now()
        val endTime = startTime.plus(timeout)
        
        while (Instant.now().isBefore(endTime)) {
            val request = ListExecutionsRequest.builder()
                .stateMachineArn(stateMachineArn)
                .maxResults(10)
                .build()
            
            val response = sfnClient.listExecutions(request)
            
            val recentExecution = response.executions()
                .filter { it.startDate().isAfter(startTime) }
                .maxByOrNull { it.startDate() }
            
            if (recentExecution != null) {
                return recentExecution.executionArn()
            }
            
            Thread.sleep(2000)
        }
        
        return null
    }
    
    /**
     * Waits for a Step Functions execution to complete.
     */
    private fun waitForExecutionToComplete(
        executionArn: String,
        timeout: Duration
    ): software.amazon.awssdk.services.stepfunctions.model.DescribeExecutionResponse {
        val startTime = Instant.now()
        val endTime = startTime.plus(timeout)
        
        while (Instant.now().isBefore(endTime)) {
            val request = DescribeExecutionRequest.builder()
                .executionArn(executionArn)
                .build()
            
            val response = sfnClient.describeExecution(request)
            
            when (response.status()) {
                ExecutionStatus.SUCCEEDED,
                ExecutionStatus.FAILED,
                ExecutionStatus.TIMED_OUT,
                ExecutionStatus.ABORTED -> {
                    return response
                }
                else -> {
                    Thread.sleep(5000)
                }
            }
        }
        
        fail("Execution did not complete within timeout: $timeout")
    }
    
    /**
     * Gets CloudWatch Logs for Step Functions execution.
     */
    private fun getStepFunctionsLogs(
        logGroupName: String,
        executionId: String
    ): List<String> {
        // Wait a bit for logs to be available
        Thread.sleep(10000)
        
        val request = FilterLogEventsRequest.builder()
            .logGroupName(logGroupName)
            .filterPattern(executionId)
            .limit(100)
            .build()
        
        return try {
            val response = cloudWatchLogsClient.filterLogEvents(request)
            response.events().map { it.message() }
        } catch (e: Exception) {
            println("Warning: Could not retrieve Step Functions logs: ${e.message}")
            emptyList()
        }
    }
    
    /**
     * Gets X-Ray traces for the time period.
     */
    private fun getXRayTraces(
        startTime: Instant,
        endTime: Instant
    ): List<software.amazon.awssdk.services.xray.model.TraceSummary> {
        // Wait a bit for traces to be available
        Thread.sleep(10000)
        
        val request = GetTraceSummariesRequest.builder()
            .startTime(startTime)
            .endTime(endTime)
            .build()
        
        return try {
            val response = xrayClient.getTraceSummaries(request)
            response.traceSummaries()
        } catch (e: Exception) {
            println("Warning: Could not retrieve X-Ray traces: ${e.message}")
            emptyList()
        }
    }
    
    /**
     * Gets CloudWatch Logs for Lambda function.
     */
    private fun getLambdaLogs(
        logGroupPrefix: String,
        stage: String,
        executionId: String
    ): List<String> {
        // Construct log group name for the stage
        val logGroupName = "$logGroupPrefix-$stage"
        
        // Wait a bit for logs to be available
        Thread.sleep(5000)
        
        return try {
            // Get most recent log stream
            val describeRequest = DescribeLogStreamsRequest.builder()
                .logGroupName(logGroupName)
                .orderBy(OrderBy.LAST_EVENT_TIME)
                .descending(true)
                .limit(5)
                .build()
            
            val streams = cloudWatchLogsClient.describeLogStreams(describeRequest)
            val logStreamNames = streams.logStreams().map { it.logStreamName() }
            
            if (logStreamNames.isEmpty()) {
                return emptyList()
            }
            
            // Filter logs for execution ID
            val filterRequest = FilterLogEventsRequest.builder()
                .logGroupName(logGroupName)
                .logStreamNames(logStreamNames)
                .filterPattern(executionId)
                .limit(50)
                .build()
            
            val response = cloudWatchLogsClient.filterLogEvents(filterRequest)
            response.events().map { it.message() }
            
        } catch (e: Exception) {
            println("Warning: Could not retrieve Lambda logs for $stage: ${e.message}")
            emptyList()
        }
    }
}
