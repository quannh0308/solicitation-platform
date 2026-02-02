# Fraud Detection ML Pipeline - Client Implementation Guide

## For Clients: Quick Start

**You are a client** who wants to build a fraud detection system. This guide shows you how to:
1. ✅ Clone the CEAP platform (this repository)
2. ✅ Deploy CEAP workflows to your AWS account
3. ✅ Create your own fraud detection repository
4. ✅ Build your Lambda handlers that run on CEAP workflows
5. ✅ Deploy and test your fraud detection system

**Time to implement**: 2-3 weeks  
**Cost**: ~$144/month  
**What you reuse from CEAP**: 60% of infrastructure  
**What you build**: 40% (your fraud detection logic)

## Overview

This guide shows how to build a fraud detection system using the **CEAP (Customer Engagement & Action Platform)** workflow orchestration framework. As a client, you'll leverage CEAP's infrastructure to build two pipelines:

1. **Training Pipeline** (Weekly/Monthly): Train ML model on historical data, deploy to SageMaker
2. **Inference Pipeline** (Daily): Score daily transactions for fraud, alert on suspicious activity

## Prerequisites

### What You Need
- AWS Account with appropriate permissions
- GitHub account
- Basic knowledge of Kotlin and Python
- Historical transaction data with fraud labels

### CEAP Platform Access

**Option 1: Clone CEAP Repository (Recommended)**
```bash
# Clone the CEAP platform repository
git clone https://github.com/quannh0308/customer-engagement-platform.git ceap-platform

# This gives you the complete workflow orchestration framework
cd ceap-platform
```

**What you get**:
- ✅ Step Functions orchestration (Express + Standard workflows)
- ✅ S3-based intermediate storage
- ✅ Lambda deployment infrastructure
- ✅ Glue job integration
- ✅ Retry logic and error handling
- ✅ CloudWatch monitoring
- ✅ Deployment scripts

**Option 2: Fork CEAP Repository**
```bash
# Fork the repository on GitHub to your account
# Then clone your fork
git clone https://github.com/YOUR-USERNAME/customer-engagement-platform.git ceap-platform
```

**Benefits**:
- Keep your customizations separate
- Pull updates from upstream CEAP
- Contribute improvements back

### Your Project Structure

After cloning CEAP, create your fraud detection project:

```
your-workspace/
├── ceap-platform/                    # Cloned CEAP framework (read-only)
│   ├── infrastructure/               # Workflow orchestration infrastructure
│   ├── docs/                         # CEAP documentation
│   └── [other CEAP modules]
│
└── fraud-detection-system/           # Your new project (your code)
    ├── .git/                         # Your Git repository
    ├── fraud-training-pipeline/      # Training Lambda handlers
    │   ├── src/main/kotlin/
    │   └── build.gradle.kts
    ├── fraud-inference-pipeline/     # Inference Lambda handlers
    │   ├── src/main/kotlin/
    │   └── build.gradle.kts
    ├── glue-scripts/                 # Your Glue ETL scripts
    │   ├── fraud_data_extract.py
    │   └── fraud_feature_engineering.py
    ├── infrastructure/               # Your infrastructure code
    │   └── FraudDetectionStack.kt    # Uses CEAP workflows
    ├── build.gradle.kts              # Your project build
    └── README.md                     # Your project docs
```

## Repository Setup

### Step 1: Create Your GitHub Repository

```bash
# Create new repository on GitHub
# Name: fraud-detection-system
# Description: Fraud detection ML pipelines using CEAP platform

# Initialize your project
mkdir fraud-detection-system
cd fraud-detection-system
git init
git remote add origin https://github.com/YOUR-USERNAME/fraud-detection-system.git
```

### Step 2: Reference CEAP as Dependency

**Option A: Git Submodule (Recommended)**
```bash
# Add CEAP as a submodule
cd fraud-detection-system
git submodule add https://github.com/quannh0308/customer-engagement-platform.git ceap-platform

# Your project uses CEAP's infrastructure
```

**Option B: Copy CEAP Infrastructure**
```bash
# Copy only the infrastructure you need
cp -r ../ceap-platform/infrastructure ./ceap-infrastructure
cp -r ../ceap-platform/docs/WORKFLOW-*.md ./docs/

# Customize for your needs
```

**Option C: Gradle Dependency (If CEAP is published)**
```kotlin
// In your build.gradle.kts
dependencies {
    implementation("com.ceap:ceap-workflow-orchestration:1.0.0")
}
```

