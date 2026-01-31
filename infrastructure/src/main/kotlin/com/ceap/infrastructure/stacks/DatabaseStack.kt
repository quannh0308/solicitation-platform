package com.ceap.infrastructure.stacks

import software.amazon.awscdk.CfnOutput
import software.amazon.awscdk.RemovalPolicy
import software.amazon.awscdk.Stack
import software.amazon.awscdk.StackProps
import software.amazon.awscdk.services.dynamodb.*
import software.constructs.Construct

/**
 * Database stack containing all DynamoDB tables for the Customer Engagement & Action Platform (CEAP).
 * 
 * Tables:
 * - Candidates: Stores customer engagement candidates with GSIs for querying
 * - ProgramConfig: Stores program configurations
 * - ScoreCache: Caches scoring results with TTL
 */
class DatabaseStack(
    scope: Construct,
    id: String,
    props: StackProps,
    val envName: String
) : Stack(scope, id, props) {
    
    /**
     * Candidates table with GSIs for program+channel and program+date queries
     */
    val candidatesTable: Table = Table.Builder.create(this, "CandidatesTable")
        .tableName("Candidates-$envName")
        .partitionKey(Attribute.builder()
            .name("PK")
            .type(AttributeType.STRING)
            .build())
        .sortKey(Attribute.builder()
            .name("SK")
            .type(AttributeType.STRING)
            .build())
        .billingMode(BillingMode.PAY_PER_REQUEST)
        .timeToLiveAttribute("ttl")
        .pointInTimeRecovery(true)
        .encryption(TableEncryption.AWS_MANAGED)
        .removalPolicy(if (envName == "prod") RemovalPolicy.RETAIN else RemovalPolicy.DESTROY)
        .build()
    
    init {
        // GSI-1: Query by program and channel, sorted by score
        candidatesTable.addGlobalSecondaryIndex(GlobalSecondaryIndexProps.builder()
            .indexName("ProgramChannelIndex")
            .partitionKey(Attribute.builder()
                .name("GSI1PK")
                .type(AttributeType.STRING)
                .build())
            .sortKey(Attribute.builder()
                .name("GSI1SK")
                .type(AttributeType.STRING)
                .build())
            .projectionType(ProjectionType.ALL)
            .build())
        
        // GSI-2: Query by program and date, sorted by creation time
        candidatesTable.addGlobalSecondaryIndex(GlobalSecondaryIndexProps.builder()
            .indexName("ProgramDateIndex")
            .partitionKey(Attribute.builder()
                .name("GSI2PK")
                .type(AttributeType.STRING)
                .build())
            .sortKey(Attribute.builder()
                .name("GSI2SK")
                .type(AttributeType.STRING)
                .build())
            .projectionType(ProjectionType.ALL)
            .build())
    }
    
    /**
     * Program configuration table
     */
    val programConfigTable: Table = Table.Builder.create(this, "ProgramConfigTable")
        .tableName("ProgramConfig-$envName")
        .partitionKey(Attribute.builder()
            .name("PK")
            .type(AttributeType.STRING)
            .build())
        .sortKey(Attribute.builder()
            .name("SK")
            .type(AttributeType.STRING)
            .build())
        .billingMode(BillingMode.PAY_PER_REQUEST)
        .pointInTimeRecovery(true)
        .encryption(TableEncryption.AWS_MANAGED)
        .removalPolicy(if (envName == "prod") RemovalPolicy.RETAIN else RemovalPolicy.DESTROY)
        .build()
    
    /**
     * Score cache table with TTL
     */
    val scoreCacheTable: Table = Table.Builder.create(this, "ScoreCacheTable")
        .tableName("ScoreCache-$envName")
        .partitionKey(Attribute.builder()
            .name("PK")
            .type(AttributeType.STRING)
            .build())
        .sortKey(Attribute.builder()
            .name("SK")
            .type(AttributeType.STRING)
            .build())
        .billingMode(BillingMode.PAY_PER_REQUEST)
        .timeToLiveAttribute("ttl")
        .pointInTimeRecovery(true)
        .encryption(TableEncryption.AWS_MANAGED)
        .removalPolicy(RemovalPolicy.DESTROY)  // Cache can be destroyed
        .build()
    
    init {
        // Export table names and ARNs for cross-stack references
        CfnOutput.Builder.create(this, "CandidatesTableNameOutput")
            .value(candidatesTable.tableName)
            .exportName("$id-CandidatesTableName")
            .description("Candidates table name for cross-stack references")
            .build()
        
        CfnOutput.Builder.create(this, "CandidatesTableArnOutput")
            .value(candidatesTable.tableArn)
            .exportName("$id-CandidatesTableArn")
            .description("Candidates table ARN for cross-stack references")
            .build()
        
        CfnOutput.Builder.create(this, "ProgramConfigTableNameOutput")
            .value(programConfigTable.tableName)
            .exportName("$id-ProgramConfigTableName")
            .description("Program config table name for cross-stack references")
            .build()
        
        CfnOutput.Builder.create(this, "ProgramConfigTableArnOutput")
            .value(programConfigTable.tableArn)
            .exportName("$id-ProgramConfigTableArn")
            .description("Program config table ARN for cross-stack references")
            .build()
        
        CfnOutput.Builder.create(this, "ScoreCacheTableNameOutput")
            .value(scoreCacheTable.tableName)
            .exportName("$id-ScoreCacheTableName")
            .description("Score cache table name for cross-stack references")
            .build()
        
        CfnOutput.Builder.create(this, "ScoreCacheTableArnOutput")
            .value(scoreCacheTable.tableArn)
            .exportName("$id-ScoreCacheTableArn")
            .description("Score cache table ARN for cross-stack references")
            .build()
    }
}
