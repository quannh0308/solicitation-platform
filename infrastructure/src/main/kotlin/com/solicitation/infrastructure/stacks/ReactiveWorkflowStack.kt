package com.solicitation.infrastructure.stacks

import software.amazon.awscdk.Duration
import software.amazon.awscdk.Stack
import software.amazon.awscdk.StackProps
import software.constructs.Construct
import com.solicitation.infrastructure.constructs.SolicitationLambda

/**
 * Reactive Workflow Stack - Handles real-time events.
 * 
 * This is a plug-and-play workflow that can be deployed independently.
 */
class ReactiveWorkflowStack(
    scope: Construct,
    id: String,
    props: StackProps,
    val envName: String,
    val databaseStack: DatabaseStack
) : Stack(scope, id, props) {
    
    val reactiveLambda = SolicitationLambda(
        this,
        "ReactiveLambda",
        handler = "com.solicitation.workflow.ReactiveHandler::handleRequest",
        jarPath = "../solicitation-workflow-reactive/build/libs/solicitation-workflow-reactive-1.0.0-SNAPSHOT.jar",
        environment = mapOf(
            "ENVIRONMENT" to envName,
            "CANDIDATES_TABLE" to databaseStack.candidatesTable.tableName,
            "PROGRAM_CONFIG_TABLE" to databaseStack.programConfigTable.tableName,
            "SCORE_CACHE_TABLE" to databaseStack.scoreCacheTable.tableName,
            "LOG_LEVEL" to "INFO"
        ),
        tables = listOf(
            databaseStack.candidatesTable,
            databaseStack.programConfigTable,
            databaseStack.scoreCacheTable
        ),
        memorySize = 1024,
        timeout = Duration.minutes(1)
    )
}
