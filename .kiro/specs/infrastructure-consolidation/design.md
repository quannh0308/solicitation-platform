# Design Document: Infrastructure Consolidation

## Overview

This design consolidates the CEAP platform infrastructure from 7 CloudFormation stacks to 3 stacks with clear business-aligned separation. The consolidation groups resources by their role in the data lifecycle: storage (database), write path (data ingestion and processing), and read path (real-time serving).

### Current Architecture (7 Stacks)

1. **CeapDatabase-dev** - DynamoDB tables and database resources
2. **CeapEtlWorkflow-dev** - ETL Lambda functions and Step Functions
3. **CeapFilterWorkflow-dev** - Filtering Lambda functions and Step Functions
4. **CeapScoreWorkflow-dev** - Scoring Lambda functions and Step Functions
5. **CeapStoreWorkflow-dev** - Storage Lambda functions and Step Functions
6. **CeapReactiveWorkflow-dev** - Reactive serving Lambda functions and Step Functions
7. **CeapOrchestration-dev** - Orchestration Step Functions and EventBridge rules

### Target Architecture (3 Stacks)

1. **CeapDatabase-dev (Storage Layer)** - Unchanged, contains all database resources
2. **CeapDataPlatform-dev (Write Path)** - Consolidates ETL, Filter, Score, Store, and Orchestration
3. **CeapServingAPI-dev (Read Path)** - Contains Reactive workflow and future serving API

### Business Alignment

- **Write Path (CeapDataPlatform-dev)**: Focuses on B-2 capability (building datasets) - data ingestion, transformation, scoring, and storage
- **Read Path (CeapServingAPI-dev)**: Focuses on B-3 capability (low-latency retrieval) - real-time serving and reactive workflows
- **Storage Layer (CeapDatabase-dev)**: Shared foundation for both paths

## Architecture

### Stack Dependency Graph

```
CeapDatabase-dev (Storage Layer)
       ↓                    ↓
CeapDataPlatform-dev    CeapServingAPI-dev
   (Write Path)            (Read Path)
```

### Deployment Order

1. **Phase 1**: Deploy CeapDatabase-dev (base layer)
2. **Phase 2**: Deploy CeapDataPlatform-dev and CeapServingAPI-dev in parallel (both depend on Phase 1)

### Resource Distribution

#### CeapDatabase-dev (Storage Layer)
- DynamoDB tables
- DynamoDB indexes
- Database-related IAM roles
- Database monitoring alarms
- **No changes from current implementation**

#### CeapDataPlatform-dev (Write Path)
Consolidates resources from 5 stacks:
- **From CeapEtlWorkflow-dev**: ETL Lambda functions, ETL Step Functions, ETL EventBridge rules
- **From CeapFilterWorkflow-dev**: Filter Lambda functions, Filter Step Functions
- **From CeapScoreWorkflow-dev**: Score Lambda functions, Score Step Functions
- **From CeapStoreWorkflow-dev**: Store Lambda functions, Store Step Functions
- **From CeapOrchestration-dev**: Orchestration Step Functions, EventBridge orchestration rules

#### CeapServingAPI-dev (Read Path)
- **From CeapReactiveWorkflow-dev**: Reactive Lambda functions, Reactive Step Functions
- Future: API Gateway resources
- Future: Lambda authorizers
- Future: Serving-specific EventBridge rules

## Components and Interfaces

### CloudFormation Template Structure

#### CeapDataPlatform-dev Template Organization

