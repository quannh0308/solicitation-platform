# Resource Mapping: 7-Stack to 3-Stack Architecture
## CEAP Infrastructure Consolidation

**Date:** 2025-01-27  
**Purpose:** Detailed mapping of every resource from source stacks to target stacks

---

## Mapping Overview

| Source Stack | Target Stack | Resource Count | Migration Complexity |
|--------------|--------------|----------------|---------------------|
| CeapDatabase-dev | CeapDatabase-dev | 3 tables | NONE (unchanged) |
| CeapEtlWorkflow-dev | CeapDataPlatform-dev | 1 Lambda | LOW |
| CeapFilterWorkflow-dev | CeapDataPlatform-dev | 1 Lambda | LOW |
| CeapScoreWorkflow-dev | CeapDataPlatform-dev | 1 Lambda | LOW |
| CeapStoreWorkflow-dev | CeapDataPlatform-dev | 1 Lambda | LOW |
| CeapOrchestration-dev | CeapDataPlatform-dev | 1 State Machine, 1 Rule | MEDIUM |
| CeapReactiveWorkflow-dev | CeapServingAPI-dev | 1 Lambda, 1 Table, 1 Rule | LOW |

---

## Detailed Resource Mapping

### Source: CeapDatabase-dev → Target: CeapDatabase-dev

**Status:** UNCHANGED (Requirement 1.2)

| Resource Logical ID | Resource Type | Physical Name | Target Logical ID | Notes |
|---------------------|---------------|---------------|-------------------|-------|
| CandidatesTable | AWS::DynamoDB::Table | Candidates-dev | CandidatesTable | No changes |
| ProgramConfigTable | AWS::DynamoDB::Table | ProgramConfig-dev | ProgramConfigTable | No changes |
| ScoreCacheTable | AWS::DynamoDB::Table | ScoreCache-dev | ScoreCacheTable | No changes |

**Dependencies:** None  
**Cross-Stack References:** Referenced by all workflow stacks

---

### Source: CeapEtlWorkflow-dev → Target: CeapDataPlatform-dev

| Resource Logical ID | Resource Type | Physical Name Pattern | Target Logical ID | Preserve Name? |
|---------------------|---------------|----------------------|-------------------|----------------|
| ETLLambda | AWS::Lambda::Function | CeapEtlWorkflow-dev-ETLLambda-* | ETLLambda | YES (Req 8.1) |
| ETLLambdaRole | AWS::IAM::Role | CeapEtlWorkflow-dev-ETLLambdaRole-* | ETLLambdaRole | YES |
| ETLLambdaLogGroup | AWS::Logs::LogGroup | /aws/lambda/CeapEtlWorkflow-dev-ETLLambda-* | ETLLambdaLogGroup | Auto-generated |

**Dependencies:**
- DatabaseStack.CandidatesTable (table name in env var)
- DatabaseStack.ProgramConfigTable (table name in env var)

**Environment Variables:**
- ENVIRONMENT: dev
- CANDIDATES_TABLE: Candidates-dev
- PROGRAM_CONFIG_TABLE: ProgramConfig-dev
- BATCH_SIZE: 1000
- LOG_LEVEL: INFO

**IAM Permissions:**
- dynamodb:GetItem, PutItem, Query, Scan on CandidatesTable
- dynamodb:GetItem, Query on ProgramConfigTable
- logs:CreateLogGroup, CreateLogStream, PutLogEvents

**Migration Notes:**
- Copy Lambda definition exactly
- Update table references to use Fn::ImportValue if needed
- Preserve logical ID "ETLLambda"

---

### Source: CeapFilterWorkflow-dev → Target: CeapDataPlatform-dev

| Resource Logical ID | Resource Type | Physical Name Pattern | Target Logical ID | Preserve Name? |
|---------------------|---------------|----------------------|-------------------|----------------|
| FilterLambda | AWS::Lambda::Function | CeapFilterWorkflow-dev-FilterLambda-* | FilterLambda | YES (Req 8.1) |
| FilterLambdaRole | AWS::IAM::Role | CeapFilterWorkflow-dev-FilterLambdaRole-* | FilterLambdaRole | YES |
| FilterLambdaLogGroup | AWS::Logs::LogGroup | /aws/lambda/CeapFilterWorkflow-dev-FilterLambda-* | FilterLambdaLogGroup | Auto-generated |

