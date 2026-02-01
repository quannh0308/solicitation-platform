# Workflow Comparison: Express vs Standard (with Glue)

## Side-by-Side Comparison

### Express Workflow (realtime)

```
┌─────────────────────────────────────────────────┐
│         Ceap-realtime-Workflow (EXPRESS)        │
├─────────────────────────────────────────────────┤
│                                                 │
│  SQS Queue                                      │
│     ↓                                           │
│  ETL Lambda (5 sec)                             │
│     ↓ S3                                        │
│  Filter Lambda (3 sec)                          │
│     ↓ S3                                        │
│  Score Lambda (4 sec)                           │
│     ↓ S3                                        │
│  Store Lambda (2 sec)                           │
│     ↓ S3                                        │
│  Reactive Lambda (1 sec)                        │
│     ↓ S3                                        │
│  ✅ Complete                                    │
│                                                 │
│  Total Duration: ~15 seconds                    │
│  Invocation: Synchronous (REQUEST_RESPONSE)     │
│  Cost: $1 per million + Lambda                  │
└─────────────────────────────────────────────────┘
```

### Standard Workflow (batch) with Glue

```
┌─────────────────────────────────────────────────┐
│          Ceap-batch-Workflow (STANDARD)         │
├─────────────────────────────────────────────────┤
│                                                 │
│  SQS Queue                                      │
│     ↓                                           │
│  ETL Lambda (5 sec)                             │
│     ↓ S3                                        │
│  Filter Lambda (3 sec)                          │
│     ↓ S3                                        │
│  ⭐ Glue Job (2 hours) ⭐                       │
│     │ - 2 DPUs (G.1X workers)                   │
│     │ - Distributed Spark processing            │
│     │ - Handles large datasets (GB/TB)          │
│     │ - Complex transformations                 │
│     ↓ S3                                        │
│  Score Lambda (4 sec)                           │
│     ↓ S3                                        │
│  Store Lambda (2 sec)                           │
│     ↓ S3                                        │
│  Reactive Lambda (1 sec)                        │
│     ↓ S3                                        │
│  ✅ Complete                                    │
│                                                 │
│  Total Duration: ~2 hours                       │
│  Invocation: Asynchronous (FIRE_AND_FORGET)     │
│  Cost: $25 per million + Lambda + Glue          │
└─────────────────────────────────────────────────┘
```

## Feature Comparison

| Feature | Express (realtime) | Standard (batch) |
|---------|-------------------|------------------|
| **Max Duration** | 5 minutes | 1 year |
| **Glue Support** | ❌ No | ✅ Yes |
| **Invocation** | Synchronous | Asynchronous |
| **Cost per million** | $1 | $25 |
| **Glue Cost** | N/A | $0.44/DPU-hour |
| **Use Case** | Real-time | Batch + Heavy ETL |
| **Stages** | 5 Lambda | 5 Lambda + 1 Glue |
| **Typical Duration** | 15 seconds | 2+ hours |
| **Dataset Size** | <10 MB | GB to TB |

## When to Use Each Workflow

### Use Express Workflow (realtime) When:
- ✅ All processing completes in <5 minutes
- ✅ Dataset is small (<10 MB)
- ✅ Need immediate response
- ✅ Cost optimization is priority
- ✅ Simple transformations only

**Example Use Cases**:
- Real-time API enrichment
- Event-driven reactive processing
- Fast data validation
- Lightweight ETL

### Use Standard Workflow (batch) When:
- ✅ Processing takes >15 minutes
- ✅ Dataset is large (>100 MB)
- ✅ Need distributed computing (Spark)
- ✅ Complex joins and aggregations
- ✅ Data warehouse loading

**Example Use Cases**:
- Nightly batch processing
- Large dataset transformations
- Complex data enrichment
- ML feature engineering
- Multi-hour ETL jobs

## Glue Job Details

### Configuration
- **Job Name**: `ceap-workflow-batch-heavy-etl`
- **Script**: `s3://ceap-glue-scripts-728093470684/scripts/heavy-etl.py`
- **Glue Version**: 4.0 (latest)
- **Worker Type**: G.1X (4 vCPU, 16 GB memory)
- **Number of Workers**: 2 (minimum for distributed processing)
- **Timeout**: 120 minutes (2 hours)
- **Max Retries**: 0 (handled by Step Functions)

### Integration Pattern
- **Type**: RUN_JOB (synchronous)
- **Behavior**: Step Functions waits for Glue job completion
- **Retry**: 2 attempts, 5 min interval, 2x backoff
- **On Failure**: Workflow fails, message goes to DLQ

### S3 Data Flow
```
Input:  s3://ceap-workflow-batch-728093470684/executions/{executionId}/FilterStage/output.json
Output: s3://ceap-workflow-batch-728093470684/executions/{executionId}/HeavyETLStage/output.json
```

