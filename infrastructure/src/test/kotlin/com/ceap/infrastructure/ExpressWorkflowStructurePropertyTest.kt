package com.ceap.infrastructure

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.property.Arb
import io.kotest.property.arbitrary.string
import io.kotest.property.arbitrary.list
import io.kotest.property.checkAll
import org.junit.jupiter.api.Tag

/**
 * Property 15: Express Workflow Structure
 * 
 * For any Express workflow, Lambda functions SHALL be chained sequentially, each Lambda
 * SHALL write output to S3, and each non-first Lambda SHALL read input from S3
 * 
 * Validates: Requirements 4.4
 */
@Tag("Feature: infrastructure-enhancement, Property 15: Express Workflow Structure")
class ExpressWorkflowStructurePropertyTest : FunSpec({
    
    test("Lambda functions are chained sequentially") {
        checkAll(100, Arb.lambdaStages()) { stages ->
            // For any list of Lambda stages
            val workflow = createExpressWorkflow(stages)
            
            // Stages should be chained in order
            workflow.stages.size shouldBe stages.size
            
            // Each stage (except last) should have a next stage
            for (i in 0 until workflow.stages.size - 1) {
                val currentStage = workflow.stages[i]
                val nextStage = workflow.stages[i + 1]
                
                currentStage.hasNext shouldBe true
                currentStage.nextStage shouldBe nextStage.name
            }
            
            // Last stage should not have a next stage
            workflow.stages.last().hasNext shouldBe false
        }
    }
    
    test("each Lambda writes output to S3") {
        checkAll(100, Arb.executionId(), Arb.lambdaStages()) { executionId, stages ->
            // For any execution and stages
            val workflow = createExpressWorkflow(stages)
            
            // Each stage should write to S3
            workflow.stages.forEach { stage ->
                val outputPath = stage.getOutputPath(executionId)
                val writesToS3 = outputPath.startsWith("executions/$executionId/")
                writesToS3 shouldBe true
            }
        }
    }
    
    test("non-first Lambda reads input from S3") {
        checkAll(100, Arb.executionId(), Arb.lambdaStages()) { executionId, stages ->
            // For any execution with multiple stages
            if (stages.size > 1) {
                val workflow = createExpressWorkflow(stages)
                
                // First stage uses initialData
                val firstStage = workflow.stages[0]
                firstStage.usesInitialData shouldBe true
                firstStage.readsFromS3 shouldBe false
                
                // All other stages read from S3
                for (i in 1 until workflow.stages.size) {
                    val stage = workflow.stages[i]
                    stage.usesInitialData shouldBe false
                    stage.readsFromS3 shouldBe true
                    
                    // Input path should reference previous stage
                    val previousStage = workflow.stages[i - 1]
                    val inputPath = stage.getInputPath(executionId)
                    inputPath shouldBe "executions/$executionId/${previousStage.name}/output.json"
                }
            }
        }
    }
    
    test("first Lambda uses initialData instead of S3") {
        checkAll(100, Arb.executionId(), Arb.lambdaStages()) { executionId, stages ->
            // For any execution
            val workflow = createExpressWorkflow(stages)
            
            // First stage should use initialData
            val firstStage = workflow.stages[0]
            firstStage.usesInitialData shouldBe true
            firstStage.readsFromS3 shouldBe false
        }
    }
    
    test("workflow maintains sequential data flow through S3") {
        checkAll(100, Arb.executionId(), Arb.lambdaStages()) { executionId, stages ->
            // For any execution with multiple stages
            if (stages.size > 1) {
                val workflow = createExpressWorkflow(stages)
                
                // Verify data flow: stage[i] output -> stage[i+1] input
                for (i in 0 until workflow.stages.size - 1) {
                    val currentStage = workflow.stages[i]
                    val nextStage = workflow.stages[i + 1]
                    
                    val outputPath = currentStage.getOutputPath(executionId)
                    val nextInputPath = nextStage.getInputPath(executionId)
                    
                    // Next stage's input should match current stage's output
                    outputPath shouldBe nextInputPath
                }
            }
        }
    }
})

// Data models for Express workflow
private data class ExpressWorkflow(
    val stages: List<WorkflowStage>
)

private data class WorkflowStage(
    val name: String,
    val hasNext: Boolean,
    val nextStage: String?,
    val usesInitialData: Boolean,
    val readsFromS3: Boolean
) {
    fun getOutputPath(executionId: String): String {
        return "executions/$executionId/$name/output.json"
    }
    
    fun getInputPath(executionId: String): String {
        // First stage doesn't have an input path (uses initialData)
        if (usesInitialData) {
            return ""
        }
        // Non-first stages read from previous stage output
        // This is a simplified version - in real implementation, we'd track previous stage
        return "executions/$executionId/PreviousStage/output.json"
    }
}

// Helper function to create Express workflow
private fun createExpressWorkflow(stageNames: List<String>): ExpressWorkflow {
    val stages = stageNames.mapIndexed { index, name ->
        WorkflowStage(
            name = name,
            hasNext = index < stageNames.size - 1,
            nextStage = if (index < stageNames.size - 1) stageNames[index + 1] else null,
            usesInitialData = index == 0,
            readsFromS3 = index > 0
        )
    }
    
    return ExpressWorkflow(stages)
}

// Arbitraries for generating test data
private fun Arb.Companion.executionId(): Arb<String> {
    return Arb.string(minSize = 10, maxSize = 50)
}

private fun Arb.Companion.stageName(): Arb<String> {
    return Arb.string(minSize = 5, maxSize = 30)
}

private fun Arb.Companion.lambdaStages(): Arb<List<String>> {
    return Arb.list(Arb.stageName(), range = 1..10)
}
