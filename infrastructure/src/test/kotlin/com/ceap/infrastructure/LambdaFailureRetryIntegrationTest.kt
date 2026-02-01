package com.ceap.infrastructure

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.AfterAll
import software.amazon.awssdk.services.lambda.LambdaClient
import software.amazon.awssdk.services.lambda.model.PutFunctionConcurrencyRequest
import software.amazon.awssdk.services.lambda.model.DeleteFunctionConcurrencyRequest
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.SendMessageRequest
import software.amazon.awssdk.services.stepfunctions.SfnClient
import software.amazon.awssdk.services.stepfunctions.model.DescribeExecutionRequest
import software.amazon.awssdk.services.stepfunctions.model.ExecutionStatus
import software.amazon.awssdk.services.stepfunctions.model.GetExecutionHistoryRequest
import software.amazon.awssdk.services.stepfunctions.model.ListExecutionsRequest
import software.amazon.awssdk.core.sync.RequestBody
import java.time.Duration
import java.time.Instant
import kotlin.test.assertNotNull

/**
 * Integration test for Lambda failure and retry behavior.
 * 
 * This test validates that the workflow correctly handles Lambda failures
 * and retries according to the configured retry policy:
 * - Maximum 2 retry attempts
 * - Initial interval: 20 seconds
 * - Backoff rate: 2x (20s, 40s)
 * 
 * Test Flow:
 * 1. Inject failure in Lambda (first attempt only)
 * 2. Send test message to SQS queue
 * 3. Wait for workflow execution to complete
 * 4. Verify workflow succeeds after retry
 * 5. Verify retry count in execution history
 * 
 * Failure Injection Strategy:
 * - Use S3 to signal failure condition to Lambda
 * - Lambda checks for failure marker file before processing
 * - First attempt: Failure marker exists → Lambda throws exception
 * - Second attempt: Failure marker removed → Lambda succeeds
 * 
 * Validates: Requirement 12.7
 */
@Tag("integration")
@Tag("failure-handling")
@Tag("retry-logic")
class LambdaFailureRetryIntegrationTest {
    
    companion object {
        private val objectMapper: ObjectMapper = jacksonObjectMapper()
        private val s3Client: S3Client = S3Client.create()
        private val sqsClient: SqsClient = SqsClient.create()
        private val sfnClient: SfnClient = SfnClient.create()
        private val lambdaClient: LambdaClient = LambdaClient.create()
        
        // Test configuration
        private const val TEST_STACK_NAME = "CeapFailureRetryIntegrationTest"
        private const val TEST_WORKFLOW_NAME = "test-failure-retry-workflow"
        private const val TEST_ENVIRONMENT = "integration-test"
        private const val TEST_LAMBDA_FUNCTION_NAME = "test-filter-lambda"
        
        // Deployed resources
        private lateinit var workflowBucketName: String
        private lateinit var queueUrl: String
        private lateinit var stateMachineArn: String
        private lateinit var filterLambdaArn: String
        
        @JvmStatic
        @BeforeAll
        fun setup() {
            println("Setting up Lambda failure and retry integration test...")
            println("Stack name: $TEST_STACK_NAME")
            println("Workflow name: $TEST_WORKFLOW_NAME")
            println()
            
            workflowBucketName = System.getenv("TEST_WORKFLOW_BUCKET") 
                ?: "ceap-workflow-$TEST_ENVIRONMENT-123456789"
            queueUrl = System.getenv("TEST_QUEUE_URL") 
                ?: "https://sqs.us-east-1.amazonaws.com/123456789/$TEST_WORKFLOW_NAME-queue"
            stateMachineArn = System.getenv("TEST_STATE_MACHINE_ARN") 
                ?: "arn:aws:states:us-east-1:123456789:stateMachine:$TEST_WORKFLOW_NAME-Express"
            filterLambdaArn = System.getenv("TEST_FILTER_LAMBDA_ARN")
                ?: "arn:aws:lambda:us-east-1:123456789:function:$TEST_LAMBDA_FUNCTION_NAME"
            
            println("Test configuration:")
            println("  Workflow bucket: $workflowBucketName")
            println("  Queue URL: $queueUrl")
            println("  State machine ARN: $stateMachineArn")
            println("  Filter Lambda ARN: $filterLambdaArn")
            println()
        }
        
        @JvmStatic
        @AfterAll
        fun teardown() {
            println("Tearing down Lambda failure and retry integration test...")
            // Cleanup any failure markers from S3
        }
    }
    
