# Cross-Stack Dependencies Analysis
## CEAP Infrastructure Consolidation

**Date:** 2025-01-27  
**Purpose:** Document all cross-stack references and dependencies

---

## Current Architecture Dependencies (7 Stacks)

### Dependency Graph

```
                    CeapDatabase-dev
                           |
        +------------------+------------------+
        |                  |                  |
        v                  v                  v
CeapEtlWorkflow-dev  CeapFilterWorkflow-dev  CeapScoreWorkflow-dev
        |                  |                  |
        +------------------+------------------+
                           |
                           v
                  CeapStoreWorkflow-dev
                           |
        +------------------+------------------+
        |                                     |
        v                                     v
CeapReactiveWorkflow-dev          CeapOrchestration-dev
        |                                     |
        +-------------------------------------+
```

### Dependency Matrix

| Stack | Depends On | Referenced By | Dependency Type |
|-------|------------|---------------|-----------------|
| CeapDatabase-dev | None | All workflow stacks | Direct CDK reference |
| CeapEtlWorkflow-dev | DatabaseStack | OrchestrationStack | Direct CDK reference |
| CeapFilterWorkflow-dev | DatabaseStack | OrchestrationStack | Direct CDK reference |
| CeapScoreWorkflow-dev | DatabaseStack | OrchestrationStack | Direct CDK reference |
| CeapStoreWorkflow-dev | DatabaseStack | OrchestrationStack | Direct CDK reference |
| CeapReactiveWorkflow-dev | DatabaseStack | OrchestrationStack (parameter only) | Direct CDK reference |
| CeapOrchestration-dev | All workflow stacks | None | Direct CDK reference |

---

## Detailed Dependency Analysis

### 1. CeapDatabase-dev Dependencies

**Depends On:** None (base layer)

**Referenced By:**
- CeapEtlWorkflow-dev
  - CandidatesTable (table name in Lambda env var)
  - ProgramConfigTable (table name in Lambda env var)
  - IAM permissions (read/write on CandidatesTable, read on ProgramConfigTable)

- CeapFilterWorkflow-dev
  - ProgramConfigTable (table name in Lambda env var)
  - IAM permissions (read on ProgramConfigTable)

- CeapScoreWorkflow-dev
  - ScoreCacheTable (table name in Lambda env var)
  - IAM permissions (read/write on ScoreCacheTable)

- CeapStoreWorkflow-dev
  - CandidatesTable (table name in Lambda env var)
  - IAM permissions (write on CandidatesTable)

- CeapReactiveWorkflow-dev
  - CandidatesTable (table name in Lambda env var)
  - ProgramConfigTable (table name in Lambda env var)
  - ScoreCacheTable (table name in Lambda env var)
  - IAM permissions (read/write on all three tables)

**Reference Mechanism:**
```kotlin
// In workflow stack constructors
class EtlWorkflowStack(
    scope: Construct,
    id: String,
    props: StackProps,
    val envName: String,
    val databaseStack: DatabaseStack  // Direct CDK reference
) : Stack(scope, id, props) {
    val etlLambda = CeapLambda(
        this,
        "ETLLambda",
        environment = mapOf(
            "CANDIDATES_TABLE" to databaseStack.candidatesTable.tableName,
            "PROGRAM_CONFIG_TABLE" to databaseStack.programConfigTable.tableName
        ),
        tables = listOf(
            databaseStack.candidatesTable,  // Grants IAM permissions
            databaseStack.programConfigTable
        )
    )
}
```

---

### 2. CeapEtlWorkflow-dev Dependencies

**Depends On:**
- CeapDatabase-dev
  - CandidatesTable (table name, IAM permissions)
  - ProgramConfigTable (table name, IAM permissions)

**Referenced By:**
- CeapOrchestration-dev
  - ETLLambda function (invoked by Step Functions ETLTask)