### Step 3: Create Your Project Structure

```bash
# Create your Lambda modules
mkdir -p fraud-training-pipeline/src/main/kotlin/com/yourcompany/fraud/training
mkdir -p fraud-inference-pipeline/src/main/kotlin/com/yourcompany/fraud/inference
mkdir -p glue-scripts
mkdir -p infrastructure/src/main/kotlin/com/yourcompany/fraud/infrastructure
```

**Your `settings.gradle.kts`**:
```kotlin
rootProject.name = "fraud-detection-system"

// Your modules
include("fraud-training-pipeline")
include("fraud-inference-pipeline")

// Reference CEAP platform (if using submodule)
includeBuild("ceap-platform")
```

## Client Setup Visualization

```
┌────────────────────────────────────────────────────────────────┐
│                    Your Setup as a Client                       │
└────────────────────────────────────────────────────────────────┘

Step 1: Clone CEAP Platform
────────────────────────────
GitHub: quannh0308/customer-engagement-platform (Public)
    ↓ git clone
Your Machine: ceap-platform/ (Read-only reference)


Step 2: Create Your Repository
───────────────────────────────
GitHub: YOUR-USERNAME/fraud-detection-system (Your repo)
    ↓ git init
Your Machine: fraud-detection-system/ (Your code)
    ├── ceap-platform/ (submodule → CEAP)
    ├── fraud-training-pipeline/ (your code)
    ├── fraud-inference-pipeline/ (your code)
    └── glue-scripts/ (your scripts)


Step 3: Deploy to Your AWS Account
───────────────────────────────────
Your AWS Account (YOUR-ACCOUNT-ID)
    ↓ ./deploy-workflow-simple.sh
    ├── Step Functions: Ceap-fraud-training-Workflow
    ├── Step Functions: Ceap-fraud-detection-Workflow
    ├── Lambda: YOUR fraud detection handlers
    ├── S3: YOUR transaction data
    ├── SageMaker: YOUR fraud model
    └── DynamoDB: YOUR fraud scores


Result: Your fraud detection system running on CEAP infrastructure!
```

## What You Own vs What CEAP Provides

### CEAP Platform Provides (You Reuse) ✅
- Step Functions workflow templates
- S3 orchestration patterns
- Lambda base classes (WorkflowLambdaHandler)
- Deployment scripts
- Retry logic and error handling
- CloudWatch monitoring setup
- Documentation and examples

### You Build (Your IP) 🔨
- Lambda handlers with fraud detection logic
- Glue scripts for feature engineering
- SageMaker training configuration
- Fraud detection algorithms
- Alerting rules and thresholds
- Your transaction data and models
- Your DynamoDB tables and schemas

**Key Point**: CEAP is infrastructure, your fraud detection logic is yours!

## Technology Stack

### Core AWS Services
- **AWS Step Functions**: Workflow orchestration (Express + Standard)
- **AWS Lambda**: Serverless compute for data processing
- **Amazon S3**: Data lake for transactions and intermediate storage
- **Amazon SageMaker**: ML model training and inference
- **AWS Glue**: ETL for large-scale data processing
- **Amazon DynamoDB**: Storage for fraud scores and metadata
- **Amazon SNS**: Alerting for high-risk transactions
- **Amazon CloudWatch**: Monitoring and logging
- **AWS X-Ray**: Distributed tracing

### ML/AI Services
- **Amazon SageMaker Training**: Model training with built-in algorithms
  - Algorithm: XGBoost (gradient boosting for fraud classification)
  - Alternative: Random Forest, Linear Learner
- **Amazon SageMaker Endpoints**: Real-time model inference
  - Instance: ml.t2.medium (cost-effective for moderate traffic)
  - Auto-scaling: Scale up during peak hours
- **Amazon SageMaker Feature Store** (Optional): Centralized feature repository
- **Amazon Bedrock** (Optional): For LLM-based fraud pattern analysis
  - Use case: Analyze transaction descriptions for suspicious patterns
  - Model: Claude or Titan for text analysis

### Data Processing
- **AWS Glue 4.0**: Distributed Spark processing
  - Worker Type: G.1X (4 vCPU, 16 GB memory)
  - Python 3 with PySpark
- **Apache Spark**: Data transformation and feature engineering
- **Parquet Format**: Efficient columnar storage for transactions

