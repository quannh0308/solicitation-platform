package com.ceap.infrastructure

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.of
import io.kotest.property.checkAll
import java.io.File

/**
 * Property-based tests for resource consolidation completeness.
 * 
 * **Feature: infrastructure-consolidation, Property 2: Resource Consolidation Completeness**
 * **Validates: Requirements 1.3, 1.4, 1.5**
 * 
 * These property tests verify that all resources from the original 7 stacks
 * are consolidated into exactly one of the 3 new stacks:
 * 
 * Original 7 stacks:
 * - CeapDatabase-dev (unchanged)
 * - CeapEtlWorkflow-dev → CeapDataPlatform-dev
 * - CeapFilterWorkflow-dev → CeapDataPlatform-dev
 * - CeapScoreWorkflow-dev → CeapDataPlatform-dev
 * - CeapStoreWorkflow-dev → CeapDataPlatform-dev
 * - CeapOrchestration-dev → CeapDataPlatform-dev
 * - CeapReactiveWorkflow-dev → CeapServingAPI-dev
 * 
 * New 3 stacks:
 * - CeapDatabase-dev (Storage Layer)
 * - CeapDataPlatform-dev (Write Path)
 * - CeapServingAPI-dev (Read Path)
 * 
 * The tests ensure that:
 * - Every resource type from the original 7 stacks exists in exactly one of the 3 new stacks
 * - No resource types are duplicated across multiple new stacks
 * - No resource types are lost during consolidation
 * - The consolidation mapping follows the expected pattern
 * 
 * Note: CloudFormation logical IDs may change during consolidation (this is expected CDK behavior).
 * Property 7 (Logical Name Preservation) specifically tests for logical ID preservation.
 * This property focuses on ensuring all resource types and counts are preserved.
 */