**Reference Mechanism:**
```kotlin
// In OrchestrationStack
class OrchestrationStack(
    scope: Construct,
    id: String,
    props: StackProps,
    val envName: String,
    val etlStack: EtlWorkflowStack  // Direct CDK reference
) : Stack(scope, id, props) {
    val etlTask = LambdaInvoke.Builder.create(this, "ETLTask")
        .lambdaFunction(etlStack.etlLambda.function)  // Direct reference
        .build()
}
```

---

### 3. CeapFilterWorkflow-dev Dependencies

**Depends On:**
- CeapDatabase-dev
  - ProgramConfigTable (table name, IAM permissions)

**Referenced By:**
- CeapOrchestration-dev
  - FilterLambda function (invoked by Step Functions FilterTask)

**Reference Mechanism:** Same as EtlWorkflow (direct CDK reference)

---

### 4. CeapScoreWorkflow-dev Dependencies

**Depends On:**
- CeapDatabase-dev
  - ScoreCacheTable (table name, IAM permissions)

**Referenced By:**
- CeapOrchestration-dev
  - ScoreLambda function (invoked by Step Functions ScoreTask)

**Reference Mechanism:** Same as EtlWorkflow (direct CDK reference)

---

### 5. CeapStoreWorkflow-dev Dependencies

**Depends On:**
- CeapDatabase-dev
  - CandidatesTable (table name, IAM permissions)

**Referenced By:**
- CeapOrchestration-dev
  - StoreLambda function (invoked by Step Functions StoreTask)

**Reference Mechanism:** Same as EtlWorkflow (direct CDK reference)

---

### 6. CeapReactiveWorkflow-dev Dependencies

**Depends On:**
- CeapDatabase-dev
  - CandidatesTable (table name, IAM permissions)
  - ProgramConfigTable (table name, IAM permissions)
  - ScoreCacheTable (table name, IAM permissions)

**Referenced By:**
- CeapOrchestration-dev (parameter only, not actively used)

**Reference Mechanism:** Same as EtlWorkflow (direct CDK reference)

**Note:** ReactiveStack is passed to OrchestrationStack but not currently used in the workflow definition.

---

### 7. CeapOrchestration-dev Dependencies

**Depends On:**
- CeapEtlWorkflow-dev (ETLLambda function)
- CeapFilterWorkflow-dev (FilterLambda function)
- CeapScoreWorkflow-dev (ScoreLambda function)
- CeapStoreWorkflow-dev (StoreLambda function)
- CeapReactiveWorkflow-dev (parameter only, not used)
- CeapDatabase-dev (indirectly via Lambda functions)

**Referenced By:** None

**Reference Mechanism:** Direct CDK references to Lambda functions

---

## Target Architecture Dependencies (3 Stacks)

### Dependency Graph

```
        CeapDatabase-dev
               |
        +------+------+
        |             |
        v             v
CeapDataPlatform-dev  CeapServingAPI-dev
```

### Dependency Matrix

| Stack | Depends On | Referenced By | Dependency Type |
|-------|------------|---------------|-----------------|
| CeapDatabase-dev | None | DataPlatform, ServingAPI | CloudFormation Export |
| CeapDataPlatform-dev | DatabaseStack | ServingAPI (future) | CloudFormation Export |
| CeapServingAPI-dev | DatabaseStack | None | CloudFormation Export |

---

## Migration Strategy for Dependencies

### Phase 1: Add CloudFormation Exports to CeapDatabase-dev

**Current State:** No CloudFormation exports (uses direct CDK references)

**Target State:** Export table names and ARNs

