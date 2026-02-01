# Fraud Detection ML Pipeline - Architecture Design

## Overview

This document describes a complete fraud detection solution using CEAP workflow orchestration infrastructure with two pipelines:

1. **Training Pipeline** (Weekly/Monthly): Train ML model on historical data, deploy to SageMaker
2. **Inference Pipeline** (Daily): Score daily transactions for fraud, alert on suspicious activity

## High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                    Fraud Detection System                            │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  ┌────────────────────────────────────────────────────────────┐    │
│  │ Training Pipeline (Weekly/Monthly)                         │    │
│  ├────────────────────────────────────────────────────────────┤    │
│  │ S3 Historical Data → Glue ETL → Feature Engineering →      │    │
│  │ SageMaker Training → Model Evaluation → Deploy to          │    │
│  │ SageMaker Endpoint                                         │    │
│  │                                                            │    │
│  │ Duration: 2-4 hours | Cost: ~$50/run                       │    │
│  └────────────────────────────────────────────────────────────┘    │
│                                                                      │
│  ┌────────────────────────────────────────────────────────────┐    │
│  │ Inference Pipeline (Daily)                                 │    │
│  ├────────────────────────────────────────────────────────────┤    │
│  │ S3 Daily Transactions → ETL → Feature Extraction →         │    │
│  │ SageMaker Inference → Fraud Scoring → Alert High Risk →   │    │
│  │ Store Results                                              │    │
│  │                                                            │    │
│  │ Duration: 15-30 min | Cost: ~$5/day                        │    │
│  └────────────────────────────────────────────────────────────┘    │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

## Pipeline 1: Model Training Pipeline

### Purpose
Train fraud detection model on historical transaction data and deploy to SageMaker.

### Workflow Type
**Standard Workflow** (long-running, supports Glue)

### Architecture

```
┌──────────────────────────────────────────────────────────────────┐
│              Training Pipeline (Standard Workflow)                │
├──────────────────────────────────────────────────────────────────┤
│                                                                   │
│  EventBridge Schedule (Weekly)                                    │
│         ↓                                                         │
│  SQS Queue                                                        │
│         ↓                                                         │
│  Step Functions (STANDARD)                                        │
│         ↓                                                         │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │ Stage 1: Data Extraction (Glue Job - 30 min)            │    │
│  │ - Read historical transactions from S3                   │    │
│  │ - Filter last 6 months of data                           │    │
│  │ - Join with fraud labels                                 │    │
│  │ - Output: s3://bucket/executions/{id}/DataExtract/       │    │
│  └─────────────────────────────────────────────────────────┘    │
│         ↓ S3                                                      │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │ Stage 2: Feature Engineering (Glue Job - 1 hour)        │    │
│  │ - Calculate transaction patterns                         │    │
│  │ - Aggregate customer behavior                            │    │
│  │ - Create time-based features                             │    │
│  │ - Output: s3://bucket/executions/{id}/Features/          │    │
│  └─────────────────────────────────────────────────────────┘    │
│         ↓ S3                                                      │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │ Stage 3: Train Model (Lambda)                            │    │
│  │ - Start SageMaker training job                           │    │
│  │ - Algorithm: XGBoost or Random Forest                    │    │
│  │ - Wait for training completion                           │    │
│  │ - Output: Model artifacts to S3                          │    │
│  └─────────────────────────────────────────────────────────┘    │
│         ↓ S3                                                      │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │ Stage 4: Model Evaluation (Lambda)                       │    │
│  │ - Load test dataset                                      │    │
│  │ - Run predictions                                        │    │
│  │ - Calculate metrics (precision, recall, F1)             │    │
│  │ - Output: Evaluation metrics                             │    │
│  └─────────────────────────────────────────────────────────┘    │
│         ↓ S3                                                      │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │ Stage 5: Deploy Model (Lambda)                           │    │
│  │ - Create/Update SageMaker endpoint                       │    │
│  │ - Deploy model to endpoint                               │    │
│  │ - Update endpoint configuration                          │    │
│  │ - Output: Endpoint ARN and status                        │    │
│  └─────────────────────────────────────────────────────────┘    │
│         ↓                                                         │
│  ✅ Model deployed to SageMaker                                  │
│                                                                   │
└──────────────────────────────────────────────────────────────────┘
```

