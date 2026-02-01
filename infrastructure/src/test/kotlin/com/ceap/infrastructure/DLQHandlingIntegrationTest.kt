package com.ceap.infrastructure

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.AfterAll
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.SendMessageRequest
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesRequest
import software.amazon.awssdk.services.sqs.model.QueueAttributeName
import software.amazon.awssdk.core.sync.RequestBody
import java.time.Duration
import java.time.Instant
import kotlin.test.assertNotNull

/**
 * Integration test for Dead Letter Queue (DLQ) handling.
 * 
 * This test validates that messages are moved to the DLQ after exhausting
 * all retry attempts:
 * - SQS maxReceiveCount = 3
 * - After 3 failed attempts, message moves to DLQ
 * - DLQ message contains original payload
 * 
 * Test Flow:
 * 1. Inject permanent failure in Lambda
 * 2. Send test message to SQS queue
 * 3. Wait for message to be retried 3 times
 * 4. Verify message moves to DLQ
 * 5. Verify DLQ message contains original payload
 * 
 * Failure Injection Strategy:
 * - Use S3 to signal permanent failure to Lambda
 * - Lambda checks for failure marker and always throws exception
 * - After 3 receive attempts, SQS moves message to DLQ
 * 
 * Validates: Requirement 12.7
 */
@Tag("integration")
@Tag("dlq-handling")
@Tag("error-handling")
class DLQHandlingIntegrationTest {
    
    companion object {
        private val objectMapper: ObjectMapper = jacksonObjectMapper()
        private val s3Client: S3Client = S3Client.create()
        private val sqsClient: SqsClient = SqsClient.create()
        
        // Test configuration
        private const val TEST_STACK_NAME = "CeapDLQHandlingIntegrationTest"
        private const val TEST_WORKFLOW_NAME = "test-dlq-workflow"
        private const val TEST_ENVIRONMENT = "integration-test"
        
        // Deployed resources
        private lateinit var workflowBucketName: String
        private lateinit var queueUrl: String
        private lateinit var dlqUrl: String
        
        @JvmStatic
        @BeforeAll
        fun setup() {
            println("Setting up DLQ handling integration test...")
            println("Stack name: $TEST_STACK_NAME")
            println("Workflow name: $TEST_WORKFLOW_NAME")
            println()
            
            workflowBucketName = System.getenv("TEST_WORKFLOW_BUCKET") 
                ?: "ceap-workflow-$TEST_ENVIRONMENT-123456789"
            queueUrl = System.getenv("TEST_QUEUE_URL") 
                ?: "https://sqs.us-east-1.amazonaws.com/123456789/$TEST_WORKFLOW_NAME-queue"
            dlqUrl = System.getenv("TEST_DLQ_URL")
                ?: "https://sqs.us-east-1.amazonaws.com/123456789/$TEST_WORKFLOW_NAME-dlq"
            
            println("Test configuration:")
            println("  Workflow bucket: $workflowBucketName")
            println("  Queue URL: $queueUrl")
            println("  DLQ URL: $dlqUrl")
            println()
        }
        
        @JvmStatic
        @AfterAll
        fun teardown() {
            println("Tearing down DLQ handling integration test...")
            // Cleanup failure markers and DLQ messages
        }
    }
    
    /**
     * Test: Message moves to DLQ after max retries.
     * 
     * This test validates the DLQ behavior when a message fails repeatedly:
     * 1. Inject permanent failure in Lambda (all attempts fail)
     * 2. Send test message to SQS queue
     * 3. Wait for message to be retried 3 times (maxReceiveCount)
     * 4. Verify message is no longer in main queue
     * 5. Verify message appears in DLQ
     * 6. Verify DLQ message contains original payload
     * 
     * Expected behavior:
     * - Attempt 1: Lambda fails, message becomes visible again
     * - Attempt 2: Lambda fails, message becomes visible again
     * - Attempt 3: Lambda fails, message moves to DLQ
     * - DLQ message contains original payload for debugging
     * 
     * Timing:
     * - Visibility timeout: 10 minutes per attempt
     * - Total time: ~30 minutes for 3 attempts
     * - Test timeout: 45 minutes to allow for processing
     * 
     * Validates: Requirement 12.7
     */
    @Test
    fun `moves message to DLQ after max retries`() {
        println("Starting DLQ handling test...")
        
        // Generate unique execution ID for this test
        val testExecutionId = "dlq-test-${System.currentTimeMillis()}"
        
        // Step 1: Inject permanent failure marker
        println("Injecting permanent failure marker for all stages...")
        injectPermanentFailureMarker(
            bucket = workflowBucketName,
            executionId = testExecutionId
        )
        
        // Step 2: Send test message to SQS
        val testMessage = createTestMessage(testExecutionId)
        val messageId = sendSqsMessage(queueUrl, testMessage)
        println("Sent test message to SQS: messageId=$messageId")
        
        // Step 3: Wait for message to be retried 3 times and moved to DLQ
        // This can take 30+ minutes due to visibility timeout (10 min per attempt)
        println("Waiting for message to be retried 3 times and moved to DLQ...")
        println("This may take up to 30-40 minutes due to visibility timeout...")
        
        val dlqMessage = waitForDlqMessage(
            dlqUrl = dlqUrl,
            timeout = Duration.ofMinutes(45),
            expectedMessageId = messageId
        )
        
        // Step 4: Verify DLQ message exists
        assertNotNull(dlqMessage, "Message should appear in DLQ after max retries")
        println("Message found in DLQ: ${dlqMessage.messageId()}")
        
        // Step 5: Verify DLQ message contains original payload
        println("Verifying DLQ message payload...")
        val dlqMessageBody = dlqMessage.body()
        assertNotNull(dlqMessageBody, "DLQ message body should not be null")
        
        val dlqData = objectMapper.readTree(dlqMessageBody)
        assertTrue(
            dlqData.has("executionId") || dlqData.has("candidates"),
            "DLQ message should contain original payload"
        )
        
        // Verify execution ID matches
        if (dlqData.has("executionId")) {
            assertEquals(
                testExecutionId,
                dlqData.get("executionId").asText(),
                "DLQ message should contain original execution ID"
            )
        }
        
        println("DLQ message payload verified successfully")
        
        // Step 6: Verify message is no longer in main queue
        println("Verifying message is no longer in main queue...")
        val mainQueueMessage = receiveMessageFromQueue(queueUrl, waitTimeSeconds = 5)
        
        // If we receive a message, it should not be our test message
        if (mainQueueMessage != null) {
            assertNotEquals(
                messageId,
                mainQueueMessage.messageId(),
                "Test message should not be in main queue after moving to DLQ"
            )
        }
        
        println("Verified message is no longer in main queue")
        
        // Step 7: Verify receive count in DLQ message attributes
        println("Verifying receive count in DLQ message attributes...")
        val approximateReceiveCount = dlqMessage.attributes()[QueueAttributeName.APPROXIMATE_RECEIVE_COUNT]?.toIntOrNull()
        
        if (approximateReceiveCount != null) {
            assertTrue(
                approximateReceiveCount >= 3,
                "Message should have been received at least 3 times before moving to DLQ, was $approximateReceiveCount"
            )
            println("Receive count: $approximateReceiveCount (expected >= 3)")
        }
        
        println("DLQ handling test completed ✓")
    }
    