**Required Exports:**
```yaml
Outputs:
  CandidatesTableName:
    Description: Name of the Candidates DynamoDB table
    Value: !Ref CandidatesTable
    Export:
      Name: !Sub "${AWS::StackName}-CandidatesTableName"
  
  CandidatesTableArn:
    Description: ARN of the Candidates DynamoDB table
    Value: !GetAtt CandidatesTable.Arn
    Export:
      Name: !Sub "${AWS::StackName}-CandidatesTableArn"
  
  ProgramConfigTableName:
    Description: Name of the ProgramConfig DynamoDB table
    Value: !Ref ProgramConfigTable
    Export:
      Name: !Sub "${AWS::StackName}-ProgramConfigTableName"
  
  ProgramConfigTableArn:
    Description: ARN of the ProgramConfig DynamoDB table
    Value: !GetAtt ProgramConfigTable.Arn
    Export:
      Name: !Sub "${AWS::StackName}-ProgramConfigTableArn"
  
  ScoreCacheTableName:
    Description: Name of the ScoreCache DynamoDB table
    Value: !Ref ScoreCacheTable
    Export:
      Name: !Sub "${AWS::StackName}-ScoreCacheTableName"
  
  ScoreCacheTableArn:
    Description: ARN of the ScoreCache DynamoDB table
    Value: !GetAtt ScoreCacheTable.Arn
    Export:
      Name: !Sub "${AWS::StackName}-ScoreCacheTableArn"
```

**CDK Implementation:**
```kotlin
// In DatabaseStack
init {
    // Export table names and ARNs
    CfnOutput.Builder.create(this, "CandidatesTableNameOutput")
        .value(candidatesTable.tableName)
        .exportName("$stackName-CandidatesTableName")
        .description("Name of the Candidates DynamoDB table")
        .build()
    
    CfnOutput.Builder.create(this, "CandidatesTableArnOutput")
        .value(candidatesTable.tableArn)
        .exportName("$stackName-CandidatesTableArn")
        .description("ARN of the Candidates DynamoDB table")
        .build()
    
    // Repeat for ProgramConfigTable and ScoreCacheTable
}
```

---

### Phase 2: Update CeapDataPlatform-dev to Use Imports

**Current State:** Direct CDK references to DatabaseStack

**Target State:** CloudFormation imports via Fn::ImportValue

**Required Parameters:**
```yaml
Parameters:
  DatabaseStackName:
    Type: String
    Default: CeapDatabase-dev
    Description: Name of the database stack for cross-stack references
```

**Import Pattern:**
```yaml
Resources:
  ETLLambda:
    Type: AWS::Lambda::Function
    Properties:
      Environment:
        Variables:
          CANDIDATES_TABLE:
            Fn::ImportValue: !Sub "${DatabaseStackName}-CandidatesTableName"
          PROGRAM_CONFIG_TABLE:
            Fn::ImportValue: !Sub "${DatabaseStackName}-ProgramConfigTableName"
  
  ETLLambdaRole:
    Type: AWS::IAM::Role
    Properties:
      Policies:
        - PolicyName: DynamoDBAccess
          PolicyDocument:
            Statement:
              - Effect: Allow
                Action:
                  - dynamodb:GetItem
                  - dynamodb:PutItem
                  - dynamodb:Query
                  - dynamodb:Scan
                Resource:
                  Fn::ImportValue: !Sub "${DatabaseStackName}-CandidatesTableArn"
```

**CDK Implementation:**
```kotlin
// In CeapDataPlatform-dev stack
class DataPlatformStack(
    scope: Construct,
    id: String,
    props: StackProps,
    val envName: String
) : Stack(scope, id, props) {
    
    // Add parameter for database stack name
    val databaseStackName = CfnParameter.Builder.create(this, "DatabaseStackName")
        .type("String")
        .default("CeapDatabase-$envName")
        .description("Name of the database stack for cross-stack references")
        .build()
    
    // Import table names
    val candidatesTableName = Fn.importValue("${databaseStackName.valueAsString}-CandidatesTableName")
    val candidatesTableArn = Fn.importValue("${databaseStackName.valueAsString}-CandidatesTableArn")
    
    // Use in Lambda environment
    val etlLambda = Function.Builder.create(this, "ETLLambda")
        .environment(mapOf(
            "CANDIDATES_TABLE" to candidatesTableName
        ))
        .build()
    
    // Grant IAM permissions using ARN
    etlLambda.addToRolePolicy(
        PolicyStatement.Builder.create()
            .actions(listOf("dynamodb:GetItem", "dynamodb:PutItem"))
            .resources(listOf(candidatesTableArn))
            .build()
    )
}
```

