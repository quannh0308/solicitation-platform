#!/bin/bash

# Validate CloudFormation template for DynamoDB tables
# Usage: ./validate-template.sh

set -e

TEMPLATE_FILE="dynamodb-tables.yaml"

echo "=========================================="
echo "Validating CloudFormation Template"
echo "=========================================="
echo "Template: $TEMPLATE_FILE"
echo "=========================================="

# Check if AWS CLI is installed
if ! command -v aws &> /dev/null; then
    echo "Error: AWS CLI is not installed"
    exit 1
fi

# Check if template file exists
if [ ! -f "$TEMPLATE_FILE" ]; then
    echo "Error: Template file $TEMPLATE_FILE not found"
    exit 1
fi

# Validate the template
echo "Validating template syntax..."
aws cloudformation validate-template \
    --template-body file://"$TEMPLATE_FILE" \
    > /dev/null

if [ $? -eq 0 ]; then
    echo "=========================================="
    echo "✓ Template is valid!"
    echo "=========================================="
    
    # Display template summary
    echo ""
    echo "Template Summary:"
    aws cloudformation validate-template \
        --template-body file://"$TEMPLATE_FILE" \
        --query '{Description:Description,Parameters:Parameters[*].ParameterKey}' \
        --output table
    
    echo ""
    echo "Resources defined:"
    grep -E "^  [A-Z].*Table:" "$TEMPLATE_FILE" | sed 's/://g'
else
    echo "=========================================="
    echo "✗ Template validation failed!"
    echo "=========================================="
    exit 1
fi