### Implementation

**Workflow Configuration**:
```kotlin
val trainingWorkflow = WorkflowConfiguration(
    workflowName = "fraud-model-training",
    workflowType = WorkflowType.STANDARD,  // Long-running with Glue
    steps = listOf(
        WorkflowStepType.Glue(GlueStep("DataExtraction", "fraud-data-extract")),
        WorkflowStepType.Glue(GlueStep("FeatureEngineering", "fraud-feature-eng")),
        WorkflowStepType.Lambda(LambdaStep("TrainModel", "trainModelLambda")),
        WorkflowStepType.Lambda(LambdaStep("EvaluateModel", "evaluateModelLambda")),
        WorkflowStepType.Lambda(LambdaStep("DeployModel", "deployModelLambda"))
    )
)
```

**Schedule**: Weekly (Sunday 2 AM) or Monthly (1st of month)

**Cost Estimate**:
- Glue: 2 DPUs × 1.5 hours = $1.32
- SageMaker Training: ml.m5.xlarge × 1 hour = $0.23
- Lambda: ~$0.10
- **Total per run**: ~$1.65

## Pipeline 2: Daily Fraud Detection Pipeline

### Purpose
Score daily transactions for fraud using the deployed SageMaker model.

### Workflow Type
**Express Workflow** (fast, cost-optimized)

### Architecture

```
┌──────────────────────────────────────────────────────────────────┐
│           Daily Inference Pipeline (Express Workflow)             │
├──────────────────────────────────────────────────────────────────┤
│                                                                   │
│  EventBridge Schedule (Daily 6 AM)                                │
│         ↓                                                         │
│  SQS Queue                                                        │
│         ↓                                                         │
│  Step Functions (EXPRESS)                                         │
│         ↓                                                         │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │ Stage 1: ETL - Extract Daily Transactions (Lambda)      │    │
│  │ - Read yesterday's transactions from S3                  │    │
│  │ - Parse and validate data                                │    │
│  │ - Output: s3://bucket/executions/{id}/ETL/               │    │
│  └─────────────────────────────────────────────────────────┘    │
│         ↓ S3                                                      │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │ Stage 2: Feature Extraction (Lambda)                     │    │
│  │ - Calculate transaction features                         │    │
│  │ - Aggregate customer history                             │    │
│  │ - Prepare for SageMaker inference                        │    │
│  │ - Output: s3://bucket/executions/{id}/Features/          │    │
│  └─────────────────────────────────────────────────────────┘    │
│         ↓ S3                                                      │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │ Stage 3: Fraud Scoring (Lambda)                          │    │
│  │ - Call SageMaker endpoint for batch inference            │    │
│  │ - Get fraud probability for each transaction             │    │
│  │ - Output: s3://bucket/executions/{id}/Scores/            │    │
│  └─────────────────────────────────────────────────────────┘    │
│         ↓ S3                                                      │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │ Stage 4: Filter High Risk (Lambda)                       │    │
│  │ - Filter transactions with fraud_score > 0.8             │    │
│  │ - Rank by risk level                                     │    │
│  │ - Output: s3://bucket/executions/{id}/HighRisk/          │    │
│  └─────────────────────────────────────────────────────────┘    │
│         ↓ S3                                                      │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │ Stage 5: Alert & Store (Lambda)                          │    │
│  │ - Send alerts for high-risk transactions                 │    │
│  │ - Store results in DynamoDB                              │    │
│  │ - Trigger fraud investigation workflow                   │    │
│  │ - Output: s3://bucket/executions/{id}/Results/           │    │
│  └─────────────────────────────────────────────────────────┘    │
│         ↓                                                         │
│  ✅ Daily fraud detection complete                                │
│                                                                   │
└──────────────────────────────────────────────────────────────────┘
```

### Implementation