    /**
     * Injects a permanent failure marker in S3.
     * 
     * This marker causes Lambda to always fail, simulating a permanent error
     * that cannot be resolved by retries.
     */
    private fun injectPermanentFailureMarker(
        bucket: String,
        executionId: String
    ) {
        val markerKey = "test-failures/$executionId/permanent-failure.json"
        val markerData = mapOf(
            "failPermanently" to true,
            "errorMessage" to "Simulated permanent failure for DLQ testing",
            "errorType" to "PermanentProcessingError",
            "injectedAt" to Instant.now().toString()
        )
        
        val markerJson = objectMapper.writeValueAsString(markerData)
        
        val request = PutObjectRequest.builder()
            .bucket(bucket)
            .key(markerKey)
            .contentType("application/json")
            .build()
        
        s3Client.putObject(request, RequestBody.fromString(markerJson))
        
        println("Permanent failure marker injected: s3://$bucket/$markerKey")
    }
    
    /**
     * Creates a test message with execution ID for tracking.
     */
    private fun createTestMessage(executionId: String): String {
        val testData = mapOf(
            "executionId" to executionId,
            "candidates" to listOf(
                mapOf(
                    "id" to "dlq-test-candidate-1",
                    "name" to "DLQ Test Candidate 1",
                    "score" to 75
                )
            ),
            "metadata" to mapOf(
                "testRun" to true,
                "testType" to "dlq-handling",
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
     * Waits for a message to appear in the DLQ.
     * 
     * Polls the DLQ periodically until the expected message appears
     * or timeout is reached.
     */
    private fun waitForDlqMessage(
        dlqUrl: String,
        timeout: Duration,
        expectedMessageId: String? = null
    ): software.amazon.awssdk.services.sqs.model.Message? {
        val startTime = Instant.now()
        val endTime = startTime.plus(timeout)
        
        var pollCount = 0
        while (Instant.now().isBefore(endTime)) {
            pollCount++
            
            // Poll DLQ for messages
            val message = receiveMessageFromQueue(dlqUrl, waitTimeSeconds = 20)
            
            if (message != null) {
                // If we're looking for a specific message ID, check it
                if (expectedMessageId == null || message.messageId() == expectedMessageId) {
                    println("Found message in DLQ after $pollCount polls")
                    return message
                }
            }
            
            // Log progress every 5 polls (~2 minutes)
            if (pollCount % 5 == 0) {
                val elapsed = Duration.between(startTime, Instant.now())
                println("  Still waiting for DLQ message... (${elapsed.toMinutes()} minutes elapsed)")
            }
            
            // Wait before next poll
            Thread.sleep(5000)
        }
        
        return null
    }
    
    /**
     * Receives a message from an SQS queue.
     * 
     * Uses long polling to reduce API calls.
     */
    private fun receiveMessageFromQueue(
        queueUrl: String,
        waitTimeSeconds: Int = 20
    ): software.amazon.awssdk.services.sqs.model.Message? {
        val request = ReceiveMessageRequest.builder()
            .queueUrl(queueUrl)
            .maxNumberOfMessages(1)
            .waitTimeSeconds(waitTimeSeconds)
            .attributeNames(QueueAttributeName.ALL)
            .build()
        
        val response = sqsClient.receiveMessage(request)
        return response.messages().firstOrNull()
    }
}