### Programming Languages
- **Kotlin**: Lambda handlers and CDK infrastructure
- **Python**: Glue scripts and SageMaker training scripts
- **SQL**: Athena queries for data exploration (optional)

### Why These Technologies?

**SageMaker vs Bedrock**:
- **SageMaker**: Best for structured data (transactions, amounts, timestamps)
  - Optimized for tabular data
  - Built-in algorithms (XGBoost, Random Forest)
  - Cost-effective for batch inference
  - **Recommended for fraud detection** ✅

- **Bedrock**: Best for unstructured data (text, descriptions)
  - Use for analyzing transaction descriptions
  - Detect suspicious patterns in merchant names
  - Complement to SageMaker (not replacement)
  - Optional enhancement

**Recommendation**: Use **SageMaker** as primary ML service, optionally add **Bedrock** for text analysis of transaction descriptions.

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


## Implementation Roadmap (Client Perspective)

### Phase 0: Setup Your Project (Day 1)

#### 0.1 Clone CEAP Platform
```bash
# Clone CEAP platform
git clone https://github.com/quannh0308/customer-engagement-platform.git ceap-platform

# Explore the platform
cd ceap-platform
cat README.md
cat TECH-STACK.md
cat docs/WORKFLOW-ORCHESTRATION-GUIDE.md
```

#### 0.2 Create Your Project Repository
```bash
# Create your fraud detection project
mkdir fraud-detection-system
cd fraud-detection-system
git init

# Add CEAP as submodule
git submodule add https://github.com/quannh0308/customer-engagement-platform.git ceap-platform

# Create initial structure
mkdir -p fraud-training-pipeline/src/main/kotlin/com/yourcompany/fraud/training
mkdir -p fraud-inference-pipeline/src/main/kotlin/com/yourcompany/fraud/inference
mkdir -p glue-scripts
mkdir -p infrastructure

# Initialize Git
git add .
git commit -m "Initial project structure with CEAP platform"

# Push to your GitHub
git remote add origin https://github.com/YOUR-USERNAME/fraud-detection-system.git
git push -u origin main
```

**Deliverables**:
- ✅ Your GitHub repository created
- ✅ CEAP platform referenced as submodule
- ✅ Project structure initialized

**Effort**: 2 hours

### Phase 1: Deploy CEAP Workflows to Your AWS Account (Day 1-2)

#### 1.1 Configure AWS Credentials
```bash
# Configure your AWS account
aws configure
# Enter your AWS Access Key ID
# Enter your AWS Secret Access Key
# Enter region: us-east-1
```

#### 1.2 Deploy CEAP Workflows
```bash
# Navigate to CEAP platform
cd ceap-platform/infrastructure

# Deploy training workflow (Standard with Glue support)
./deploy-workflow-simple.sh -n fraud-training -t standard

# Deploy inference workflow (Express, fast)
./deploy-workflow-simple.sh -n fraud-detection -t express
```

**What gets deployed to YOUR AWS account**:
- ✅ 2 Step Functions workflows
- ✅ 10 Lambda functions (placeholder handlers from CEAP)
- ✅ 2 S3 buckets for intermediate storage
- ✅ 4 SQS queues (2 main + 2 DLQ)
- ✅ CloudWatch Logs and X-Ray tracing

**Deliverables**:
- ✅ CEAP workflows running in your AWS account
- ✅ Infrastructure ready for your Lambda handlers

**Effort**: 4 hours (including AWS setup)

#### 1.3 Create Your DynamoDB Tables
```bash
# In your fraud-detection-system project
cd ../fraud-detection-system

# Create fraud scores table
aws dynamodb create-table \
  --table-name FraudScores \
  --attribute-definitions \
    AttributeName=transaction_id,AttributeType=S \
    AttributeName=date,AttributeType=S \
  --key-schema \
    AttributeName=transaction_id,KeyType=HASH \
    AttributeName=date,KeyType=RANGE \
  --billing-mode PAY_PER_REQUEST

# Create model metadata table
aws dynamodb create-table \
  --table-name FraudModels \
  --attribute-definitions \
    AttributeName=model_version,AttributeType=S \
  --key-schema \
    AttributeName=model_version,KeyType=HASH \
  --billing-mode PAY_PER_REQUEST
```

**Deliverables**:
- ✅ FraudScores table (your data)
- ✅ FraudModels table (your models)