**Workflow Configuration**:
```kotlin
val inferenceWorkflow = WorkflowConfiguration(
    workflowName = "fraud-detection-daily",
    workflowType = WorkflowType.EXPRESS,  // Fast, cost-optimized
    steps = listOf(
        WorkflowStepType.Lambda(LambdaStep("ETL", "extractTransactionsLambda")),
        WorkflowStepType.Lambda(LambdaStep("FeatureExtraction", "extractFeaturesLambda")),
        WorkflowStepType.Lambda(LambdaStep("FraudScoring", "scoreFraudLambda")),
        WorkflowStepType.Lambda(LambdaStep("FilterHighRisk", "filterHighRiskLambda")),
        WorkflowStepType.Lambda(LambdaStep("AlertAndStore", "alertStoreLambda"))
    )
)
```

**Schedule**: Daily at 6 AM (after previous day's transactions are available)

**Cost Estimate**:
- Step Functions: $0.001/day
- Lambda: ~$0.50/day
- SageMaker Inference: $0.05/1000 transactions
- **Total per day**: ~$1-2 (depending on transaction volume)

## Complete System Diagram

```
┌────────────────────────────────────────────────────────────────────────────┐
│                     Fraud Detection ML System                               │
└────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────┐
│ Data Sources                                                                 │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  S3 Bucket: bank-transactions/                                               │
│  ├── raw/                                                                    │
│  │   ├── 2026-01-01/transactions.parquet                                    │
│  │   ├── 2026-01-02/transactions.parquet                                    │
│  │   └── ...                                                                │
│  ├── labeled/                                                                │
│  │   └── fraud_labels.parquet (historical fraud cases)                      │
│  └── features/                                                               │
│      └── engineered_features.parquet                                         │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
                                    ↓
┌─────────────────────────────────────────────────────────────────────────────┐
│ Training Pipeline (Weekly - Standard Workflow)                               │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  EventBridge Schedule (Weekly)                                               │
│         ↓                                                                    │
│  Ceap-fraud-training-Workflow (STANDARD)                                     │
│         ↓                                                                    │
│  [Glue: Data Extract] → [Glue: Feature Eng] → [Lambda: Train] →            │
│  [Lambda: Evaluate] → [Lambda: Deploy]                                      │
│         ↓                ↓                      ↓                            │
│        S3              S3                   SageMaker                        │
│                                                                              │
│  Output: SageMaker Endpoint (fraud-detection-model-v1)                      │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
                                    ↓
┌─────────────────────────────────────────────────────────────────────────────┐
│ SageMaker Model Endpoint                                                     │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  Endpoint: fraud-detection-model-v1                                          │
│  Instance: ml.t2.medium (always-on)                                          │
│  Model: XGBoost fraud classifier                                             │
│  Input: Transaction features (JSON)                                          │
│  Output: Fraud probability (0.0 - 1.0)                                       │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
                                    ↓
┌─────────────────────────────────────────────────────────────────────────────┐
│ Inference Pipeline (Daily - Express Workflow)                                │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  EventBridge Schedule (Daily 6 AM)                                           │
│         ↓                                                                    │
│  Ceap-fraud-detection-Workflow (EXPRESS)                                     │
│         ↓                                                                    │
│  [Lambda: ETL] → [Lambda: Features] → [Lambda: Score] →                     │
│  [Lambda: Filter] → [Lambda: Alert]                                         │
│         ↓              ↓                 ↓                                   │
│        S3             S3            SageMaker                                │
│                                                                              │
│  Outputs:                                                                    │
│  - High-risk transactions → SNS → Security Team                             │
│  - All scores → DynamoDB (fraud_scores table)                               │
│  - Daily report → S3                                                         │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

## Detailed Stage Breakdown

### Training Pipeline Stages


#### Stage 1: Data Extraction (Glue Job)
**Duration**: 30 minutes  
**Input**: S3 paths to historical transactions  
**Processing**:
- Read 6 months of transaction data (Parquet format)
- Join with fraud labels
- Filter valid transactions
- Sample for training (if dataset too large)

**Output to S3**:
```json
{
  "transactions": [
    {
      "transaction_id": "TXN-001",
      "amount": 1500.00,
      "merchant": "Online Store",
      "timestamp": "2026-01-15T14:30:00Z",
      "is_fraud": false
    }
  ],
  "record_count": 500000,
  "fraud_count": 2500,
  "fraud_rate": 0.005
}
```

#### Stage 2: Feature Engineering (Glue Job)
**Duration**: 1 hour  
**Input**: Raw transactions from Stage 1  
**Processing**:
- Calculate transaction velocity (transactions per hour)
- Aggregate spending patterns
- Time-based features (hour of day, day of week)
- Merchant category analysis
- Geographic features

**Output to S3**:
```json
{
  "features": [
    {
      "transaction_id": "TXN-001",
      "amount": 1500.00,
      "amount_zscore": 2.5,
      "velocity_1h": 3,
      "velocity_24h": 15,
      "merchant_risk_score": 0.3,
      "hour_of_day": 14,
      "is_weekend": false,
      "is_fraud": false
    }
  ],
  "feature_count": 25,
  "record_count": 500000
}
```

#### Stage 3: Train Model (Lambda)
**Duration**: 5 minutes (to start training)  
**Input**: Feature dataset from Stage 2  
**Processing**:
- Start SageMaker training job
- Algorithm: XGBoost
- Hyperparameters: max_depth=5, eta=0.1, objective=binary:logistic
- Training data: 80% of dataset
- Validation data: 20% of dataset

**Output to S3**:
```json
{
  "training_job_name": "fraud-model-2026-02-01",
  "training_job_arn": "arn:aws:sagemaker:...",
  "status": "InProgress",
  "model_s3_path": "s3://models/fraud-model-2026-02-01/"
}
```

#### Stage 4: Model Evaluation (Lambda)
**Duration**: 2 minutes  
**Input**: Trained model from Stage 3  
**Processing**:
- Load test dataset
- Run batch predictions
- Calculate metrics: precision, recall, F1, AUC
- Compare with previous model

**Output to S3**:
```json
{
  "model_version": "v2026-02-01",
  "metrics": {
    "precision": 0.92,
    "recall": 0.88,
    "f1_score": 0.90,
    "auc": 0.95
  },
  "test_samples": 100000,
  "true_positives": 2200,
  "false_positives": 190,
  "deploy_recommended": true
}
```

#### Stage 5: Deploy Model (Lambda)
**Duration**: 3 minutes  
**Input**: Model evaluation from Stage 4  
**Processing**:
- Create SageMaker endpoint configuration
- Deploy model to endpoint
- Update endpoint (or create new)
- Run smoke test

**Output to S3**:
```json
{
  "endpoint_name": "fraud-detection-model-v1",
  "endpoint_arn": "arn:aws:sagemaker:...",
  "status": "InService",
  "instance_type": "ml.t2.medium",
  "model_version": "v2026-02-01",
  "deployed_at": "2026-02-01T08:30:00Z"
}
```

### Inference Pipeline Stages

#### Stage 1: ETL - Extract Transactions (Lambda)
**Duration**: 30 seconds  
**Input**: Date range (yesterday)  
**Processing**:
- Read yesterday's transactions from S3
- Parse Parquet files
- Validate data quality

**Output to S3**:
```json
{
  "transactions": [...],
  "date": "2026-02-01",
  "record_count": 50000
}
```

#### Stage 2: Feature Extraction (Lambda)
**Duration**: 45 seconds  
**Input**: Raw transactions from Stage 1  
**Processing**:
- Calculate same features as training pipeline
- Use pre-computed aggregates from DynamoDB
- Format for SageMaker inference

**Output to S3**:
```json
{
  "features": [
    {
      "transaction_id": "TXN-12345",
      "features": [1500.0, 2.5, 3, 15, 0.3, 14, 0]
    }
  ],
  "record_count": 50000
}
```

#### Stage 3: Fraud Scoring (Lambda)
**Duration**: 2 minutes  
**Input**: Features from Stage 2  
**Processing**:
- Call SageMaker endpoint in batches (100 transactions per call)
- Get fraud probability for each transaction
- Add confidence scores

**Output to S3**:
```json
{
  "scored_transactions": [
    {
      "transaction_id": "TXN-12345",
      "fraud_score": 0.92,
      "confidence": 0.88,
      "risk_level": "HIGH"
    }
  ],
  "high_risk_count": 125,
  "medium_risk_count": 450,
  "low_risk_count": 49425
}
```

#### Stage 4: Filter High Risk (Lambda)
**Duration**: 10 seconds  
**Input**: Scored transactions from Stage 3  
**Processing**:
- Filter fraud_score > 0.8
- Rank by score
- Add transaction details

**Output to S3**:
```json
{
  "high_risk_transactions": [
    {
      "transaction_id": "TXN-12345",
      "amount": 5000.00,
      "merchant": "Suspicious Store",
      "fraud_score": 0.92,
      "customer_id": "CUST-789",
      "alert_priority": "URGENT"
    }
  ],
  "count": 125
}
```

#### Stage 5: Alert & Store (Lambda)
**Duration**: 15 seconds  
**Input**: High-risk transactions from Stage 4  
**Processing**:
- Send SNS alerts for URGENT cases
- Store all scores in DynamoDB
- Create daily fraud report
- Trigger investigation workflow

**Output to S3**:
```json
{
  "alerts_sent": 125,
  "records_stored": 50000,
  "report_s3_path": "s3://reports/fraud-daily-2026-02-01.pdf",
  "status": "complete"
}
```

## Data Flow Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                    Data Flow Overview                            │
└─────────────────────────────────────────────────────────────────┘

Historical Data (S3)
    ↓
[Training Pipeline - Weekly]
    ↓
SageMaker Model
    ↓
SageMaker Endpoint (Always-On)
    ↓
[Inference Pipeline - Daily]
    ↓
Fraud Alerts + DynamoDB Storage


Detailed Flow:

Week 1: Train Model
─────────────────────
S3: 6 months transactions
    ↓ Glue ETL (30 min)
S3: Clean data
    ↓ Glue Feature Eng (1 hour)
S3: Features
    ↓ Lambda: Start Training (5 min)
SageMaker: Training Job (1 hour)
    ↓ Lambda: Evaluate (2 min)
S3: Metrics
    ↓ Lambda: Deploy (3 min)
SageMaker Endpoint: fraud-detection-model-v1 ✅


Day 1: Detect Fraud
───────────────────
S3: Yesterday's transactions
    ↓ Lambda: ETL (30 sec)
S3: Parsed transactions
    ↓ Lambda: Features (45 sec)
S3: Feature vectors
    ↓ Lambda: Score (2 min) → SageMaker Endpoint
S3: Fraud scores
    ↓ Lambda: Filter (10 sec)
S3: High-risk transactions
    ↓ Lambda: Alert (15 sec)
SNS: Alerts sent ✅
DynamoDB: Scores stored ✅
```

## Implementation Guide

### Step 1: Deploy Training Pipeline

```bash
# Deploy Standard workflow for training
./infrastructure/deploy-workflow-simple.sh -n fraud-training -t standard
```

### Step 2: Deploy Inference Pipeline

```bash
# Deploy Express workflow for daily detection
./infrastructure/deploy-workflow-simple.sh -n fraud-detection -t express
```

### Step 3: Create Lambda Handlers

**Training Lambdas**:
- `TrainModelLambda.kt` - Starts SageMaker training job
- `EvaluateModelLambda.kt` - Evaluates model performance
- `DeployModelLambda.kt` - Deploys model to endpoint

**Inference Lambdas**:
- `ExtractTransactionsLambda.kt` - Reads daily transactions
- `ExtractFeaturesLambda.kt` - Calculates features
- `ScoreFraudLambda.kt` - Calls SageMaker for predictions
- `FilterHighRiskLambda.kt` - Filters high-risk transactions
- `AlertStoreLambda.kt` - Sends alerts and stores results

### Step 4: Create Glue Scripts

**Data Extraction Script** (`fraud_data_extract.py`):
```python
# Read historical transactions
df = spark.read.parquet(f"s3://{input_bucket}/raw/")

# Join with fraud labels
labels_df = spark.read.parquet(f"s3://{input_bucket}/labeled/fraud_labels.parquet")
df = df.join(labels_df, "transaction_id", "left")

# Write to output
df.write.parquet(output_path)
```

**Feature Engineering Script** (`fraud_feature_eng.py`):
```python
# Calculate features
df = df.withColumn("amount_zscore", (col("amount") - mean) / stddev)
df = df.withColumn("velocity_1h", count_window("1 hour"))
# ... more features

df.write.parquet(output_path)
```

### Step 5: Schedule Workflows

**Training Pipeline** (Weekly):
```bash
aws events put-rule \
  --name fraud-training-weekly \
  --schedule-expression "cron(0 2 ? * SUN *)" \
  --state ENABLED

aws events put-targets \
  --rule fraud-training-weekly \
  --targets "Id=1,Arn=arn:aws:states:...:stateMachine:Ceap-fraud-training-Workflow"
```

**Inference Pipeline** (Daily):
```bash
aws events put-rule \
  --name fraud-detection-daily \
  --schedule-expression "cron(0 6 * * ? *)" \
  --state ENABLED

aws events put-targets \
  --rule fraud-detection-daily \
  --targets "Id=1,Arn=arn:aws:states:...:stateMachine:Ceap-fraud-detection-Workflow"
```

## Benefits of This Architecture

### 1. Scalability
- Glue handles large datasets (millions of transactions)
- SageMaker scales inference automatically
- S3 intermediate storage handles any data size

### 2. Cost Optimization
- Training: Weekly (not daily) - saves costs
- Inference: Express workflow - $1 per million transitions
- SageMaker: Right-sized instances

### 3. Observability
- S3 outputs at each stage for debugging
- CloudWatch Logs for all executions
- X-Ray tracing for performance analysis

### 4. Reliability
- Automatic retries on failures
- Dead Letter Queue for failed messages
- Model versioning in SageMaker

### 5. Flexibility
- Easy to add new features
- Can retrain model anytime
- Can adjust scoring thresholds

## Cost Breakdown

### Training Pipeline (Weekly)
- Glue: 2 DPUs × 1.5 hours × $0.44/DPU-hour = $1.32
- SageMaker Training: ml.m5.xlarge × 1 hour × $0.23/hour = $0.23
- Lambda: ~$0.10
- **Weekly**: $1.65
- **Monthly**: $6.60

### Inference Pipeline (Daily)
- Step Functions: 5 stages × 30 days × $1/million = $0.001
- Lambda: ~$0.50/day
- SageMaker Inference: 50K transactions × $0.05/1K = $2.50/day
- **Daily**: $3.00
- **Monthly**: $90.00

### SageMaker Endpoint (Always-On)
- ml.t2.medium × 730 hours × $0.065/hour = $47.45/month

**Total Monthly Cost**: ~$144/month

## Alternative: Real-Time Fraud Detection

For real-time fraud detection (as transactions occur):

```
Transaction Event → EventBridge → Lambda → SageMaker → 
Fraud Score → Alert if High Risk
```

Use **Express Workflow** for real-time:
- Duration: <5 seconds
- Cost: $1 per million transactions
- No batch processing needed

## Monitoring & Alerts

### CloudWatch Alarms
1. **High Fraud Rate**: fraud_rate > 2%
2. **Model Drift**: accuracy < 85%
3. **Pipeline Failures**: execution_failed > 0
4. **SageMaker Errors**: endpoint_errors > 10

### Daily Reports
- Total transactions processed
- Fraud cases detected
- False positive rate
- Model performance metrics

## Next Steps

1. **Implement Lambda handlers** for both pipelines
2. **Create Glue scripts** for data extraction and feature engineering
3. **Train initial model** using SageMaker
4. **Deploy both workflows** using existing infrastructure
5. **Set up monitoring** and alerts
6. **Test end-to-end** with sample data

This architecture leverages your existing CEAP workflow infrastructure perfectly!