---

### Phase 3: Update CeapServingAPI-dev to Use Imports

**Current State:** Direct CDK references to DatabaseStack

**Target State:** CloudFormation imports via Fn::ImportValue

**Required Parameters:**
```yaml
Parameters:
  DatabaseStackName:
    Type: String
    Default: CeapDatabase-dev
    Description: Name of the database stack for cross-stack references
  
  DataPlatformStackName:
    Type: String
    Default: CeapDataPlatform-dev
    Description: Name of the data platform stack (for future cross-stack references)
```

**Import Pattern:** Same as CeapDataPlatform-dev

**CDK Implementation:** Same pattern as DataPlatformStack

---

## Validation Requirements

### Cross-Stack Reference Validation (Requirement 10.4)

**Validation Points:**
1. **Template Validation:** CloudFormation validates exports exist during deployment
2. **Deployment Order:** Database stack must be deployed before dependent stacks
3. **Export Naming:** Consistent naming pattern for all exports
4. **Import Resolution:** All Fn::ImportValue references resolve correctly

**Validation Script:**
```bash
#!/bin/bash
# Validate cross-stack references

# Check if database stack exports exist
aws cloudformation list-exports --query "Exports[?Name.contains(@, 'CeapDatabase-dev')]"

# Validate imports in data platform stack
aws cloudformation validate-template --template-body file://CeapDataPlatform-dev.template.json

# Validate imports in serving API stack
aws cloudformation validate-template --template-body file://CeapServingAPI-dev.template.json
```

---

## Rollback Considerations

### Export Deletion Protection

**Issue:** CloudFormation exports cannot be deleted if they are being imported by other stacks

**Solution:**
1. Delete dependent stacks first (CeapDataPlatform-dev, CeapServingAPI-dev)
2. Then delete or update CeapDatabase-dev to remove exports
3. Or keep exports in place for future use

### Rollback to 7-Stack Architecture

**Steps:**
1. Delete new 3-stack architecture (CeapDataPlatform-dev, CeapServingAPI-dev)
2. Remove exports from CeapDatabase-dev (optional)
3. Redeploy original 7 stacks from backup templates
4. Verify all cross-stack references work with direct CDK references

---

## Future Cross-Stack References

### Potential Future Dependencies

**CeapServingAPI-dev → CeapDataPlatform-dev:**
- Scenario: Serving API needs to trigger batch ingestion workflow
- Reference: BatchIngestionWorkflow ARN
- Implementation: Export from DataPlatform, import in ServingAPI

**Example Export:**
```yaml
# In CeapDataPlatform-dev
Outputs:
  BatchIngestionWorkflowArn:
    Description: ARN of the batch ingestion Step Functions workflow
    Value: !GetAtt BatchIngestionWorkflow.Arn
    Export:
      Name: !Sub "${AWS::StackName}-BatchIngestionWorkflowArn"
```

**Example Import:**
```yaml
# In CeapServingAPI-dev
Resources:
  TriggerBatchIngestionLambda:
    Type: AWS::Lambda::Function
    Properties:
      Environment:
        Variables:
          BATCH_WORKFLOW_ARN:
            Fn::ImportValue: !Sub "${DataPlatformStackName}-BatchIngestionWorkflowArn"
```

---

## Summary

### Current State (7 Stacks)
- **Reference Type:** Direct CDK object references
- **Deployment:** All stacks in same CDK app
- **Flexibility:** High (easy to refactor)
- **CloudFormation Exports:** None

### Target State (3 Stacks)
- **Reference Type:** CloudFormation exports + imports
- **Deployment:** Sequential (Database → DataPlatform/ServingAPI)
- **Flexibility:** Medium (exports create dependencies)
- **CloudFormation Exports:** 6 from Database, 1+ from DataPlatform

### Migration Complexity
- **Low:** Adding exports to DatabaseStack
- **Medium:** Converting direct references to imports in DataPlatform and ServingAPI
- **Low:** Validation and testing

---

**End of Cross-Stack Dependencies Analysis**
