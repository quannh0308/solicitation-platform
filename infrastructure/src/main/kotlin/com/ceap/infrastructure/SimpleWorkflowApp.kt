package com.ceap.infrastructure

import software.amazon.awscdk.App
import software.amazon.awscdk.CfnOutput
import software.amazon.awscdk.Duration
import software.amazon.awscdk.Environment
import software.amazon.awscdk.Stack
import software.amazon.awscdk.StackProps
import software.amazon.awscdk.services.iam.PolicyStatement
import software.amazon.awscdk.services.iam.Role
import software.amazon.awscdk.services.iam.ServicePrincipal
import software.amazon.awscdk.services.lambda.Code
import software.amazon.awscdk.services.lambda.Function
import software.amazon.awscdk.services.lambda.Runtime
import software.amazon.awscdk.services.logs.LogGroup
import software.amazon.awscdk.services.logs.RetentionDays
import software.amazon.awscdk.services.pipes.CfnPipe
import software.amazon.awscdk.services.s3.BlockPublicAccess
import software.amazon.awscdk.services.s3.Bucket
import software.amazon.awscdk.services.s3.BucketEncryption
import software.amazon.awscdk.services.s3.LifecycleRule
import software.amazon.awscdk.services.sqs.DeadLetterQueue
import software.amazon.awscdk.services.sqs.Queue
import software.amazon.awscdk.services.stepfunctions.*
import software.amazon.awscdk.services.stepfunctions.tasks.LambdaInvoke
import software.constructs.Construct

/**
 * Simple Workflow Stack - Minimal working implementation for deployment.
 */
