package com.solicitation.infrastructure.stacks

import software.amazon.awscdk.Stack
import software.amazon.awscdk.StackProps
import software.amazon.awscdk.services.events.Rule
import software.amazon.awscdk.services.events.Schedule
import software.amazon.awscdk.services.events.targets.SfnStateMachine
import software.amazon.awscdk.services.stepfunctions.*
import software.amazon.awscdk.services.stepfunctions.tasks.LambdaInvoke
import software.constructs.Construct

/**
 * Orchestration stack containing Step Functions workflows and EventBridge rules.
 * 
 * Workflows:
 * - Batch Ingestion: ETL → Filter → Score → Store
 * - Reactive Solicitation: Real-time event processing
 */
class OrchestrationStack(
    scope: Construct,
    id: String,
    props: StackProps,
    val envName: String,
    val etlStack: EtlWorkflowStack,
    val filterStack: FilterWorkflowStack,
    val scoreStack: ScoreWorkflowStack,
    val storeStack: StoreWorkflowStack,
    val reactiveStack: ReactiveWorkflowStack
) : Stack(scope, id, props) {
    
    /**
     * Batch Ingestion Workflow: ETL → Filter → Score → Store
     */
    val batchIngestionWorkflow: StateMachine
    
    init {
        // Create Step Functions tasks
        val etlTask = LambdaInvoke.Builder.create(this, "ETLTask")
            .lambdaFunction(etlStack.etlLambda.function)
            .outputPath("$.Payload")
            .build()
        
        val filterTask = LambdaInvoke.Builder.create(this, "FilterTask")
            .lambdaFunction(filterStack.filterLambda.function)
            .outputPath("$.Payload")
            .build()
        
        val scoreTask = LambdaInvoke.Builder.create(this, "ScoreTask")
            .lambdaFunction(scoreStack.scoreLambda.function)
            .outputPath("$.Payload")
            .build()
        
        val storeTask = LambdaInvoke.Builder.create(this, "StoreTask")
            .lambdaFunction(storeStack.storeLambda.function)
            .outputPath("$.Payload")
            .build()
        
        // Chain tasks: ETL → Filter → Score → Store
        val definition = etlTask
            .next(filterTask)
            .next(scoreTask)
            .next(storeTask)
        
        // Create State Machine
        batchIngestionWorkflow = StateMachine.Builder.create(this, "BatchIngestionWorkflow")
            .stateMachineName("SolicitationBatchIngestion-$envName")
            .definitionBody(DefinitionBody.fromChainable(definition))
            .timeout(software.amazon.awscdk.Duration.minutes(15))
            .build()
        
        // Schedule batch ingestion (daily at 2 AM UTC)
        Rule.Builder.create(this, "BatchIngestionSchedule")
            .ruleName("SolicitationBatchIngestion-$envName")
            .schedule(Schedule.cron(
                software.amazon.awscdk.services.events.CronOptions.builder()
                    .minute("0")
                    .hour("2")
                    .build()
            ))
            .targets(listOf(SfnStateMachine(batchIngestionWorkflow)))
            .build()
    }
}