**Effort**: 30 minutes

### Phase 2: Build Your Training Pipeline (Day 3-7)

#### 1.1 Deploy Workflows
```bash
# Deploy training pipeline (Standard workflow)
./infrastructure/deploy-workflow-simple.sh -n fraud-training -t standard

# Deploy inference pipeline (Express workflow)
./infrastructure/deploy-workflow-simple.sh -n fraud-detection -t express
```

**Deliverables**:
- ✅ 2 Step Functions workflows
- ✅ 10 Lambda functions (5 per workflow)
- ✅ 2 S3 buckets for intermediate storage
- ✅ SQS queues with DLQ

**Effort**: 2 hours (using existing CEAP infrastructure)

#### 1.2 Create DynamoDB Tables
```bash
# Fraud scores table
aws dynamodb create-table \
  --table-name FraudScores \
  --attribute-definitions \
    AttributeName=transaction_id,AttributeType=S \
    AttributeName=date,AttributeType=S \
  --key-schema \
    AttributeName=transaction_id,KeyType=HASH \
    AttributeName=date,KeyType=RANGE \
  --billing-mode PAY_PER_REQUEST

# Model metadata table
aws dynamodb create-table \
  --table-name FraudModels \
  --attribute-definitions \
    AttributeName=model_version,AttributeType=S \
  --key-schema \
    AttributeName=model_version,KeyType=HASH \
  --billing-mode PAY_PER_REQUEST
```

**Deliverables**:
- ✅ FraudScores table (stores daily fraud scores)
- ✅ FraudModels table (tracks model versions and metrics)

**Effort**: 30 minutes

#### 1.3 Create S3 Buckets
```bash
# Transaction data lake
aws s3 mb s3://bank-transactions-728093470684

# Model artifacts
aws s3 mb s3://fraud-models-728093470684

# Daily reports
aws s3 mb s3://fraud-reports-728093470684
```

**Deliverables**:
- ✅ S3 buckets for data, models, and reports

**Effort**: 15 minutes

### Phase 2: Training Pipeline Implementation (3-5 days)

#### 2.1 Create Glue Scripts (2 days)

**File**: `infrastructure/glue-scripts/fraud_data_extract.py`
```python
# Extract 6 months of transaction data
# Join with fraud labels
# Output to S3
```

**File**: `infrastructure/glue-scripts/fraud_feature_engineering.py`
```python
# Calculate 25+ features:
# - Transaction velocity
# - Amount patterns
# - Merchant risk scores
# - Time-based features
# - Customer behavior
```

**Effort**: 2 days (including testing)

#### 2.2 Implement Training Lambda Handlers (2 days)

**File**: `ceap-workflow-train/src/main/kotlin/com/ceap/workflow/train/TrainModelHandler.kt`
```kotlin
class TrainModelHandler : WorkflowLambdaHandler() {
    override fun processData(input: JsonNode): JsonNode {
        // Start SageMaker training job
        val trainingJobName = "fraud-model-${System.currentTimeMillis()}"
        
        sageMakerClient.createTrainingJob(
            CreateTrainingJobRequest.builder()
                .trainingJobName(trainingJobName)
                .algorithmSpecification(AlgorithmSpecification.builder()
                    .trainingImage("xgboost-container-image")
                    .trainingInputMode("File")
                    .build())
                .roleArn(sageMakerRoleArn)
                .inputDataConfig(/* S3 paths from input */)
                .outputDataConfig(/* S3 output path */)
                .resourceConfig(ResourceConfig.builder()
                    .instanceType("ml.m5.xlarge")
                    .instanceCount(1)
                    .volumeSizeInGB(30)
                    .build())
                .hyperParameters(mapOf(
                    "max_depth" to "5",
                    "eta" to "0.1",
                    "objective" to "binary:logistic",
                    "num_round" to "100"
                ))
                .build()
        )
        
        // Wait for training completion
        // Return model artifacts location
    }
}
```

**File**: `ceap-workflow-train/src/main/kotlin/com/ceap/workflow/train/EvaluateModelHandler.kt`
```kotlin
class EvaluateModelHandler : WorkflowLambdaHandler() {
    override fun processData(input: JsonNode): JsonNode {
        // Load test dataset
        // Run predictions
        // Calculate metrics (precision, recall, F1, AUC)
        // Compare with previous model
        // Decide if deployment is recommended
    }
}
```