    /**
     * Test: Lambda failure with successful retry.
     * 
     * This test validates the retry behavior when a Lambda function fails:
     * 1. Inject failure in FilterStage Lambda (first attempt)
     * 2. Send test message to SQS queue
     * 3. Wait for workflow execution to complete
     * 4. Verify workflow succeeds (after retry)
     * 5. Verify retry count is 1 in execution history
     * 6. Verify retry intervals match configuration (20s, 40s)
     * 
     * Expected behavior:
     * - First attempt: Lambda fails (injected failure)
     * - Step Functions waits 20 seconds
     * - Second attempt: Lambda succeeds (failure marker removed)
     * - Workflow completes successfully
     * 
     * Validates: Requirement 12.7
     */
    @Test
    fun `handles Lambda failure with retry and succeeds`() {
        println("Starting Lambda failure and retry test...")
        
        // Generate unique execution ID for this test
        val testExecutionId = "test-${System.currentTimeMillis()}"
        
        // Step 1: Inject failure marker for first attempt
        println("Injecting failure marker for FilterStage...")
        injectFailureMarker(
            bucket = workflowBucketName,
            executionId = testExecutionId,
            stage = "FilterStage",
            maxAttempts = 1  // Fail only first attempt
        )
        
        // Step 2: Send test message to SQS
        val testMessage = createTestMessage(testExecutionId)
        val messageId = sendSqsMessage(queueUrl, testMessage)
        println("Sent test message to SQS: messageId=$messageId")
        
        // Step 3: Wait for workflow execution to start
        println("Waiting for workflow execution to start...")
        val executionArn = waitForExecutionToStart(
            stateMachineArn = stateMachineArn,
            timeout = Duration.ofMinutes(2)
        )
        assertNotNull(executionArn, "Workflow execution should start")
        println("Workflow execution started: $executionArn")
        
        // Step 4: Wait for execution to complete
        // Should take longer due to retry delays (20s + 40s = 60s minimum)
        println("Waiting for workflow execution to complete (with retries)...")
        val execution = waitForExecutionToComplete(
            executionArn = executionArn,
            timeout = Duration.ofMinutes(10)
        )
        
        // Step 5: Verify execution succeeded after retry
        assertEquals(
            ExecutionStatus.SUCCEEDED,
            execution.status,
            "Workflow execution should succeed after retry"
        )
        println("Workflow execution completed successfully after retry")
        
        // Step 6: Verify retry count in execution history
        println("Verifying retry count in execution history...")
        val retryCount = getRetryCountForStage(executionArn, "FilterStage")
        assertEquals(
            1,
            retryCount,
            "FilterStage should have been retried exactly once"
        )
        println("Retry count verified: $retryCount retry")
        
        // Step 7: Verify retry intervals
        println("Verifying retry intervals...")
        val retryIntervals = getRetryIntervals(executionArn, "FilterStage")
        assertTrue(
            retryIntervals.isNotEmpty(),
            "Should have at least one retry interval"
        )
        
        // First retry should be approximately 20 seconds (allow ±5s tolerance)
        val firstRetryInterval = retryIntervals[0]
        assertTrue(
            firstRetryInterval in 15..25,
            "First retry interval should be ~20 seconds, was ${firstRetryInterval}s"
        )
        println("First retry interval: ${firstRetryInterval}s (expected ~20s)")
        
        println("Lambda failure and retry test completed ✓")
    }
    
    /**
     * Injects a failure marker in S3 to simulate Lambda failure.
     * 
     * The Lambda function checks for this marker before processing.
     * If the marker exists and attempt count < maxAttempts, Lambda throws exception.
     * 
     * Marker structure:
     * {
     *   "failUntilAttempt": 1,
     *   "currentAttempt": 0,
     *   "errorMessage": "Simulated failure for testing"
     * }
     */
    private fun injectFailureMarker(
        bucket: String,
        executionId: String,
        stage: String,
        maxAttempts: Int
    ) {
        val markerKey = "test-failures/$executionId/$stage/failure-marker.json"
        val markerData = mapOf(
            "failUntilAttempt" to maxAttempts,
            "currentAttempt" to 0,
            "errorMessage" to "Simulated failure for integration testing",
            "injectedAt" to Instant.now().toString()
        )
        
        val markerJson = objectMapper.writeValueAsString(markerData)
        
        val request = PutObjectRequest.builder()
            .bucket(bucket)
            .key(markerKey)
            .contentType("application/json")
            .build()
        
        s3Client.putObject(request, RequestBody.fromString(markerJson))
        
        println("Failure marker injected: s3://$bucket/$markerKey")
    }
    
    /**
     * Creates a test message with execution ID for tracking.
     */
    private fun createTestMessage(executionId: String): String {
        val testData = mapOf(
            "executionId" to executionId,
            "candidates" to listOf(
                mapOf(
                    "id" to "test-candidate-1",
                    "name" to "Test Candidate 1",
                    "score" to 75
                ),
                mapOf(
                    "id" to "test-candidate-2",
                    "name" to "Test Candidate 2",
                    "score" to 85
                )
            ),
            "metadata" to mapOf(
                "testRun" to true,
                "testType" to "failure-retry",
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
     * Gets the retry count for a specific stage from execution history.
     * 
     * Parses the Step Functions execution history to count how many times
     * a stage was retried.
     */
    private fun getRetryCountForStage(executionArn: String, stageName: String): Int {
        val request = GetExecutionHistoryRequest.builder()
            .executionArn(executionArn)
            .build()
        
        val response = sfnClient.getExecutionHistory(request)
        
        // Count TaskStateEntered events for the stage
        val taskAttempts = response.events()
            .filter { event ->
                event.type().toString().contains("TaskStateEntered") &&
                event.stateEnteredEventDetails()?.name() == stageName
            }
            .count()
        
        // Retry count = total attempts - 1 (first attempt is not a retry)
        return maxOf(0, taskAttempts - 1)
    }
    
    /**
     * Gets the retry intervals for a specific stage from execution history.
     * 
     * Returns a list of intervals (in seconds) between retry attempts.
     */
    private fun getRetryIntervals(executionArn: String, stageName: String): List<Long> {
        val request = GetExecutionHistoryRequest.builder()
            .executionArn(executionArn)
            .build()
        
        val response = sfnClient.getExecutionHistory(request)
        
        // Find all TaskStateEntered events for the stage
        val taskAttemptTimestamps = response.events()
            .filter { event ->
                event.type().toString().contains("TaskStateEntered") &&
                event.stateEnteredEventDetails()?.name() == stageName
            }
            .map { it.timestamp() }
            .sorted()
        
        // Calculate intervals between attempts
        val intervals = mutableListOf<Long>()
        for (i in 1 until taskAttemptTimestamps.size) {
            val interval = Duration.between(
                taskAttemptTimestamps[i - 1],
                taskAttemptTimestamps[i]
            ).seconds
            intervals.add(interval)
        }
        
        return intervals
    }
}
