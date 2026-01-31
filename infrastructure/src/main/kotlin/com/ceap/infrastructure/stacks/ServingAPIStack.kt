package com.ceap.infrastructure.stacks

import software.amazon.awscdk.CfnOutput
import software.amazon.awscdk.CfnParameter
import software.amazon.awscdk.Duration
import software.amazon.awscdk.Fn
import software.amazon.awscdk.Stack
import software.amazon.awscdk.StackProps
import software.amazon.awscdk.services.dynamodb.Attribute
import software.amazon.awscdk.services.dynamodb.AttributeType
import software.amazon.awscdk.services.dynamodb.BillingMode
import software.amazon.awscdk.services.dynamodb.Table
import software.amazon.awscdk.services.events.EventPattern
import software.amazon.awscdk.services.events.Rule
import software.amazon.awscdk.services.events.targets.LambdaFunction
import software.amazon.awscdk.services.iam.PolicyStatement
import software.amazon.awscdk.services.lambda.Code
import software.amazon.awscdk.services.lambda.Function
import software.amazon.awscdk.services.lambda.Runtime
import software.amazon.awscdk.services.logs.RetentionDays
import software.constructs.Construct

/**
 * Serving API Stack - Consolidates the Read Path for CEAP.
 * 
 * This stack consolidates resources from 1 original stack:
 * - CeapReactiveWorkflow-dev (Reactive Lambda, EventBridge rules, and deduplication table)
 * 
 * The Serving API focuses on the B-3 capability (low-latency retrieval):
 * - Real-time event processing (Reactive workflow)
 * - Future: API Gateway for serving endpoints
 * - Future: Lambda authorizers for API security
 * - Future: Serving-specific EventBridge rules
 * 
 * Resources (to be migrated in Task 2.8):
 * - 1 Lambda Function (Reactive)
 * - 1 EventBridge Rule (CustomerEventRule)
 * - 1 DynamoDB Table (DeduplicationTable)
 * 
 * Dependencies:
 * - CeapDatabase-dev stack (for DynamoDB table references)
 * - CeapDataPlatform-dev stack (for potential workflow orchestration references)
 * 
 * Cross-Stack References:
 * - Uses CloudFormation imports (Fn::ImportValue) to reference database tables
 * - Uses CloudFormation imports (Fn::ImportValue) to reference data platform workflows if needed
 * - Exports reactive workflow resources for potential use by other stacks
 * 
 * Requirements Validated:
 * - Requirement 1.4: Read Path consolidated into CeapServingAPI-dev
 * - Requirement 7.2: Depends on CeapDatabase-dev stack
 * - Requirement 10.2: Uses CloudFormation exports for database cross-stack references
 * - Requirement 10.3: Uses CloudFormation exports for data platform cross-stack references
 */
