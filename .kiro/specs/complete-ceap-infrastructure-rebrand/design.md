# Design Document: Complete CEAP Infrastructure Rebrand

## Overview

This design document outlines the approach for completing the CEAP infrastructure rebrand by systematically updating all remaining functional code identifiers from "solicitation" to "ceap" terminology. The rebrand affects approximately 50 identifiers across infrastructure code, runtime identifiers, and CDK constructs.

The rebrand is organized into three main categories:

1. **CDK Infrastructure Code**: Class names, construct names, and stack definitions
2. **AWS Resource Names**: CloudFormation stacks, DynamoDB tables, EventBridge rules, Step Functions state machines
3. **Observability Identifiers**: CloudWatch namespaces, dashboard names, alarm names, and metric dimensions

The project is in development phase, making this an ideal time to implement breaking changes. The design prioritizes consistency, maintainability, and clear traceability between old and new identifiers.

## Architecture

### Component Organization

The CEAP infrastructure follows a modular architecture with clear separation of concerns:

```
infrastructure/
├── SolicitationPlatformApp.kt          → CeapPlatformApp.kt (main entry point)
├── stacks/
│   ├── DatabaseStack.kt                (no changes - already correct)
│   ├── EtlWorkflowStack.kt            (update SolicitationLambda imports)
│   ├── FilterWorkflowStack.kt         (update SolicitationLambda imports)
│   ├── ScoreWorkflowStack.kt          (update SolicitationLambda imports)
│   ├── StoreWorkflowStack.kt          (update SolicitationLambda imports)
│   ├── ReactiveWorkflowStack.kt       (update SolicitationLambda imports + table names)
│   └── OrchestrationStack.kt          (update state machine names)
└── constructs/
    ├── SolicitationLambda.kt          → CeapLambda.kt (reusable construct)
    └── ObservabilityDashboard.kt      (update all metric namespaces)
```

### Refactoring Strategy

The refactoring follows a **bottom-up approach** to minimize compilation errors:

1. **Phase 1: Rename Leaf Constructs** - Start with `SolicitationLambda` construct (no dependencies)
2. **Phase 2: Update Stack Imports** - Update all workflow stacks to import `CeapLambda`
3. **Phase 3: Update Resource Names** - Change CloudFormation stack names, DynamoDB tables, EventBridge rules
4. **Phase 4: Update Observability** - Change CloudWatch namespaces, dashboards, alarms
5. **Phase 5: Rename Main Application** - Finally rename `SolicitationPlatformApp` to `CeapPlatformApp`

This approach ensures that at each step, the code remains compilable and testable.

### Naming Conventions

The rebrand follows these consistent naming patterns:

| Resource Type | Old Pattern | New Pattern | Example |
|--------------|-------------|-------------|---------|
| CDK Classes | `Solicitation*` | `Ceap*` | `SolicitationLambda` → `CeapLambda` |
| CloudFormation Stacks | `Solicitation*-$envName` | `Ceap*-$envName` | `SolicitationDatabase-dev` → `CeapDatabase-dev` |
| EventBridge Rules | `solicitation-*-$envName` | `ceap-*-$envName` | `solicitation-customer-events-dev` → `ceap-customer-events-dev` |
| EventBridge Sources | `solicitation.*` | `ceap.*` | `solicitation.workflow` → `ceap.workflow` |
| DynamoDB Tables | `solicitation-*-$envName` | `ceap-*-$envName` | `solicitation-event-deduplication-dev` → `ceap-event-deduplication-dev` |
| CloudWatch Namespaces | `SolicitationPlatform/*` | `CeapPlatform/*` | `SolicitationPlatform/Workflow` → `CeapPlatform/Workflow` |
| CloudWatch Dashboards | `SolicitationPlatform-*` | `CeapPlatform-*` | `SolicitationPlatform-retail` → `CeapPlatform-retail` |
| CloudWatch Alarms | `SolicitationPlatform-*` | `CeapPlatform-*` | `SolicitationPlatform-ApiLatency-retail` → `CeapPlatform-ApiLatency-retail` |
| Lambda Function Names | `SolicitationPlatform-*` | `CeapPlatform-*` | `SolicitationPlatform-ETL-retail` → `CeapPlatform-ETL-retail` |
| DynamoDB Table Names (Metrics) | `SolicitationCandidates-*` | `CeapCandidates-*` | `SolicitationCandidates-retail` → `CeapCandidates-retail` |