**Dependencies:**
- DatabaseStack.ProgramConfigTable (table name in env var)

**Environment Variables:**
- ENVIRONMENT: dev
- PROGRAM_CONFIG_TABLE: ProgramConfig-dev
- LOG_LEVEL: INFO

**IAM Permissions:**
- dynamodb:GetItem, Query on ProgramConfigTable
- logs:CreateLogGroup, CreateLogStream, PutLogEvents

**Migration Notes:**
- Copy Lambda definition exactly
- Update table references to use Fn::ImportValue if needed
- Preserve logical ID "FilterLambda"

---

### Source: CeapScoreWorkflow-dev → Target: CeapDataPlatform-dev

| Resource Logical ID | Resource Type | Physical Name Pattern | Target Logical ID | Preserve Name? |
|---------------------|---------------|----------------------|-------------------|----------------|
| ScoreLambda | AWS::Lambda::Function | CeapScoreWorkflow-dev-ScoreLambda-* | ScoreLambda | YES (Req 8.1) |
| ScoreLambdaRole | AWS::IAM::Role | CeapScoreWorkflow-dev-ScoreLambdaRole-* | ScoreLambdaRole | YES |
| ScoreLambdaLogGroup | AWS::Logs::LogGroup | /aws/lambda/CeapScoreWorkflow-dev-ScoreLambda-* | ScoreLambdaLogGroup | Auto-generated |

**Dependencies:**
- DatabaseStack.ScoreCacheTable (table name in env var)

**Environment Variables:**
- ENVIRONMENT: dev
- SCORE_CACHE_TABLE: ScoreCache-dev
- CACHE_TTL_HOURS: 24
- LOG_LEVEL: INFO

**IAM Permissions:**
- dynamodb:GetItem, PutItem, Query on ScoreCacheTable
- logs:CreateLogGroup, CreateLogStream, PutLogEvents

**Migration Notes:**
- Copy Lambda definition exactly
- Update table references to use Fn::ImportValue if needed
- Preserve logical ID "ScoreLambda"
- TODO: Add SageMaker permissions when implementing scoring engine

---

### Source: CeapStoreWorkflow-dev → Target: CeapDataPlatform-dev

| Resource Logical ID | Resource Type | Physical Name Pattern | Target Logical ID | Preserve Name? |
|---------------------|---------------|----------------------|-------------------|----------------|
| StoreLambda | AWS::Lambda::Function | CeapStoreWorkflow-dev-StoreLambda-* | StoreLambda | YES (Req 8.1) |
| StoreLambdaRole | AWS::IAM::Role | CeapStoreWorkflow-dev-StoreLambdaRole-* | StoreLambdaRole | YES |
| StoreLambdaLogGroup | AWS::Logs::LogGroup | /aws/lambda/CeapStoreWorkflow-dev-StoreLambda-* | StoreLambdaLogGroup | Auto-generated |

**Dependencies:**
- DatabaseStack.CandidatesTable (table name in env var)

**Environment Variables:**
- ENVIRONMENT: dev
- CANDIDATES_TABLE: Candidates-dev
- BATCH_SIZE: 25
- LOG_LEVEL: INFO

**IAM Permissions:**
- dynamodb:PutItem, BatchWriteItem on CandidatesTable
- logs:CreateLogGroup, CreateLogStream, PutLogEvents

**Migration Notes:**
- Copy Lambda definition exactly
- Update table references to use Fn::ImportValue if needed
- Preserve logical ID "StoreLambda"

---

### Source: CeapOrchestration-dev → Target: CeapDataPlatform-dev

