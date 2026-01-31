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
 * Property-based tests for cross-stack reference mechanisms.
 * 
 * **Feature: infrastructure-consolidation, Property 9: Cross-Stack Reference Mechanisms**
 * **Validates: Requirements 10.1, 10.2, 10.3**
 * 
 * These property tests verify that all cross-stack references in the consolidated
 * CloudFormation templates use proper mechanisms:
 * 
 * - Requirement 10.1: CeapDataPlatform-dev references Storage_Layer resources using
 *   CloudFormation exports (Fn::ImportValue) or parameter passing
 * 
 * - Requirement 10.2: CeapServingAPI-dev references Storage_Layer resources using
 *   CloudFormation exports (Fn::ImportValue) or parameter passing
 * 
 * - Requirement 10.3: CeapServingAPI-dev references Write_Path resources using
 *   CloudFormation exports (Fn::ImportValue) or parameter passing
 * 
 * The tests ensure that:
 * - All cross-stack references use Fn::ImportValue intrinsic function
 * - All stacks define parameters for cross-stack dependencies
 * - All stacks export resources that may be referenced by other stacks
 * - No hard-coded ARNs or resource names are used for cross-stack references
 * - Export names follow consistent naming conventions
 */
class CrossStackReferenceMechanismsPropertyTest : StringSpec({
    
    val projectRoot = File(System.getProperty("user.dir"))
    val objectMapper = ObjectMapper()
    
    // Determine correct paths based on working directory
    val consolidatedTemplatesDir = if (File(projectRoot, "cdk.out.consolidated").exists()) {
        File(projectRoot, "cdk.out.consolidated")
    } else {
        File(projectRoot, "infrastructure/cdk.out.consolidated")
    }
    
    /**
     * Load CloudFormation template as JsonNode.
     */
    fun loadTemplate(templateFile: File): JsonNode? {
        if (!templateFile.exists()) {
            return null
        }
        return objectMapper.readTree(templateFile)
    }
    
    /**
     * Extract all Fn::ImportValue references from a template.
     * Returns a list of export names being imported.
     */
    fun extractImportValueReferences(template: JsonNode): List<String> {
        val imports = mutableListOf<String>()
        
        fun traverseNode(node: JsonNode) {
            when {
                node.isObject -> {
                    // Check if this is a Fn::ImportValue intrinsic function
                    if (node.has("Fn::ImportValue")) {
                        val importValue = node.get("Fn::ImportValue")
                        when {
                            importValue.isTextual -> {
                                imports.add(importValue.asText())
                            }
                            importValue.isObject && importValue.has("Fn::Sub") -> {
                                // Handle Fn::Sub within Fn::ImportValue
                                val subValue = importValue.get("Fn::Sub")
                                if (subValue.isTextual) {
                                    imports.add(subValue.asText())
                                }
                            }
                            importValue.isObject && importValue.has("Fn::Join") -> {
                                // Handle Fn::Join within Fn::ImportValue
                                // Fn::Join format: ["delimiter", [parts...]]
                                val joinArray = importValue.get("Fn::Join")
                                if (joinArray.isArray && joinArray.size() >= 2) {
                                    val parts = joinArray.get(1)
                                    if (parts.isArray) {
                                        // Extract the string parts from the join
                                        val joinedParts = mutableListOf<String>()
                                        parts.forEach { part ->
                                            when {
                                                part.isTextual -> joinedParts.add(part.asText())
                                                part.isObject && part.has("Ref") -> {
                                                    // Include the parameter name in the reference
                                                    val paramName = part.get("Ref").asText()
                                                    joinedParts.add(paramName)
                                                }
                                            }
                                        }
                                        // Add the joined string (this will contain the export name pattern)
                                        imports.add(joinedParts.joinToString(""))
                                    }
                                }
                            }
                        }
                    }
                    // Recursively traverse all fields
                    node.fields().forEach { (_, value) ->
                        traverseNode(value)
                    }
                }
                node.isArray -> {
                    node.forEach { element ->
                        traverseNode(element)
                    }
                }
            }
        }
        
        traverseNode(template)
        return imports
    }
    
    /**
     * Extract all stack parameters from a template.
     * Returns a map of parameter name to parameter definition.
     */
    fun extractParameters(template: JsonNode): Map<String, JsonNode> {
        val parameters = template.get("Parameters") ?: return emptyMap()
        val paramMap = mutableMapOf<String, JsonNode>()
        
        parameters.fields().forEach { (name, definition) ->
            paramMap[name] = definition
        }
        
        return paramMap
    }
    
    /**
     * Extract all stack outputs/exports from a template.
     * Returns a map of export name to output definition.
     */
    fun extractExports(template: JsonNode): Map<String, JsonNode> {
        val outputs = template.get("Outputs") ?: return emptyMap()
        val exportMap = mutableMapOf<String, JsonNode>()
        
        outputs.fields().forEach { (_, output) ->
            val exportName = output.get("Export")?.get("Name")
            if (exportName != null) {
                val name = when {
                    exportName.isTextual -> exportName.asText()
                    exportName.isObject && exportName.has("Fn::Sub") -> {
                        exportName.get("Fn::Sub").asText()
                    }
                    else -> null
                }
                if (name != null) {
                    exportMap[name] = output
                }
            }
        }
        
        return exportMap
    }
    
    /**
     * Check if a resource definition contains hard-coded ARNs or resource names
     * that should be using cross-stack references instead.
     */
    fun containsHardCodedCrossStackReferences(resource: JsonNode): Boolean {
        val resourceString = resource.toString()
        
        // Check for hard-coded stack names (should use parameters instead)
        val hardCodedStackNames = listOf(
            "CeapDatabase-dev",
            "CeapDataPlatform-dev",
            "CeapServingAPI-dev"
        )
        
        // Check for hard-coded ARN patterns (should use Fn::ImportValue instead)
        val hardCodedArnPatterns = listOf(
            "arn:aws:dynamodb:.*:table/ceap-",
            "arn:aws:lambda:.*:function:ceap-",
            "arn:aws:states:.*:stateMachine:ceap-"
        )
        
        // Allow stack names in parameter default values and export names
        if (resource.has("Default") || resource.has("Export")) {
            return false
        }
        
        // Check for hard-coded stack names in resource properties
        for (stackName in hardCodedStackNames) {
            if (resourceString.contains("\"$stackName\"") && 
                !resourceString.contains("Fn::Sub") &&
                !resourceString.contains("Ref")) {
                return true
            }
        }
        
        // Check for hard-coded ARN patterns
        for (pattern in hardCodedArnPatterns) {
            if (resourceString.matches(Regex(".*$pattern.*"))) {
                return true
            }
        }
        
        return false
    }
    
    /**
     * Property: CeapDataPlatform-dev uses Fn::ImportValue for database cross-stack references.
     * 
     * **Validates: Requirement 10.1**
     * 
     * This property verifies that CeapDataPlatform-dev stack uses CloudFormation
     * exports (Fn::ImportValue) to reference database resources from CeapDatabase-dev.
     */
    "Property: CeapDataPlatform-dev uses Fn::ImportValue for database cross-stack references" {
        val dataPlatformFile = File(consolidatedTemplatesDir, "CeapDataPlatform-dev.template.json")
        val template = loadTemplate(dataPlatformFile)
        
        template shouldNotBe null
        
        val imports = extractImportValueReferences(template!!)
        
        // Expected database table imports
        val expectedDatabaseImports = listOf(
            "CandidatesTableName",
            "CandidatesTableArn",
            "ProgramConfigTableName",
            "ProgramConfigTableArn",
            "ScoreCacheTableName",
            "ScoreCacheTableArn"
        )
        
        checkAll(100, Arb.of(expectedDatabaseImports)) { expectedImport ->
            // Each expected database import should be present in the imports list
            imports.any { it.contains(expectedImport) } shouldBe true
        }
    }
    
    /**
     * Property: CeapServingAPI-dev uses Fn::ImportValue for database cross-stack references.
     * 
     * **Validates: Requirement 10.2**
     * 
     * This property verifies that CeapServingAPI-dev stack uses CloudFormation
     * exports (Fn::ImportValue) to reference database resources from CeapDatabase-dev.
     */
    "Property: CeapServingAPI-dev uses Fn::ImportValue for database cross-stack references" {
        val servingAPIFile = File(consolidatedTemplatesDir, "CeapServingAPI-dev.template.json")
        val template = loadTemplate(servingAPIFile)
        
        template shouldNotBe null
        
        val imports = extractImportValueReferences(template!!)
        
        // Expected database table imports
        val expectedDatabaseImports = listOf(
            "CandidatesTableName",
            "CandidatesTableArn",
            "ProgramConfigTableName",
            "ProgramConfigTableArn",
            "ScoreCacheTableName",
            "ScoreCacheTableArn"
        )
        
        checkAll(100, Arb.of(expectedDatabaseImports)) { expectedImport ->
            // Each expected database import should be present in the imports list
            imports.any { it.contains(expectedImport) } shouldBe true
        }
    }
    
    /**
     * Property: CeapServingAPI-dev has parameter for data platform cross-stack references.
     * 
     * **Validates: Requirement 10.3**
     * 
     * This property verifies that CeapServingAPI-dev stack defines a parameter
     * for referencing the CeapDataPlatform-dev stack, enabling potential
     * cross-stack references to write path resources.
     */
    "Property: CeapServingAPI-dev has parameter for data platform cross-stack references" {
        val servingAPIFile = File(consolidatedTemplatesDir, "CeapServingAPI-dev.template.json")
        val template = loadTemplate(servingAPIFile)
        
        template shouldNotBe null
        
        val parameters = extractParameters(template!!)
        
        checkAll(100, Arb.of(Unit)) {
            // Should have DataPlatformStackName parameter
            parameters.keys shouldContain "DataPlatformStackName"
            
            // Parameter should have correct type
            val dataPlatformParam = parameters["DataPlatformStackName"]!!
            dataPlatformParam.get("Type").asText() shouldBe "String"
            
            // Parameter should have default value
            dataPlatformParam.has("Default") shouldBe true
        }
    }
    
    /**
     * Property: All consolidated stacks define parameters for cross-stack dependencies.
     * 
     * **Validates: Requirements 10.1, 10.2, 10.3**
     * 
     * This property verifies that all consolidated stacks define parameters
     * for their cross-stack dependencies, enabling flexible stack references.
     */
    "Property: All consolidated stacks define parameters for cross-stack dependencies" {
        val stackConfigs = listOf(
            "CeapDataPlatform-dev" to listOf("DatabaseStackName"),
            "CeapServingAPI-dev" to listOf("DatabaseStackName", "DataPlatformStackName")
        )
        
        checkAll(100, Arb.of(stackConfigs)) { (stackName, expectedParams) ->
            val templateFile = File(consolidatedTemplatesDir, "$stackName.template.json")
            val template = loadTemplate(templateFile)
            
            template shouldNotBe null
            
            val parameters = extractParameters(template!!)
            
            // All expected parameters should be defined
            expectedParams.forEach { expectedParam ->
                parameters.keys shouldContain expectedParam
            }
        }
    }
    
    /**
     * Property: CeapDatabase-dev exports all required resources for cross-stack references.
     * 
     * **Validates: Requirements 10.1, 10.2**
     * 
     * This property verifies that CeapDatabase-dev stack exports all database
     * resources that are referenced by other stacks.
     */
    "Property: CeapDatabase-dev exports all required resources for cross-stack references" {
        val databaseFile = File(consolidatedTemplatesDir, "CeapDatabase-dev.template.json")
        val template = loadTemplate(databaseFile)
        
        template shouldNotBe null
        
        val exports = extractExports(template!!)
        
        // Expected database exports
        val expectedExports = listOf(
            "CandidatesTableName",
            "CandidatesTableArn",
            "ProgramConfigTableName",
            "ProgramConfigTableArn",
            "ScoreCacheTableName",
            "ScoreCacheTableArn"
        )
        
        checkAll(100, Arb.of(expectedExports)) { expectedExport ->
            // Each expected export should be present
            exports.keys.any { it.contains(expectedExport) } shouldBe true
        }
    }
    
    /**
     * Property: CeapDataPlatform-dev exports workflow resources for potential cross-stack references.
     * 
     * **Validates: Requirement 10.3**
     * 
     * This property verifies that CeapDataPlatform-dev stack exports workflow
     * resources that may be referenced by CeapServingAPI-dev or other stacks.
     */
    "Property: CeapDataPlatform-dev exports workflow resources for potential cross-stack references" {
        val dataPlatformFile = File(consolidatedTemplatesDir, "CeapDataPlatform-dev.template.json")
        val template = loadTemplate(dataPlatformFile)
        
        template shouldNotBe null
        
        val exports = extractExports(template!!)
        
        // Expected workflow exports
        val expectedExports = listOf(
            "BatchIngestionWorkflowArn",
            "BatchIngestionWorkflowName",
            "ETLLambdaArn",
            "FilterLambdaArn",
            "ScoreLambdaArn",
            "StoreLambdaArn"
        )
        
        checkAll(100, Arb.of(expectedExports)) { expectedExport ->
            // Each expected export should be present
            exports.keys.any { it.contains(expectedExport) } shouldBe true
        }
    }
    
    /**
     * Property: Export names follow consistent naming convention.
     * 
     * **Validates: Requirements 10.1, 10.2, 10.3**
     * 
     * This property verifies that all CloudFormation exports follow a consistent
     * naming convention: {StackName}-{ResourceName}
     */
    "Property: Export names follow consistent naming convention" {
        val consolidatedStacks = listOf(
            "CeapDatabase-dev",
            "CeapDataPlatform-dev",
            "CeapServingAPI-dev"
        )
        
        checkAll(100, Arb.of(consolidatedStacks)) { stackName ->
            val templateFile = File(consolidatedTemplatesDir, "$stackName.template.json")
            val template = loadTemplate(templateFile)
            
            if (template != null) {
                val exports = extractExports(template)
                
                // All exports should follow the naming convention
                exports.keys.forEach { exportName ->
                    // Export name should contain stack name or use Fn::Sub with stack name
                    val containsStackName = exportName.contains(stackName) || 
                                          exportName.contains("\${AWS::StackName}")
                    containsStackName shouldBe true
                }
            }
        }
    }
    
    /**
     * Property: No hard-coded ARNs or resource names in cross-stack references.
     * 
     * **Validates: Requirements 10.1, 10.2, 10.3**
     * 
     * This property verifies that no resources use hard-coded ARNs or resource
     * names for cross-stack references. All cross-stack references should use
     * Fn::ImportValue or parameters.
     */
    "Property: No hard-coded ARNs or resource names in cross-stack references" {
        val consolidatedStacks = listOf(
            "CeapDataPlatform-dev",
            "CeapServingAPI-dev"
        )
        
        checkAll(100, Arb.of(consolidatedStacks)) { stackName ->
            val templateFile = File(consolidatedTemplatesDir, "$stackName.template.json")
            val template = loadTemplate(templateFile)
            
            if (template != null) {
                val resources = template.get("Resources")
                
                if (resources != null) {
                    resources.fields().forEach { (logicalId, resource) ->
                        // Skip CDK metadata resources
                        if (!logicalId.contains("CDKMetadata") && 
                            !logicalId.contains("CheckBootstrapVersion")) {
                            
                            // Resource should not contain hard-coded cross-stack references
                            containsHardCodedCrossStackReferences(resource) shouldBe false
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Property: All Fn::ImportValue references have corresponding exports.
     * 
     * **Validates: Requirements 10.1, 10.2, 10.3**
     * 
     * This property verifies that for every Fn::ImportValue reference in a stack,
     * there exists a corresponding export in another stack.
     */
    "Property: All Fn::ImportValue references have corresponding exports" {
        // Load all templates
        val databaseTemplate = loadTemplate(File(consolidatedTemplatesDir, "CeapDatabase-dev.template.json"))
        val dataPlatformTemplate = loadTemplate(File(consolidatedTemplatesDir, "CeapDataPlatform-dev.template.json"))
        val servingAPITemplate = loadTemplate(File(consolidatedTemplatesDir, "CeapServingAPI-dev.template.json"))
        
        // Collect all exports from all stacks
        val allExports = mutableSetOf<String>()
        databaseTemplate?.let { allExports.addAll(extractExports(it).keys) }
        dataPlatformTemplate?.let { allExports.addAll(extractExports(it).keys) }
        servingAPITemplate?.let { allExports.addAll(extractExports(it).keys) }
        
        // Check imports in CeapDataPlatform-dev
        if (dataPlatformTemplate != null) {
            val dataPlatformImports = extractImportValueReferences(dataPlatformTemplate)
            
            checkAll(100, Arb.of(dataPlatformImports)) { importName ->
                // Each import should have a corresponding export
                // Note: Import names may contain parameter references like ${DatabaseStackName}
                // so we check if any export contains the resource name part
                val resourceName = importName.substringAfterLast("-")
                allExports.any { it.contains(resourceName) } shouldBe true
            }
        }
        
        // Check imports in CeapServingAPI-dev
        if (servingAPITemplate != null) {
            val servingAPIImports = extractImportValueReferences(servingAPITemplate)
            
            checkAll(100, Arb.of(servingAPIImports)) { importName ->
                // Each import should have a corresponding export
                val resourceName = importName.substringAfterLast("-")
                allExports.any { it.contains(resourceName) } shouldBe true
            }
        }
    }
    
    /**
     * Property: Cross-stack references use parameter-based stack names.
     * 
     * **Validates: Requirements 10.1, 10.2, 10.3**
     * 
     * This property verifies that all Fn::ImportValue references use parameter-based
     * stack names (via Fn::Sub) rather than hard-coded stack names, enabling
     * flexible stack naming across environments.
     */
    "Property: Cross-stack references use parameter-based stack names" {
        val consolidatedStacks = listOf(
            "CeapDataPlatform-dev",
            "CeapServingAPI-dev"
        )
        
        checkAll(100, Arb.of(consolidatedStacks)) { stackName ->
            val templateFile = File(consolidatedTemplatesDir, "$stackName.template.json")
            val template = loadTemplate(templateFile)
            
            if (template != null) {
                val imports = extractImportValueReferences(template)
                
                // All imports should use parameter-based stack names
                imports.forEach { importName ->
                    // Import should contain parameter reference or Fn::Sub
                    val usesParameter = importName.contains("\${") || 
                                      importName.contains("DatabaseStackName") ||
                                      importName.contains("DataPlatformStackName")
                    usesParameter shouldBe true
                }
            }
        }
    }
    
    /**
     * Property: Stack parameters have appropriate default values.
     * 
     * **Validates: Requirements 10.1, 10.2, 10.3**
     * 
     * This property verifies that all stack parameters for cross-stack references
     * have appropriate default values that follow the naming convention.
     */
    "Property: Stack parameters have appropriate default values" {
        val stackConfigs = listOf(
            "CeapDataPlatform-dev" to mapOf(
                "DatabaseStackName" to "CeapDatabase-dev"
            ),
            "CeapServingAPI-dev" to mapOf(
                "DatabaseStackName" to "CeapDatabase-dev",
                "DataPlatformStackName" to "CeapDataPlatform-dev"
            )
        )
        
        checkAll(100, Arb.of(stackConfigs)) { (stackName, expectedDefaults) ->
            val templateFile = File(consolidatedTemplatesDir, "$stackName.template.json")
            val template = loadTemplate(templateFile)
            
            if (template != null) {
                val parameters = extractParameters(template)
                
                expectedDefaults.forEach { (paramName, expectedDefault) ->
                    // Parameter should exist
                    parameters.keys shouldContain paramName
                    
                    // Parameter should have correct default value
                    val param = parameters[paramName]!!
                    val defaultValue = param.get("Default")?.asText()
                    defaultValue shouldBe expectedDefault
                }
            }
        }
    }
})
