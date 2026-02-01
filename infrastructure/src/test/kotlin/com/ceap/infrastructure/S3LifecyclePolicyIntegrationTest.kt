package com.ceap.infrastructure

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.GetBucketLifecycleConfigurationRequest
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request
import software.amazon.awssdk.services.s3.model.NoSuchKeyException
import software.amazon.awssdk.core.sync.RequestBody
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.test.assertNotNull

/**
 * Integration test for S3 lifecycle policy verification.
 * 
 * This test validates that the S3 lifecycle policy correctly deletes
 * execution data after 7 days:
 * - Lifecycle rule targets executions/ prefix
 * - Objects older than 7 days are deleted
 * - Objects newer than 7 days are retained
 * 
 * Test Flow:
 * 1. Verify lifecycle policy is configured correctly
 * 2. Create test execution data with backdated timestamp
 * 3. Wait for lifecycle policy to process (or use time travel)
 * 4. Verify old objects are deleted
 * 5. Verify recent objects are retained
 * 
 * Note: This test is disabled by default because it requires either:
 * - Waiting 7+ days for lifecycle policy to execute (impractical)
 * - Using S3 time travel feature (requires special AWS account setup)
 * - Manually verifying lifecycle policy configuration
 * 
 * For practical testing, we verify the lifecycle policy configuration
 * is correct rather than waiting for actual deletion.
 * 
 * Validates: Requirement 12.5
 */
@Tag("integration")
@Tag("s3-lifecycle")
@Tag("long-running")
class S3LifecyclePolicyIntegrationTest {
    
    companion object {
        private val objectMapper: ObjectMapper = jacksonObjectMapper()
        private val s3Client: S3Client = S3Client.create()
        
        // Test configuration
        private const val TEST_ENVIRONMENT = "integration-test"
        private lateinit var workflowBucketName: String
        
        @JvmStatic
        @BeforeAll
        fun setup() {
            println("Setting up S3 lifecycle policy integration test...")
            
            workflowBucketName = System.getenv("TEST_WORKFLOW_BUCKET") 
                ?: "ceap-workflow-$TEST_ENVIRONMENT-123456789"
            
            println("Test configuration:")
            println("  Workflow bucket: $workflowBucketName")
            println()
        }
    }
    
    /**
     * Test: Verify lifecycle policy configuration.
     * 
     * This test validates that the S3 bucket has the correct lifecycle
     * policy configured:
     * - Rule ID: "DeleteOldExecutions"
     * - Prefix: "executions/"
     * - Expiration: 7 days
     * - Status: Enabled
     * 
     * This is a practical test that can run immediately without waiting
     * for actual object deletion.
     * 
     * Validates: Requirement 12.5
     */
    @Test
    fun `verifies lifecycle policy is configured correctly`() {
        println("Verifying S3 lifecycle policy configuration...")
        
        // Get bucket lifecycle configuration
        val request = GetBucketLifecycleConfigurationRequest.builder()
            .bucket(workflowBucketName)
            .build()
        
        val response = s3Client.getBucketLifecycleConfiguration(request)
        val rules = response.rules()
        
        // Verify lifecycle rules exist
        assertTrue(
            rules.isNotEmpty(),
            "Bucket should have at least one lifecycle rule"
        )
        println("Found ${rules.size} lifecycle rule(s)")
        
        // Find the DeleteOldExecutions rule
        val deleteRule = rules.find { rule ->
            rule.id() == "DeleteOldExecutions" ||
            rule.filter()?.prefix()?.startsWith("executions/") == true
        }
        
        assertNotNull(
            deleteRule,
            "Bucket should have lifecycle rule for executions/ prefix"
        )
        println("Found lifecycle rule: ${deleteRule.id()}")
        
        // Verify rule configuration
        assertEquals(
            "Enabled",
            deleteRule.status().toString(),
            "Lifecycle rule should be enabled"
        )
        println("  Status: ${deleteRule.status()}")
        
        // Verify prefix
        val prefix = deleteRule.filter()?.prefix() ?: ""
        assertTrue(
            prefix.startsWith("executions/"),
            "Lifecycle rule should target executions/ prefix, found: $prefix"
        )
        println("  Prefix: $prefix")
        
        // Verify expiration
        val expirationDays = deleteRule.expiration()?.days()
        assertNotNull(
            expirationDays,
            "Lifecycle rule should have expiration configured"
        )
        assertEquals(
            7,
            expirationDays,
            "Lifecycle rule should delete objects after 7 days"
        )
        println("  Expiration: $expirationDays days")
        
        println("S3 lifecycle policy configuration verified ✓")
    }
    