```yaml
Parameters:
  DatabaseStackName:
    Type: String
    Description: Name of the database stack for cross-stack references

Resources:
  # ETL Resources (from CeapEtlWorkflow-dev)
  EtlLambdaFunction:
    Type: AWS::Lambda::Function
    # ... existing configuration
  
  EtlStateMachine:
    Type: AWS::StepFunctions::StateMachine
    # ... existing configuration
  
  # Filter Resources (from CeapFilterWorkflow-dev)
  FilterLambdaFunction:
    Type: AWS::Lambda::Function
    # ... existing configuration
  
  FilterStateMachine:
    Type: AWS::StepFunctions::StateMachine
    # ... existing configuration
  
  # Score Resources (from CeapScoreWorkflow-dev)
  ScoreLambdaFunction:
    Type: AWS::Lambda::Function
    # ... existing configuration
  
  ScoreStateMachine:
    Type: AWS::StepFunctions::StateMachine
    # ... existing configuration
  
  # Store Resources (from CeapStoreWorkflow-dev)
  StoreLambdaFunction:
    Type: AWS::Lambda::Function
    # ... existing configuration
  
  StoreStateMachine:
    Type: AWS::StepFunctions::StateMachine
    # ... existing configuration
  
  # Orchestration Resources (from CeapOrchestration-dev)
  OrchestrationStateMachine:
    Type: AWS::StepFunctions::StateMachine
    # ... existing configuration
  
  OrchestrationEventRule:
    Type: AWS::Events::Rule
    # ... existing configuration

Outputs:
  DataPlatformStateMachineArn:
    Value: !GetAtt OrchestrationStateMachine.Arn
    Export:
      Name: !Sub "${AWS::StackName}-OrchestrationArn"
```

#### CeapServingAPI-dev Template Organization

```yaml
Parameters:
  DatabaseStackName:
    Type: String
    Description: Name of the database stack for cross-stack references
  
  DataPlatformStackName:
    Type: String
    Description: Name of the data platform stack for cross-stack references

Resources:
  # Reactive Resources (from CeapReactiveWorkflow-dev)
  ReactiveLambdaFunction:
    Type: AWS::Lambda::Function
    # ... existing configuration
  
  ReactiveStateMachine:
    Type: AWS::StepFunctions::StateMachine
    # ... existing configuration
  
  # Future: API Gateway resources
  # Future: Lambda authorizers

Outputs:
  ServingAPIStateMachineArn:
    Value: !GetAtt ReactiveStateMachine.Arn
    Export:
      Name: !Sub "${AWS::StackName}-ReactiveArn"
```

### Cross-Stack Reference Patterns

#### Pattern 1: Database Table References

```yaml
# In CeapDataPlatform-dev or CeapServingAPI-dev
Resources:
  LambdaFunction:
    Type: AWS::Lambda::Function
    Properties:
      Environment:
        Variables:
          TABLE_NAME:
            Fn::ImportValue: !Sub "${DatabaseStackName}-TableName"
```

#### Pattern 2: IAM Role Permissions

```yaml
# In CeapDataPlatform-dev
Resources:
  EtlLambdaRole:
    Type: AWS::IAM::Role
    Properties:
      Policies:
        - PolicyName: DynamoDBAccess
          PolicyDocument:
            Statement:
              - Effect: Allow
                Action:
                  - dynamodb:PutItem
                  - dynamodb:GetItem
                  - dynamodb:Query
                Resource:
                  Fn::ImportValue: !Sub "${DatabaseStackName}-TableArn"
```

#### Pattern 3: Step Function Chaining

```yaml
# In CeapServingAPI-dev referencing CeapDataPlatform-dev
Resources:
  ReactiveStateMachine:
    Type: AWS::StepFunctions::StateMachine
    Properties:
      DefinitionString:
        Fn::Sub:
          - |
            {
              "StartAt": "InvokeDataPlatform",
              "States": {
                "InvokeDataPlatform": {
                  "Type": "Task",
                  "Resource": "arn:aws:states:::states:startExecution.sync",
                  "Parameters": {
                    "StateMachineArn": "${DataPlatformArn}"
                  }
                }
              }
            }
          - DataPlatformArn:
              Fn::ImportValue: !Sub "${DataPlatformStackName}-OrchestrationArn"
```

## Data Models

### Stack Configuration Model

```typescript
interface StackConfiguration {
  stackName: string;
  templatePath: string;
  parameters: Record<string, string>;
  dependencies: string[];
  tags: Record<string, string>;
}

interface ConsolidatedArchitecture {
  storageLayer: StackConfiguration;
  writePath: StackConfiguration;
  readPath: StackConfiguration;
}
```

### Resource Migration Mapping

```typescript
interface ResourceMapping {
  sourceStack: string;
  targetStack: string;
  resourceLogicalId: string;
  resourceType: string;
  dependencies: string[];
}

interface MigrationPlan {
  resourceMappings: ResourceMapping[];
  validationSteps: string[];
  rollbackProcedure: string[];
}
```

### CloudFormation Export Model

