package com.ceap.infrastructure

import com.ceap.infrastructure.workflow.WorkflowConfiguration
import com.ceap.infrastructure.workflow.WorkflowFactory
import com.ceap.infrastructure.workflow.WorkflowType
import com.ceap.infrastructure.workflow.WorkflowStepType
import com.ceap.infrastructure.workflow.LambdaStep
import com.ceap.infrastructure.workflow.RetryConfiguration
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.AfterAll
import software.amazon.awscdk.App
import software.amazon.awscdk.Stack
import software.amazon.awscdk.services.lambda.Code
import software.amazon.awscdk.services.lambda.Function
import software.amazon.awscdk.services.lambda.Runtime
import software.amazon.awscdk.services.s3.Bucket
import software.amazon.awscdk.services.sqs.Queue
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.NoSuchKeyException
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.SendMessageRequest
import software.amazon.awssdk.services.stepfunctions.SfnClient
import software.amazon.awssdk.services.stepfunctions.model.DescribeExecutionRequest
import software.amazon.awssdk.services.stepfunctions.model.ExecutionStatus
import software.amazon.awssdk.services.stepfunctions.model.ListExecutionsRequest
import java.time.Duration
import java.time.Instant
import kotlin.test.assertNotNull

/**
 * End-to-end integration test for Express workflow.
 * 
 * This test validates the complete Express workflow execution including:
 * - SQS message triggering
 * - Step Functions execution
 * - Lambda function invocations
 * - S3 intermediate storage
 * - Workflow completion
 * - Output correctness
 * 
 * Test Flow:
 * 1. Deploy test stack with Express workflow
 * 2. Send test message to SQS queue
 * 3. Wait for workflow execution to complete
 * 4. Verify S3 outputs exist for all stages
 * 5. Verify final output correctness
 * 
 * Validates: Requirement 12.3
 */
@Tag("integration")
@Tag("express-workflow")
class ExpressWorkflowIntegrationTest {
    
    companion object {
        private val objectMapper: ObjectMapper = jacksonObjectMapper()
        private val s3Client: S3Client = S3Client.create()
        private val sqsClient: SqsClient = SqsClient.create()
        private val sfnClient: SfnClient = SfnClient.create()
        
        // Test configuration
        private const val TEST_STACK_NAME = "CeapExpressWorkflowIntegrationTest"
        private const val TEST_WORKFLOW_NAME = "test-express-workflow"
        private const val TEST_ENVIRONMENT = "integration-test"
        
        // Deployed resources (populated during setup)
        private lateinit var workflowBucketName: String
        private lateinit var queueUrl: String
        private lateinit var stateMachineArn: String
        
        @JvmStatic
        @BeforeAll
        fun setup() {
            println("Setting up Express workflow integration test...")
            println("Note: This test requires a deployed test stack.")
            println("Stack name: $TEST_STACK_NAME")
            println("Workflow name: $TEST_WORKFLOW_NAME")
            println()
            println("To deploy the test stack, run:")
            println("  cd infrastructure")
            println("  cdk deploy $TEST_STACK_NAME")
            println()
            
            // In a real integration test, these would be populated from CDK outputs
            // or environment variables. For now, we'll use placeholder values.
            // 
            // Example CDK output retrieval:
            // val outputs = CloudFormationClient.create().describeStacks(...)
            // workflowBucketName = outputs.find { it.outputKey == "WorkflowBucketName" }?.outputValue
            // queueUrl = outputs.find { it.outputKey == "QueueUrl" }?.outputValue
            // stateMachineArn = outputs.find { it.outputKey == "StateMachineArn" }?.outputValue
            
            workflowBucketName = System.getenv("TEST_WORKFLOW_BUCKET") 
                ?: "ceap-workflow-$TEST_ENVIRONMENT-123456789"
            queueUrl = System.getenv("TEST_QUEUE_URL") 
                ?: "https://sqs.us-east-1.amazonaws.com/123456789/$TEST_WORKFLOW_NAME-queue"
            stateMachineArn = System.getenv("TEST_STATE_MACHINE_ARN") 
                ?: "arn:aws:states:us-east-1:123456789:stateMachine:$TEST_WORKFLOW_NAME-Express"
            
            println("Test configuration:")
            println("  Workflow bucket: $workflowBucketName")
            println("  Queue URL: $queueUrl")
            println("  State machine ARN: $stateMachineArn")
            println()
        }
        
        @JvmStatic
        @AfterAll
        fun teardown() {
            println("Tearing down Express workflow integration test...")
            // Cleanup test data from S3 if needed
            // In production, S3 lifecycle policy handles cleanup after 7 days
        }
    }
    