**File**: `ceap-workflow-train/src/main/kotlin/com/ceap/workflow/train/DeployModelHandler.kt`
```kotlin
class DeployModelHandler : WorkflowLambdaHandler() {
    override fun processData(input: JsonNode): JsonNode {
        // Create endpoint configuration
        // Create/update SageMaker endpoint
        // Wait for endpoint to be InService
        // Run smoke test
        // Return endpoint ARN
    }
}
```

**Effort**: 2 days (including testing)

#### 2.3 Upload Glue Scripts
```bash
aws s3 cp infrastructure/glue-scripts/fraud_data_extract.py \
  s3://ceap-glue-scripts-728093470684/scripts/

aws s3 cp infrastructure/glue-scripts/fraud_feature_engineering.py \
  s3://ceap-glue-scripts-728093470684/scripts/
```

**Effort**: 15 minutes

### Phase 3: Inference Pipeline Implementation (2-3 days)

#### 3.1 Implement Inference Lambda Handlers (2 days)

**File**: `ceap-workflow-fraud/src/main/kotlin/com/ceap/workflow/fraud/ExtractTransactionsHandler.kt`
```kotlin
class ExtractTransactionsHandler : WorkflowLambdaHandler() {
    override fun processData(input: JsonNode): JsonNode {
        // Read yesterday's transactions from S3
        // Parse Parquet files
        // Validate data quality
        // Return transactions
    }
}
```

**File**: `ceap-workflow-fraud/src/main/kotlin/com/ceap/workflow/fraud/ExtractFeaturesHandler.kt`
```kotlin
class ExtractFeaturesHandler : WorkflowLambdaHandler() {
    override fun processData(input: JsonNode): JsonNode {
        // Calculate features for each transaction
        // Use same feature engineering as training
        // Format for SageMaker inference
        // Return feature vectors
    }
}
```

**File**: `ceap-workflow-fraud/src/main/kotlin/com/ceap/workflow/fraud/ScoreFraudHandler.kt`
```kotlin
class ScoreFraudHandler : WorkflowLambdaHandler() {
    override fun processData(input: JsonNode): JsonNode {
        // Call SageMaker endpoint in batches
        val endpoint = "fraud-detection-model-v1"
        
        transactions.chunked(100).forEach { batch ->
            val response = sageMakerRuntimeClient.invokeEndpoint(
                InvokeEndpointRequest.builder()
                    .endpointName(endpoint)
                    .contentType("application/json")
                    .body(SdkBytes.fromUtf8String(toJson(batch)))
                    .build()
            )
            // Parse fraud scores
        }
        
        // Return scored transactions
    }
}
```

**File**: `ceap-workflow-fraud/src/main/kotlin/com/ceap/workflow/fraud/FilterHighRiskHandler.kt`
```kotlin
class FilterHighRiskHandler : WorkflowLambdaHandler() {
    override fun processData(input: JsonNode): JsonNode {
        // Filter fraud_score > 0.8
        // Rank by risk level
        // Add transaction details
        // Return high-risk transactions
    }
}
```

**File**: `ceap-workflow-fraud/src/main/kotlin/com/ceap/workflow/fraud/AlertStoreHandler.kt`
```kotlin
class AlertStoreHandler : WorkflowLambdaHandler() {
    override fun processData(input: JsonNode): JsonNode {
        // Send SNS alerts for URGENT cases
        highRiskTransactions.filter { it.score > 0.9 }.forEach { txn ->
            snsClient.publish(
                PublishRequest.builder()
                    .topicArn(alertTopicArn)
                    .subject("URGENT: High-Risk Transaction Detected")
                    .message("Transaction ${txn.id}: \$${txn.amount} - Score: ${txn.score}")
                    .build()
            )
        }
        
        // Store all scores in DynamoDB
        // Create daily report
        // Return summary
    }
}
```

**Effort**: 2 days (including testing)

#### 3.2 Create Gradle Modules
```bash
# Create new modules for fraud detection
mkdir -p ceap-workflow-train/src/main/kotlin/com/ceap/workflow/train
mkdir -p ceap-workflow-fraud/src/main/kotlin/com/ceap/workflow/fraud
```

**Add to `settings.gradle.kts`**:
```kotlin
include("ceap-workflow-train")
include("ceap-workflow-fraud")
```

