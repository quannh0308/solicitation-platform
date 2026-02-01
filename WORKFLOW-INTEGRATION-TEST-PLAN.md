# Workflow Integration Test Plan

## Overview

This document describes the integration tests for both Express and Standard workflows, including handling of empty input (streaming mechanism).

## Test Scenarios

### Test 1: Express Workflow - Normal Input ✅

**Purpose**: Verify Express workflow processes normal data correctly

**Input**:
```json
{
  "test": "normal-data",
  "timestamp": "2026-02-01T15:00:00Z",
  "candidates": [
    {"id": 1, "score": 85},
    {"id": 2, "score": 92}
  ]
}
```

**Expected Behavior**:
1. Message sent to SQS queue
2. Step Functions execution starts immediately (synchronous)
3. All 5 Lambda stages execute in sequence
4. Each stage writes output to S3
5. Execution completes in ~15 seconds
6. Status: SUCCEEDED

**Verification**:
```bash
# Send message
aws sqs send-message \
  --queue-url https://sqs.us-east-1.amazonaws.com/728093470684/ceap-workflow-realtime-queue \
  --message-body '{"test": "normal-data", "candidates": [{"id": 1, "score": 85}]}'

# Check execution
aws stepfunctions list-executions \
  --state-machine-arn arn:aws:states:us-east-1:728093470684:stateMachine:Ceap-realtime-Workflow \
  --max-results 1

# Verify S3 outputs
aws s3 ls s3://ceap-workflow-realtime-728093470684/executions/ --recursive
```

### Test 2: Express Workflow - Empty Input (Streaming) ✅

**Purpose**: Verify Express workflow handles empty input gracefully (streaming mechanism)

**Input**:
```json
{}
```

**Expected Behavior**:
1. Message sent to SQS queue
2. Step Functions execution starts
3. Lambda stages process empty input without errors
4. Each stage handles null/empty data gracefully
5. Execution completes successfully
6. Status: SUCCEEDED

**Rationale**: In streaming systems, empty messages are normal and should be processed without failure. This tests the robustness of the workflow.

**Verification**:
```bash
# Send empty message
aws sqs send-message \
  --queue-url https://sqs.us-east-1.amazonaws.com/728093470684/ceap-workflow-realtime-queue \
  --message-body '{}'

# Check execution status
aws stepfunctions describe-execution \
  --execution-arn <execution-arn> \
  --query 'status'
```

**Success Criteria**:
- ✅ Execution status: SUCCEEDED
- ✅ No Lambda errors
- ✅ S3 outputs created (even if empty)
- ✅ No messages in DLQ

### Test 3: Standard Workflow - Normal Input ✅

**Purpose**: Verify Standard workflow with Glue job processes data correctly

**Input**:
```json
{
  "test": "batch-data",
  "timestamp": "2026-02-01T15:00:00Z",
  "candidates": [
    {"id": 1, "score": 85},
    {"id": 2, "score": 92},
    {"id": 3, "score": 78}
  ]
}
```

**Expected Behavior**:
1. Message sent to SQS queue
2. Step Functions execution starts (asynchronous)
3. ETL Lambda executes
4. Filter Lambda executes
5. **Glue job starts** (2 DPUs, distributed processing)
6. Step Functions waits for Glue job completion (~2 hours)
7. Score Lambda executes
8. Store Lambda executes
9. Reactive Lambda executes
10. Execution completes
11. Status: SUCCEEDED

**Verification**:
```bash
# Send message
aws sqs send-message \
  --queue-url https://sqs.us-east-1.amazonaws.com/728093470684/ceap-workflow-batch-queue \
  --message-body '{"test": "batch-data", "candidates": [{"id": 1, "score": 85}]}'

# Check execution
aws stepfunctions list-executions \
  --state-machine-arn arn:aws:states:us-east-1:728093470684:stateMachine:Ceap-batch-Workflow \
  --max-results 1

# Check Glue job run
aws glue get-job-runs --job-name ceap-workflow-batch-heavy-etl --max-results 1

# Monitor Glue logs
aws logs tail /aws-glue/jobs/output --follow
```

**Success Criteria**:
- ✅ Execution status: SUCCEEDED (after ~2 hours)
- ✅ Glue job status: SUCCEEDED
- ✅ All S3 outputs created (including HeavyETLTask)
- ✅ No messages in DLQ

### Test 4: Standard Workflow - Empty Input (Streaming) ✅

**Purpose**: Verify Standard workflow with Glue handles empty input gracefully

**Input**:
```json
{}
```

**Expected Behavior**:
1. Message sent to SQS queue
2. Step Functions execution starts
3. Lambda stages process empty input
4. **Glue job processes empty input** (creates empty DataFrame)
5. Glue job completes successfully
6. Remaining Lambda stages execute
7. Execution completes
8. Status: SUCCEEDED

**Verification**:
```bash
# Send empty message
aws sqs send-message \
  --queue-url https://sqs.us-east-1.amazonaws.com/728093470684/ceap-workflow-batch-queue \
  --message-body '{}'

# Monitor execution
aws stepfunctions describe-execution --execution-arn <execution-arn>

# Check Glue job handled empty input
aws glue get-job-run --job-name ceap-workflow-batch-heavy-etl --run-id <run-id>
```

**Success Criteria**:
- ✅ Execution status: SUCCEEDED
- ✅ Glue job status: SUCCEEDED (even with empty input)
- ✅ All stages complete without errors
- ✅ No messages in DLQ

