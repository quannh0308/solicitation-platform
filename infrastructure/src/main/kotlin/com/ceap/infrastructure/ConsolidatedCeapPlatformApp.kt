package com.ceap.infrastructure

import software.amazon.awscdk.App
import software.amazon.awscdk.Environment
import software.amazon.awscdk.StackProps
import com.ceap.infrastructure.stacks.*

/**
 * Consolidated CDK application for the Customer Engagement & Action Platform (CEAP).
 * 
 * This app creates the consolidated 3-stack architecture:
 * - CeapDatabase-dev (Storage Layer) - unchanged from original
 * - CeapDataPlatform-dev (Write Path) - consolidates ETL, Filter, Score, Store, and Orchestration
 * - CeapServingAPI-dev (Read Path) - consolidates Reactive workflow
 * 
 * This replaces the original 7-stack architecture with a cleaner, business-aligned structure.
 */
fun main() {
    val app = App()
    
    // Get environment from context or use defaults
    val envName = app.node.tryGetContext("environment")?.toString() ?: "dev"
    val account = System.getenv("CDK_DEFAULT_ACCOUNT") ?: System.getenv("AWS_ACCOUNT_ID")
    val region = System.getenv("CDK_DEFAULT_REGION") ?: "us-east-1"
    
    val env = Environment.builder()
        .account(account)
        .region(region)
        .build()
    
    val stackProps = StackProps.builder()
        .env(env)
        .build()
    
    // Create database stack (shared across all workflows)
    // This stack remains unchanged from the original architecture
    val databaseStack = DatabaseStack(app, "CeapDatabase-$envName", stackProps, envName)
    
    // Create consolidated Data Platform stack (Write Path)
    // Consolidates: ETL, Filter, Score, Store, and Orchestration workflows
    val dataPlatformStack = DataPlatformStack(app, "CeapDataPlatform-$envName", stackProps, envName)
    dataPlatformStack.addDependency(databaseStack)
    
    // Create consolidated Serving API stack (Read Path)
    // Consolidates: Reactive workflow and future serving API resources
    val servingAPIStack = ServingAPIStack(app, "CeapServingAPI-$envName", stackProps, envName)
    servingAPIStack.addDependency(databaseStack)
    
    app.synth()
}
