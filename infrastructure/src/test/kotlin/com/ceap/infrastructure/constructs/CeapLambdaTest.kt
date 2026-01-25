package com.ceap.infrastructure.constructs

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.io.File

/**
 * Unit tests for CeapLambda class existence and structure.
 * 
 * **Validates: Requirements 3.1**
 * 
 * These tests verify that:
 * 1. The CeapLambda.kt file exists in the correct location
 * 2. The CeapLambda class is defined with the correct constructor signature
 * 3. The old SolicitationLambda.kt file does not exist
 * 
 * Note: These tests use file-based verification to avoid compilation dependencies
 * on other infrastructure modules that may have incomplete refactoring.
 */
class CeapLambdaTest {
    
    private val projectRoot = File(System.getProperty("user.dir"))
    
    // When running tests from the infrastructure module, the working directory is the infrastructure directory
    // When running from the root, we need to include the infrastructure prefix
    private val ceapLambdaPath = if (File(projectRoot, "src/main/kotlin/com/ceap/infrastructure/constructs/CeapLambda.kt").exists()) {
        "src/main/kotlin/com/ceap/infrastructure/constructs/CeapLambda.kt"
    } else {
        "infrastructure/src/main/kotlin/com/ceap/infrastructure/constructs/CeapLambda.kt"
    }
    
    private val oldSolicitationLambdaPath = if (File(projectRoot, "src/main/kotlin/com/ceap/infrastructure/constructs").exists()) {
        "src/main/kotlin/com/ceap/infrastructure/constructs/SolicitationLambda.kt"
    } else {
        "infrastructure/src/main/kotlin/com/ceap/infrastructure/constructs/SolicitationLambda.kt"
    }
    
    /**
     * Test that CeapLambda.kt file exists in the correct location.
     * 
     * **Validates: Requirements 3.1**
     */
    @Test
    fun `CeapLambda file should exist`() {
        val ceapLambdaFile = File(projectRoot, ceapLambdaPath)
        assertTrue(
            ceapLambdaFile.exists(),
            "CeapLambda.kt file should exist at $ceapLambdaPath"
        )
        assertTrue(
            ceapLambdaFile.isFile,
            "CeapLambda.kt should be a file, not a directory"
        )
    }
    
    /**
     * Test that CeapLambda class is defined with correct constructor signature.
     * 
     * **Validates: Requirements 3.1**
     * 
     * Expected constructor parameters:
     * - scope: Construct
     * - id: String
     * - handler: String
     * - jarPath: String
     * - environment: Map<String, String> (default: emptyMap())
     * - tables: List<ITable> (default: emptyList())
     * - memorySize: Int (default: 512)
     * - timeout: Duration (default: Duration.minutes(1))
     * - logRetention: RetentionDays (default: RetentionDays.ONE_MONTH)
     */
    @Test
    fun `CeapLambda class should be defined with correct constructor signature`() {
        val ceapLambdaFile = File(projectRoot, ceapLambdaPath)
        val content = ceapLambdaFile.readText()
        
        // Verify the class is defined
        assertTrue(
            content.contains("class CeapLambda"),
            "File should contain 'class CeapLambda' definition"
        )
        
        // Verify the class extends Construct
        assertTrue(
            content.contains(": Construct(scope, id)"),
            "CeapLambda should extend Construct"
        )
        
        // Verify all required constructor parameters are present
        val requiredParams = listOf(
            "scope: Construct",
            "id: String",
            "handler: String",
            "jarPath: String"
        )
        
        requiredParams.forEach { param ->
            assertTrue(
                content.contains(param),
                "Constructor should have parameter: $param"
            )
        }
        
        // Verify optional parameters with defaults
        val optionalParams = listOf(
            "environment: Map<String, String> = emptyMap()",
            "tables: List<ITable> = emptyList()",
            "memorySize: Int = 512",
            "timeout: Duration = Duration.minutes(1)",
            "logRetention: RetentionDays = RetentionDays.ONE_MONTH"
        )
        
        optionalParams.forEach { param ->
            assertTrue(
                content.contains(param),
                "Constructor should have optional parameter: $param"
            )
        }
    }
    
    /**
     * Test that old SolicitationLambda.kt file does not exist.
     * 
     * **Validates: Requirements 3.1**
     * 
     * This verifies that the file rename from task 1.1 was successful and
     * no legacy files remain in the codebase.
     */
    @Test
    fun `old SolicitationLambda file should not exist`() {
        val oldFile = File(projectRoot, oldSolicitationLambdaPath)
        assertFalse(
            oldFile.exists(),
            "SolicitationLambda.kt file should not exist - it should have been renamed to CeapLambda.kt"
        )
    }
    
    /**
     * Test that CeapLambda class has correct package declaration.
     * 
     * **Validates: Requirements 3.1**
     */
    @Test
    fun `CeapLambda should have correct package declaration`() {
        val ceapLambdaFile = File(projectRoot, ceapLambdaPath)
        val content = ceapLambdaFile.readText()
        
        assertTrue(
            content.contains("package com.ceap.infrastructure.constructs"),
            "File should have correct package declaration"
        )
    }
    
    /**
     * Test that CeapLambda class has correct imports.
     * 
     * **Validates: Requirements 3.1**
     */
    @Test
    fun `CeapLambda should have required CDK imports`() {
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
            assertTrue(
                content.contains(import),
                "File should contain import: $import"
            )
        }
    }
    
    /**
     * Test that CeapLambda class has a function property.
     * 
     * **Validates: Requirements 3.1**
     */
    @Test
    fun `CeapLambda should have a function property`() {
        val ceapLambdaFile = File(projectRoot, ceapLambdaPath)
        val content = ceapLambdaFile.readText()
        
        assertTrue(
            content.contains("val function: Function"),
            "Class should have a 'function' property of type Function"
        )
    }
}
