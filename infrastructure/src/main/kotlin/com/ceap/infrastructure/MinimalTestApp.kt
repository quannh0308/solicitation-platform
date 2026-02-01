package com.ceap.infrastructure

import software.amazon.awscdk.App
import software.amazon.awscdk.CfnOutput
import software.amazon.awscdk.Duration
import software.amazon.awscdk.Environment
import software.amazon.awscdk.Stack
import software.amazon.awscdk.StackProps
import software.amazon.awscdk.services.s3.BlockPublicAccess
import software.amazon.awscdk.services.s3.Bucket
import software.amazon.awscdk.services.s3.BucketEncryption
import software.amazon.awscdk.services.s3.LifecycleRule
import software.constructs.Construct

class MinimalTestStack(
    scope: Construct,
    id: String,
    props: StackProps,
    private val workflowName: String
) : Stack(scope, id, props) {
    
    init {
        val bucket = Bucket.Builder.create(this, "Bucket")
            .bucketName("ceap-workflow-$workflowName-${this.account}")
            .encryption(BucketEncryption.S3_MANAGED)
            .blockPublicAccess(BlockPublicAccess.BLOCK_ALL)
            .lifecycleRules(listOf(
                LifecycleRule.builder()
                    .prefix("executions/")
                    .expiration(Duration.days(7))
                    .enabled(true)
                    .build()
            ))
            .build()
        
        CfnOutput.Builder.create(this, "BucketName")
            .value(bucket.bucketName)
            .build()
    }
}

fun main() {
    val app = App()
    
    val workflowName = app.node.tryGetContext("workflowName")?.toString() ?: "test"
    
    val account = System.getenv("CDK_DEFAULT_ACCOUNT")
    val region = System.getenv("CDK_DEFAULT_REGION") ?: "us-east-1"
    
    val env = Environment.builder()
        .account(account)
        .region(region)
        .build()
    
    val props = StackProps.builder()
        .env(env)
        .build()
    
    MinimalTestStack(app, "CeapWorkflowTest-$workflowName", props, workflowName)
    
    app.synth()
}