### Monitoring Glue Jobs

```bash
# List recent Glue job runs
aws glue get-job-runs --job-name ceap-workflow-batch-heavy-etl --max-results 10

# Get specific job run details
aws glue get-job-run --job-name ceap-workflow-batch-heavy-etl --run-id jr_xxxxx

# View Glue job logs
aws logs tail /aws-glue/jobs/output --follow

# View Glue job errors
aws logs tail /aws-glue/jobs/error --follow
```

## Cost Optimization Tips

### For Express Workflow
1. Right-size Lambda memory (reduce if possible)
2. Optimize Lambda code for speed
3. Use this for high-volume, fast processing

### For Standard Workflow with Glue
1. **Optimize Glue DPUs**: Start with 2, scale up only if needed
2. **Optimize Spark code**: Reduce processing time
3. **Batch processing**: Process multiple records per execution
4. **Monitor costs**: Set CloudWatch billing alarms
5. **Use Glue only when necessary**: Keep fast stages in Lambda

## Architecture Diagram

```
┌──────────────────────────────────────────────────────────────────┐
│                    AWS Account (us-east-1)                        │
│                                                                   │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │ Express Workflow (realtime) - Fast Processing               │ │
│  ├─────────────────────────────────────────────────────────────┤ │
│  │ SQS → Step Functions (EXPRESS)                              │ │
│  │   ETL → Filter → Score → Store → Reactive                   │ │
│  │    ↓      ↓       ↓       ↓        ↓                        │ │
│  │   S3     S3      S3      S3       S3                        │ │
│  │ Duration: 15 seconds | Cost: $0.16/month                    │ │
│  └─────────────────────────────────────────────────────────────┘ │
│                                                                   │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │ Standard Workflow (batch) - Heavy ETL with Glue             │ │
│  ├─────────────────────────────────────────────────────────────┤ │
│  │ SQS → Step Functions (STANDARD)                             │ │
│  │   ETL → Filter → ⭐ Glue Job ⭐ → Score → Store → Reactive │ │
│  │    ↓      ↓           ↓            ↓       ↓        ↓       │ │
│  │   S3     S3          S3           S3      S3       S3       │ │
│  │ Duration: 2 hours | Cost: $26.42/month                      │ │
│  └─────────────────────────────────────────────────────────────┘ │
│                                                                   │
│  Shared Resources:                                                │
│  - S3 Glue Scripts: ceap-glue-scripts-728093470684               │
│  - CloudWatch Logs for all workflows                             │
│  - X-Ray traces for distributed tracing                          │
└──────────────────────────────────────────────────────────────────┘
```

## What's Different Between the Two

### Express Workflow
- **5 Lambda stages** in sequence
- **No Glue job** - all processing in Lambda
- **Fast** - completes in seconds
- **Cheap** - $0.16/month
- **Synchronous** - immediate feedback

### Standard Workflow  
- **5 Lambda stages + 1 Glue job**
- **Glue job** between Filter and Score
- **Slow** - can take hours
- **Expensive** - $26.42/month (mostly Glue)
- **Asynchronous** - fire-and-forget
- **Scalable** - handles GB/TB datasets

## Next Steps

1. **Test both workflows** - Send test messages and verify execution
2. **Monitor Glue job** - Check CloudWatch Logs for Glue execution
3. **Optimize Glue script** - Customize the PySpark logic for your use case
4. **Set up alerts** - CloudWatch alarms for failures
5. **Cost monitoring** - Track Glue DPU usage

## Glue Job Customization

The Glue script is located at:
- **Local**: `infrastructure/glue-scripts/workflow_etl_template.py`
- **S3**: `s3://ceap-glue-scripts-728093470684/scripts/heavy-etl.py`

To customize:
1. Edit `workflow_etl_template.py` with your ETL logic
2. Upload to S3: `aws s3 cp infrastructure/glue-scripts/workflow_etl_template.py s3://ceap-glue-scripts-728093470684/scripts/heavy-etl.py`
3. Glue will use the updated script on next execution

## Summary

You now have **two fully functional workflows**:

1. **Express (realtime)**: Fast, cheap, Lambda-only
2. **Standard (batch)**: Slow, powerful, with Glue for heavy ETL

Both workflows:
- ✅ Use clean Lambda naming (no `-dev` postfix)
- ✅ Store intermediate outputs in S3
- ✅ Have retry logic and DLQ
- ✅ Support CloudWatch monitoring
- ✅ Can be deployed multiple times in same account

The Standard workflow additionally:
- ✅ Integrates AWS Glue for distributed processing
- ✅ Handles large datasets (GB/TB)
- ✅ Supports long-running jobs (hours)
- ✅ Uses Spark for complex transformations
