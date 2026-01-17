package com.solicitation.infrastructure.stacks

import software.amazon.awscdk.Duration
import software.amazon.awscdk.Stack
import software.amazon.awscdk.StackProps
import software.constructs.Construct
import com.solicitation.infrastructure.constructs.SolicitationLambda

/**
 * Filter Workflow Stack - Applies filtering rules to candidates.
 * 
 * This is a plug-and-play workflow that can be deployed independently.
 */
class FilterWorkflowStack(
    scope: Construct,
    id: String,
    props: StackProps,
    val envName: String,
    val databaseStack: DatabaseStack
) : Stack(scope, id, props) {
    
    val filterLambda = SolicitationLambda(
        this,
        "FilterLambda",
        handler = "com.solicitation.workflow.FilterHandler::handleRequest",
        jarPath = "../solicitation-workflow-filter/build/libs/solicitation-workflow-filter-1.0.0-SNAPSHOT.jar",
        environment = mapOf(
            "ENVIRONMENT" to envName,
            "PROGRAM_CONFIG_TABLE" to databaseStack.programConfigTable.tableName,
            "LOG_LEVEL" to "INFO"
        ),
        tables = listOf(databaseStack.programConfigTable),
        memorySize = 512,
        timeout = Duration.seconds(60)
    )
}
