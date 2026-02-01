#!/bin/bash

###############################################################################
# Simple Workflow Deployment Script
#
# Deploys Express or Standard workflows using the SimpleWorkflowApp.
#
# Usage:
#   ./infrastructure/deploy-workflow-simple.sh -n realtime -t express
#   ./infrastructure/deploy-workflow-simple.sh -n batch -t standard
###############################################################################

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Defaults
WORKFLOW_NAME="realtime"
WORKFLOW_TYPE="express"
AWS_PROFILE="default"

# Parse arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -n|--name) WORKFLOW_NAME="$2"; shift 2 ;;
        -t|--type) WORKFLOW_TYPE="$2"; shift 2 ;;
        -p|--profile) AWS_PROFILE="$2"; shift 2 ;;
        -h|--help) echo "Usage: $0 -n <name> -t <type>"; exit 0 ;;
        *) echo -e "${RED}Unknown option: $1${NC}"; exit 1 ;;
    esac
done

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Deploying Workflow: $WORKFLOW_NAME${NC}"
echo -e "${BLUE}Type: $WORKFLOW_TYPE${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# Build infrastructure
echo -e "${YELLOW}Building infrastructure...${NC}"
./gradlew :infrastructure:build -x test
echo -e "${GREEN}✓ Build complete${NC}"
echo ""

# Deploy
echo -e "${YELLOW}Deploying to AWS...${NC}"
cd infrastructure
cdk deploy "CeapWorkflow-$WORKFLOW_NAME" \
    --app "./gradlew runWorkflowApp" \
    --context workflowName="$WORKFLOW_NAME" \
    --context workflowType="$WORKFLOW_TYPE" \
    --require-approval never \
    --profile "$AWS_PROFILE"

if [ $? -ne 0 ]; then
    echo -e "${RED}Deployment failed${NC}"
    exit 1
fi

echo ""
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}Deployment Complete!${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""
echo -e "Workflow: ${GREEN}$WORKFLOW_NAME${NC}"
echo -e "Type: ${GREEN}$WORKFLOW_TYPE${NC}"
echo ""
echo "Check AWS Console for deployed resources:"
echo "  - CloudFormation Stack: CeapWorkflow-$WORKFLOW_NAME"
echo "  - Step Functions: Ceap-$WORKFLOW_NAME-Workflow"
echo "  - S3 Bucket: ceap-workflow-$WORKFLOW_NAME-*"
echo ""
