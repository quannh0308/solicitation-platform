#!/bin/bash

# Validate Lambda Functions CloudFormation Template
# This script validates the Lambda configuration template without deploying

set -e

# Configuration
TEMPLATE_FILE="lambda-functions.yaml"
AWS_REGION="${AWS_REGION:-us-east-1}"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Lambda Template Validation${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# Check if AWS CLI is installed
if ! command -v aws &> /dev/null; then
    echo -e "${RED}Error: AWS CLI is not installed${NC}"
    echo "Please install AWS CLI: https://aws.amazon.com/cli/"
    exit 1
fi

# Check if template file exists
if [ ! -f "$TEMPLATE_FILE" ]; then
    echo -e "${RED}Error: Template file $TEMPLATE_FILE not found${NC}"
    exit 1
fi

echo -e "${YELLOW}Validating template: $TEMPLATE_FILE${NC}"
echo ""

# Validate the CloudFormation template
VALIDATION_OUTPUT=$(aws cloudformation validate-template \
    --template-body file://$TEMPLATE_FILE \
    --region $AWS_REGION 2>&1)

VALIDATION_STATUS=$?

if [ $VALIDATION_STATUS -eq 0 ]; then
    echo -e "${GREEN}✓ Template validation successful${NC}"
    echo ""
    
    # Display template details
    echo -e "${BLUE}Template Details:${NC}"
    echo "$VALIDATION_OUTPUT" | jq -r '.Description // "No description"'
    echo ""
    
    # Display parameters
    echo -e "${BLUE}Parameters:${NC}"
    echo "$VALIDATION_OUTPUT" | jq -r '.Parameters[] | "  - \(.ParameterKey): \(.Description // "No description")"'
    echo ""
    
    # Count resources
    RESOURCE_COUNT=$(grep -c "Type: AWS::" $TEMPLATE_FILE || true)
    echo -e "${BLUE}Resources:${NC}"
    echo "  Total resources defined: $RESOURCE_COUNT"
    echo ""
    
    # List Lambda functions
    echo -e "${BLUE}Lambda Functions:${NC}"
    grep "Type: AWS::Lambda::Function" $TEMPLATE_FILE -B 2 | grep "^  [A-Z]" | sed 's/://g' | sed 's/^/  - /'
    echo ""
    
    # List IAM roles
    echo -e "${BLUE}IAM Roles:${NC}"
    grep "Type: AWS::IAM::Role" $TEMPLATE_FILE -B 2 | grep "^  [A-Z]" | sed 's/://g' | sed 's/^/  - /'
    echo ""
    
    # List Log Groups
    echo -e "${BLUE}CloudWatch Log Groups:${NC}"
    grep "Type: AWS::Logs::LogGroup" $TEMPLATE_FILE -B 2 | grep "^  [A-Z]" | sed 's/://g' | sed 's/^/  - /'
    echo ""
    
    # Check for common best practices
    echo -e "${BLUE}Best Practices Check:${NC}"
    
    # Check for tags
    if grep -q "Tags:" $TEMPLATE_FILE; then
        echo -e "  ${GREEN}✓${NC} Resources are tagged"
    else
        echo -e "  ${YELLOW}⚠${NC} No tags found - consider adding tags for cost tracking"
    fi
    
    # Check for log retention
    if grep -q "RetentionInDays:" $TEMPLATE_FILE; then
        echo -e "  ${GREEN}✓${NC} Log retention configured"
    else
        echo -e "  ${YELLOW}⚠${NC} No log retention configured"
    fi
    
    # Check for environment variables
    if grep -q "Environment:" $TEMPLATE_FILE; then
        echo -e "  ${GREEN}✓${NC} Environment variables configured"
    else
        echo -e "  ${YELLOW}⚠${NC} No environment variables found"
    fi
    
    # Check for IAM capabilities
    if grep -q "CAPABILITY_NAMED_IAM" deploy-lambda.sh 2>/dev/null; then
        echo -e "  ${GREEN}✓${NC} IAM capabilities configured in deployment script"
    else
        echo -e "  ${YELLOW}⚠${NC} Ensure IAM capabilities are specified during deployment"
    fi
    
    echo ""
    echo -e "${GREEN}========================================${NC}"
    echo -e "${GREEN}Validation Complete - Template is valid!${NC}"
    echo -e "${GREEN}========================================${NC}"
    echo ""
    echo -e "${YELLOW}Next steps:${NC}"
    echo "1. Review the template configuration"
    echo "2. Set environment variables (ENVIRONMENT, PROJECT_NAME, AWS_REGION)"
    echo "3. Run ./deploy-lambda.sh to deploy the stack"
    echo ""
    
else
    echo -e "${RED}✗ Template validation failed${NC}"
    echo ""
    echo -e "${RED}Error details:${NC}"
    echo "$VALIDATION_OUTPUT"
    echo ""
    exit 1
fi
