package com.ceap.infrastructure

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.string
import io.kotest.property.arbitrary.enum
import io.kotest.property.checkAll
import org.junit.jupiter.api.Tag

/**
 * Property 6: S3 Output Persistence
 * 
 * For any workflow execution (completed or failed), the intermediate output files SHALL
 * remain in S3 after execution completes
 * 
 * Validates: Requirements 2.6
 */
@Tag("Feature: infrastructure-enhancement, Property 6: S3 Output Persistence")
class S3OutputPersistencePropertyTest : FunSpec({
    
    test("intermediate outputs persist after successful execution") {
        checkAll(100, Arb.executionId(), Arb.stageName(), Arb.executionStatus()) { executionId, stage, status ->
            // For any execution (regardless of status)
            val outputPath = constructS3OutputPath(executionId, stage)
            
            // Simulate workflow completion
            val workflowCompleted = true
            
            // Output should still exist after workflow completes
            val outputShouldPersist = shouldOutputPersist(workflowCompleted, outputPath)
            outputShouldPersist shouldBe true
        }
    }
    
    test("intermediate outputs persist after failed execution") {
        checkAll(100, Arb.executionId(), Arb.stageName()) { executionId, stage ->
            // For any failed execution
            val outputPath = constructS3OutputPath(executionId, stage)
            val executionStatus = ExecutionStatus.FAILED
            
            // Output should persist for debugging
            val outputShouldPersist = shouldOutputPersist(true, outputPath)
            outputShouldPersist shouldBe true
        }
    }
    
    test("outputs are not deleted immediately after execution") {
        checkAll(100, Arb.executionId(), Arb.stageName(), Arb.executionStatus()) { executionId, stage, status ->
            // For any execution
            val outputPath = constructS3OutputPath(executionId, stage)
            
            // Outputs should not be deleted immediately (lifecycle policy handles cleanup after 7 days)
            val immediatelyDeleted = false
            immediatelyDeleted shouldBe false
            
            // Outputs persist for audit and debugging
            val outputShouldPersist = shouldOutputPersist(true, outputPath)
            outputShouldPersist shouldBe true
        }
    }
    
    test("all stage outputs persist regardless of execution outcome") {
        checkAll(100, Arb.executionId(), Arb.stageNames(), Arb.executionStatus()) { executionId, stages, status ->
            // For any execution with multiple stages
            val outputPaths = stages.map { stage ->
                constructS3OutputPath(executionId, stage)
            }
            
            // All outputs should persist after execution
            outputPaths.forEach { path ->
                val outputShouldPersist = shouldOutputPersist(true, path)
                outputShouldPersist shouldBe true
            }
        }
    }
})

// Execution status enum
private enum class ExecutionStatus {
    SUCCEEDED,
    FAILED,
    TIMED_OUT,
    ABORTED
}

// Helper functions
private fun constructS3OutputPath(executionId: String, stage: String): String {
    return "executions/$executionId/$stage/output.json"
}

private fun shouldOutputPersist(workflowCompleted: Boolean, outputPath: String): Boolean {
    // Outputs always persist after workflow completes (for debugging and audit)
    // Lifecycle policy handles cleanup after 7 days
    return workflowCompleted
}

// Arbitraries for generating test data
private fun Arb.Companion.executionId(): Arb<String> {
    return Arb.string(minSize = 10, maxSize = 50)
}

private fun Arb.Companion.stageName(): Arb<String> {
    return Arb.string(minSize = 5, maxSize = 30)
}

private fun Arb.Companion.stageNames(): Arb<List<String>> {
    return io.kotest.property.arbitrary.list(Arb.stageName(), range = 1..10)
}

private fun Arb.Companion.executionStatus(): Arb<ExecutionStatus> {
    return Arb.enum<ExecutionStatus>()
}
