# CEAP Project Structure

## Root Directory

```
.
├── README.md                                    # Main project documentation
├── TECH-STACK.md                               # Technology stack reference
├── INFRASTRUCTURE-ENHANCEMENT-COMPLETE.md      # Infrastructure completion summary
├── WORKFLOW-COMPARISON.md                      # Express vs Standard comparison
├── WORKFLOW-DEPLOYMENT-SUMMARY.md              # Deployment details
├── WORKFLOW-INTEGRATION-TEST-PLAN.md           # Testing guide
├── build.gradle.kts                            # Root Gradle build
├── settings.gradle.kts                         # Gradle settings
└── gradlew / gradlew.bat                       # Gradle wrapper
```

## Documentation (`docs/`)

```
docs/
├── BRANDING.md                                 # CEAP branding guidelines
├── DEPLOYMENT-GUIDE.md                         # Deployment procedures
├── MIGRATION-RUNBOOK.md                        # Migration procedures
├── MULTI-TENANCY-GUIDE.md                      # Multi-tenancy design
├── TROUBLESHOOTING.md                          # Troubleshooting guide
├── USE-CASES.md                                # Use case examples
├── VISUAL-ARCHITECTURE.md                      # Architecture diagrams
├── WORKFLOW-ORCHESTRATION-GUIDE.md             # Workflow usage guide
├── WORKFLOW-OPERATIONS-RUNBOOK.md              # Operations procedures
├── integrations/                               # Integration guides
│   ├── DATA-SOURCE-CONFIGURATION-GUIDE.md
│   ├── ETL-CONFIGURATION-GUIDE.md
│   ├── NOTIFICATION-CONFIGURATION-GUIDE.md
│   ├── SCORING-CONFIGURATION-GUIDE.md
│   └── STORAGE-CONFIGURATION-GUIDE.md
├── usecases/                                   # Use case examples
│   ├── E-COMMERCE-PRODUCT-REVIEWS.md
│   ├── EVENT-PARTICIPATION-REQUESTS.md
│   ├── MUSIC-TRACK-FEEDBACK.md
│   ├── SERVICE-EXPERIENCE-SURVEYS.md
│   ├── VIDEO-RATINGS-REACTIVE.md
│   └── expansion/                              # Future use cases
└── archive/                                    # Historical documents
```

## Infrastructure (`infrastructure/`)

```
infrastructure/
├── README.md                                   # Infrastructure documentation
├── build.gradle.kts                            # Infrastructure build
├── cdk.json                                    # CDK configuration
├── deploy-workflow-simple.sh                   # Workflow deployment script
├── test-workflows.sh                           # Integration test script
├── glue-scripts/
│   ├── README.md
│   └── workflow_etl_template.py                # Glue PySpark template
└── src/
    ├── main/kotlin/com/ceap/infrastructure/
    │   ├── ConsolidatedCeapPlatformApp.kt      # Main CDK app (3-stack)
    │   ├── SimpleWorkflowApp.kt                # Workflow CDK app
    │   ├── MinimalTestApp.kt                   # Test app
    │   ├── constructs/
    │   │   └── ObservabilityDashboard.kt
    │   └── stacks/
    │       ├── DatabaseStack.kt
    │       ├── DataPlatformStack.kt
    │       ├── ServingAPIStack.kt
    │       └── ObservabilityStack.kt
    └── test/kotlin/com/ceap/infrastructure/
        └── [test files]
```

## Application Modules

```
ceap-channels/                                  # Channel adapters (email, SMS, etc)
ceap-common/                                    # Common utilities
ceap-connectors/                                # Data connectors
ceap-filters/                                   # Filtering logic
ceap-models/                                    # Data models
ceap-scoring/                                   # Scoring engine
ceap-serving/                                   # Serving API
ceap-storage/                                   # Storage layer
ceap-workflow-etl/                              # ETL workflow
ceap-workflow-filter/                           # Filter workflow
ceap-workflow-reactive/                         # Reactive workflow
ceap-workflow-score/                            # Score workflow
ceap-workflow-store/                            # Store workflow
```

## Specs (`.kiro/specs/`)

```
.kiro/specs/
├── complete-ceap-infrastructure-rebrand/
├── complete-solicitation-rename/
├── final-documentation-cleanup/
├── infrastructure-consolidation/
├── infrastructure-enhancement/                 # ✅ COMPLETE
│   ├── requirements.md
│   ├── design.md
│   └── tasks.md
└── platform-rebranding/
```

## Key Files by Purpose

### Deployment
- `infrastructure/deploy-workflow-simple.sh` - Deploy Express/Standard workflows
- `infrastructure/README.md` - Deployment documentation

### Testing
- `infrastructure/test-workflows.sh` - Integration tests
- `WORKFLOW-INTEGRATION-TEST-PLAN.md` - Test plan

### Documentation
- `README.md` - Project overview
- `TECH-STACK.md` - Technology reference
- `docs/WORKFLOW-ORCHESTRATION-GUIDE.md` - Workflow usage
- `docs/WORKFLOW-OPERATIONS-RUNBOOK.md` - Operations

### Monitoring
- `WORKFLOW-DEPLOYMENT-SUMMARY.md` - Deployed resources
- `WORKFLOW-COMPARISON.md` - Workflow comparison
- `INFRASTRUCTURE-ENHANCEMENT-COMPLETE.md` - Completion summary

## Quick Reference

### Deploy Workflows
```bash
./infrastructure/deploy-workflow-simple.sh -n <name> -t <express|standard>
```

### Test Workflows
```bash
./infrastructure/test-workflows.sh
```

### View Documentation
```bash
# Main docs
cat README.md
cat TECH-STACK.md

# Workflow docs
cat docs/WORKFLOW-ORCHESTRATION-GUIDE.md
cat docs/WORKFLOW-OPERATIONS-RUNBOOK.md

# Deployment info
cat WORKFLOW-DEPLOYMENT-SUMMARY.md
cat INFRASTRUCTURE-ENHANCEMENT-COMPLETE.md
```

## File Count Summary

- **Root**: 6 markdown files (essential)
- **Infrastructure**: 3 files (README + 2 scripts)
- **Docs**: 20+ files (organized by category)
- **Total**: Clean, focused structure

All outdated and redundant files have been removed. The remaining files are current, essential, and well-organized.