**Create `ceap-workflow-train/build.gradle.kts`**:
```kotlin
plugins {
    id("com.github.johnrengelman.shadow")
}

dependencies {
    implementation(project(":ceap-common"))
    implementation(project(":ceap-models"))
    
    // AWS Lambda
    implementation("com.amazonaws:aws-lambda-java-core:1.2.3")
    
    // AWS SDK v2
    implementation("software.amazon.awssdk:sagemaker:2.20.26")
    implementation("software.amazon.awssdk:s3:2.20.26")
    
    // Jackson
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.2")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.15.2")
}

tasks.shadowJar {
    archiveClassifier.set("")
    mergeServiceFiles()
}
```

**Effort**: 1 hour

### Phase 4: SageMaker Setup (1-2 days)

#### 4.1 Prepare Training Data
```bash
# Upload historical transactions with labels
aws s3 cp historical_transactions.parquet \
  s3://bank-transactions-728093470684/labeled/

# Format: transaction_id, amount, merchant, timestamp, is_fraud, ...
```

**Effort**: 4 hours (data preparation and validation)

#### 4.2 Create SageMaker IAM Role
```bash
# Create role for SageMaker
aws iam create-role \
  --role-name SageMakerFraudDetectionRole \
  --assume-role-policy-document '{
    "Version": "2012-10-17",
    "Statement": [{
      "Effect": "Allow",
      "Principal": {"Service": "sagemaker.amazonaws.com"},
      "Action": "sts:AssumeRole"
    }]
  }'

# Attach policies
aws iam attach-role-policy \
  --role-name SageMakerFraudDetectionRole \
  --policy-arn arn:aws:iam::aws:policy/AmazonSageMakerFullAccess
```

**Effort**: 30 minutes

#### 4.3 Create Training Script (Optional - if using custom algorithm)
```python
# training_script.py
import pandas as pd
import xgboost as xgb
from sklearn.model_selection import train_test_split

# Load data
df = pd.read_parquet('/opt/ml/input/data/training/')

# Split features and labels
X = df.drop('is_fraud', axis=1)
y = df['is_fraud']

# Train model
model = xgb.XGBClassifier(
    max_depth=5,
    learning_rate=0.1,
    n_estimators=100,
    objective='binary:logistic'
)
model.fit(X, y)

# Save model
model.save_model('/opt/ml/model/xgboost-model')
```

**Effort**: 4 hours (if using custom algorithm; 0 hours if using built-in XGBoost)

### Phase 5: Testing & Validation (2-3 days)

#### 5.1 Test Training Pipeline
```bash
# Trigger training workflow
aws sqs send-message \
  --queue-url https://sqs.us-east-1.amazonaws.com/728093470684/ceap-workflow-fraud-training-queue \
  --message-body '{
    "training_data_path": "s3://bank-transactions-728093470684/labeled/",
    "model_version": "v1",
    "test": true
  }'

# Monitor execution
aws stepfunctions list-executions \
  --state-machine-arn arn:aws:states:...:stateMachine:Ceap-fraud-training-Workflow

# Verify SageMaker endpoint created
aws sagemaker describe-endpoint --endpoint-name fraud-detection-model-v1
```

**Effort**: 1 day

#### 5.2 Test Inference Pipeline
```bash
# Trigger inference workflow
aws sqs send-message \
  --queue-url https://sqs.us-east-1.amazonaws.com/728093470684/ceap-workflow-fraud-detection-queue \
  --message-body '{
    "date": "2026-02-01",
    "transaction_path": "s3://bank-transactions-728093470684/raw/2026-02-01/",
    "test": true
  }'

# Verify fraud scores in DynamoDB
aws dynamodb scan --table-name FraudScores --max-items 10

# Check alerts sent
aws sns list-subscriptions-by-topic --topic-arn arn:aws:sns:...:fraud-alerts
```

**Effort**: 1 day

#### 5.3 End-to-End Validation
- Run training pipeline with sample data
- Verify model deployed to SageMaker
- Run inference pipeline
- Verify fraud scores calculated
- Verify alerts sent for high-risk transactions

**Effort**: 1 day

### Phase 6: Production Deployment (1 day)

#### 6.1 Set Up Schedules
```bash
# Training: Weekly on Sunday 2 AM
aws events put-rule \
  --name fraud-training-weekly \
  --schedule-expression "cron(0 2 ? * SUN *)"

# Inference: Daily at 6 AM
aws events put-rule \
  --name fraud-detection-daily \
  --schedule-expression "cron(0 6 * * ? *)"
```