## Components and Interfaces

### 1. CDK Application Class

**Current**: `SolicitationPlatformApp`
**New**: `CeapPlatformApp`

**Location**: `infrastructure/src/main/kotlin/com/ceap/infrastructure/SolicitationPlatformApp.kt` → `CeapPlatformApp.kt`

**Changes**:
- Rename file from `SolicitationPlatformApp.kt` to `CeapPlatformApp.kt`
- Update all CloudFormation stack name parameters in stack instantiations
- No interface changes - maintains same CDK App structure

**Stack Instantiation Updates**:
```kotlin
// Old
val databaseStack = DatabaseStack(app, "SolicitationDatabase-$envName", stackProps, envName)
val etlStack = EtlWorkflowStack(app, "SolicitationEtlWorkflow-$envName", stackProps, envName, databaseStack)

// New
val databaseStack = DatabaseStack(app, "CeapDatabase-$envName", stackProps, envName)
val etlStack = EtlWorkflowStack(app, "CeapEtlWorkflow-$envName", stackProps, envName, databaseStack)
```

### 2. Lambda Construct

**Current**: `SolicitationLambda`
**New**: `CeapLambda`

**Location**: `infrastructure/src/main/kotlin/com/ceap/infrastructure/constructs/SolicitationLambda.kt` → `CeapLambda.kt`

**Changes**:
- Rename file from `SolicitationLambda.kt` to `CeapLambda.kt`
- Rename class from `SolicitationLambda` to `CeapLambda`
- Update class documentation to reference CEAP instead of solicitation
- No interface changes - maintains same constructor signature and public API

**Interface** (unchanged):
```kotlin
class CeapLambda(
    scope: Construct,
    id: String,
    handler: String,
    jarPath: String,
    environment: Map<String, String> = emptyMap(),
    tables: List<ITable> = emptyList(),
    memorySize: Int = 512,
    timeout: Duration = Duration.minutes(1),
    logRetention: RetentionDays = RetentionDays.ONE_MONTH
) : Construct(scope, id)
```

### 3. Workflow Stacks

**Affected Files**:
- `EtlWorkflowStack.kt`
- `FilterWorkflowStack.kt`
- `ScoreWorkflowStack.kt`
- `StoreWorkflowStack.kt`
- `ReactiveWorkflowStack.kt`

**Changes**:
- Update import statement: `import com.ceap.infrastructure.constructs.SolicitationLambda` → `import com.ceap.infrastructure.constructs.CeapLambda`
- Update instantiation: `SolicitationLambda(...)` → `CeapLambda(...)`
- In `ReactiveWorkflowStack.kt`: Update DynamoDB table name from `solicitation-event-deduplication-$envName` to `ceap-event-deduplication-$envName`
- In `ReactiveWorkflowStack.kt`: Update EventBridge rule name from `solicitation-customer-events-$envName` to `ceap-customer-events-$envName`
- In `ReactiveWorkflowStack.kt`: Update EventBridge source from `solicitation.customer-events` to `ceap.customer-events`

### 4. Orchestration Stack

**Location**: `infrastructure/src/main/kotlin/com/ceap/infrastructure/stacks/OrchestrationStack.kt`

**Changes**:
- Update Step Functions state machine name from `SolicitationBatchIngestion-$envName` to `CeapBatchIngestion-$envName`
- Update EventBridge schedule rule name from `SolicitationBatchIngestion-$envName` to `CeapBatchIngestion-$envName`

### 5. Observability Dashboard

**Location**: `infrastructure/src/main/kotlin/com/ceap/infrastructure/constructs/ObservabilityDashboard.kt`