| Resource Logical ID | Resource Type | Physical Name | Target Logical ID | Preserve Name? |
|---------------------|---------------|---------------|-------------------|----------------|
| BatchIngestionWorkflow | AWS::StepFunctions::StateMachine | CeapBatchIngestion-dev | BatchIngestionWorkflow | YES (Req 8.1) |
| BatchIngestionWorkflowRole | AWS::IAM::Role | CeapOrchestration-dev-BatchIngestionWorkflowRole-* | BatchIngestionWorkflowRole | YES |
| BatchIngestionSchedule | AWS::Events::Rule | CeapBatchIngestion-dev | BatchIngestionSchedule | YES (Req 8.1) |
| ETLTask | Step Functions Task State | N/A (embedded in definition) | ETLTask | YES |
| FilterTask | Step Functions Task State | N/A (embedded in definition) | FilterTask | YES |
| ScoreTask | Step Functions Task State | N/A (embedded in definition) | ScoreTask | YES |
| StoreTask | Step Functions Task State | N/A (embedded in definition) | StoreTask | YES |
| ETLFailed | Step Functions Fail State | N/A (embedded in definition) | ETLFailed | YES |
| FilterFailed | Step Functions Fail State | N/A (embedded in definition) | FilterFailed | YES |
| ScoreFailed | Step Functions Fail State | N/A (embedded in definition) | ScoreFailed | YES |
| StoreFailed | Step Functions Fail State | N/A (embedded in definition) | StoreFailed | YES |
| WorkflowSuccess | Step Functions Succeed State | N/A (embedded in definition) | WorkflowSuccess | YES |

**Dependencies:**
- EtlWorkflowStack.ETLLambda (Lambda ARN in Step Functions definition)
- FilterWorkflowStack.FilterLambda (Lambda ARN in Step Functions definition)
- ScoreWorkflowStack.ScoreLambda (Lambda ARN in Step Functions definition)
- StoreWorkflowStack.StoreLambda (Lambda ARN in Step Functions definition)

**Step Functions Definition Structure:**
```
ETLTask (LambdaInvoke)
  → Retry: 5 attempts, exponential backoff (1s, 2s, 4s, 8s, 16s)
  → Catch: ETLFailed
  → Next: FilterTask

FilterTask (LambdaInvoke)
  → Retry: 5 attempts, exponential backoff
  → Catch: FilterFailed
  → Next: ScoreTask

ScoreTask (LambdaInvoke)
  → Retry: 5 attempts, exponential backoff
  → Catch: ScoreFailed
  → Next: StoreTask

StoreTask (LambdaInvoke)
  → Retry: 5 attempts, exponential backoff
  → Catch: StoreFailed
  → Next: WorkflowSuccess

WorkflowSuccess (Succeed)
```

**EventBridge Schedule:**
- Schedule Expression: cron(0 2 * * ? *)
- Description: Daily at 2 AM UTC
- Target: BatchIngestionWorkflow

**IAM Permissions:**
- lambda:InvokeFunction on ETLLambda, FilterLambda, ScoreLambda, StoreLambda
- states:StartExecution on self (for nested executions if needed)
- logs:CreateLogGroup, CreateLogStream, PutLogEvents

**Migration Notes:**
- **CRITICAL:** Preserve exact Step Functions definition structure
- All Lambda references must point to Lambdas in same stack (CeapDataPlatform-dev)
- Preserve all retry configurations (5 attempts, exponential backoff)
- Preserve all error handling (Catch blocks)
- Preserve timeout (4 hours)
- Update Lambda ARN references to use Fn::GetAtt within same stack

---

### Source: CeapReactiveWorkflow-dev → Target: CeapServingAPI-dev

| Resource Logical ID | Resource Type | Physical Name | Target Logical ID | Preserve Name? |
|---------------------|---------------|---------------|-------------------|----------------|
| ReactiveLambda | AWS::Lambda::Function | CeapReactiveWorkflow-dev-ReactiveLambda-* | ReactiveLambda | YES (Req 8.2) |
| ReactiveLambdaRole | AWS::IAM::Role | CeapReactiveWorkflow-dev-ReactiveLambdaRole-* | ReactiveLambdaRole | YES |
| ReactiveLambdaLogGroup | AWS::Logs::LogGroup | /aws/lambda/CeapReactiveWorkflow-dev-ReactiveLambda-* | ReactiveLambdaLogGroup | Auto-generated |
| DeduplicationTable | AWS::DynamoDB::Table | ceap-event-deduplication-dev | DeduplicationTable | YES (Req 8.2) |
| CustomerEventRule | AWS::Events::Rule | ceap-customer-events-dev | CustomerEventRule | YES (Req 8.2) |