```typescript
interface StackExport {
  exportName: string;
  exportValue: string;
  exportingStack: string;
  importingStacks: string[];
}

interface CrossStackReferences {
  exports: StackExport[];
  validationRules: string[];
}
```

## Migration Strategy

### Phase 1: Preparation

1. **Audit Current Resources**: Document all resources in each of the 7 stacks
2. **Identify Dependencies**: Map all cross-stack references and dependencies
3. **Create Resource Mapping**: Build complete mapping from 7-stack to 3-stack architecture
4. **Backup Current State**: Export current CloudFormation templates and resource configurations

### Phase 2: Template Consolidation

1. **Create CeapDataPlatform-dev Template**: Merge resources from ETL, Filter, Score, Store, and Orchestration stacks
2. **Create CeapServingAPI-dev Template**: Move resources from Reactive stack
3. **Update Cross-Stack References**: Replace old stack references with new stack exports
4. **Add Stack Parameters**: Define parameters for cross-stack dependencies

### Phase 3: Validation

1. **Template Validation**: Use CloudFormation validation to check template syntax
2. **Dependency Validation**: Verify all cross-stack references are correct
3. **Resource Count Validation**: Ensure all resources from 7 stacks are present in 3 stacks
4. **IAM Permission Validation**: Verify all IAM roles maintain necessary permissions

### Phase 4: Deployment

1. **Deploy to Test Environment**: Deploy 3-stack architecture to test environment first
2. **Run Integration Tests**: Verify all workflows execute correctly
3. **Performance Testing**: Ensure deployment completes within 15 minutes
4. **Deploy to Production**: Execute production deployment with rollback plan ready

### Phase 5: Cleanup

1. **Verify New Stacks**: Confirm all resources are functioning in new stacks
2. **Remove Old Stacks**: Delete the 5 consolidated stacks (keep CeapDatabase-dev)
3. **Update Documentation**: Update all references to new 3-stack architecture
4. **Archive Old Templates**: Store old templates for reference and rollback capability

## Rollback Strategy

### Rollback Triggers

- CloudFormation deployment failure
- Resource validation failure
- Integration test failure
- Deployment time exceeds 15 minutes

### Rollback Procedure

1. **Halt Deployment**: Stop any in-progress CloudFormation operations
2. **Restore Old Stacks**: Redeploy the original 7-stack configuration from backup
3. **Verify Restoration**: Run integration tests to confirm system functionality
4. **Document Failure**: Record failure reason and lessons learned
5. **Plan Remediation**: Address issues before attempting consolidation again



## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system—essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

### Property 1: Database Stack Preservation

*For any* resource in the CeapDatabase-dev stack before consolidation, that resource should exist with identical configuration in the CeapDatabase-dev stack after consolidation.

**Validates: Requirements 1.2**

### Property 2: Resource Consolidation Completeness

*For any* resource in the original 7 stacks (CeapEtlWorkflow-dev, CeapFilterWorkflow-dev, CeapScoreWorkflow-dev, CeapStoreWorkflow-dev, CeapReactiveWorkflow-dev, CeapOrchestration-dev, CeapDatabase-dev), that resource should exist in exactly one of the 3 new stacks (CeapDataPlatform-dev, CeapServingAPI-dev, CeapDatabase-dev).

**Validates: Requirements 1.3, 1.4, 1.5**

### Property 3: Resource Preservation

*For any* resource (Lambda function, Step Function, EventBridge rule, DynamoDB table, IAM role) in the original 7 stacks, that resource should exist in the new 3 stacks with equivalent configuration (same properties, permissions, and behavior).

**Validates: Requirements 2.1, 2.2, 2.3, 2.4, 2.5**

### Property 4: Functional Equivalence

*For any* workflow input (ETL, Filter, Score, Store, Reactive, or Orchestration), executing the workflow in the new 3-stack architecture should produce the same output as executing it in the original 7-stack architecture.

**Validates: Requirements 3.1, 3.2, 3.3, 3.4, 3.5, 3.6**

### Property 5: Resource Validation Before Cleanup

*For any* resource in the original 7 stacks, the migration validation should verify that resource exists in the new 3 stacks before allowing the old stack to be deleted.

**Validates: Requirements 5.3**

### Property 6: Documentation Reference Cleanup

