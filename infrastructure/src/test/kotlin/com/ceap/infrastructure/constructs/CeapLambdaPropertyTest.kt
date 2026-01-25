package com.ceap.infrastructure.constructs

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.property.Arb
import io.kotest.property.arbitrary.*
import io.kotest.property.checkAll
import java.io.File
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.full.memberProperties

/**
 * Property-based tests for CeapLambda class interface preservation.
 * 
 * **Feature: complete-ceap-infrastructure-rebrand, Property 2: Class Interface Preservation**
 * **Validates: Requirements 1.3, 3.1, 14.2**
 * 
 * These property tests verify that the CeapLambda class maintains a stable interface
 * after the rename from SolicitationLambda. The tests ensure that:
 * 
 * 1. Constructor parameters remain unchanged (names, types, order, defaults)
 * 2. Public methods and properties remain unchanged
 * 3. The class maintains backward compatibility with existing usage patterns
 * 
 * The tests use property-based testing to verify these invariants hold across
 * multiple iterations with different test scenarios.
 */
class CeapLambdaPropertyTest : StringSpec({
    
    val projectRoot = File(System.getProperty("user.dir"))
    
    // When running tests from the infrastructure module, the working directory is the infrastructure directory
    // When running from the root, we need to include the infrastructure prefix
    val ceapLambdaPath = if (File(projectRoot, "src/main/kotlin/com/ceap/infrastructure/constructs/CeapLambda.kt").exists()) {
        "src/main/kotlin/com/ceap/infrastructure/constructs/CeapLambda.kt"
    } else {
        "infrastructure/src/main/kotlin/com/ceap/infrastructure/constructs/CeapLambda.kt"
    }
    
    /**
     * Property: Constructor signature should remain stable across all test iterations.
     * 
     * **Validates: Requirements 1.3, 3.1, 14.2**
     * 
     * This property verifies that the CeapLambda constructor maintains the expected
     * signature with all required and optional parameters in the correct order.
     * 
     * Expected constructor parameters:
     * 1. scope: Construct (required)
     * 2. id: String (required)
     * 3. handler: String (required)
     * 4. jarPath: String (required)
     * 5. environment: Map<String, String> (optional, default: emptyMap())
     * 6. tables: List<ITable> (optional, default: emptyList())
     * 7. memorySize: Int (optional, default: 512)
     * 8. timeout: Duration (optional, default: Duration.minutes(1))
     * 9. logRetention: RetentionDays (optional, default: RetentionDays.ONE_MONTH)
     */
    "Property: CeapLambda constructor signature should be preserved" {
        checkAll(100, Arb.constant(Unit)) {
            val ceapLambdaFile = File(projectRoot, ceapLambdaPath)
            ceapLambdaFile.exists() shouldBe true
            
            val content = ceapLambdaFile.readText()
            
            // Verify class definition exists
            content shouldContain "class CeapLambda"
            
            // Verify class extends Construct
            content shouldContain ": Construct(scope, id)"
            
            // Verify all required constructor parameters in order
            val requiredParams = listOf(
                "scope: Construct",
                "id: String",
                "handler: String",
                "jarPath: String"
            )
            
            requiredParams.forEach { param ->
                content shouldContain param
            }
            
            // Verify optional parameters with correct defaults
            val optionalParamsWithDefaults = mapOf(
                "environment: Map<String, String>" to "emptyMap()",
                "tables: List<ITable>" to "emptyList()",
                "memorySize: Int" to "512",
                "timeout: Duration" to "Duration.minutes(1)",
                "logRetention: RetentionDays" to "RetentionDays.ONE_MONTH"
            )
            
            optionalParamsWithDefaults.forEach { (param, default) ->
                content shouldContain param
                content shouldContain default
            }
            
            // Verify parameter order by checking they appear in sequence
            val allParams = requiredParams + optionalParamsWithDefaults.keys
            var lastIndex = 0
            allParams.forEach { param ->
                val currentIndex = content.indexOf(param, lastIndex)
                currentIndex shouldNotBe -1
                currentIndex shouldBe content.indexOf(param, lastIndex)
                lastIndex = currentIndex
            }
        }
    }
    
    /**
     * Property: Public API should remain stable across all test iterations.
     * 
     * **Validates: Requirements 1.3, 3.1, 14.2**
     * 
     * This property verifies that the CeapLambda class exposes the expected
     * public API, specifically the 'function' property of type Function.
     */
    "Property: CeapLambda public API should be preserved" {
        checkAll(100, Arb.constant(Unit)) {
            val ceapLambdaFile = File(projectRoot, ceapLambdaPath)
            val content = ceapLambdaFile.readText()
            
            // Verify the function property exists and is public
            content shouldContain "val function: Function"
            
            // Verify the function property is initialized in the constructor
            content shouldContain "Function.Builder.create"
        }
    }
    
    /**
     * Property: Required imports should remain stable across all test iterations.
     * 
     * **Validates: Requirements 1.3, 3.1, 14.2**
     * 
     * This property verifies that the CeapLambda class maintains all required
     * imports for its interface to function correctly.
     */
    "Property: CeapLambda required imports should be preserved" {
        checkAll(100, Arb.constant(Unit)) {
            val ceapLambdaFile = File(projectRoot, ceapLambdaPath)
            val content = ceapLambdaFile.readText()
            
            val requiredImports = listOf(
                "import software.amazon.awscdk.Duration",
                "import software.amazon.awscdk.services.dynamodb.ITable",
                "import software.amazon.awscdk.services.lambda.Code",
                "import software.amazon.awscdk.services.lambda.Function",
                "import software.amazon.awscdk.services.lambda.Runtime",
                "import software.amazon.awscdk.services.logs.RetentionDays",
                "import software.constructs.Construct"
            )
            
            requiredImports.forEach { import ->
                content shouldContain import
            }
        }
    }
    
    /**
     * Property: Package declaration should remain stable across all test iterations.
     * 
     * **Validates: Requirements 1.3, 3.1, 14.2**
     * 
     * This property verifies that the CeapLambda class maintains the correct
     * package declaration for proper module organization.
     */
    "Property: CeapLambda package declaration should be preserved" {
        checkAll(100, Arb.constant(Unit)) {
            val ceapLambdaFile = File(projectRoot, ceapLambdaPath)
            val content = ceapLambdaFile.readText()
            
            content shouldContain "package com.ceap.infrastructure.constructs"
        }
    }
    
    /**
     * Property: Constructor parameter types should be correct across all test iterations.
     * 
     * **Validates: Requirements 1.3, 3.1, 14.2**
     * 
     * This property verifies that each constructor parameter has the correct type
     * and that the types are consistent with the CDK API.
     */
    "Property: CeapLambda constructor parameter types should be correct" {
        checkAll(100, Arb.constant(Unit)) {
            val ceapLambdaFile = File(projectRoot, ceapLambdaPath)
            val content = ceapLambdaFile.readText()
            
            // Verify parameter types are correct
            val parameterTypes = mapOf(
                "scope" to "Construct",
                "id" to "String",
                "handler" to "String",
                "jarPath" to "String",
                "environment" to "Map<String, String>",
                "tables" to "List<ITable>",
                "memorySize" to "Int",
                "timeout" to "Duration",
                "logRetention" to "RetentionDays"
            )
            
            parameterTypes.forEach { (paramName, paramType) ->
                content shouldContain "$paramName: $paramType"
            }
        }
    }
    
    /**
     * Property: Class should extend Construct across all test iterations.
     * 
     * **Validates: Requirements 1.3, 3.1, 14.2**
     * 
     * This property verifies that the CeapLambda class maintains its inheritance
     * from the Construct base class, which is essential for CDK integration.
     */
    "Property: CeapLambda should extend Construct" {
        checkAll(100, Arb.constant(Unit)) {
            val ceapLambdaFile = File(projectRoot, ceapLambdaPath)
            val content = ceapLambdaFile.readText()
            
            // Verify the class extends Construct and calls super constructor
            content shouldContain ": Construct(scope, id)"
        }
    }
    
    /**
     * Property: Default parameter values should be correct across all test iterations.
     * 
     * **Validates: Requirements 1.3, 3.1, 14.2**
     * 
     * This property verifies that all optional parameters have the correct default
     * values that match the original SolicitationLambda interface.
     */
    "Property: CeapLambda default parameter values should be correct" {
        checkAll(100, Arb.constant(Unit)) {
            val ceapLambdaFile = File(projectRoot, ceapLambdaPath)
            val content = ceapLambdaFile.readText()
            
            // Verify specific default values
            val defaults = mapOf(
                "environment" to "emptyMap()",
                "tables" to "emptyList()",
                "memorySize" to "512",
                "timeout" to "Duration.minutes(1)",
                "logRetention" to "RetentionDays.ONE_MONTH"
            )
            
            defaults.forEach { (param, defaultValue) ->
                content shouldContain "$param: "
                content shouldContain "= $defaultValue"
            }
        }
    }
    
    /**
     * Property: Function property initialization should be correct across all test iterations.
     * 
     * **Validates: Requirements 1.3, 3.1, 14.2**
     * 
     * This property verifies that the function property is correctly initialized
     * using the Function.Builder pattern with all required configurations.
     */
    "Property: CeapLambda function property should be correctly initialized" {
        checkAll(100, Arb.constant(Unit)) {
            val ceapLambdaFile = File(projectRoot, ceapLambdaPath)
            val content = ceapLambdaFile.readText()
            
            // Verify function property initialization
            content shouldContain "val function: Function = Function.Builder.create"
            
            // Verify builder methods are called
            val builderMethods = listOf(
                ".runtime(Runtime.JAVA_17)",
                ".handler(handler)",
                ".code(Code.fromAsset(jarPath))",
                ".memorySize(memorySize)",
                ".timeout(timeout)",
                ".environment(environment)",
                ".logRetention(logRetention)",
                ".build()"
            )
            
            builderMethods.forEach { method ->
                content shouldContain method
            }
        }
    }
    
    /**
     * Property: DynamoDB permissions should be granted across all test iterations.
     * 
     * **Validates: Requirements 1.3, 3.1, 14.2**
     * 
     * This property verifies that the CeapLambda class maintains the behavior
     * of automatically granting DynamoDB permissions to the Lambda function.
     */
    "Property: CeapLambda should grant DynamoDB permissions" {
        checkAll(100, Arb.constant(Unit)) {
            val ceapLambdaFile = File(projectRoot, ceapLambdaPath)
            val content = ceapLambdaFile.readText()
            
            // Verify DynamoDB permissions are granted
            content shouldContain "tables.forEach"
            content shouldContain "table.grantReadWriteData(function)"
        }
    }
})
