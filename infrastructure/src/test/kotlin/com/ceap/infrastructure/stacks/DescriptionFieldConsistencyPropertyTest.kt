package com.ceap.infrastructure.stacks

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.kotest.property.Arb
import io.kotest.property.arbitrary.constant
import io.kotest.property.checkAll
import java.io.File

/**
 * Property-based tests for description field consistency across all infrastructure code.
 * 
 * **Feature: complete-ceap-infrastructure-rebrand, Property 3: Description Field Consistency**
 * **Validates: Requirements 13.1, 13.2**
 * 
 * These property tests verify that all description fields in infrastructure code
 * (CloudFormation templates, EventBridge rules, state machine parameters, Kotlin code)
 * have been updated to use CEAP branding instead of solicitation terminology.
 * 
 * The tests ensure that:
 * 1. No description fields contain the word "solicitation" (case-insensitive)
 * 2. Appropriate description fields contain "ceap" or "CEAP" branding
 * 3. All human-readable descriptions are consistent with the new branding
 * 
 * The tests use property-based testing to verify these invariants hold across
 * multiple iterations with different test scenarios.
 */
class DescriptionFieldConsistencyPropertyTest : StringSpec({
    
    val projectRoot = File(System.getProperty("user.dir"))
    
    // Determine correct paths based on working directory
    val reactiveStackPath = if (File(projectRoot, "src/main/kotlin/com/ceap/infrastructure/stacks/ReactiveWorkflowStack.kt").exists()) {
        "src/main/kotlin/com/ceap/infrastructure/stacks/ReactiveWorkflowStack.kt"
    } else {
        "infrastructure/src/main/kotlin/com/ceap/infrastructure/stacks/ReactiveWorkflowStack.kt"
    }
    
    val orchestrationStackPath = if (File(projectRoot, "src/main/kotlin/com/ceap/infrastructure/stacks/OrchestrationStack.kt").exists()) {
        "src/main/kotlin/com/ceap/infrastructure/stacks/OrchestrationStack.kt"
    } else {
        "infrastructure/src/main/kotlin/com/ceap/infrastructure/stacks/OrchestrationStack.kt"
    }
    
    val observabilityDashboardPath = if (File(projectRoot, "src/main/kotlin/com/ceap/infrastructure/constructs/ObservabilityDashboard.kt").exists()) {
        "src/main/kotlin/com/ceap/infrastructure/constructs/ObservabilityDashboard.kt"
    } else {
        "infrastructure/src/main/kotlin/com/ceap/infrastructure/constructs/ObservabilityDashboard.kt"
    }
    
    val eventbridgeRulesPath = if (File(projectRoot, "eventbridge-rules.yaml").exists()) {
        "eventbridge-rules.yaml"
    } else {
        "infrastructure/eventbridge-rules.yaml"
    }
    
    val stepFunctionsPath = if (File(projectRoot, "step-functions.yaml").exists()) {
        "step-functions.yaml"
    } else {
        "infrastructure/step-functions.yaml"
    }
    
    val lambdaFunctionsPath = if (File(projectRoot, "lambda-functions.yaml").exists()) {
        "lambda-functions.yaml"
    } else {
        "infrastructure/lambda-functions.yaml"
    }
    
    val dynamodbTablesPath = if (File(projectRoot, "dynamodb-tables.yaml").exists()) {
        "dynamodb-tables.yaml"
    } else {
        "infrastructure/dynamodb-tables.yaml"
    }
    
    /**
     * Property: No description fields should contain "solicitation" (case-insensitive).
     * 
     * **Validates: Requirements 13.1**
     * 
     * This property verifies that all description fields in infrastructure code
     * do not contain the legacy "solicitation" terminology. This includes:
     * - EventBridge rule descriptions
     * - State machine parameter descriptions
     * - CloudFormation template descriptions
     * - Alarm descriptions
     * - Lambda function descriptions
     * - Any other human-readable description fields
     */
    "Property: No description fields should contain 'solicitation'" {
        checkAll(100, Arb.constant(Unit)) {
            val reactiveStackFile = File(projectRoot, reactiveStackPath)
            val orchestrationStackFile = File(projectRoot, orchestrationStackPath)
            val observabilityFile = File(projectRoot, observabilityDashboardPath)
            val eventbridgeFile = File(projectRoot, eventbridgeRulesPath)
            val stepFunctionsFile = File(projectRoot, stepFunctionsPath)
            val lambdaFunctionsFile = File(projectRoot, lambdaFunctionsPath)
            val dynamodbTablesFile = File(projectRoot, dynamodbTablesPath)
            
            // Collect all file contents
            val allContents = mutableListOf<String>()
            
            if (reactiveStackFile.exists()) {
                allContents.add(reactiveStackFile.readText())
            }
            if (orchestrationStackFile.exists()) {
                allContents.add(orchestrationStackFile.readText())
            }
            if (observabilityFile.exists()) {
                allContents.add(observabilityFile.readText())
            }
            if (eventbridgeFile.exists()) {
                allContents.add(eventbridgeFile.readText())
            }
            if (stepFunctionsFile.exists()) {
                allContents.add(stepFunctionsFile.readText())
            }
            if (lambdaFunctionsFile.exists()) {
                allContents.add(lambdaFunctionsFile.readText())
            }
            if (dynamodbTablesFile.exists()) {
                allContents.add(dynamodbTablesFile.readText())
            }
            
            // Extract description fields from all files
            val descriptionPattern = Regex(
                """(?:description|Description|alarmDescription|DESCRIPTION)\s*[=:("']\s*["']?([^"'\n]+)["']?""",
                RegexOption.IGNORE_CASE
            )
            
            allContents.forEach { content ->
                val matches = descriptionPattern.findAll(content)
                matches.forEach { match ->
                    val descriptionText = match.groupValues[1].lowercase()
                    // Verify no description contains "solicitation"
                    descriptionText shouldNotContain "solicitation"
                }
            }
        }
    }
    
    /**
     * Property: Appropriate description fields should contain "ceap" or "CEAP".
     * 
     * **Validates: Requirements 13.2**
     * 
     * This property verifies that description fields that reference the platform
     * or system use "ceap" or "CEAP" branding instead of the old terminology.
     * 
     * This includes descriptions for:
     * - EventBridge rules that trigger CEAP workflows
     * - State machine parameters for CEAP state machines
     * - CloudFormation templates for CEAP resources
     * - Alarms related to CEAP platform components
     */
    "Property: Appropriate description fields should contain 'ceap' or 'CEAP'" {
        checkAll(100, Arb.constant(Unit)) {
            val eventbridgeFile = File(projectRoot, eventbridgeRulesPath)
            val stepFunctionsFile = File(projectRoot, stepFunctionsPath)
            
            if (eventbridgeFile.exists()) {
                val content = eventbridgeFile.readText()
                
                // Verify EventBridge rule descriptions use "ceap"
                // These are descriptions that specifically reference the platform
                val ceapRuleDescriptions = listOf(
                    "retail ceap program",
                    "subscription ceap program",
                    "high-frequency ceap program",
                    "reactive ceap"
                )
                
                ceapRuleDescriptions.forEach { description ->
                    content shouldContain description
                }
            }
            
            if (stepFunctionsFile.exists()) {
                val content = stepFunctionsFile.readText()
                
                // Verify state machine parameter descriptions use "CEAP"
                // Look for "Reactive CEAP" or similar patterns
                if (content.contains("Reactive") && content.contains("State Machine")) {
                    // If there's a reactive state machine, its description should use CEAP
                    content shouldContain "CEAP"
                }
            }
        }
    }
    
    /**
     * Property: EventBridge rule descriptions should use CEAP branding.
     * 
     * **Validates: Requirements 13.1**
     * 
     * This property specifically verifies that EventBridge rule descriptions
     * in the eventbridge-rules.yaml file use CEAP branding consistently.
     */
    "Property: EventBridge rule descriptions should use CEAP branding" {
        checkAll(100, Arb.constant(Unit)) {
            val eventbridgeFile = File(projectRoot, eventbridgeRulesPath)
            
            if (eventbridgeFile.exists()) {
                val content = eventbridgeFile.readText()
                
                // Extract all Description fields from YAML
                val descriptionPattern = Regex("""Description:\s*['"]([^'"]+)['"]""")
                val descriptions = descriptionPattern.findAll(content).map { it.groupValues[1] }.toList()
                
                // Verify no descriptions contain "solicitation"
                descriptions.forEach { description ->
                    description.lowercase() shouldNotContain "solicitation"
                }
                
                // Verify descriptions that reference the platform use "ceap"
                val platformDescriptions = descriptions.filter { 
                    it.lowercase().contains("program") || 
                    it.lowercase().contains("reactive") ||
                    it.lowercase().contains("trigger")
                }
                
                platformDescriptions.forEach { description ->
                    if (description.lowercase().contains("program") || 
                        description.lowercase().contains("reactive")) {
                        description.lowercase() shouldContain "ceap"
                    }
                }
            }
        }
    }
    
    /**
     * Property: State machine parameter descriptions should use CEAP branding.
     * 
     * **Validates: Requirements 13.2**
     * 
     * This property specifically verifies that state machine parameter descriptions
     * in CloudFormation templates use CEAP branding instead of Solicitation.
     */
    "Property: State machine parameter descriptions should use CEAP branding" {
        checkAll(100, Arb.constant(Unit)) {
            val stepFunctionsFile = File(projectRoot, stepFunctionsPath)
            
            if (stepFunctionsFile.exists()) {
                val content = stepFunctionsFile.readText()
                
                // Extract all Description fields from YAML
                val descriptionPattern = Regex("""Description:\s*(.+)""")
                val descriptions = descriptionPattern.findAll(content).map { it.groupValues[1] }.toList()
                
                // Verify no descriptions contain "Solicitation"
                descriptions.forEach { description ->
                    description shouldNotContain "Solicitation"
                }
                
                // Verify descriptions that reference state machines use "CEAP"
                val stateMachineDescriptions = descriptions.filter { 
                    it.contains("State Machine") || it.contains("StateMachine")
                }
                
                stateMachineDescriptions.forEach { description ->
                    if (description.contains("Reactive")) {
                        description shouldContain "CEAP"
                    }
                }
            }
        }
    }
    
    /**
     * Property: Kotlin code description fields should use CEAP branding.
     * 
     * **Validates: Requirements 13.1, 13.2**
     * 
     * This property verifies that description fields in Kotlin infrastructure code
     * (EventBridge rules, alarms, etc.) use CEAP branding consistently.
     */
    "Property: Kotlin code description fields should use CEAP branding" {
        checkAll(100, Arb.constant(Unit)) {
            val reactiveStackFile = File(projectRoot, reactiveStackPath)
            val observabilityFile = File(projectRoot, observabilityDashboardPath)
            
            if (reactiveStackFile.exists()) {
                val content = reactiveStackFile.readText()
                
                // Extract description method calls
                val descriptionPattern = Regex("""\\.description\("([^"]+)"\)""")
                val descriptions = descriptionPattern.findAll(content).map { it.groupValues[1] }.toList()
                
                // Verify no descriptions contain "solicitation"
                descriptions.forEach { description ->
                    description.lowercase() shouldNotContain "solicitation"
                }
                
                // Verify descriptions that reference workflows use "ceap"
                val workflowDescriptions = descriptions.filter { 
                    it.lowercase().contains("workflow") || 
                    it.lowercase().contains("reactive")
                }
                
                workflowDescriptions.forEach { description ->
                    description.lowercase() shouldContain "ceap"
                }
            }
            
            if (observabilityFile.exists()) {
                val content = observabilityFile.readText()
                
                // Extract alarmDescription method calls
                val alarmDescriptionPattern = Regex("""\\.alarmDescription\("([^"]+)"\)""")
                val alarmDescriptions = alarmDescriptionPattern.findAll(content).map { it.groupValues[1] }.toList()
                
                // Verify no alarm descriptions contain "solicitation"
                alarmDescriptions.forEach { description ->
                    description.lowercase() shouldNotContain "solicitation"
                }
            }
        }
    }
    
    /**
     * Property: CloudFormation alarm names and descriptions should be consistent.
     * 
     * **Validates: Requirements 13.1**
     * 
     * This property verifies that CloudFormation alarm names and descriptions
     * in YAML files are consistent with CEAP branding.
     */
    "Property: CloudFormation alarm names and descriptions should be consistent" {
        checkAll(100, Arb.constant(Unit)) {
            val eventbridgeFile = File(projectRoot, eventbridgeRulesPath)
            
            if (eventbridgeFile.exists()) {
                val content = eventbridgeFile.readText()
                
                // Extract alarm names and descriptions
                val alarmNamePattern = Regex("""AlarmName:\s*!Sub\s*['"]([^'"]+)['"]""")
                val alarmDescriptionPattern = Regex("""AlarmDescription:\s*['"]([^'"]+)['"]""")
                
                val alarmNames = alarmNamePattern.findAll(content).map { it.groupValues[1] }.toList()
                val alarmDescriptions = alarmDescriptionPattern.findAll(content).map { it.groupValues[1] }.toList()
                
                // Verify no alarm names contain "solicitation"
                alarmNames.forEach { name ->
                    name.lowercase() shouldNotContain "solicitation"
                }
                
                // Verify no alarm descriptions contain "solicitation"
                alarmDescriptions.forEach { description ->
                    description.lowercase() shouldNotContain "solicitation"
                }
                
                // Verify alarm descriptions that reference reactive workflows use "ceap"
                val reactiveAlarmDescriptions = alarmDescriptions.filter { 
                    it.lowercase().contains("reactive")
                }
                
                reactiveAlarmDescriptions.forEach { description ->
                    description.lowercase() shouldContain "ceap"
                }
            }
        }
    }
    
    /**
     * Property: All infrastructure description fields should be consistent.
     * 
     * **Validates: Requirements 13.1, 13.2**
     * 
     * This comprehensive property verifies that all description fields across
     * all infrastructure files (Kotlin and YAML) consistently use CEAP branding
     * and do not contain any legacy solicitation terminology.
     */
    "Property: All infrastructure description fields should be consistent" {
        checkAll(100, Arb.constant(Unit)) {
            val reactiveStackFile = File(projectRoot, reactiveStackPath)
            val orchestrationStackFile = File(projectRoot, orchestrationStackPath)
            val observabilityFile = File(projectRoot, observabilityDashboardPath)
            val eventbridgeFile = File(projectRoot, eventbridgeRulesPath)
            val stepFunctionsFile = File(projectRoot, stepFunctionsPath)
            val lambdaFunctionsFile = File(projectRoot, lambdaFunctionsPath)
            val dynamodbTablesFile = File(projectRoot, dynamodbTablesPath)
            
            // Collect all file contents
            val allFiles = listOf(
                reactiveStackFile,
                orchestrationStackFile,
                observabilityFile,
                eventbridgeFile,
                stepFunctionsFile,
                lambdaFunctionsFile,
                dynamodbTablesFile
            ).filter { it.exists() }
            
            allFiles.forEach { file ->
                val content = file.readText()
                
                // Extract all description-like fields
                val patterns = listOf(
                    Regex("""\\.description\("([^"]+)"\)"""),
                    Regex("""\\.alarmDescription\("([^"]+)"\)"""),
                    Regex("""Description:\s*['"]([^'"]+)['"]"""),
                    Regex("""Description:\s*(.+)""")
                )
                
                patterns.forEach { pattern ->
                    val matches = pattern.findAll(content)
                    matches.forEach { match ->
                        val descriptionText = match.groupValues[1]
                        // Verify no description contains "solicitation" (case-insensitive)
                        descriptionText.lowercase() shouldNotContain "solicitation"
                    }
                }
            }
        }
    }
    
    /**
     * Property: Description fields should use consistent terminology.
     * 
     * **Validates: Requirements 13.1, 13.2**
     * 
     * This property verifies that description fields use consistent terminology
     * across all infrastructure files. Specifically:
     * - Use "ceap" (lowercase) in kebab-case contexts
     * - Use "CEAP" (uppercase) in formal contexts like parameter descriptions
     * - Never use "solicitation" or "Solicitation"
     */
    "Property: Description fields should use consistent terminology" {
        checkAll(100, Arb.constant(Unit)) {
            val eventbridgeFile = File(projectRoot, eventbridgeRulesPath)
            val stepFunctionsFile = File(projectRoot, stepFunctionsPath)
            val reactiveStackFile = File(projectRoot, reactiveStackPath)
            
            // Check EventBridge rules use lowercase "ceap" in descriptions
            if (eventbridgeFile.exists()) {
                val content = eventbridgeFile.readText()
                val ruleDescriptions = Regex("""Description:\s*['"]([^'"]+program[^'"]+)['"]""")
                    .findAll(content)
                    .map { it.groupValues[1] }
                    .toList()
                
                ruleDescriptions.forEach { description ->
                    // Program descriptions should use lowercase "ceap"
                    if (description.lowercase().contains("program")) {
                        description.lowercase() shouldContain "ceap"
                        description shouldNotContain "Solicitation"
                        description shouldNotContain "solicitation"
                    }
                }
            }
            
            // Check state machine parameter descriptions use uppercase "CEAP"
            if (stepFunctionsFile.exists()) {
                val content = stepFunctionsFile.readText()
                val paramDescriptions = Regex("""Description:\s*ARN of the ([^'\n]+)""")
                    .findAll(content)
                    .map { it.groupValues[1] }
                    .toList()
                
                paramDescriptions.forEach { description ->
                    // State machine ARN descriptions should use uppercase "CEAP"
                    if (description.contains("Reactive")) {
                        description shouldContain "CEAP"
                        description shouldNotContain "Solicitation"
                    }
                }
            }
            
            // Check Kotlin code uses lowercase "ceap" in descriptions
            if (reactiveStackFile.exists()) {
                val content = reactiveStackFile.readText()
                val kotlinDescriptions = Regex("""\\.description\("([^"]+)"\)""")
                    .findAll(content)
                    .map { it.groupValues[1] }
                    .toList()
                
                kotlinDescriptions.forEach { description ->
                    // Workflow descriptions should use lowercase "ceap"
                    if (description.lowercase().contains("workflow") || 
                        description.lowercase().contains("reactive")) {
                        description.lowercase() shouldContain "ceap"
                        description shouldNotContain "solicitation"
                    }
                }
            }
        }
    }
})