**Changes**:
- Update dashboard name from `SolicitationPlatform-$programId` to `CeapPlatform-$programId`
- Update all CloudWatch namespace references:
  - `SolicitationPlatform/Workflow` → `CeapPlatform/Workflow`
  - `SolicitationPlatform/Channels` → `CeapPlatform/Channels`
  - `SolicitationPlatform/Rejections` → `CeapPlatform/Rejections`
  - `SolicitationPlatform/Costs` → `CeapPlatform/Costs`
- Update all alarm names:
  - `SolicitationPlatform-ApiLatency-$programId` → `CeapPlatform-ApiLatency-$programId`
  - `SolicitationPlatform-WorkflowFailure-$programId` → `CeapPlatform-WorkflowFailure-$programId`
  - `SolicitationPlatform-DataQuality-$programId` → `CeapPlatform-DataQuality-$programId`
- Update Lambda function name references in metrics:
  - `SolicitationPlatform-ETL-$programId` → `CeapPlatform-ETL-$programId`
  - `SolicitationPlatform-Serve-$programId` → `CeapPlatform-Serve-$programId`
- Update DynamoDB table name references in metrics:
  - `SolicitationCandidates-$programId` → `CeapCandidates-$programId`

### 6. EventBridge Configuration

**Location**: `infrastructure/eventbridge-rules.yaml`

**Changes**:
- Update parameter description from "Reactive Solicitation" to "Reactive CEAP"
- Update rule descriptions to replace "solicitation" with "ceap"
- Parameter name `ReactiveSolicitationStateMachineArn` → `ReactiveCeapStateMachineArn`

## Data Models

### Identifier Mapping Table

This table provides a complete mapping of all identifiers being changed:

| Category | Old Identifier | New Identifier | File Location |
|----------|---------------|----------------|---------------|
| **CDK Classes** |
| Main App | `SolicitationPlatformApp` | `CeapPlatformApp` | `SolicitationPlatformApp.kt` → `CeapPlatformApp.kt` |
| Lambda Construct | `SolicitationLambda` | `CeapLambda` | `constructs/SolicitationLambda.kt` → `CeapLambda.kt` |
| **CloudFormation Stacks** |
| Database Stack | `SolicitationDatabase-$envName` | `CeapDatabase-$envName` | `SolicitationPlatformApp.kt` (line 35) |
| ETL Stack | `SolicitationEtlWorkflow-$envName` | `CeapEtlWorkflow-$envName` | `SolicitationPlatformApp.kt` (line 38) |
| Filter Stack | `SolicitationFilterWorkflow-$envName` | `CeapFilterWorkflow-$envName` | `SolicitationPlatformApp.kt` (line 39) |
| Score Stack | `SolicitationScoreWorkflow-$envName` | `CeapScoreWorkflow-$envName` | `SolicitationPlatformApp.kt` (line 40) |
| Store Stack | `SolicitationStoreWorkflow-$envName` | `CeapStoreWorkflow-$envName` | `SolicitationPlatformApp.kt` (line 41) |
| Reactive Stack | `SolicitationReactiveWorkflow-$envName` | `CeapReactiveWorkflow-$envName` | `SolicitationPlatformApp.kt` (line 42) |
| Orchestration Stack | `SolicitationOrchestration-$envName` | `CeapOrchestration-$envName` | `SolicitationPlatformApp.kt` (line 47) |
| **Step Functions** |
| Batch Ingestion | `SolicitationBatchIngestion-$envName` | `CeapBatchIngestion-$envName` | `OrchestrationStack.kt` (line 156) |
| **EventBridge Rules** |
| Batch Schedule | `SolicitationBatchIngestion-$envName` | `CeapBatchIngestion-$envName` | `OrchestrationStack.kt` (line 163) |
| Customer Events | `solicitation-customer-events-$envName` | `ceap-customer-events-$envName` | `ReactiveWorkflowStack.kt` (line 75) |
| **EventBridge Sources** |
| Customer Events | `solicitation.customer-events` | `ceap.customer-events` | `ReactiveWorkflowStack.kt` (line 79) |
| Workflow Events | `solicitation.workflow` | `ceap.workflow` | (EventBridge patterns) |
| **DynamoDB Tables** |
| Deduplication | `solicitation-event-deduplication-$envName` | `ceap-event-deduplication-$envName` | `ReactiveWorkflowStack.kt` (line 38) |
| **CloudWatch Namespaces** |
| Workflow | `SolicitationPlatform/Workflow` | `CeapPlatform/Workflow` | `ObservabilityDashboard.kt` (lines 75, 76, 90-92, 201, 227) |
| Channels | `SolicitationPlatform/Channels` | `CeapPlatform/Channels` | `ObservabilityDashboard.kt` (lines 106-108) |
| Rejections | `SolicitationPlatform/Rejections` | `CeapPlatform/Rejections` | `ObservabilityDashboard.kt` (line 122) |
| Costs | `SolicitationPlatform/Costs` | `CeapPlatform/Costs` | (if exists in code) |
| **CloudWatch Dashboards** |
| Main Dashboard | `SolicitationPlatform-$programId` | `CeapPlatform-$programId` | `ObservabilityDashboard.kt` (line 44) |
| **CloudWatch Alarms** |
| API Latency | `SolicitationPlatform-ApiLatency-$programId` | `CeapPlatform-ApiLatency-$programId` | `ObservabilityDashboard.kt` (line 183) |
| Workflow Failure | `SolicitationPlatform-WorkflowFailure-$programId` | `CeapPlatform-WorkflowFailure-$programId` | `ObservabilityDashboard.kt` (line 209) |
| Data Quality | `SolicitationPlatform-DataQuality-$programId` | `CeapPlatform-DataQuality-$programId` | `ObservabilityDashboard.kt` (line 238) |
| **Lambda Function Names (Metrics)** |
| ETL Function | `SolicitationPlatform-ETL-$programId` | `CeapPlatform-ETL-$programId` | `ObservabilityDashboard.kt` (line 152) |
| Serve Function | `SolicitationPlatform-Serve-$programId` | `CeapPlatform-Serve-$programId` | `ObservabilityDashboard.kt` (lines 153, 177) |
| **DynamoDB Table Names (Metrics)** |
| Candidates Table | `SolicitationCandidates-$programId` | `CeapCandidates-$programId` | `ObservabilityDashboard.kt` (lines 137, 138) |
| **CloudFormation Parameters** |
| State Machine ARN | `ReactiveSolicitationStateMachineArn` | `ReactiveCeapStateMachineArn` | `eventbridge-rules.yaml` (line 23) |