class SimpleWorkflowStack(
    scope: Construct,
    id: String,
    props: StackProps,
    private val workflowName: String,
    private val workflowType: String
) : Stack(scope, id, props) {
    
    init {
        // S3 Bucket
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
        
        // SQS Queues
        val dlq = Queue.Builder.create(this, "DLQ")
            .queueName("ceap-workflow-$workflowName-dlq")
            .retentionPeriod(Duration.days(14))
            .build()
        
        val queue = Queue.Builder.create(this, "Queue")
            .queueName("ceap-workflow-$workflowName-queue")
            .visibilityTimeout(Duration.minutes(10))
            .deadLetterQueue(DeadLetterQueue.builder()
                .queue(dlq)
                .maxReceiveCount(3)
                .build())
            .build()
        
        // Lambda Functions
        val etlLambda = Function.Builder.create(this, "ETLLambda")
            .functionName("CeapWorkflow-$workflowName-ETLLambda")
            .runtime(Runtime.JAVA_17)
            .handler("com.ceap.workflow.etl.ETLHandler::handleRequest")
            .code(Code.fromAsset("../ceap-workflow-etl/build/libs/ceap-workflow-etl-1.0.0-SNAPSHOT.jar"))
            .memorySize(1024)
            .timeout(Duration.minutes(5))
            .logRetention(RetentionDays.TWO_WEEKS)
            .build()
        
        val filterLambda = Function.Builder.create(this, "FilterLambda")
            .functionName("CeapWorkflow-$workflowName-FilterLambda")
            .runtime(Runtime.JAVA_17)
            .handler("com.ceap.workflow.filter.FilterHandler::handleRequest")
            .code(Code.fromAsset("../ceap-workflow-filter/build/libs/ceap-workflow-filter-1.0.0-SNAPSHOT.jar"))
            .memorySize(512)
            .timeout(Duration.seconds(60))
            .logRetention(RetentionDays.TWO_WEEKS)
            .build()
        
        val scoreLambda = Function.Builder.create(this, "ScoreLambda")
            .functionName("CeapWorkflow-$workflowName-ScoreLambda")
            .runtime(Runtime.JAVA_17)
            .handler("com.ceap.workflow.score.ScoreHandler::handleRequest")
            .code(Code.fromAsset("../ceap-workflow-score/build/libs/ceap-workflow-score-1.0.0-SNAPSHOT.jar"))
            .memorySize(1024)
            .timeout(Duration.minutes(2))
            .logRetention(RetentionDays.TWO_WEEKS)
            .build()
        
        val storeLambda = Function.Builder.create(this, "StoreLambda")
            .functionName("CeapWorkflow-$workflowName-StoreLambda")
            .runtime(Runtime.JAVA_17)
            .handler("com.ceap.workflow.store.StoreHandler::handleRequest")
            .code(Code.fromAsset("../ceap-workflow-store/build/libs/ceap-workflow-store-1.0.0-SNAPSHOT.jar"))
            .memorySize(512)
            .timeout(Duration.seconds(60))
            .logRetention(RetentionDays.TWO_WEEKS)
            .build()
        
        val reactiveLambda = Function.Builder.create(this, "ReactiveLambda")
            .functionName("CeapWorkflow-$workflowName-ReactiveLambda")
            .runtime(Runtime.JAVA_17)
            .handler("com.ceap.workflow.reactive.ReactiveHandler::handleRequest")
            .code(Code.fromAsset("../ceap-workflow-reactive/build/libs/ceap-workflow-reactive-1.0.0-SNAPSHOT.jar"))
            .memorySize(1024)
            .timeout(Duration.minutes(1))
            .logRetention(RetentionDays.TWO_WEEKS)
            .build()
        
        // Grant S3 permissions
        bucket.grantReadWrite(etlLambda)
        bucket.grantReadWrite(filterLambda)
        bucket.grantReadWrite(scoreLambda)
        bucket.grantReadWrite(storeLambda)
        bucket.grantReadWrite(reactiveLambda)
        
        // Create Step Functions tasks
        val etlTask = LambdaInvoke.Builder.create(this, "ETLTask")
            .lambdaFunction(etlLambda)
            .resultPath(JsonPath.DISCARD)
            .build()
            .addRetry(RetryProps.builder()
                .errors(listOf(Errors.ALL))
                .interval(Duration.seconds(20))
                .maxAttempts(2)
                .backoffRate(2.0)
                .build())
        
        val filterTask = LambdaInvoke.Builder.create(this, "FilterTask")
            .lambdaFunction(filterLambda)
            .resultPath(JsonPath.DISCARD)
            .build()
            .addRetry(RetryProps.builder()
                .errors(listOf(Errors.ALL))
                .interval(Duration.seconds(20))
                .maxAttempts(2)
                .backoffRate(2.0)
                .build())
        
        val scoreTask = LambdaInvoke.Builder.create(this, "ScoreTask")
            .lambdaFunction(scoreLambda)
            .resultPath(JsonPath.DISCARD)
            .build()
            .addRetry(RetryProps.builder()
                .errors(listOf(Errors.ALL))
                .interval(Duration.seconds(20))
                .maxAttempts(2)
                .backoffRate(2.0)
                .build())
        
        val storeTask = LambdaInvoke.Builder.create(this, "StoreTask")
            .lambdaFunction(storeLambda)
            .resultPath(JsonPath.DISCARD)
            .build()
            .addRetry(RetryProps.builder()
                .errors(listOf(Errors.ALL))
                .interval(Duration.seconds(20))
                .maxAttempts(2)
                .backoffRate(2.0)
                .build())
        
        val reactiveTask = LambdaInvoke.Builder.create(this, "ReactiveTask")
            .lambdaFunction(reactiveLambda)
            .resultPath(JsonPath.DISCARD)
            .build()
            .addRetry(RetryProps.builder()
                .errors(listOf(Errors.ALL))
                .interval(Duration.seconds(20))
                .maxAttempts(2)
                .backoffRate(2.0)
                .build())
        
        // Chain tasks
        val definition = etlTask
            .next(filterTask)
            .next(scoreTask)
            .next(storeTask)
            .next(reactiveTask)
        
        // CloudWatch Logs
        val logGroup = LogGroup.Builder.create(this, "Logs")
            .logGroupName("/aws/stepfunctions/Ceap-$workflowName-Workflow")
            .retention(RetentionDays.TWO_WEEKS)
            .build()
        
        // Step Functions State Machine
        val stateMachineType = if (workflowType == "express") StateMachineType.EXPRESS else StateMachineType.STANDARD
        val stateMachine = StateMachine.Builder.create(this, "StateMachine")
            .stateMachineName("Ceap-$workflowName-Workflow")
            .definitionBody(DefinitionBody.fromChainable(definition))
            .stateMachineType(stateMachineType)
            .tracingEnabled(true)
            .logs(LogOptions.builder()
                .destination(logGroup)
                .includeExecutionData(true)
                .level(LogLevel.ALL)
                .build())
            .build()
        
        bucket.grantReadWrite(stateMachine)
        
        // EventBridge Pipe
        val pipeRole = Role.Builder.create(this, "PipeRole")
            .assumedBy(ServicePrincipal("pipes.amazonaws.com"))
            .build()
        
        queue.grantConsumeMessages(pipeRole)
        stateMachine.grantStartExecution(pipeRole)
        
        val invocationType = if (workflowType == "express") "REQUEST_RESPONSE" else "FIRE_AND_FORGET"
        
        CfnPipe.Builder.create(this, "Pipe")
            .name("ceap-workflow-$workflowName-pipe")
            .roleArn(pipeRole.roleArn)
            .source(queue.queueArn)
            .sourceParameters(CfnPipe.PipeSourceParametersProperty.builder()
                .sqsQueueParameters(CfnPipe.PipeSourceSqsQueueParametersProperty.builder()
                    .batchSize(1)
                    .build())
                .build())
            .target(stateMachine.stateMachineArn)
            .targetParameters(CfnPipe.PipeTargetParametersProperty.builder()
                .stepFunctionStateMachineParameters(
                    CfnPipe.PipeTargetStateMachineParametersProperty.builder()
                        .invocationType(invocationType)
                        .build()
                )
                .build())
            .desiredState("RUNNING")
            .build()
        
        // Outputs
        CfnOutput.Builder.create(this, "WorkflowBucketNameOutput")
            .value(bucket.bucketName)
            .exportName("$stackName-WorkflowBucketName")
            .build()
        
        CfnOutput.Builder.create(this, "StateMachineArnOutput")
            .value(stateMachine.stateMachineArn)
            .exportName("$stackName-StateMachineArn")
            .build()
        
        CfnOutput.Builder.create(this, "QueueUrlOutput")
            .value(queue.queueUrl)
            .exportName("$stackName-QueueUrl")
            .build()
        
        CfnOutput.Builder.create(this, "DLQUrlOutput")
            .value(dlq.queueUrl)
            .exportName("$stackName-DLQUrl")
            .build()
    }
}

fun main() {
    val app = App()
    
    val workflowName = app.node.tryGetContext("workflowName")?.toString() ?: "realtime"
    val workflowType = app.node.tryGetContext("workflowType")?.toString() ?: "express"
    val environment = app.node.tryGetContext("environment")?.toString() ?: "dev"
    
    val account = System.getenv("CDK_DEFAULT_ACCOUNT")
    val region = System.getenv("CDK_DEFAULT_REGION") ?: "us-east-1"
    
    val env = Environment.builder()
        .account(account)
        .region(region)
        .build()
    
    val props = StackProps.builder()
        .env(env)
        .build()
    
    SimpleWorkflowStack(app, "CeapWorkflow-$workflowName", props, workflowName, workflowType)
    
    app.synth()
}