*For any* reference to old stack names (CeapEtlWorkflow-dev, CeapFilterWorkflow-dev, CeapScoreWorkflow-dev, CeapStoreWorkflow-dev, CeapOrchestration-dev) in documentation, that reference should be updated to the corresponding new stack name (CeapDataPlatform-dev or CeapServingAPI-dev).

**Validates: Requirements 6.4**

### Property 7: Logical Name Preservation

*For any* resource moved during consolidation, its CloudFormation logical ID should remain unchanged in the new stack.

**Validates: Requirements 8.1, 8.2**

### Property 8: Naming Consistency

*For any* resource within a stack, the resource name should follow a consistent naming pattern that includes the stack identifier or purpose.

**Validates: Requirements 8.3, 8.4**

### Property 9: Cross-Stack Reference Mechanisms

*For any* cross-stack reference in the new CloudFormation templates (CeapDataPlatform-dev or CeapServingAPI-dev referencing CeapDatabase-dev, or CeapServingAPI-dev referencing CeapDataPlatform-dev), the reference should use CloudFormation exports (Fn::ImportValue) or parameter passing.

**Validates: Requirements 10.1, 10.2, 10.3**

### Property 10: Cross-Stack Reference Validation

*For any* cross-stack reference in the new templates, CloudFormation deployment validation should verify that the referenced export or parameter exists before allowing deployment to proceed.

**Validates: Requirements 10.4**

## Error Handling

### CloudFormation Deployment Errors

**Error Type**: Stack creation failure due to resource conflicts

**Handling Strategy**:
- CloudFormation automatically rolls back on failure
- Preserve original 7-stack configuration
- Log detailed error messages for debugging
- Provide clear error messages indicating which resource caused the failure

**Error Type**: Cross-stack reference not found

**Handling Strategy**:
- Fail fast during template validation phase
- Provide clear error message indicating missing export name
- Suggest checking that dependency stacks are deployed first
- Prevent deployment until references are resolved

**Error Type**: Resource limit exceeded

**Handling Strategy**:
- Check AWS service quotas before deployment
- Provide clear error message indicating which limit was exceeded
- Suggest requesting quota increase or optimizing resource usage
- Halt deployment to prevent partial stack creation

### Migration Validation Errors

**Error Type**: Resource count mismatch

**Handling Strategy**:
- Compare resource counts between old and new stacks
- Generate detailed report of missing resources
- Halt migration and preserve old stacks
- Require manual review before proceeding

**Error Type**: Resource configuration mismatch

**Handling Strategy**:
- Compare resource properties between old and new stacks
- Generate detailed diff report
- Halt migration and preserve old stacks
- Require manual review and correction

**Error Type**: Functional test failure

**Handling Strategy**:
- Run integration tests after deployment
- If tests fail, trigger automatic rollback
- Generate detailed test failure report
- Preserve old stacks for comparison

### Rollback Errors

**Error Type**: Rollback deployment failure

**Handling Strategy**:
- Attempt rollback up to 3 times with exponential backoff
- If rollback fails, preserve current state and alert operators
- Provide manual rollback instructions
- Document state for post-mortem analysis

**Error Type**: Data inconsistency after rollback

**Handling Strategy**:
- Run data validation checks after rollback
- If inconsistencies detected, generate detailed report
- Provide data reconciliation procedures
- Alert operators for manual intervention

## Testing Strategy

### Dual Testing Approach

This infrastructure consolidation requires both unit testing and property-based testing to ensure correctness:

- **Unit tests**: Verify specific CloudFormation template structures, resource configurations, and migration steps
- **Property tests**: Verify universal properties across all resources and workflows

Both approaches are complementary and necessary for comprehensive validation.

### Unit Testing

**Focus Areas**:
- CloudFormation template syntax validation
- Specific resource configuration examples
- Cross-stack reference examples
- Migration script logic
- Rollback procedure steps

**Example Unit Tests**:
1. Test that CeapDataPlatform-dev template contains ETL Lambda function with correct configuration
2. Test that CeapServingAPI-dev template references CeapDatabase-dev exports correctly
3. Test that migration script correctly maps resources from source to target stacks
4. Test that rollback script restores original stack configuration
5. Test that stack dependency parameters are correctly defined

**Testing Tools**:
- AWS CloudFormation template validation (cfn-lint)
- Python unittest or pytest for migration scripts
- AWS CloudFormation ChangeSet for deployment preview