    /**
     * Test: End-to-end Express workflow execution.
     * 
     * This test validates the complete workflow from SQS message to final output:
     * 1. Send test message to SQS queue
     * 2. Wait for Step Functions execution to start
     * 3. Wait for execution to complete (max 5 minutes for Express)
     * 4. Verify execution status is SUCCEEDED
     * 5. Verify S3 outputs exist for all stages
     * 6. Verify final output correctness
     * 
     * Expected stages:
     * - ETLStage: Extract, transform, load data
     * - FilterStage: Filter candidates based on criteria
     * - ScoreStage: Score filtered candidates
     * - StoreStage: Store candidates in DynamoDB
     * - ReactiveStage: Trigger reactive processing
     * 
     * Validates: Requirement 12.3
     */
    @Test
    fun `executes complete Express workflow end-to-end`() {
        println("Starting Express workflow end-to-end test...")
        
        // Step 1: Send test message to SQS
        val testMessage = createTestMessage()
        val messageId = sendSqsMessage(queueUrl, testMessage)
        println("Sent test message to SQS: messageId=$messageId")
        
        // Step 2: Wait for workflow execution to start
        println("Waiting for workflow execution to start...")
        val executionArn = waitForExecutionToStart(
            stateMachineArn = stateMachineArn,
            timeout = Duration.ofMinutes(2)
        )
        assertNotNull(executionArn, "Workflow execution should start")
        println("Workflow execution started: $executionArn")
        
        // Extract execution ID from ARN
        val executionId = executionArn.substringAfterLast(":")
        
        // Step 3: Wait for execution to complete
        println("Waiting for workflow execution to complete...")
        val execution = waitForExecutionToComplete(
            executionArn = executionArn,
            timeout = Duration.ofMinutes(5)
        )
        
        // Step 4: Verify execution status
        assertEquals(
            ExecutionStatus.SUCCEEDED,
            execution.status,
            "Workflow execution should succeed"
        )
        println("Workflow execution completed successfully")
        
        // Step 5: Verify S3 outputs exist for all stages
        val stages = listOf("ETLStage", "FilterStage", "ScoreStage", "StoreStage", "ReactiveStage")
        println("Verifying S3 outputs for all stages...")
        
        stages.forEach { stage ->
            val outputKey = "executions/$executionId/$stage/output.json"
            val outputExists = s3ObjectExists(workflowBucketName, outputKey)
            assertTrue(
                outputExists,
                "S3 output should exist for stage $stage at s3://$workflowBucketName/$outputKey"
            )
            println("  ✓ $stage output exists")
        }
        
        // Step 6: Verify final output correctness
        println("Verifying final output correctness...")
        val finalOutputKey = "executions/$executionId/ReactiveStage/output.json"
        val finalOutput = getS3Object(workflowBucketName, finalOutputKey)
        val finalData = objectMapper.readTree(finalOutput)
        
        // Verify final output structure
        assertNotNull(finalData, "Final output should not be null")
        assertTrue(
            finalData.has("status") || finalData.has("candidates"),
            "Final output should contain status or candidates field"
        )
        
        println("Final output verified successfully")
        println("Express workflow end-to-end test completed ✓")
    }
    
    /**
     * Creates a test message for the workflow.
     * 
     * The message contains sample candidate data that will be processed
     * through all workflow stages.
     */
    private fun createTestMessage(): String {
        val testData = mapOf(
            "candidates" to listOf(
                mapOf(
                    "id" to "test-candidate-1",
                    "name" to "Test Candidate 1",
                    "score" to 75,
                    "attributes" to mapOf(
                        "category" to "premium",
                        "region" to "us-east-1"
                    )
                ),
                mapOf(
                    "id" to "test-candidate-2",
                    "name" to "Test Candidate 2",
                    "score" to 85,
                    "attributes" to mapOf(
                        "category" to "standard",
                        "region" to "us-west-2"
                    )
                )
            ),
            "metadata" to mapOf(
                "testRun" to true,
                "timestamp" to Instant.now().toString()
            )
        )
        
        return objectMapper.writeValueAsString(testData)
    }
    
    /**
     * Sends a message to the SQS queue.
     * 
     * @param queueUrl SQS queue URL
     * @param messageBody Message body (JSON string)
     * @return Message ID
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
     * 
     * Polls the Step Functions API for new executions of the state machine.
     * 
     * @param stateMachineArn State machine ARN
     * @param timeout Maximum time to wait
     * @return Execution ARN if found, null if timeout
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
            
            // Find most recent execution
            val recentExecution = response.executions()
                .filter { it.startDate().isAfter(startTime) }
                .maxByOrNull { it.startDate() }
            
            if (recentExecution != null) {
                return recentExecution.executionArn()
            }
            
            // Wait before polling again
            Thread.sleep(2000)
        }
        
        return null
    }
    
    /**
     * Waits for a Step Functions execution to complete.
     * 
     * Polls the execution status until it reaches a terminal state
     * (SUCCEEDED, FAILED, TIMED_OUT, ABORTED).
     * 
     * @param executionArn Execution ARN
     * @param timeout Maximum time to wait
     * @return Execution description
     * @throws AssertionError if execution times out
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
            
            // Check if execution reached terminal state
            when (response.status()) {
                ExecutionStatus.SUCCEEDED,
                ExecutionStatus.FAILED,
                ExecutionStatus.TIMED_OUT,
                ExecutionStatus.ABORTED -> {
                    return response
                }
                else -> {
                    // Still running, wait and poll again
                    Thread.sleep(5000)
                }
            }
        }
        
        fail("Execution did not complete within timeout: $timeout")
    }
    
    /**
     * Checks if an S3 object exists.
     * 
     * @param bucket S3 bucket name
     * @param key S3 object key
     * @return true if object exists, false otherwise
     */
    private fun s3ObjectExists(bucket: String, key: String): Boolean {
        return try {
            val request = GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build()
            
            s3Client.getObject(request)
            true
        } catch (e: NoSuchKeyException) {
            false
        }
    }
    
    /**
     * Gets an S3 object as a string.
     * 
     * @param bucket S3 bucket name
     * @param key S3 object key
     * @return Object content as string
     */
    private fun getS3Object(bucket: String, key: String): String {
        val request = GetObjectRequest.builder()
            .bucket(bucket)
            .key(key)
            .build()
        
        val response = s3Client.getObject(request)
        return response.readAllBytes().toString(Charsets.UTF_8)
    }
}