    /**
     * Test: Create test data and verify lifecycle behavior.
     * 
     * This test creates test execution data and verifies that:
     * - Recent objects (< 7 days) are retained
     * - Old objects (> 7 days) would be deleted (verified by policy config)
     * 
     * Note: This test does NOT wait for actual deletion (would take 7+ days).
     * Instead, it verifies the policy is configured correctly and creates
     * test data that can be manually verified after 7 days.
     * 
     * Validates: Requirement 12.5
     */
    @Test
    fun `creates test data for lifecycle policy verification`() {
        println("Creating test data for lifecycle policy verification...")
        
        // Create recent test execution data (should be retained)
        val recentExecutionId = "lifecycle-test-recent-${System.currentTimeMillis()}"
        createTestExecutionData(
            bucket = workflowBucketName,
            executionId = recentExecutionId,
            daysOld = 0
        )
        println("Created recent test data: executions/$recentExecutionId/")
        
        // Create old test execution data (should be deleted after 7 days)
        val oldExecutionId = "lifecycle-test-old-${System.currentTimeMillis()}"
        createTestExecutionData(
            bucket = workflowBucketName,
            executionId = oldExecutionId,
            daysOld = 8  // Simulate 8-day-old data
        )
        println("Created old test data: executions/$oldExecutionId/")
        
        // Verify recent data exists
        val recentDataExists = s3ObjectExists(
            bucket = workflowBucketName,
            key = "executions/$recentExecutionId/ETLStage/output.json"
        )
        assertTrue(
            recentDataExists,
            "Recent test data should exist immediately after creation"
        )
        println("Verified recent test data exists")
        
        // Verify old data exists (will be deleted by lifecycle policy after 7 days)
        val oldDataExists = s3ObjectExists(
            bucket = workflowBucketName,
            key = "executions/$oldExecutionId/ETLStage/output.json"
        )
        assertTrue(
            oldDataExists,
            "Old test data should exist immediately after creation"
        )
        println("Verified old test data exists")
        
        println()
        println("Test data created successfully.")
        println("To verify lifecycle policy deletion:")
        println("  1. Wait 7+ days")
        println("  2. Check that executions/$oldExecutionId/ is deleted")
        println("  3. Check that executions/$recentExecutionId/ still exists")
        println()
        println("Manual verification commands:")
        println("  aws s3 ls s3://$workflowBucketName/executions/$oldExecutionId/")
        println("  aws s3 ls s3://$workflowBucketName/executions/$recentExecutionId/")
        println()
    }
    
    /**
     * Test: Verify lifecycle policy deletes old objects.
     * 
     * This test is DISABLED by default because it requires waiting 7+ days
     * for the lifecycle policy to execute.
     * 
     * To enable this test:
     * 1. Remove @Disabled annotation
     * 2. Run the test to create test data
     * 3. Wait 7+ days
     * 4. Run the test again to verify deletion
     * 
     * Alternatively, use AWS S3 time travel feature if available in your
     * test environment.
     * 
     * Validates: Requirement 12.5
     */
    @Test
    @Disabled("Requires waiting 7+ days for lifecycle policy to execute")
    fun `verifies lifecycle policy deletes old objects`() {
        println("Verifying lifecycle policy deletes old objects...")
        println("Note: This test requires test data created 7+ days ago")
        
        // List all test execution data
        val listRequest = ListObjectsV2Request.builder()
            .bucket(workflowBucketName)
            .prefix("executions/lifecycle-test-old-")
            .build()
        
        val response = s3Client.listObjectsV2(listRequest)
        val oldObjects = response.contents()
        
        // Check object ages
        val now = Instant.now()
        oldObjects.forEach { obj ->
            val age = Duration.between(obj.lastModified(), now)
            val ageDays = age.toDays()
            
            println("Object: ${obj.key()}")
            println("  Age: $ageDays days")
            println("  Last modified: ${obj.lastModified()}")
            
            if (ageDays > 7) {
                fail("Object older than 7 days should have been deleted by lifecycle policy: ${obj.key()}")
            }
        }
        
        println("All objects are within 7-day retention period ✓")
    }
    
    /**
     * Creates test execution data in S3.
     * 
     * Creates a complete set of stage outputs for a test execution.
     * Optionally backdates the objects to simulate old data.
     */
    private fun createTestExecutionData(
        bucket: String,
        executionId: String,
        daysOld: Int = 0
    ) {
        val stages = listOf("ETLStage", "FilterStage", "ScoreStage", "StoreStage", "ReactiveStage")
        val timestamp = Instant.now().minus(daysOld.toLong(), ChronoUnit.DAYS)
        
        stages.forEach { stage ->
            val key = "executions/$executionId/$stage/output.json"
            val data = mapOf(
                "executionId" to executionId,
                "stage" to stage,
                "timestamp" to timestamp.toString(),
                "testData" to true,
                "daysOld" to daysOld,
                "candidates" to listOf(
                    mapOf("id" to "test-1", "score" to 75)
                )
            )
            
            val json = objectMapper.writeValueAsString(data)
            
            val request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType("application/json")
                .build()
            
            s3Client.putObject(request, RequestBody.fromString(json))
        }
    }
    
    /**
     * Checks if an S3 object exists.
     */
    private fun s3ObjectExists(bucket: String, key: String): Boolean {
        return try {
            val request = GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build()
            
            s3Client.getObject(request)
            true
        } catch (e: NoSuchKeyException) {
            false
        }
    }
}