**Dependencies:**
- DatabaseStack.CandidatesTable (table name in env var)
- DatabaseStack.ProgramConfigTable (table name in env var)
- DatabaseStack.ScoreCacheTable (table name in env var)
- DeduplicationTable (table name in env var, same stack)

**Environment Variables:**
- ENVIRONMENT: dev
- CANDIDATES_TABLE: Candidates-dev
- PROGRAM_CONFIG_TABLE: ProgramConfig-dev
- SCORE_CACHE_TABLE: ScoreCache-dev
- DEDUPLICATION_TABLE: ceap-event-deduplication-dev
- LOG_LEVEL: INFO

**IAM Permissions:**
- dynamodb:GetItem, PutItem, Query on CandidatesTable, ProgramConfigTable, ScoreCacheTable, DeduplicationTable
- logs:CreateLogGroup, CreateLogStream, PutLogEvents
- Lambda invocation permission from EventBridge

**EventBridge Rule:**
- Event Pattern:
  - Source: ceap.customer-events
  - DetailType: OrderDelivered, ProductPurchased, VideoWatched, TrackPlayed, ServiceCompleted, EventAttended
- Target: ReactiveLambda

**Migration Notes:**
- Copy Lambda definition exactly
- Copy DeduplicationTable definition exactly
- Copy CustomerEventRule definition exactly
- Update database table references to use Fn::ImportValue from CeapDatabase-dev
- DeduplicationTable stays in same stack as ReactiveLambda
- Preserve all logical IDs

---

## Cross-Stack Reference Migration

### Current State (7 Stacks)
All references use **direct CDK object references** within the same CDK app:
```kotlin
// Example: Lambda referencing table
environment = mapOf(
    "CANDIDATES_TABLE" to databaseStack.candidatesTable.tableName
)

// Example: Step Functions referencing Lambda
val etlTask = LambdaInvoke.Builder.create(this, "ETLTask")
    .lambdaFunction(etlStack.etlLambda.function)
    .build()
```

### Target State (3 Stacks)

#### Within CeapDataPlatform-dev (same stack)
- ETL, Filter, Score, Store Lambdas → BatchIngestionWorkflow: **Direct references** (same stack)
- Lambdas → Database tables: **Fn::ImportValue** (cross-stack)

#### Within CeapServingAPI-dev (same stack)
- ReactiveLambda → DeduplicationTable: **Direct reference** (same stack)
- ReactiveLambda → Database tables: **Fn::ImportValue** (cross-stack)

#### Required CloudFormation Exports

**From CeapDatabase-dev:**
```yaml
Outputs:
  CandidatesTableName:
    Value: !Ref CandidatesTable
    Export:
      Name: !Sub "${AWS::StackName}-CandidatesTableName"
  
  CandidatesTableArn:
    Value: !GetAtt CandidatesTable.Arn
    Export:
      Name: !Sub "${AWS::StackName}-CandidatesTableArn"
  
  ProgramConfigTableName:
    Value: !Ref ProgramConfigTable
    Export:
      Name: !Sub "${AWS::StackName}-ProgramConfigTableName"
  
  ProgramConfigTableArn:
    Value: !GetAtt ProgramConfigTable.Arn
    Export:
      Name: !Sub "${AWS::StackName}-ProgramConfigTableArn"
  
  ScoreCacheTableName:
    Value: !Ref ScoreCacheTable
    Export:
      Name: !Sub "${AWS::StackName}-ScoreCacheTableName"
  
  ScoreCacheTableArn:
    Value: !GetAtt ScoreCacheTable.Arn
    Export:
      Name: !Sub "${AWS::StackName}-ScoreCacheTableArn"
```

