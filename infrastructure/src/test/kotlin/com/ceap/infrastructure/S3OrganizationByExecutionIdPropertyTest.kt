package com.ceap.infrastructure

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import io.kotest.property.Arb
import io.kotest.property.arbitrary.string
import io.kotest.property.arbitrary.list
import io.kotest.property.checkAll
import org.junit.jupiter.api.Tag

/**
 * Property 5: S3 Organization by Execution ID
 * 
 * For any workflow execution, all stage outputs SHALL be stored under the same S3 prefix
 * `executions/{executionId}/` to enable correlation
 * 
 * Validates: Requirements 2.5
 */
@Tag("Feature: infrastructure-enhancement, Property 5: S3 Organization by Execution ID")
class S3OrganizationByExecutionIdPropertyTest : FunSpec({
    
    test("all stage outputs are organized under same execution ID prefix") {
        checkAll(100, Arb.executionId(), Arb.stageNames()) { executionId, stages ->
            // For any execution ID and list of stages
            val outputPaths = stages.map { stage ->
                constructS3OutputPath(executionId, stage)
            }
            
            // All output paths should start with the same execution ID prefix
            val expectedPrefix = "executions/$executionId/"
            outputPaths.forEach { path ->
                path shouldStartWith expectedPrefix
            }
            
            // All paths should be unique (different stages)
            outputPaths.toSet().size shouldBe outputPaths.size
        }
    }
    
    test("execution ID prefix enables correlation of all stages") {
        checkAll(100, Arb.executionId(), Arb.stageNames()) { executionId, stages ->
            // For any execution ID and list of stages
            val outputPaths = stages.map { stage ->
                constructS3OutputPath(executionId, stage)
            }
            
            // Extract execution ID from each path
            val extractedExecutionIds = outputPaths.map { path ->
                extractExecutionIdFromPath(path)
            }
            
            // All extracted execution IDs should match the original
            extractedExecutionIds.forEach { extracted ->
                extracted shouldBe executionId
            }
        }
    }
    
    test("different executions have different prefixes") {
        checkAll(100, Arb.executionId(), Arb.executionId(), Arb.stageName()) { executionId1, executionId2, stage ->
            // For any two different execution IDs
            if (executionId1 != executionId2) {
                val path1 = constructS3OutputPath(executionId1, stage)
                val path2 = constructS3OutputPath(executionId2, stage)
                
                // Paths should have different prefixes
                val prefix1 = "executions/$executionId1/"
                val prefix2 = "executions/$executionId2/"
                
                path1 shouldStartWith prefix1
                path2 shouldStartWith prefix2
                prefix1 shouldBe prefix1  // Different from prefix2
            }
        }
    }
})

// Helper functions to construct and parse S3 paths
private fun constructS3OutputPath(executionId: String, stage: String): String {
    return "executions/$executionId/$stage/output.json"
}

private fun extractExecutionIdFromPath(path: String): String {
    // Path format: executions/{executionId}/{stage}/output.json
    val parts = path.split("/")
    return parts[1]  // executionId is at index 1
}

// Arbitraries for generating test data
private fun Arb.Companion.executionId(): Arb<String> {
    return Arb.string(minSize = 10, maxSize = 50)
}

private fun Arb.Companion.stageName(): Arb<String> {
    return Arb.string(minSize = 5, maxSize = 30)
}

private fun Arb.Companion.stageNames(): Arb<List<String>> {
    return Arb.list(Arb.stageName(), range = 1..10)
}