## Lambda Handler Requirements for Empty Input

To ensure empty input is handled correctly, Lambda handlers should:

1. **Check for null/empty input**:
```kotlin
override fun handleRequest(input: Map<String, Any>?, context: Context): Response {
    // Handle empty input gracefully
    if (input == null || input.isEmpty()) {
        logger.info("Empty input received - processing as streaming no-op")
        return Response(status = "SUCCESS", recordsProcessed = 0)
    }
    // ... normal processing
}
```

2. **Return success for empty input**:
- Don't throw exceptions
- Return valid response structure
- Log the empty input case
- Write empty output to S3 (for next stage)

3. **Propagate empty state**:
- Next stage should also handle empty input
- Maintain workflow continuity
- Don't break the chain

## Glue Script Requirements for Empty Input

The Glue script should handle empty DataFrames:

```python
# Read input (may be empty)
input_df = spark.read.json(input_path)

# Check if empty
input_record_count = input_df.count()
if input_record_count == 0:
    print("Empty input received - creating empty output")
    # Create empty DataFrame with expected schema
    output_df = spark.createDataFrame([], input_df.schema)
else:
    # Normal processing
    output_df = input_df.transform(...)

# Write output (even if empty)
output_df.write.mode('overwrite').json(output_path)
```

## Running the Tests

### Automated Test Script

```bash
# Run all tests
./infrastructure/test-workflows.sh
```

### Manual Testing

**Express Workflow**:
```bash
# Normal input
aws sqs send-message \
  --queue-url https://sqs.us-east-1.amazonaws.com/728093470684/ceap-workflow-realtime-queue \
  --message-body '{"test": "data"}'

# Empty input
aws sqs send-message \
  --queue-url https://sqs.us-east-1.amazonaws.com/728093470684/ceap-workflow-realtime-queue \
  --message-body '{}'
```

**Standard Workflow**:
```bash
# Normal input
aws sqs send-message \
  --queue-url https://sqs.us-east-1.amazonaws.com/728093470684/ceap-workflow-batch-queue \
  --message-body '{"test": "data"}'

# Empty input
aws sqs send-message \
  --queue-url https://sqs.us-east-1.amazonaws.com/728093470684/ceap-workflow-batch-queue \
  --message-body '{}'
```

## Expected Results

### Express Workflow (realtime)
- **Duration**: 15-30 seconds
- **S3 Outputs**: 5 files (one per stage)
- **CloudWatch Logs**: Execution details in `/aws/stepfunctions/Ceap-realtime-Workflow`
- **Lambda Logs**: Individual stage logs in `/aws/lambda/CeapWorkflow-realtime-*`

### Standard Workflow (batch)
- **Duration**: ~2 hours (due to Glue job)
- **S3 Outputs**: 6 files (5 Lambda + 1 Glue)
- **CloudWatch Logs**: Execution details in `/aws/stepfunctions/Ceap-batch-Workflow`
- **Glue Logs**: Job logs in `/aws-glue/jobs/output` and `/aws-glue/jobs/error`
- **Lambda Logs**: Individual stage logs in `/aws/lambda/CeapWorkflow-batch-*`

## Troubleshooting

### If Execution Fails

1. **Check Step Functions execution history**:
```bash
aws stepfunctions get-execution-history \
  --execution-arn <execution-arn> \
  --max-results 100
```

2. **Check Lambda logs**:
```bash
aws logs tail /aws/lambda/CeapWorkflow-realtime-ETLLambda --since 1h
```

3. **Check Glue job logs** (Standard workflow):
```bash
aws logs tail /aws-glue/jobs/output --since 1h
aws logs tail /aws-glue/jobs/error --since 1h
```

4. **Check S3 intermediate outputs**:
```bash
aws s3 ls s3://ceap-workflow-realtime-728093470684/executions/<execution-id>/ --recursive
```

### If Message Goes to DLQ

1. **Retrieve DLQ messages**:
```bash
# Express workflow DLQ
aws sqs receive-message \
  --queue-url https://sqs.us-east-1.amazonaws.com/728093470684/ceap-workflow-realtime-dlq

# Standard workflow DLQ
aws sqs receive-message \
  --queue-url https://sqs.us-east-1.amazonaws.com/728093470684/ceap-workflow-batch-dlq
```

2. **Analyze failure pattern**
3. **Fix issue**
4. **Reprocess message**

## Success Criteria

For tests to pass:

- ✅ All 4 test messages sent successfully
- ✅ All executions start without errors
- ✅ Express workflow completes in <1 minute
- ✅ Standard workflow starts Glue job successfully
- ✅ Empty input processed without failures
- ✅ S3 outputs created for all stages
- ✅ No messages in DLQ
- ✅ CloudWatch Logs show execution details

## Notes

- **Empty input handling** is critical for streaming systems where messages may be empty
- **Glue job** in Standard workflow will take significantly longer (~2 hours)
- **Asynchronous execution** means Standard workflow returns immediately
- **S3 intermediate storage** allows debugging at each stage
- **Retry logic** handles transient failures automatically

## Next Steps After Testing

1. **Monitor for 24 hours** - Ensure stability
2. **Check costs** - Verify Glue DPU usage
3. **Optimize if needed** - Adjust Lambda memory, Glue workers
4. **Set up alerts** - CloudWatch alarms for failures
5. **Document learnings** - Update runbook with findings