**From CeapDataPlatform-dev:**
```yaml
Outputs:
  BatchIngestionWorkflowArn:
    Value: !GetAtt BatchIngestionWorkflow.Arn
    Export:
      Name: !Sub "${AWS::StackName}-BatchIngestionWorkflowArn"
```

**Import in CeapDataPlatform-dev:**
```yaml
Parameters:
  DatabaseStackName:
    Type: String
    Default: CeapDatabase-dev
    Description: Name of the database stack

Resources:
  ETLLambda:
    Type: AWS::Lambda::Function
    Properties:
      Environment:
        Variables:
          CANDIDATES_TABLE:
            Fn::ImportValue: !Sub "${DatabaseStackName}-CandidatesTableName"
```

**Import in CeapServingAPI-dev:**
```yaml
Parameters:
  DatabaseStackName:
    Type: String
    Default: CeapDatabase-dev
    Description: Name of the database stack
  
  DataPlatformStackName:
    Type: String
    Default: CeapDataPlatform-dev
    Description: Name of the data platform stack (for future use)

Resources:
  ReactiveLambda:
    Type: AWS::Lambda::Function
    Properties:
      Environment:
        Variables:
          CANDIDATES_TABLE:
            Fn::ImportValue: !Sub "${DatabaseStackName}-CandidatesTableName"
```

---

## Migration Validation Checklist

### Resource Count Validation
- [ ] 5 Lambda Functions mapped (ETL, Filter, Score, Store, Reactive)
- [ ] 4 DynamoDB Tables mapped (3 in Database, 1 in Reactive)
- [ ] 1 Step Functions State Machine mapped
- [ ] 2 EventBridge Rules mapped
- [ ] All IAM Roles mapped (auto-generated by CDK)
- [ ] All CloudWatch Log Groups mapped (auto-generated)

### Logical ID Preservation (Requirement 8.1, 8.2)
- [ ] ETLLambda preserved
- [ ] FilterLambda preserved
- [ ] ScoreLambda preserved
- [ ] StoreLambda preserved
- [ ] ReactiveLambda preserved
- [ ] BatchIngestionWorkflow preserved
- [ ] BatchIngestionSchedule preserved
- [ ] CustomerEventRule preserved
- [ ] DeduplicationTable preserved
- [ ] All Step Functions state names preserved

### Configuration Preservation (Requirement 2.1-2.5)
- [ ] All Lambda handlers preserved
- [ ] All Lambda memory sizes preserved
- [ ] All Lambda timeouts preserved
- [ ] All environment variables preserved
- [ ] All IAM permissions preserved
- [ ] All retry configurations preserved
- [ ] All error handling preserved
- [ ] All EventBridge patterns preserved
- [ ] All DynamoDB configurations preserved

### Cross-Stack Reference Migration (Requirement 10.1-10.4)
- [ ] CloudFormation exports added to CeapDatabase-dev
- [ ] Fn::ImportValue used in CeapDataPlatform-dev for database tables
- [ ] Fn::ImportValue used in CeapServingAPI-dev for database tables
- [ ] Parameters added for stack name references
- [ ] All references validated during deployment

---

## Deployment Order

### Phase 1: Deploy CeapDatabase-dev
- Status: Already deployed (unchanged)
- Action: Add CloudFormation exports via stack update
- Validation: Verify exports are created

### Phase 2: Deploy CeapDataPlatform-dev
- Dependencies: CeapDatabase-dev (must be deployed first)
- Action: Deploy new consolidated stack
- Validation: Verify all resources created, imports resolved

### Phase 3: Deploy CeapServingAPI-dev
- Dependencies: CeapDatabase-dev (must be deployed first)
- Action: Deploy new consolidated stack
- Validation: Verify all resources created, imports resolved

### Phase 4: Cleanup Old Stacks
- Action: Delete CeapEtlWorkflow-dev, CeapFilterWorkflow-dev, CeapScoreWorkflow-dev, CeapStoreWorkflow-dev, CeapOrchestration-dev, CeapReactiveWorkflow-dev
- Validation: Verify new stacks functioning before deletion (Requirement 5.3)

---

**End of Resource Mapping**
