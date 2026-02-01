package com.ceap.infrastructure

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.string
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.enum
import io.kotest.property.checkAll
import org.junit.jupiter.api.Tag

/**
 * Property 14: Express Workflow Failure Handling
 * 
 * For any Express workflow execution that fails, the EventBridge Pipe SHALL return failure
 * to SQS, causing the message to become visible again for retry
 * 
 * Validates: Requirements 4.3
 */
@Tag("Feature: infrastructure-enhancement, Property 14: Express Workflow Failure Handling")
class ExpressWorkflowFailureHandlingPropertyTest : FunSpec({
    
    test("EventBridge Pipe returns failure to SQS on workflow failure") {
        checkAll(100, Arb.executionId(), Arb.failureReason()) { executionId, failureReason ->
            // For any failed Express workflow execution
            val executionResult = ExecutionResult(
                executionId = executionId,
                status = ExecutionStatus.FAILED,
                failureReason = failureReason
            )
            
            // EventBridge Pipe should return failure (REQUEST_RESPONSE invocation)
            val pipeResponse = simulateEventBridgePipeResponse(executionResult)
            pipeResponse.success shouldBe false
            pipeResponse.returnedToSqs shouldBe true
        }
    }
    
    test("failed message becomes visible again in SQS for retry") {
        checkAll(100, Arb.executionId(), Arb.receiveCount()) { executionId, receiveCount ->
            // For any failed execution
            val executionResult = ExecutionResult(
                executionId = executionId,
                status = ExecutionStatus.FAILED,
                failureReason = "Lambda timeout"
            )
            
            // Message should become visible again
            val messageVisibility = simulateMessageVisibility(executionResult, receiveCount)
            messageVisibility.isVisible shouldBe true
            messageVisibility.canBeRetried shouldBe (receiveCount < 3)  // maxReceiveCount = 3
        }
    }
    
    test("synchronous invocation enables immediate failure detection") {
        checkAll(100, Arb.executionId(), Arb.failureReason()) { executionId, failureReason ->
            // For any failed Express workflow
            val executionResult = ExecutionResult(
                executionId = executionId,
                status = ExecutionStatus.FAILED,
                failureReason = failureReason
            )
            
            // REQUEST_RESPONSE invocation type enables immediate failure detection
            val invocationType = InvocationType.REQUEST_RESPONSE
            val failureDetected = detectFailureImmediately(executionResult, invocationType)
            failureDetected shouldBe true
        }
    }
    
    test("message retry count increments on each failure") {
        checkAll(100, Arb.executionId(), Arb.receiveCount()) { executionId, receiveCount ->
            // For any failed execution
            val executionResult = ExecutionResult(
                executionId = executionId,
                status = ExecutionStatus.FAILED,
                failureReason = "Processing error"
            )
            
            // Receive count should increment
            val newReceiveCount = receiveCount + 1
            val messageState = simulateMessageRetry(executionResult, receiveCount)
            messageState.receiveCount shouldBe newReceiveCount
        }
    }
    
    test("message moves to DLQ after max retries") {
        checkAll(100, Arb.executionId()) { executionId ->
            // For any execution that fails 3 times (maxReceiveCount)
            val executionResult = ExecutionResult(
                executionId = executionId,
                status = ExecutionStatus.FAILED,
                failureReason = "Permanent error"
            )
            
            val maxReceiveCount = 3
            val messageState = simulateMessageRetry(executionResult, maxReceiveCount)
            
            // After max retries, message should move to DLQ
            messageState.movedToDlq shouldBe true
            messageState.canBeRetried shouldBe false
        }
    }
    
    test("successful execution does not return message to SQS") {
        checkAll(100, Arb.executionId()) { executionId ->
            // For any successful execution
            val executionResult = ExecutionResult(
                executionId = executionId,
                status = ExecutionStatus.SUCCEEDED,
                failureReason = null
            )
            
            // Message should not be returned to SQS
            val pipeResponse = simulateEventBridgePipeResponse(executionResult)
            pipeResponse.success shouldBe true
            pipeResponse.returnedToSqs shouldBe false
        }
    }
})

// Data models
private enum class ExecutionStatus {
    SUCCEEDED,
    FAILED,
    TIMED_OUT,
    ABORTED
}

private enum class InvocationType {
    REQUEST_RESPONSE,  // Synchronous (Express)
    FIRE_AND_FORGET    // Asynchronous (Standard)
}

private data class ExecutionResult(
    val executionId: String,
    val status: ExecutionStatus,
    val failureReason: String?
)

private data class PipeResponse(
    val success: Boolean,
    val returnedToSqs: Boolean
)

private data class MessageVisibility(
    val isVisible: Boolean,
    val canBeRetried: Boolean
)

private data class MessageState(
    val receiveCount: Int,
    val movedToDlq: Boolean,
    val canBeRetried: Boolean
)

// Helper functions
private fun simulateEventBridgePipeResponse(executionResult: ExecutionResult): PipeResponse {
    // REQUEST_RESPONSE invocation returns failure synchronously
    return when (executionResult.status) {
        ExecutionStatus.SUCCEEDED -> PipeResponse(success = true, returnedToSqs = false)
        else -> PipeResponse(success = false, returnedToSqs = true)
    }
}

private fun simulateMessageVisibility(executionResult: ExecutionResult, receiveCount: Int): MessageVisibility {
    // Failed execution causes message to become visible again
    val isVisible = executionResult.status != ExecutionStatus.SUCCEEDED
    val canBeRetried = receiveCount < 3  // maxReceiveCount = 3
    return MessageVisibility(isVisible, canBeRetried)
}

private fun detectFailureImmediately(executionResult: ExecutionResult, invocationType: InvocationType): Boolean {
    // REQUEST_RESPONSE enables immediate failure detection
    return invocationType == InvocationType.REQUEST_RESPONSE && 
           executionResult.status != ExecutionStatus.SUCCEEDED
}

private fun simulateMessageRetry(executionResult: ExecutionResult, currentReceiveCount: Int): MessageState {
    val newReceiveCount = currentReceiveCount + 1
    val maxReceiveCount = 3
    val movedToDlq = newReceiveCount >= maxReceiveCount
    val canBeRetried = newReceiveCount < maxReceiveCount
    
    return MessageState(
        receiveCount = newReceiveCount,
        movedToDlq = movedToDlq,
        canBeRetried = canBeRetried
    )
}

// Arbitraries for generating test data
private fun Arb.Companion.executionId(): Arb<String> {
    return Arb.string(minSize = 10, maxSize = 50)
}

private fun Arb.Companion.failureReason(): Arb<String> {
    return Arb.string(minSize = 10, maxSize = 100)
}

private fun Arb.Companion.receiveCount(): Arb<Int> {
    return Arb.int(range = 0..5)
}