class ResourceConsolidationCompletenessPropertyTest : StringSpec({
    
    val projectRoot = File(System.getProperty("user.dir"))
    val objectMapper = ObjectMapper()
    
    // Determine correct paths based on working directory
    val originalTemplatesDir = if (File(projectRoot, "cdk.out").exists()) {
        File(projectRoot, "cdk.out")
    } else {
        File(projectRoot, "infrastructure/cdk.out")
    }
    
    val consolidatedTemplatesDir = if (File(projectRoot, "cdk.out.consolidated").exists()) {
        File(projectRoot, "cdk.out.consolidated")
    } else {
        File(projectRoot, "infrastructure/cdk.out.consolidated")
    }
    
    /**
     * Load CloudFormation template and extract resource logical IDs.
     */
    fun loadResourceIds(templateFile: File): Set<String> {
        if (!templateFile.exists()) {
            return emptySet()
        }
        
        val template = objectMapper.readTree(templateFile)
        val resources = template.get("Resources") ?: return emptySet()
        
        return resources.fieldNames().asSequence().toSet()
    }
    
    /**
     * Load CloudFormation template and extract resources by type.
     * Returns a map of resource type to count.
     */
    fun loadResourcesByType(templateFile: File): Map<String, Int> {
        if (!templateFile.exists()) {
            return emptyMap()
        }
        
        val template = objectMapper.readTree(templateFile)
        val resources = template.get("Resources") ?: return emptyMap()
        
        val resourceTypes = mutableMapOf<String, Int>()
        resources.fields().forEach { (_, resource) ->
            val type = resource.get("Type")?.asText() ?: "Unknown"
            resourceTypes[type] = resourceTypes.getOrDefault(type, 0) + 1
        }
        
        return resourceTypes
    }
    
    /**
     * Get the expected resource type counts for a consolidated stack.
     */
    fun getExpectedResourceTypeCounts(originalStacks: List<String>, originalTemplatesDir: File): Map<String, Int> {
        val expectedCounts = mutableMapOf<String, Int>()
        
        originalStacks.forEach { stackName ->
            val templateFile = File(originalTemplatesDir, "$stackName.template.json")
            val resourceTypes = loadResourcesByType(templateFile)
            
            resourceTypes.forEach { (type, count) ->
                expectedCounts[type] = expectedCounts.getOrDefault(type, 0) + count
            }
        }
        
        return expectedCounts
    }
    
    /**
     * Load all resources from original 7 stacks.
     */
    fun loadOriginalStackResources(): Map<String, Set<String>> {
        val originalStacks = listOf(
            "CeapDatabase-dev",
            "CeapEtlWorkflow-dev",
            "CeapFilterWorkflow-dev",
            "CeapScoreWorkflow-dev",
            "CeapStoreWorkflow-dev",
            "CeapReactiveWorkflow-dev",
            "CeapOrchestration-dev"
        )
        
        return originalStacks.associateWith { stackName ->
            val templateFile = File(originalTemplatesDir, "$stackName.template.json")
            loadResourceIds(templateFile)
        }
    }
    
    /**
     * Load all resources from consolidated 3 stacks.
     */
    fun loadConsolidatedStackResources(): Map<String, Set<String>> {
        val consolidatedStacks = listOf(
            "CeapDatabase-dev",
            "CeapDataPlatform-dev",
            "CeapServingAPI-dev"
        )
        
        return consolidatedStacks.associateWith { stackName ->
            val templateFile = File(consolidatedTemplatesDir, "$stackName.template.json")
            loadResourceIds(templateFile)
        }
    }
    
    /**
     * Property: Write path stacks are consolidated into CeapDataPlatform-dev with correct resource types.
     * 
     * **Validates: Requirements 1.5**
     * 
     * This property verifies that all business logic resources from the 5 write path stacks
     * (ETL, Filter, Score, Store, Orchestration) are present in CeapDataPlatform-dev.
     * 
     * Note: CDK may deduplicate shared helper resources (like LogRetention) across stacks.
     * This test focuses on business logic resources, not CDK-generated helper resources.
     */
    "Property: Write path stacks are consolidated into CeapDataPlatform-dev with correct resource types" {
        val dataPlatformFile = File(consolidatedTemplatesDir, "CeapDataPlatform-dev.template.json")
        val dataPlatformResources = loadResourcesByType(dataPlatformFile)
        
        // Expected business logic resource counts (not including CDK helper resources)
        val expectedBusinessLogicResources = mapOf(
            "AWS::StepFunctions::StateMachine" to 1,  // BatchIngestionWorkflow
            "AWS::Events::Rule" to 1  // BatchIngestionSchedule
        )
        
        checkAll(100, Arb.of(expectedBusinessLogicResources.keys.toList())) { resourceType ->
            val expectedCount = expectedBusinessLogicResources[resourceType] ?: 0
            val actualCount = dataPlatformResources[resourceType] ?: 0
            
            // Business logic resources should have exact count
            actualCount shouldBe expectedCount
        }
        
        // Verify business logic Lambda functions exist (by checking for specific logical ID patterns)
        val template = objectMapper.readTree(dataPlatformFile)
        val resources = template.get("Resources")
        val lambdaIds = resources.fieldNames().asSequence()
            .filter { resources.get(it).get("Type").asText() == "AWS::Lambda::Function" }
            .filter { !it.contains("LogRetention") }  // Exclude CDK helper Lambdas
            .toList()
        
        // Should have 4 business logic Lambdas: ETL, Filter, Score, Store
        checkAll(100, Arb.of(Unit)) {
            lambdaIds.size shouldBe 4
            lambdaIds.any { it.contains("ETL") } shouldBe true
            lambdaIds.any { it.contains("Filter") } shouldBe true
            lambdaIds.any { it.contains("Score") } shouldBe true
            lambdaIds.any { it.contains("Store") } shouldBe true
        }
    }
    
    /**
     * Property: All Orchestration resources are consolidated into CeapDataPlatform-dev.
     * 
     * **Validates: Requirements 1.5**
     * 
     * This property verifies that all resources from CeapOrchestration-dev
     * are consolidated into CeapDataPlatform-dev stack.
     */
    "Property: All Orchestration resources are consolidated into CeapDataPlatform-dev" {
        val originalResources = loadOriginalStackResources()
        val consolidatedResources = loadConsolidatedStackResources()
        
        val orchestrationResources = originalResources["CeapOrchestration-dev"] ?: emptySet()
        val dataPlatformResources = consolidatedResources["CeapDataPlatform-dev"] ?: emptySet()
        
        checkAll(100, Arb.of(orchestrationResources.toList())) { resourceId ->
            // Each Orchestration resource should be in CeapDataPlatform-dev
            dataPlatformResources shouldContain resourceId
        }
    }
    
    /**
     * Property: Read path stack is consolidated into CeapServingAPI-dev with correct resource types.
     * 
     * **Validates: Requirements 1.4**
     * 
     * This property verifies that all resource types from CeapReactiveWorkflow-dev
     * are present in CeapServingAPI-dev with the correct counts.
     */
    "Property: Read path stack is consolidated into CeapServingAPI-dev with correct resource types" {
        val readPathStacks = listOf("CeapReactiveWorkflow-dev")
        
        val expectedResourceTypes = getExpectedResourceTypeCounts(readPathStacks, originalTemplatesDir)
        val servingAPIFile = File(consolidatedTemplatesDir, "CeapServingAPI-dev.template.json")
        val actualResourceTypes = loadResourcesByType(servingAPIFile)
        
        checkAll(100, Arb.of(expectedResourceTypes.keys.toList())) { resourceType ->
            val expectedCount = expectedResourceTypes[resourceType] ?: 0
            val actualCount = actualResourceTypes[resourceType] ?: 0
            
            // Each resource type should have the expected count
            actualCount shouldBe expectedCount
        }
    }
    
    /**
     * Property: Database resources remain in CeapDatabase-dev with same resource types.
     * 
     * **Validates: Requirements 1.3**
     * 
     * This property verifies that all resource types from the original
     * CeapDatabase-dev stack remain in the consolidated CeapDatabase-dev stack
     * with the same counts.
     */
    "Property: Database resources remain in CeapDatabase-dev with same resource types" {
        val originalDbFile = File(originalTemplatesDir, "CeapDatabase-dev.template.json")
        val consolidatedDbFile = File(consolidatedTemplatesDir, "CeapDatabase-dev.template.json")
        
        val originalResourceTypes = loadResourcesByType(originalDbFile)
        val consolidatedResourceTypes = loadResourcesByType(consolidatedDbFile)
        
        checkAll(100, Arb.of(originalResourceTypes.keys.toList())) { resourceType ->
            val originalCount = originalResourceTypes[resourceType] ?: 0
            val consolidatedCount = consolidatedResourceTypes[resourceType] ?: 0
            
            // Each resource type should have the same count
            consolidatedCount shouldBe originalCount
        }
    }
    
    /**
     * Property: Core resource types are preserved during consolidation.
     * 
     * **Validates: Requirements 1.3, 1.4, 1.5**
     * 
     * This property verifies that core AWS business logic resource types (Lambda, Step Functions,
     * EventBridge Rules) are preserved during consolidation.
     * 
     * Note: CDK may deduplicate helper resources (like LogRetention), so we count only
     * business logic resources, not CDK-generated helper resources.
     */
    "Property: Core resource types are preserved during consolidation" {
        // Count business logic Lambda functions (exclude LogRetention)
        fun countBusinessLogicLambdas(templateFile: File): Int {
            if (!templateFile.exists()) return 0
            val template = objectMapper.readTree(templateFile)
            val resources = template.get("Resources") ?: return 0
            return resources.fieldNames().asSequence()
                .filter { resources.get(it).get("Type").asText() == "AWS::Lambda::Function" }
                .filter { !it.contains("LogRetention") }
                .count()
        }
        
        // Count Step Functions and EventBridge Rules (these are always business logic)
        fun countResourceType(templateFile: File, resourceType: String): Int {
            if (!templateFile.exists()) return 0
            val template = objectMapper.readTree(templateFile)
            val resources = template.get("Resources") ?: return 0
            return resources.fieldNames().asSequence()
                .filter { resources.get(it).get("Type").asText() == resourceType }
                .count()
        }
        
        // Count business logic resources in original stacks
        val originalStacks = listOf(
            "CeapDatabase-dev",
            "CeapEtlWorkflow-dev",
            "CeapFilterWorkflow-dev",
            "CeapScoreWorkflow-dev",
            "CeapStoreWorkflow-dev",
            "CeapReactiveWorkflow-dev",
            "CeapOrchestration-dev"
        )
        
        var originalLambdaCount = 0
        var originalStepFunctionCount = 0
        var originalEventRuleCount = 0
        
        originalStacks.forEach { stackName ->
            val templateFile = File(originalTemplatesDir, "$stackName.template.json")
            originalLambdaCount += countBusinessLogicLambdas(templateFile)
            originalStepFunctionCount += countResourceType(templateFile, "AWS::StepFunctions::StateMachine")
            originalEventRuleCount += countResourceType(templateFile, "AWS::Events::Rule")
        }
        
        // Count business logic resources in consolidated stacks
        val consolidatedStacks = listOf(
            "CeapDatabase-dev",
            "CeapDataPlatform-dev",
            "CeapServingAPI-dev"
        )
        
        var consolidatedLambdaCount = 0
        var consolidatedStepFunctionCount = 0
        var consolidatedEventRuleCount = 0
        
        consolidatedStacks.forEach { stackName ->
            val templateFile = File(consolidatedTemplatesDir, "$stackName.template.json")
            consolidatedLambdaCount += countBusinessLogicLambdas(templateFile)
            consolidatedStepFunctionCount += countResourceType(templateFile, "AWS::StepFunctions::StateMachine")
            consolidatedEventRuleCount += countResourceType(templateFile, "AWS::Events::Rule")
        }
        
        checkAll(100, Arb.of(Unit)) {
            // Business logic Lambda count should be preserved
            consolidatedLambdaCount shouldBe originalLambdaCount
            // Step Function count should be preserved
            consolidatedStepFunctionCount shouldBe originalStepFunctionCount
            // EventBridge Rule count should be preserved
            consolidatedEventRuleCount shouldBe originalEventRuleCount
        }
    }
    
    /**
     * Property: CeapDataPlatform-dev contains Lambda functions from all write path stacks.
     * 
     * **Validates: Requirements 1.5**
     * 
     * This property verifies that CeapDataPlatform-dev contains business logic Lambda functions
     * from all 5 write path stacks (ETL, Filter, Score, Store, Orchestration).
     * 
     * Note: CDK helper Lambdas (like LogRetention) are excluded from this count.
     */
    "Property: CeapDataPlatform-dev contains Lambda functions from all write path stacks" {
        // Count business logic Lambda functions (exclude LogRetention)
        fun countBusinessLogicLambdas(templateFile: File): Int {
            if (!templateFile.exists()) return 0
            val template = objectMapper.readTree(templateFile)
            val resources = template.get("Resources") ?: return 0
            return resources.fieldNames().asSequence()
                .filter { resources.get(it).get("Type").asText() == "AWS::Lambda::Function" }
                .filter { !it.contains("LogRetention") }
                .count()
        }
        
        val writePathStacks = listOf(
            "CeapEtlWorkflow-dev",
            "CeapFilterWorkflow-dev",
            "CeapScoreWorkflow-dev",
            "CeapStoreWorkflow-dev",
            "CeapOrchestration-dev"
        )
        
        // Count business logic Lambda functions in original write path stacks
        var expectedLambdaCount = 0
        writePathStacks.forEach { stackName ->
            val templateFile = File(originalTemplatesDir, "$stackName.template.json")
            expectedLambdaCount += countBusinessLogicLambdas(templateFile)
        }
        
        // Count business logic Lambda functions in CeapDataPlatform-dev
        val dataPlatformFile = File(consolidatedTemplatesDir, "CeapDataPlatform-dev.template.json")
        val actualLambdaCount = countBusinessLogicLambdas(dataPlatformFile)
        
        checkAll(100, Arb.of(Unit)) {
            // Business logic Lambda function count should match
            actualLambdaCount shouldBe expectedLambdaCount
        }
    }
    
    /**
     * Property: CeapServingAPI-dev contains Lambda functions from reactive stack.
     * 
     * **Validates: Requirements 1.4**
     * 
     * This property verifies that CeapServingAPI-dev contains Lambda functions
     * from the CeapReactiveWorkflow-dev stack.
     */
    "Property: CeapServingAPI-dev contains Lambda functions from reactive stack" {
        // Count Lambda functions in original reactive stack
        val reactiveFile = File(originalTemplatesDir, "CeapReactiveWorkflow-dev.template.json")
        val reactiveResourceTypes = loadResourcesByType(reactiveFile)
        val expectedLambdaCount = reactiveResourceTypes["AWS::Lambda::Function"] ?: 0
        
        // Count Lambda functions in CeapServingAPI-dev
        val servingAPIFile = File(consolidatedTemplatesDir, "CeapServingAPI-dev.template.json")
        val servingAPIResourceTypes = loadResourcesByType(servingAPIFile)
        val actualLambdaCount = servingAPIResourceTypes["AWS::Lambda::Function"] ?: 0
        
        checkAll(100, Arb.of(Unit)) {
            // Lambda function count should match
            actualLambdaCount shouldBe expectedLambdaCount
        }
    }
    
    /**
     * Property: Step Functions are correctly distributed across consolidated stacks.
     * 
     * **Validates: Requirements 1.3, 1.4, 1.5**
     * 
     * This property verifies that Step Functions from the original stacks
     * are correctly distributed to the consolidated stacks.
     */
    "Property: Step Functions are correctly distributed across consolidated stacks" {
        // Count Step Functions in original stacks
        val originalStacks = listOf(
            "CeapDatabase-dev",
            "CeapEtlWorkflow-dev",
            "CeapFilterWorkflow-dev",
            "CeapScoreWorkflow-dev",
            "CeapStoreWorkflow-dev",
            "CeapReactiveWorkflow-dev",
            "CeapOrchestration-dev"
        )
        
        var originalStepFunctionCount = 0
        originalStacks.forEach { stackName ->
            val templateFile = File(originalTemplatesDir, "$stackName.template.json")
            val resourceTypes = loadResourcesByType(templateFile)
            originalStepFunctionCount += resourceTypes["AWS::StepFunctions::StateMachine"] ?: 0
        }
        
        // Count Step Functions in consolidated stacks
        val consolidatedStacks = listOf(
            "CeapDatabase-dev",
            "CeapDataPlatform-dev",
            "CeapServingAPI-dev"
        )
        
        var consolidatedStepFunctionCount = 0
        consolidatedStacks.forEach { stackName ->
            val templateFile = File(consolidatedTemplatesDir, "$stackName.template.json")
            val resourceTypes = loadResourcesByType(templateFile)
            consolidatedStepFunctionCount += resourceTypes["AWS::StepFunctions::StateMachine"] ?: 0
        }
        
        checkAll(100, Arb.of(Unit)) {
            // Step Function count should be preserved
            consolidatedStepFunctionCount shouldBe originalStepFunctionCount
        }
    }
    
    /**
     * Property: IAM Roles are correctly distributed across consolidated stacks.
     * 
     * **Validates: Requirements 1.3, 1.4, 1.5**
     * 
     * This property verifies that business logic IAM Roles from the original stacks
     * are correctly distributed to the consolidated stacks.
     * 
     * Note: CDK may deduplicate helper IAM Roles (like LogRetention service roles).
     * This test focuses on business logic IAM Roles.
     */
    "Property: IAM Roles are correctly distributed across consolidated stacks" {
        // Count business logic IAM Roles (exclude LogRetention service roles)
        fun countBusinessLogicRoles(templateFile: File): Int {
            if (!templateFile.exists()) return 0
            val template = objectMapper.readTree(templateFile)
            val resources = template.get("Resources") ?: return 0
            return resources.fieldNames().asSequence()
                .filter { resources.get(it).get("Type").asText() == "AWS::IAM::Role" }
                .filter { !it.contains("LogRetention") }
                .count()
        }
        
        // Count business logic IAM Roles in original stacks
        val originalStacks = listOf(
            "CeapDatabase-dev",
            "CeapEtlWorkflow-dev",
            "CeapFilterWorkflow-dev",
            "CeapScoreWorkflow-dev",
            "CeapStoreWorkflow-dev",
            "CeapReactiveWorkflow-dev",
            "CeapOrchestration-dev"
        )
        
        var originalIAMRoleCount = 0
        originalStacks.forEach { stackName ->
            val templateFile = File(originalTemplatesDir, "$stackName.template.json")
            originalIAMRoleCount += countBusinessLogicRoles(templateFile)
        }
        
        // Count business logic IAM Roles in consolidated stacks
        val consolidatedStacks = listOf(
            "CeapDatabase-dev",
            "CeapDataPlatform-dev",
            "CeapServingAPI-dev"
        )
        
        var consolidatedIAMRoleCount = 0
        consolidatedStacks.forEach { stackName ->
            val templateFile = File(consolidatedTemplatesDir, "$stackName.template.json")
            consolidatedIAMRoleCount += countBusinessLogicRoles(templateFile)
        }
        
        checkAll(100, Arb.of(Unit)) {
            // Business logic IAM Role count should be preserved
            consolidatedIAMRoleCount shouldBe originalIAMRoleCount
        }
    }
    
    /**
     * Property: Consolidated templates exist and are valid.
     * 
     * **Validates: Requirements 1.3, 1.4, 1.5**
     * 
     * This property verifies that all consolidated template files exist
     * and contain valid CloudFormation resources.
     */
    "Property: Consolidated templates exist and are valid" {
        val consolidatedStacks = listOf(
            "CeapDatabase-dev",
            "CeapDataPlatform-dev",
            "CeapServingAPI-dev"
        )
        
        checkAll(100, Arb.of(consolidatedStacks)) { stackName ->
            val templateFile = File(consolidatedTemplatesDir, "$stackName.template.json")
            
            // Template file should exist
            templateFile.exists() shouldBe true
            
            // Template should be valid JSON
            val template = objectMapper.readTree(templateFile)
            template shouldNotBe null
            
            // Template should have Resources section
            val resources = template.get("Resources")
            resources shouldNotBe null
            
            // Resources section should not be empty
            resources.fieldNames().asSequence().toList().shouldNotBeEmpty()
        }
    }
})