class ServingAPIStack(
    scope: Construct,
    id: String,
    props: StackProps,
    val envName: String
) : Stack(scope, id, props) {
    
    /**
     * Parameter for database stack name to enable cross-stack references.
     * This allows the Serving API stack to import database table names and ARNs
     * from the CeapDatabase-dev stack using CloudFormation exports.
     * 
     * Default: CeapDatabase-{envName}
     * 
     * Validates: Requirement 7.2 (stack dependencies)
     * Validates: Requirement 10.2 (cross-stack reference mechanisms for database)
     */
    val databaseStackName = CfnParameter.Builder.create(this, "DatabaseStackName")
        .type("String")
        .defaultValue("CeapDatabase-$envName")
        .description("Name of the database stack for cross-stack references. " +
                "Used to import DynamoDB table names and ARNs via CloudFormation exports.")
        .build()
    
    /**
     * Parameter for data platform stack name to enable cross-stack references.
     * This allows the Serving API stack to import data platform workflow ARNs
     * from the CeapDataPlatform-dev stack using CloudFormation exports.
     * 
     * Default: CeapDataPlatform-{envName}
     * 
     * Validates: Requirement 7.2 (stack dependencies)
     * Validates: Requirement 10.3 (cross-stack reference mechanisms for data platform)
     */
    val dataPlatformStackName = CfnParameter.Builder.create(this, "DataPlatformStackName")
        .type("String")
        .defaultValue("CeapDataPlatform-$envName")
        .description("Name of the data platform stack for cross-stack references. " +
                "Used to import workflow ARNs and other resources via CloudFormation exports.")
        .build()
    
    init {
        // Set stack description
        this.templateOptions.description = 
            "CEAP Serving API Stack - Read Path consolidation containing Reactive workflow " +
            "and future serving API resources. This stack handles real-time event processing " +
            "and low-latency data retrieval for the Customer Engagement & Action Platform. " +
            "Depends on CeapDatabase-$envName stack for DynamoDB tables and " +
            "CeapDataPlatform-$envName stack for workflow orchestration."
        
        // Set stack metadata
        this.templateOptions.metadata = mapOf(
            "Purpose" to "Serving API - Read Path",
            "BusinessCapability" to "B-3: Low-Latency Retrieval",
            "ConsolidatedFrom" to listOf(
                "CeapReactiveWorkflow-$envName"
            ),
            "Dependencies" to listOf(
                "CeapDatabase-$envName",
                "CeapDataPlatform-$envName"
            ),
            "Version" to "1.0.0",
            "LastUpdated" to "2025-01-27",
            "Requirements" to listOf(
                "1.4: Read Path consolidated into CeapServingAPI-dev",
                "7.2: Depends on CeapDatabase-dev and CeapDataPlatform-dev stacks",
                "10.2: Uses CloudFormation exports for database cross-stack references",
                "10.3: Uses CloudFormation exports for data platform cross-stack references"
            )
        )
        
        // Import database table names and ARNs from CeapDatabase-dev stack
        // These imports enable cross-stack references using CloudFormation exports
        // Validates: Requirement 10.2 (cross-stack reference mechanisms for database)
        val candidatesTableName = Fn.importValue("${databaseStackName.valueAsString}-CandidatesTableName")
        val candidatesTableArn = Fn.importValue("${databaseStackName.valueAsString}-CandidatesTableArn")
        val programConfigTableName = Fn.importValue("${databaseStackName.valueAsString}-ProgramConfigTableName")
        val programConfigTableArn = Fn.importValue("${databaseStackName.valueAsString}-ProgramConfigTableArn")
        val scoreCacheTableName = Fn.importValue("${databaseStackName.valueAsString}-ScoreCacheTableName")
        val scoreCacheTableArn = Fn.importValue("${databaseStackName.valueAsString}-ScoreCacheTableArn")
        
        // ========================================
        // Reactive Workflow Resources
        // Migrated from CeapReactiveWorkflow-dev
        // ========================================
        
        /**
         * Deduplication Table - Tracks recent events to prevent duplicate processing.
         * 
         * This table is specific to the Reactive workflow and stays in the same stack.
         * Uses TTL to automatically expire old deduplication records.
         * 
         * Logical ID: DeduplicationTableAD30DFB7 (preserved from original stack)
         * Physical Name: ceap-event-deduplication-{envName}
         * 
         * Validates: Requirement 8.2 (preserve original logical IDs)
         */
        val deduplicationTable = Table.Builder.create(this, "DeduplicationTable")
            .tableName("ceap-event-deduplication-$envName")
            .partitionKey(
                Attribute.builder()
                    .name("deduplicationKey")
                    .type(AttributeType.STRING)
                    .build()
            )
            .billingMode(BillingMode.PAY_PER_REQUEST)
            .timeToLiveAttribute("ttl")
            .build()
        
        // Override logical ID to match original stack (Requirement 8.2)
        val deduplicationTableCfn = deduplicationTable.node.defaultChild as software.amazon.awscdk.services.dynamodb.CfnTable
        deduplicationTableCfn.overrideLogicalId("DeduplicationTableAD30DFB7")
        
        /**
         * Reactive Lambda Function - Processes customer events in real-time.
         * 
         * This Lambda handles incoming customer events from EventBridge and:
         * - Checks for duplicate events using the deduplication table
         * - Retrieves program configuration
         * - Checks score cache for existing scores
         * - Updates candidate records in real-time
         * 
         * Logical ID: ReactiveLambdaFunction89310B25 (preserved from original stack)
         * Handler: com.ceap.workflow.reactive.ReactiveHandler::handleRequest
         * Memory: 1024 MB
         * Timeout: 1 minute
         * 
         * Environment Variables:
         * - ENVIRONMENT: Deployment environment (dev/prod)
         * - CANDIDATES_TABLE: Name of candidates DynamoDB table (imported from DatabaseStack)
         * - PROGRAM_CONFIG_TABLE: Name of program config DynamoDB table (imported from DatabaseStack)
         * - SCORE_CACHE_TABLE: Name of score cache DynamoDB table (imported from DatabaseStack)
         * - DEDUPLICATION_TABLE: Name of deduplication DynamoDB table (same stack)
         * - LOG_LEVEL: Logging level (INFO)
         * 
         * IAM Permissions:
         * - DynamoDB read/write on all four tables
         * - CloudWatch Logs (auto-generated)
         * - EventBridge invocation (auto-granted)
         * 
         * Validates: Requirement 2.1 (preserve Lambda functions)
         * Validates: Requirement 8.2 (preserve original logical IDs)
         * Validates: Requirement 10.2 (use Fn::ImportValue for database tables)
         */
        val reactiveLambda = Function.Builder.create(this, "ReactiveLambda")
            .runtime(Runtime.JAVA_17)
            .handler("com.ceap.workflow.reactive.ReactiveHandler::handleRequest")
            .code(Code.fromAsset("../ceap-workflow-reactive/build/libs/ceap-workflow-reactive-1.0.0-SNAPSHOT.jar"))
            .memorySize(1024)
            .timeout(Duration.minutes(1))
            .environment(mapOf(
                "ENVIRONMENT" to envName,
                "CANDIDATES_TABLE" to candidatesTableName,
                "PROGRAM_CONFIG_TABLE" to programConfigTableName,
                "SCORE_CACHE_TABLE" to scoreCacheTableName,
                "DEDUPLICATION_TABLE" to deduplicationTable.tableName,
                "LOG_LEVEL" to "INFO"
            ))
            .logRetention(RetentionDays.ONE_MONTH)
            .build()
        
        // Override logical ID to match original stack (Requirement 8.2)
        val reactiveLambdaCfn = reactiveLambda.node.defaultChild as software.amazon.awscdk.services.lambda.CfnFunction
        reactiveLambdaCfn.overrideLogicalId("ReactiveLambdaFunction89310B25")
        
        // Grant DynamoDB permissions to Reactive Lambda
        // Uses imported ARNs for cross-stack table references
        reactiveLambda.addToRolePolicy(
            PolicyStatement.Builder.create()
                .actions(listOf(
                    "dynamodb:GetItem",
                    "dynamodb:PutItem",
                    "dynamodb:Query",
                    "dynamodb:UpdateItem"
                ))
                .resources(listOf(
                    candidatesTableArn,
                    programConfigTableArn,
                    scoreCacheTableArn,
                    deduplicationTable.tableArn
                ))
                .build()
        )
        
        /**
         * Customer Event Rule - Routes customer events to Reactive Lambda.
         * 
         * This EventBridge rule listens for customer events from various sources
         * and routes them to the Reactive Lambda for real-time processing.
         * 
         * Logical ID: CustomerEventRuleC528982C (preserved from original stack)
         * Physical Name: ceap-customer-events-{envName}
         * 
         * Event Pattern:
         * - Source: ceap.customer-events
         * - DetailType: OrderDelivered, ProductPurchased, VideoWatched, TrackPlayed, 
         *               ServiceCompleted, EventAttended
         * 
         * Target: ReactiveLambda
         * 
         * Validates: Requirement 2.3 (preserve EventBridge rules)
         * Validates: Requirement 8.2 (preserve original logical IDs)
         */
        val customerEventRule = Rule.Builder.create(this, "CustomerEventRule")
            .ruleName("ceap-customer-events-$envName")
            .description("Routes customer events to reactive ceap workflow")
            .eventPattern(
                EventPattern.builder()
                    .source(listOf("ceap.customer-events"))
                    .detailType(listOf(
                        "OrderDelivered",
                        "ProductPurchased",
                        "VideoWatched",
                        "TrackPlayed",
                        "ServiceCompleted",
                        "EventAttended"
                    ))
                    .build()
            )
            .targets(listOf(LambdaFunction(reactiveLambda)))
            .build()
        
        // Override logical ID to match original stack (Requirement 8.2)
        val customerEventRuleCfn = customerEventRule.node.defaultChild as software.amazon.awscdk.services.events.CfnRule
        customerEventRuleCfn.overrideLogicalId("CustomerEventRuleC528982C")
        
        // ========================================
        // Stack Outputs
        // ========================================
        
        /**
         * Export Reactive Lambda ARN for potential use by other stacks.
         * 
         * This export allows other stacks (e.g., monitoring, observability) to reference
         * the Reactive Lambda function ARN using CloudFormation imports.
         * 
         * Export Name: {StackName}-ReactiveLambdaArn
         * 
         * Validates: Requirement 10.2 (cross-stack reference mechanisms)
         */
        CfnOutput.Builder.create(this, "ReactiveLambdaArnOutput")
            .value(reactiveLambda.functionArn)
            .exportName("$stackName-ReactiveLambdaArn")
            .description("ARN of the Reactive Lambda function for real-time event processing")
            .build()
        
        /**
         * Export Reactive Lambda name for potential use by other stacks.
         * 
         * Export Name: {StackName}-ReactiveLambdaName
         */
        CfnOutput.Builder.create(this, "ReactiveLambdaNameOutput")
            .value(reactiveLambda.functionName)
            .exportName("$stackName-ReactiveLambdaName")
            .description("Name of the Reactive Lambda function")
            .build()
        
        /**
         * Export Customer Event Rule ARN for potential use by other stacks.
         * 
         * Export Name: {StackName}-CustomerEventRuleArn
         */
        CfnOutput.Builder.create(this, "CustomerEventRuleArnOutput")
            .value(customerEventRule.ruleArn)
            .exportName("$stackName-CustomerEventRuleArn")
            .description("ARN of the Customer Event Rule for EventBridge routing")
            .build()
        
        /**
         * Export Deduplication Table name for potential use by other stacks.
         * 
         * Export Name: {StackName}-DeduplicationTableName
         */
        CfnOutput.Builder.create(this, "DeduplicationTableNameOutput")
            .value(deduplicationTable.tableName)
            .exportName("$stackName-DeduplicationTableName")
            .description("Name of the Deduplication DynamoDB table")
            .build()
        
        /**
         * Export Deduplication Table ARN for potential use by other stacks.
         * 
         * Export Name: {StackName}-DeduplicationTableArn
         */
        CfnOutput.Builder.create(this, "DeduplicationTableArnOutput")
            .value(deduplicationTable.tableArn)
            .exportName("$stackName-DeduplicationTableArn")
            .description("ARN of the Deduplication DynamoDB table")
            .build()
    }
}
