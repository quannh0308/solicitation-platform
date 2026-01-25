package com.ceap.infrastructure.stacks

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.io.File

/**
 * Unit tests for workflow stack import statement updates.
 * 
 * **Validates: Requirements 3.2**
 * 
 * These tests verify that all workflow stacks have been updated to import
 * CeapLambda instead of SolicitationLambda. This ensures the rebrand is
 * complete and consistent across all infrastructure code.
 * 
 * The tests check all 5 workflow stacks:
 * - EtlWorkflowStack
 * - FilterWorkflowStack
 * - ScoreWorkflowStack
 * - StoreWorkflowStack
 * - ReactiveWorkflowStack
 */
class WorkflowStackImportsTest {
    
    private val projectRoot = File(System.getProperty("user.dir"))
    
    // When running tests from the infrastructure module, the working directory is the infrastructure directory
    // When running from the root, we need to include the infrastructure prefix
    private val stacksPath = if (File(projectRoot, "src/main/kotlin/com/ceap/infrastructure/stacks").exists()) {
        "src/main/kotlin/com/ceap/infrastructure/stacks"
    } else {
        "infrastructure/src/main/kotlin/com/ceap/infrastructure/stacks"
    }
    
    private val workflowStacks = listOf(
        "EtlWorkflowStack.kt",
        "FilterWorkflowStack.kt",
        "ScoreWorkflowStack.kt",
        "StoreWorkflowStack.kt",
        "ReactiveWorkflowStack.kt"
    )
    
    /**
     * Test that EtlWorkflowStack imports CeapLambda.
     * 
     * **Validates: Requirements 3.2**
     */
    @Test
    fun `EtlWorkflowStack should import CeapLambda`() {
        val stackFile = File(projectRoot, "$stacksPath/EtlWorkflowStack.kt")
        assertTrue(stackFile.exists(), "EtlWorkflowStack.kt should exist")
        
        val content = stackFile.readText()
        assertTrue(
            content.contains("import com.ceap.infrastructure.constructs.CeapLambda"),
            "EtlWorkflowStack should import CeapLambda"
        )
    }
    
    /**
     * Test that FilterWorkflowStack imports CeapLambda.
     * 
     * **Validates: Requirements 3.2**
     */
    @Test
    fun `FilterWorkflowStack should import CeapLambda`() {
        val stackFile = File(projectRoot, "$stacksPath/FilterWorkflowStack.kt")
        assertTrue(stackFile.exists(), "FilterWorkflowStack.kt should exist")
        
        val content = stackFile.readText()
        assertTrue(
            content.contains("import com.ceap.infrastructure.constructs.CeapLambda"),
            "FilterWorkflowStack should import CeapLambda"
        )
    }
    
    /**
     * Test that ScoreWorkflowStack imports CeapLambda.
     * 
     * **Validates: Requirements 3.2**
     */
    @Test
    fun `ScoreWorkflowStack should import CeapLambda`() {
        val stackFile = File(projectRoot, "$stacksPath/ScoreWorkflowStack.kt")
        assertTrue(stackFile.exists(), "ScoreWorkflowStack.kt should exist")
        
        val content = stackFile.readText()
        assertTrue(
            content.contains("import com.ceap.infrastructure.constructs.CeapLambda"),
            "ScoreWorkflowStack should import CeapLambda"
        )
    }
    
    /**
     * Test that StoreWorkflowStack imports CeapLambda.
     * 
     * **Validates: Requirements 3.2**
     */
    @Test
    fun `StoreWorkflowStack should import CeapLambda`() {
        val stackFile = File(projectRoot, "$stacksPath/StoreWorkflowStack.kt")
        assertTrue(stackFile.exists(), "StoreWorkflowStack.kt should exist")
        
        val content = stackFile.readText()
        assertTrue(
            content.contains("import com.ceap.infrastructure.constructs.CeapLambda"),
            "StoreWorkflowStack should import CeapLambda"
        )
    }
    
    /**
     * Test that ReactiveWorkflowStack imports CeapLambda.
     * 
     * **Validates: Requirements 3.2**
     */
    @Test
    fun `ReactiveWorkflowStack should import CeapLambda`() {
        val stackFile = File(projectRoot, "$stacksPath/ReactiveWorkflowStack.kt")
        assertTrue(stackFile.exists(), "ReactiveWorkflowStack.kt should exist")
        
        val content = stackFile.readText()
        assertTrue(
            content.contains("import com.ceap.infrastructure.constructs.CeapLambda"),
            "ReactiveWorkflowStack should import CeapLambda"
        )
    }
    
    /**
     * Test that all workflow stacks import CeapLambda.
     * 
     * **Validates: Requirements 3.2**
     * 
     * This test verifies all 5 workflow stacks in a single assertion,
     * providing a comprehensive check of the import statement updates.
     */
    @Test
    fun `all workflow stacks should import CeapLambda`() {
        workflowStacks.forEach { stackFileName ->
            val stackFile = File(projectRoot, "$stacksPath/$stackFileName")
            assertTrue(
                stackFile.exists(),
                "$stackFileName should exist"
            )
            
            val content = stackFile.readText()
            assertTrue(
                content.contains("import com.ceap.infrastructure.constructs.CeapLambda"),
                "$stackFileName should import CeapLambda"
            )
        }
    }
    
