#!/bin/bash
# Backup script for exporting current CloudFormation templates
# Purpose: Create backup of 7-stack architecture before consolidation
# Date: 2025-01-27

set -e

# Configuration
BACKUP_DIR="backup-templates-$(date +%Y%m%d-%H%M%S)"
INFRASTRUCTURE_DIR="infrastructure"
ENV_NAME="${1:-dev}"

echo "=========================================="
echo "CEAP Infrastructure Backup Script"
echo "=========================================="
echo "Environment: $ENV_NAME"
echo "Backup directory: $BACKUP_DIR"
echo ""

# Create backup directory
mkdir -p "$BACKUP_DIR"
echo "✓ Created backup directory: $BACKUP_DIR"

# Change to infrastructure directory
cd "$INFRASTRUCTURE_DIR"

# Synthesize CloudFormation templates
echo ""
echo "Synthesizing CloudFormation templates..."
cdk synth --all --output="../$BACKUP_DIR/cdk-output"
echo "✓ Templates synthesized to $BACKUP_DIR/cdk-output"

# Copy CDK context
if [ -f "cdk.context.json" ]; then
    cp cdk.context.json "../$BACKUP_DIR/cdk.context.json"
    echo "✓ Copied cdk.context.json"
fi

# Copy CDK configuration
if [ -f "cdk.json" ]; then
    cp cdk.json "../$BACKUP_DIR/cdk.json"
    echo "✓ Copied cdk.json"
fi

# Return to root directory
cd ..

# List all stacks
echo ""
echo "Backed up stacks:"
ls -1 "$BACKUP_DIR/cdk-output" | grep -E "\.template\.(json|yaml)$" | sed 's/\.template\..*//' | sort

# Create inventory file
echo ""
echo "Creating inventory file..."
cat > "$BACKUP_DIR/INVENTORY.md" << 'EOF'
# CloudFormation Template Backup Inventory

## Backup Information
- **Date:** $(date +"%Y-%m-%d %H:%M:%S")
- **Environment:** $ENV_NAME
- **Purpose:** Pre-consolidation backup of 7-stack architecture

## Stacks Backed Up

1. **CeapDatabase-$ENV_NAME** - Storage layer (DynamoDB tables)
2. **CeapEtlWorkflow-$ENV_NAME** - ETL Lambda function
3. **CeapFilterWorkflow-$ENV_NAME** - Filter Lambda function
4. **CeapScoreWorkflow-$ENV_NAME** - Score Lambda function
5. **CeapStoreWorkflow-$ENV_NAME** - Store Lambda function
6. **CeapReactiveWorkflow-$ENV_NAME** - Reactive Lambda + EventBridge
7. **CeapOrchestration-$ENV_NAME** - Step Functions + EventBridge schedule

## Files Included

- `cdk-output/` - Synthesized CloudFormation templates
- `cdk.context.json` - CDK context (if exists)
- `cdk.json` - CDK configuration (if exists)
- `INVENTORY.md` - This file

## Restoration Instructions

To restore the original 7-stack architecture:

1. Review the templates in `cdk-output/`
2. Deploy stacks in order:
   - CeapDatabase-$ENV_NAME (first)
   - CeapEtlWorkflow-$ENV_NAME, CeapFilterWorkflow-$ENV_NAME, CeapScoreWorkflow-$ENV_NAME, CeapStoreWorkflow-$ENV_NAME, CeapReactiveWorkflow-$ENV_NAME (parallel)
   - CeapOrchestration-$ENV_NAME (last)
3. Or use CDK to redeploy from source code

## Notes

- Templates are in JSON format by default
- IAM roles and policies are included in templates
- CloudWatch Log Groups are auto-generated (not in templates)
- Physical resource names may differ on redeployment

EOF

echo "✓ Created inventory file"

# Create checksums
echo ""
echo "Creating checksums..."
cd "$BACKUP_DIR/cdk-output"
find . -type f -name "*.template.*" -exec sha256sum {} \; > ../checksums.txt
cd ../..
echo "✓ Created checksums.txt"

# Create archive
echo ""
echo "Creating compressed archive..."
tar -czf "$BACKUP_DIR.tar.gz" "$BACKUP_DIR"
echo "✓ Created archive: $BACKUP_DIR.tar.gz"

# Summary
echo ""
echo "=========================================="
echo "Backup Complete!"
echo "=========================================="
echo "Backup directory: $BACKUP_DIR"
echo "Archive: $BACKUP_DIR.tar.gz"
echo ""
echo "To restore from backup:"
echo "  1. Extract: tar -xzf $BACKUP_DIR.tar.gz"
echo "  2. Review: cat $BACKUP_DIR/INVENTORY.md"
echo "  3. Deploy: cd infrastructure && cdk deploy --all"
echo ""