### Property-Based Testing

**Configuration**:
- Use Hypothesis (Python) for property-based testing
- Minimum 100 iterations per property test
- Each test tagged with feature name and property number

**Property Test Implementation**:

Each correctness property must be implemented as a property-based test:

1. **Property 1: Database Stack Preservation**
   - Generate random subsets of database resources
   - Verify each resource exists with identical configuration after consolidation
   - Tag: **Feature: infrastructure-consolidation, Property 1: Database Stack Preservation**

2. **Property 2: Resource Consolidation Completeness**
   - Generate random resources from original 7 stacks
   - Verify each resource exists in exactly one of the 3 new stacks
   - Tag: **Feature: infrastructure-consolidation, Property 2: Resource Consolidation Completeness**

3. **Property 3: Resource Preservation**
   - Generate random resources of different types (Lambda, Step Function, etc.)
   - Verify each resource exists with equivalent configuration in new stacks
   - Tag: **Feature: infrastructure-consolidation, Property 3: Resource Preservation**

4. **Property 4: Functional Equivalence**
   - Generate random workflow inputs
   - Execute workflows in both old and new architectures
   - Verify outputs are identical
   - Tag: **Feature: infrastructure-consolidation, Property 4: Functional Equivalence**

5. **Property 5: Resource Validation Before Cleanup**
   - Generate random resource sets
   - Verify migration validation checks for each resource before allowing deletion
   - Tag: **Feature: infrastructure-consolidation, Property 5: Resource Validation Before Cleanup**

6. **Property 6: Documentation Reference Cleanup**
   - Generate random documentation files
   - Verify no references to old stack names remain
   - Tag: **Feature: infrastructure-consolidation, Property 6: Documentation Reference Cleanup**

7. **Property 7: Logical Name Preservation**
   - Generate random resources being moved
   - Verify logical IDs remain unchanged
   - Tag: **Feature: infrastructure-consolidation, Property 7: Logical Name Preservation**

8. **Property 8: Naming Consistency**
   - Generate random resources within a stack
   - Verify all follow consistent naming pattern
   - Tag: **Feature: infrastructure-consolidation, Property 8: Naming Consistency**

9. **Property 9: Cross-Stack Reference Mechanisms**
   - Generate random cross-stack references
   - Verify all use Fn::ImportValue or parameters
   - Tag: **Feature: infrastructure-consolidation, Property 9: Cross-Stack Reference Mechanisms**

10. **Property 10: Cross-Stack Reference Validation**
    - Generate random cross-stack references (valid and invalid)
    - Verify CloudFormation validation catches invalid references
    - Tag: **Feature: infrastructure-consolidation, Property 10: Cross-Stack Reference Validation**

### Integration Testing

**Test Scenarios**:
1. Deploy 3-stack architecture to test environment
2. Execute all ETL workflows and verify outputs
3. Execute all filtering workflows and verify outputs
4. Execute all scoring workflows and verify outputs
5. Execute all storage workflows and verify outputs
6. Execute all reactive workflows and verify outputs
7. Execute all orchestration workflows and verify outputs
8. Verify cross-stack references work correctly
9. Test rollback procedure
10. Verify deployment completes within 15 minutes

**Test Environment**:
- Separate AWS account or isolated environment
- Identical configuration to production
- Automated test execution via CI/CD pipeline

### Performance Testing

**Metrics to Measure**:
- CloudFormation stack deployment time
- Resource creation time per stack
- Total deployment time for all 3 stacks
- Rollback execution time

**Success Criteria**:
- CeapDataPlatform-dev deploys in < 15 minutes
- CeapServingAPI-dev deploys in < 15 minutes
- Total sequential deployment < 30 minutes
- Rollback completes in < 20 minutes

### Validation Checklist

Before production deployment:
- [ ] All unit tests pass
- [ ] All property tests pass (100+ iterations each)
- [ ] Integration tests pass in test environment
- [ ] Performance tests meet success criteria
- [ ] CloudFormation templates validated with cfn-lint
- [ ] Cross-stack references validated
- [ ] Resource count matches between old and new stacks
- [ ] IAM permissions validated
- [ ] Documentation updated
- [ ] Rollback procedure tested and verified
- [ ] Deployment runbook reviewed
- [ ] Stakeholders notified of deployment plan
