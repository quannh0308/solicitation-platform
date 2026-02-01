package com.ceap.infrastructure

import software.amazon.awscdk.App
import software.amazon.awscdk.CfnOutput
import software.amazon.awscdk.Duration
import software.amazon.awscdk.Environment
import software.amazon.awscdk.Stack
import software.amazon.awscdk.StackProps
import software.amazon.awscdk.services.glue.CfnJob
import software.amazon.awscdk.services.iam.Effect
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
import software.amazon.awscdk.services.stepfunctions.tasks.GlueStartJobRun
import software.amazon.awscdk.services.stepfunctions.tasks.LambdaInvoke
import software.constructs.Construct

/**
 * Simple Workflow Stack - Minimal working implementation with Glue support.
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
            .build()
        
        val filterLambda = Function.Builder.create(this, "FilterLambda")
            .functionName("CeapWorkflow-$workflowName-FilterLambda")
            .runtime(Runtime.JAVA_17)
            .handler("com.ceap.workflow.filter.FilterHandler::handleRequest")
            .code(Code.fromAsset("../ceap-workflow-filter/build/libs/ceap-workflow-filter-1.0.0-SNAPSHOT.jar"))
            .memorySize(512)
            .timeout(Duration.seconds(60))
            .build()
        
        val scoreLambda = Function.Builder.create(this, "ScoreLambda")
            .functionName("CeapWorkflow-$workflowName-ScoreLambda")
            .runtime(Runtime.JAVA_17)
            .handler("com.ceap.workflow.score.ScoreHandler::handleRequest")
            .code(Code.fromAsset("../ceap-workflow-score/build/libs/ceap-workflow-score-1.0.0-SNAPSHOT.jar"))
            .memorySize(1024)
            .timeout(Duration.minutes(2))
            .build()
        
        val storeLambda = Function.Builder.create(this, "StoreLambda")
            .functionName("CeapWorkflow-$workflowName-StoreLambda")
            .runtime(Runtime.JAVA_17)
            .handler("com.ceap.workflow.store.StoreHandler::handleRequest")
            .code(Code.fromAsset("../ceap-workflow-store/build/libs/ceap-workflow-store-1.0.0-SNAPSHOT.jar"))
            .memorySize(512)
            .timeout(Duration.seconds(60))
            .build()
        
        val reactiveLambda = Function.Builder.create(this, "ReactiveLambda")
            .functionName("CeapWorkflow-$workflowName-ReactiveLambda")
            .runtime(Runtime.JAVA_17)
            .handler("com.ceap.workflow.reactive.ReactiveHandler::handleRequest")
            .code(Code.fromAsset("../ceap-workflow-reactive/build/libs/ceap-workflow-reactive-1.0.0-SNAPSHOT.jar"))
            .memorySize(1024)
            .timeout(Duration.minutes(1))
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
            .payload(TaskInput.fromObject(mapOf(
                "executionId.$" to "$$.Execution.Name",
                "currentStage" to "ETLTask",
                "previousStage" to null,
                "workflowBucket" to bucket.bucketName,
                "initialData.$" to "$"
            )))
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
            .payload(TaskInput.fromObject(mapOf(
                "executionId.$" to "$$.Execution.Name",
                "currentStage" to "FilterTask",
                "previousStage" to "ETLTask",
                "workflowBucket" to bucket.bucketName,
                "initialData" to null
            )))
            .resultPath(JsonPath.DISCARD)
            .build()
            .addRetry(RetryProps.builder()
                .errors(listOf(Errors.ALL))
                .interval(Duration.seconds(20))
                .maxAttempts(2)
                .backoffRate(2.0)
                .build())
        
        // Update previousStage for tasks after Glue (if Glue is present)
        val scoreTaskPreviousStage = if (workflowType == "standard") "HeavyETLTask" else "FilterTask"
        
        val scoreTask = LambdaInvoke.Builder.create(this, "ScoreTask")
            .lambdaFunction(scoreLambda)
            .payload(TaskInput.fromObject(mapOf(
                "executionId.$" to "$$.Execution.Name",
                "currentStage" to "ScoreTask",
                "previousStage" to scoreTaskPreviousStage,
                "workflowBucket" to bucket.bucketName,
                "initialData" to null
            )))
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
            .payload(TaskInput.fromObject(mapOf(
                "executionId.$" to "$$.Execution.Name",
                "currentStage" to "StoreTask",
                "previousStage" to "ScoreTask",
                "workflowBucket" to bucket.bucketName,
                "initialData" to null
            )))
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
            .payload(TaskInput.fromObject(mapOf(
                "executionId.$" to "$$.Execution.Name",
                "currentStage" to "ReactiveTask",
                "previousStage" to "StoreTask",
                "workflowBucket" to bucket.bucketName,
                "initialData" to null
            )))
            .resultPath(JsonPath.DISCARD)
            .build()
            .addRetry(RetryProps.builder()
                .errors(listOf(Errors.ALL))
                .interval(Duration.seconds(20))
                .maxAttempts(2)
                .backoffRate(2.0)
                .build())
        
        // Glue Job (Standard workflow only)
        val glueTask = if (workflowType == "standard") {
            // Create Glue job role
            val glueRole = Role.Builder.create(this, "GlueRole")
                .assumedBy(ServicePrincipal("glue.amazonaws.com"))
                .build()
            
            // Grant S3 permissions
            bucket.grantReadWrite(glueRole)
            
            // Grant CloudWatch Logs permissions
            glueRole.addToPolicy(
                PolicyStatement.Builder.create()
                    .effect(Effect.ALLOW)
                    .actions(listOf(
                        "logs:CreateLogGroup",
                        "logs:CreateLogStream",
                        "logs:PutLogEvents"
                    ))
                    .resources(listOf("arn:aws:logs:*:*:log-group:/aws-glue/jobs/*"))
                    .build()
            )
            
            // Create Glue job
            val glueJob = CfnJob.Builder.create(this, "HeavyETLJob")
                .name("ceap-workflow-$workflowName-heavy-etl")
                .role(glueRole.roleArn)
                .command(CfnJob.JobCommandProperty.builder()
                    .name("glueetl")
                    .scriptLocation("s3://ceap-glue-scripts-${this.account}/scripts/heavy-etl.py")
                    .pythonVersion("3")
                    .build())
                .glueVersion("4.0")
                .workerType("G.1X")
                .numberOfWorkers(2)  // Minimum 2 workers for G.1X
                .maxRetries(0)  // Retries handled by Step Functions
                .timeout(120)  // 2 hours
                .defaultArguments(mapOf(
                    "--enable-metrics" to "true",
                    "--enable-spark-ui" to "true",
                    "--enable-job-insights" to "true",
                    "--TempDir" to "s3://${bucket.bucketName}/glue-temp/"
                ))
                .build()
            
            // Create Glue job task
            GlueStartJobRun.Builder.create(this, "HeavyETLTask")
                .glueJobName(glueJob.name)
                .integrationPattern(IntegrationPattern.RUN_JOB)  // Wait for completion
                .arguments(TaskInput.fromObject(mapOf(
                    "--execution-id.$" to "$$.Execution.Name",
                    "--input-bucket" to bucket.bucketName,
                    "--input-key.$" to "States.Format('executions/{}/FilterTask/output.json', $$.Execution.Name)",
                    "--output-bucket" to bucket.bucketName,
                    "--output-key.$" to "States.Format('executions/{}/HeavyETLTask/output.json', $$.Execution.Name)",
                    "--current-stage" to "HeavyETLTask",
                    "--previous-stage" to "FilterTask"
                )))
                .resultPath(JsonPath.DISCARD)
                .build()
                .addRetry(RetryProps.builder()
                    .errors(listOf("States.ALL"))
                    .interval(Duration.minutes(5))
                    .maxAttempts(2)
                    .backoffRate(2.0)
                    .build())
        } else {
            null
        }
        
        // Chain tasks
        val definition = if (workflowType == "standard" && glueTask != null) {
            // Standard workflow with Glue: ETL → Filter → Glue → Score → Store → Reactive
            etlTask
                .next(filterTask)
                .next(glueTask)
                .next(scoreTask)
                .next(storeTask)
                .next(reactiveTask)
        } else {
            // Express workflow (no Glue): ETL → Filter → Score → Store → Reactive
            etlTask
                .next(filterTask)
                .next(scoreTask)
                .next(storeTask)
                .next(reactiveTask)
        }
        
        // CloudWatch Logs
        val logGroup = LogGroup.Builder.create(this, "Logs")
            .logGroupName("/aws/stepfunctions/Ceap-$workflowName-Workflow")
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
        
        // Note: EventBridge Pipe commented out due to deployment issues
        // Can be added later or triggered manually via AWS CLI
        /*
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
        */
        
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