    /**
     * Test that EtlWorkflowStack does not import SolicitationLambda.
     * 
     * **Validates: Requirements 3.2**
     */
    @Test
    fun `EtlWorkflowStack should not import SolicitationLambda`() {
        val stackFile = File(projectRoot, "$stacksPath/EtlWorkflowStack.kt")
        val content = stackFile.readText()
        
        assertFalse(
            content.contains("import com.ceap.infrastructure.constructs.SolicitationLambda"),
            "EtlWorkflowStack should not import SolicitationLambda"
        )
    }
    
    /**
     * Test that FilterWorkflowStack does not import SolicitationLambda.
     * 
     * **Validates: Requirements 3.2**
     */
    @Test
    fun `FilterWorkflowStack should not import SolicitationLambda`() {
        val stackFile = File(projectRoot, "$stacksPath/FilterWorkflowStack.kt")
        val content = stackFile.readText()
        
        assertFalse(
            content.contains("import com.ceap.infrastructure.constructs.SolicitationLambda"),
            "FilterWorkflowStack should not import SolicitationLambda"
        )
    }
    
    /**
     * Test that ScoreWorkflowStack does not import SolicitationLambda.
     * 
     * **Validates: Requirements 3.2**
     */
    @Test
    fun `ScoreWorkflowStack should not import SolicitationLambda`() {
        val stackFile = File(projectRoot, "$stacksPath/ScoreWorkflowStack.kt")
        val content = stackFile.readText()
        
        assertFalse(
            content.contains("import com.ceap.infrastructure.constructs.SolicitationLambda"),
            "ScoreWorkflowStack should not import SolicitationLambda"
        )
    }
    
    /**
     * Test that StoreWorkflowStack does not import SolicitationLambda.
     * 
     * **Validates: Requirements 3.2**
     */
    @Test
    fun `StoreWorkflowStack should not import SolicitationLambda`() {
        val stackFile = File(projectRoot, "$stacksPath/StoreWorkflowStack.kt")
        val content = stackFile.readText()
        
        assertFalse(
            content.contains("import com.ceap.infrastructure.constructs.SolicitationLambda"),
            "StoreWorkflowStack should not import SolicitationLambda"
        )
    }
    
    /**
     * Test that ReactiveWorkflowStack does not import SolicitationLambda.
     * 
     * **Validates: Requirements 3.2**
     */
    @Test
    fun `ReactiveWorkflowStack should not import SolicitationLambda`() {
        val stackFile = File(projectRoot, "$stacksPath/ReactiveWorkflowStack.kt")
        val content = stackFile.readText()
        
        assertFalse(
            content.contains("import com.ceap.infrastructure.constructs.SolicitationLambda"),
            "ReactiveWorkflowStack should not import SolicitationLambda"
        )
    }
    
    /**
     * Test that no workflow stack imports SolicitationLambda.
     * 
     * **Validates: Requirements 3.2**
     * 
     * This test verifies that none of the 5 workflow stacks contain any
     * references to the old SolicitationLambda import, ensuring the
     * rebrand is complete.
     */
    @Test
    fun `no workflow stack should import SolicitationLambda`() {
        workflowStacks.forEach { stackFileName ->
            val stackFile = File(projectRoot, "$stacksPath/$stackFileName")
            val content = stackFile.readText()
            
            assertFalse(
                content.contains("import com.ceap.infrastructure.constructs.SolicitationLambda"),
                "$stackFileName should not import SolicitationLambda"
            )
        }
    }
    
    /**
     * Test that workflow stacks use CeapLambda in their code.
     * 
     * **Validates: Requirements 3.2**
     * 
     * This test verifies that the workflow stacks not only import CeapLambda
     * but also actually use it in their code (instantiation).
     */
    @Test
    fun `workflow stacks should instantiate CeapLambda`() {
        workflowStacks.forEach { stackFileName ->
            val stackFile = File(projectRoot, "$stacksPath/$stackFileName")
            val content = stackFile.readText()
            
            assertTrue(
                content.contains("CeapLambda("),
                "$stackFileName should instantiate CeapLambda"
            )
        }
    }
    
    /**
     * Test that workflow stacks do not reference SolicitationLambda in code.
     * 
     * **Validates: Requirements 3.2**
     * 
     * This test verifies that the workflow stacks do not contain any
     * references to SolicitationLambda in their code, not just in imports.
     */
    @Test
    fun `workflow stacks should not reference SolicitationLambda in code`() {
        workflowStacks.forEach { stackFileName ->
            val stackFile = File(projectRoot, "$stacksPath/$stackFileName")
            val content = stackFile.readText()
            
            assertFalse(
                content.contains("SolicitationLambda("),
                "$stackFileName should not instantiate SolicitationLambda"
            )
        }
    }
    
    /**
     * Test that workflow stack files have correct package declaration.
     * 
     * **Validates: Requirements 3.2**
     * 
     * This test ensures that all workflow stacks maintain the correct
     * package structure for proper module organization.
     */
    @Test
    fun `workflow stacks should have correct package declaration`() {
        workflowStacks.forEach { stackFileName ->
            val stackFile = File(projectRoot, "$stacksPath/$stackFileName")
            val content = stackFile.readText()
            
            assertTrue(
                content.contains("package com.ceap.infrastructure.stacks"),
                "$stackFileName should have correct package declaration"
            )
        }
    }
}