### File Modification Summary

| File | Type of Change | Number of Changes |
|------|---------------|-------------------|
| `SolicitationPlatformApp.kt` | Rename file + 7 stack names | 8 |
| `SolicitationLambda.kt` | Rename file + class name | 2 |
| `EtlWorkflowStack.kt` | Import + instantiation | 2 |
| `FilterWorkflowStack.kt` | Import + instantiation | 2 |
| `ScoreWorkflowStack.kt` | Import + instantiation | 2 |
| `StoreWorkflowStack.kt` | Import + instantiation | 2 |
| `ReactiveWorkflowStack.kt` | Import + instantiation + 3 resource names | 5 |
| `OrchestrationStack.kt` | 2 resource names | 2 |
| `ObservabilityDashboard.kt` | 4 namespaces + 1 dashboard + 3 alarms + 3 function names + 2 table names | 13 |
| `eventbridge-rules.yaml` | 1 parameter + descriptions | 3 |
| **Total** | | **41 changes** |


## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system—essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

For this refactoring task, correctness properties focus on ensuring completeness of the rebrand and preservation of functionality. The properties validate that all legacy terminology has been replaced and that the refactoring maintains system behavior.

### Property 1: Complete Legacy Terminology Removal

*For any* source code file in the infrastructure codebase (excluding comments, documentation strings, and test fixture strings), searching for the terms "solicitation" or "Solicitation" should return zero matches in functional code identifiers.

**Validates: Requirements 1.2, 3.2, 3.3, 15.1, 15.2, 15.3**

**Rationale**: This property ensures the rebrand is complete and no legacy terminology remains in functional code. It covers class names, variable names, resource names, and all other identifiers that affect runtime behavior.

