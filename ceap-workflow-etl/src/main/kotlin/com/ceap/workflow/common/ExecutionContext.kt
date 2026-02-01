package com.ceap.workflow.common

/**
 * Execution context passed from Step Functions to each Lambda stage.
 * 
 * This data class encapsulates the workflow execution metadata needed for
 * convention-based S3 path resolution and stage coordination.
 * 
 * @property executionId Step Functions execution name (unique per workflow run)
 * @property currentStage Name of the current Lambda stage (used for output S3 path)
 * @property previousStage Name of the previous stage (used for input S3 path), null for first stage
 * @property workflowBucket S3 bucket name for intermediate storage
 * @property initialData Original SQS message body (used by first stage only), null for non-first stages
 * 
 * Validates: Requirements 2.1
 */
data class ExecutionContext(
    var executionId: String = "",
    var currentStage: String = "",
    var previousStage: String? = null,
    var workflowBucket: String = "",
    var initialData: Map<String, Any>? = null
) {
    // No-arg constructor for Jackson
    constructor() : this("", "", null, "", null)
}