#### 6.2 Configure Monitoring
- CloudWatch dashboards for both pipelines
- Alarms for failures
- Alarms for high fraud rates
- Alarms for model drift

#### 6.3 Set Up Alerting
```bash
# Create SNS topic for fraud alerts
aws sns create-topic --name fraud-alerts

# Subscribe security team
aws sns subscribe \
  --topic-arn arn:aws:sns:...:fraud-alerts \
  --protocol email \
  --notification-endpoint security-team@company.com
```

**Effort**: 1 day

## Total Implementation Effort

| Phase | Tasks | Effort | Dependencies |
|-------|-------|--------|--------------|
| 1. Infrastructure | Deploy workflows, create tables | 1-2 days | CEAP infrastructure |
| 2. Training Pipeline | Glue scripts, Lambda handlers | 3-5 days | Phase 1 |
| 3. Inference Pipeline | Lambda handlers | 2-3 days | Phase 1 |
| 4. SageMaker Setup | Data prep, IAM, training | 1-2 days | Phase 2 |
| 5. Testing | E2E validation | 2-3 days | Phases 2-4 |
| 6. Production | Schedules, monitoring, alerts | 1 day | Phase 5 |

**Total**: 10-16 days (2-3 weeks)

**Team**: 1-2 developers

## What You Already Have ✅

From the CEAP infrastructure enhancement:
- ✅ Step Functions orchestration (Express + Standard)
- ✅ S3-based intermediate storage
- ✅ Lambda deployment infrastructure
- ✅ Glue job integration
- ✅ Retry logic and error handling
- ✅ CloudWatch monitoring
- ✅ Deployment scripts

**Reusable**: ~60% of the infrastructure is already built!

## What You Need to Build 🔨

### New Components (40% of work)

1. **Lambda Handlers** (5 for training, 5 for inference)
   - Effort: 4-5 days
   - Complexity: Medium
   - Reuses: WorkflowLambdaHandler base class

2. **Glue Scripts** (2 scripts)
   - Effort: 2 days
   - Complexity: Medium
   - Reuses: Glue job infrastructure

3. **SageMaker Integration**
   - Effort: 2 days
   - Complexity: Low (AWS SDK calls)
   - New: SageMaker training and inference

4. **DynamoDB Tables** (2 tables)
   - Effort: 1 hour
   - Complexity: Low
   - Reuses: Existing DynamoDB patterns

5. **Monitoring & Alerts**
   - Effort: 1 day
   - Complexity: Low
   - Reuses: CloudWatch infrastructure

## Quick Start Guide

### Option 1: Full Implementation (2-3 weeks)
Follow the complete roadmap above for production-ready system.

### Option 2: MVP (1 week)
Simplified version:
1. Deploy inference pipeline only (no training)
2. Use pre-trained model or AWS Fraud Detector
3. Skip Glue, use Lambda for all stages
4. Basic alerting via SNS

### Option 3: Proof of Concept (2-3 days)
Minimal version:
1. Single Lambda that calls SageMaker
2. Manual model deployment
3. Test with sample data
4. Validate approach before full build

## Recommended Approach

**Week 1**: Infrastructure + Training Pipeline
- Deploy workflows
- Implement Glue scripts
- Implement training Lambda handlers
- Test training pipeline

**Week 2**: Inference Pipeline + SageMaker
- Implement inference Lambda handlers
- Set up SageMaker endpoint
- Test inference pipeline
- End-to-end validation

**Week 3**: Production Deployment
- Set up schedules
- Configure monitoring
- Production testing
- Documentation

## Alternative: Use AWS Fraud Detector

If you want faster implementation, consider **AWS Fraud Detector** (managed service):
- No ML expertise required
- Pre-built fraud detection models
- Pay per prediction
- Faster to implement (1 week vs 3 weeks)

**Trade-offs**:
- Less customization
- Higher cost per prediction
- Less control over model

**Recommendation**: Use SageMaker for full control and lower long-term costs.

## Summary

This fraud detection system leverages your existing CEAP workflow infrastructure:
- ✅ 60% of infrastructure already built
- ✅ 10-16 days to implement
- ✅ $144/month operational cost
- ✅ Scalable to millions of transactions
- ✅ Production-ready architecture

The workflows you built (Express + Standard with Glue) are perfect for this use case!