**Testing Approach**: Use grep/ripgrep to search for "solicitation" (case-insensitive) across all `.kt`, `.yaml`, and `.json` files, then filter out matches that are in comments or documentation strings.

### Property 2: Class Interface Preservation

*For any* renamed class (`SolicitationPlatformApp` → `CeapPlatformApp`, `SolicitationLambda` → `CeapLambda`), the constructor signature (parameter names, types, and order) and all public method signatures should remain identical before and after the rename.

**Validates: Requirements 1.3, 3.1, 14.2**

**Rationale**: This property ensures that renaming classes doesn't introduce breaking changes to their interfaces. Consumers of these classes should not need to change their usage patterns.

**Testing Approach**: Compare the AST (Abstract Syntax Tree) of the old and new class definitions to verify that constructor parameters and public method signatures are identical.

### Property 3: Description Field Consistency

*For any* description field in infrastructure code (CloudFormation templates, EventBridge rules, state machine parameters), the text should not contain the word "solicitation" (case-insensitive) and should use "ceap" or "CEAP" instead.

**Validates: Requirements 13.1, 13.2**

**Rationale**: This property ensures that human-readable descriptions are updated to reflect the new branding, maintaining consistency between code identifiers and documentation.

**Testing Approach**: Parse YAML and Kotlin files to extract all description fields, then verify none contain "solicitation" and that appropriate fields contain "ceap".

### Property 4: Naming Convention Consistency

*For any* renamed resource, the new name should follow the established CEAP naming convention pattern as defined in the naming conventions table (e.g., `Ceap*` for classes, `CeapPlatform/*` for CloudWatch namespaces, `ceap-*` for kebab-case resources).

**Validates: Requirements 2.1-2.7, 4.1, 5.1-5.2, 6.1-6.2, 7.1, 8.1-8.4, 9.1, 10.1-10.3, 11.1-11.2, 12.1**

**Rationale**: This property ensures that the rebrand follows consistent naming patterns across all resource types, making the codebase more maintainable and predictable.

**Testing Approach**: For each resource type (CloudFormation stacks, EventBridge rules, CloudWatch namespaces, etc.), verify that all instances follow the corresponding naming pattern from the naming conventions table.

### Property 5: File Rename Completeness

*For any* file that is renamed (e.g., `SolicitationPlatformApp.kt` → `CeapPlatformApp.kt`), the old filename should not exist in the repository and the new filename should exist with the expected content.

**Validates: Requirements 1.1, 3.1**

**Rationale**: This property ensures that file renames are completed and no orphaned files with old names remain in the repository.

**Testing Approach**: Check that old filenames don't exist in the file system and that new filenames exist with the expected class definitions.

## Error Handling

### Compilation Errors

**Risk**: Renaming classes and updating imports could introduce compilation errors if references are missed.

**Mitigation**:
- Follow bottom-up refactoring approach (rename leaf constructs first)
- Compile after each phase to catch errors early
- Use IDE refactoring tools where possible to automatically update references

**Detection**: Run `./gradlew build` after each phase to verify compilation succeeds.

### Runtime Errors

**Risk**: Changing resource names (DynamoDB tables, EventBridge sources) could cause runtime errors if the application expects old names.

**Mitigation**:
- This is a development environment, so breaking changes are acceptable
- Document all resource name changes for deployment teams
- Update any hardcoded references in application code (outside infrastructure)

**Detection**: Deploy to development environment and run integration tests to verify all resources are accessible.

### Missing References

**Risk**: Some references to old identifiers might be missed, especially in configuration files or documentation.

**Mitigation**:
- Use comprehensive grep searches to find all occurrences
- Review the identifier mapping table to ensure all known references are updated
- Run property tests to verify no legacy terminology remains

**Detection**: Property 1 (Complete Legacy Terminology Removal) will catch any missed references.

### CloudFormation Stack Updates

**Risk**: Renaming CloudFormation stacks requires creating new stacks and deleting old ones, which could cause data loss if not handled carefully.

**Mitigation**:
- This is a development environment, so data loss is acceptable
- Document the stack name changes for deployment teams
- Consider using CloudFormation stack policies if needed in production

**Detection**: Verify new stacks are created successfully and old stacks can be deleted without errors.

## Testing Strategy

This refactoring task requires a dual testing approach combining automated verification with manual review:

### Unit Tests

Unit tests focus on specific examples and edge cases:

1. **File Existence Tests**: Verify that renamed files exist with correct names
   - Test that `CeapPlatformApp.kt` exists
   - Test that `CeapLambda.kt` exists
   - Test that old filenames don't exist

2. **Class Definition Tests**: Verify that renamed classes have correct names
   - Test that `CeapPlatformApp` class is defined
   - Test that `CeapLambda` class is defined
   - Test that old class names don't exist in the codebase

3. **Resource Name Tests**: Verify specific resource names are updated
   - Test that database stack uses `CeapDatabase-$envName` pattern
   - Test that ETL stack uses `CeapEtlWorkflow-$envName` pattern
   - Test each of the 7 CloudFormation stack names individually

4. **Import Statement Tests**: Verify import statements are updated
   - Test that workflow stacks import `CeapLambda` instead of `SolicitationLambda`
   - Test that no files import the old construct name

5. **CloudWatch Namespace Tests**: Verify CloudWatch namespaces are updated
   - Test that `CeapPlatform/Workflow` is used instead of `SolicitationPlatform/Workflow`
   - Test each of the 4 CloudWatch namespaces individually

### Property-Based Tests

Property-based tests verify universal properties across all inputs:

1. **Property Test: Complete Legacy Terminology Removal** (Property 1)
   - Generate list of all source files
   - For each file, search for "solicitation" in functional code
   - Verify zero matches (excluding comments/docs)
   - Run with minimum 100 iterations across different file types

2. **Property Test: Class Interface Preservation** (Property 2)
   - For each renamed class, extract constructor and method signatures
   - Compare old and new signatures
   - Verify they are identical
   - Run with minimum 100 iterations across different class definitions

3. **Property Test: Description Field Consistency** (Property 3)
   - Generate list of all description fields in infrastructure code
   - For each description, check for "solicitation"
   - Verify zero matches and "ceap" is used instead
   - Run with minimum 100 iterations across different description fields

4. **Property Test: Naming Convention Consistency** (Property 4)
   - For each resource type, extract all resource names
   - Verify each name matches the expected pattern from naming conventions table
   - Run with minimum 100 iterations across different resource types

5. **Property Test: File Rename Completeness** (Property 5)
   - Generate list of expected old and new filenames
   - Verify old filenames don't exist
   - Verify new filenames exist
   - Run with minimum 100 iterations across different file types

### Integration Tests

Integration tests verify the system works end-to-end after refactoring:

1. **CDK Synthesis Test**: Run `cdk synth` to verify CloudFormation templates are generated correctly
2. **CDK Deployment Test**: Deploy to development environment and verify all stacks are created
3. **Resource Accessibility Test**: Verify all AWS resources (DynamoDB tables, Lambda functions, etc.) are accessible with new names
4. **Observability Test**: Verify CloudWatch dashboards and alarms are created with new names
5. **EventBridge Test**: Verify EventBridge rules are created with new sources and patterns

### Manual Review

Manual review ensures quality and completeness:

1. **Code Review**: Review all changed files to ensure consistency and correctness
2. **Documentation Review**: Verify that all documentation is updated to reflect new names
3. **AWS Console Review**: Check AWS console to verify resources are named correctly
4. **Identifier Mapping Review**: Verify that all identifiers in the mapping table have been updated

### Test Configuration

All property-based tests must be configured with:
- **Minimum 100 iterations** per test (due to randomization)
- **Tag format**: `Feature: complete-ceap-infrastructure-rebrand, Property {number}: {property_text}`
- **Test framework**: Use Kotest property testing for Kotlin code
- **Failure reporting**: Report first failing example with full context

### Testing Order

1. Run unit tests first to catch specific issues quickly
2. Run property-based tests to verify universal properties
3. Run integration tests to verify end-to-end functionality
4. Perform manual review to ensure quality

This comprehensive testing strategy ensures that the rebrand is complete, consistent, and maintains system functionality.
