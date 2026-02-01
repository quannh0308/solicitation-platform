#!/bin/bash

###############################################################################
# Workflow Integration Test Script
#
# Tests both Express and Standard workflows with various inputs including
# empty input (streaming mechanism).
###############################################################################

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Workflow Integration Tests${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# Test 1: Express Workflow with Normal Input
echo -e "${YELLOW}Test 1: Express Workflow (realtime) - Normal Input${NC}"
MESSAGE_ID=$(aws sqs send-message \
  --queue-url https://sqs.us-east-1.amazonaws.com/728093470684/ceap-workflow-realtime-queue \
  --message-body '{"test": "normal-data", "timestamp": "2026-02-01T15:00:00Z", "candidates": [{"id": 1, "score": 85}]}' \
  --query 'MessageId' \
  --output text)

echo -e "${GREEN}✓ Message sent: $MESSAGE_ID${NC}"
echo "Waiting for execution to start..."
sleep 5

# Check for execution
EXECUTION=$(aws stepfunctions list-executions \
  --state-machine-arn arn:aws:states:us-east-1:728093470684:stateMachine:Ceap-realtime-Workflow \
  --status-filter RUNNING \
  --max-results 1 \
  --query 'executions[0].executionArn' \
  --output text 2>/dev/null)

if [ "$EXECUTION" != "None" ] && [ -n "$EXECUTION" ]; then
    echo -e "${GREEN}✓ Execution started: $EXECUTION${NC}"
    EXEC_NAME=$(echo "$EXECUTION" | awk -F: '{print $NF}')
    
    # Wait for completion (Express should be fast)
    echo "Waiting for execution to complete..."
    for i in {1..30}; do
        STATUS=$(aws stepfunctions describe-execution \
          --execution-arn "$EXECUTION" \
          --query 'status' \
          --output text 2>/dev/null)
        
        if [ "$STATUS" = "SUCCEEDED" ]; then
            echo -e "${GREEN}✓ Execution completed successfully${NC}"
            break
        elif [ "$STATUS" = "FAILED" ]; then
            echo -e "${RED}✗ Execution failed${NC}"
            aws stepfunctions describe-execution --execution-arn "$EXECUTION"
            exit 1
        fi
        sleep 2
    done
    
    # Check S3 outputs
    echo "Checking S3 outputs..."
    for stage in ETLTask FilterTask ScoreTask StoreTask ReactiveTask; do
        if aws s3 ls "s3://ceap-workflow-realtime-728093470684/executions/$EXEC_NAME/$stage/" &>/dev/null; then
            echo -e "${GREEN}✓ $stage output exists${NC}"
        else
            echo -e "${YELLOW}⚠ $stage output not found${NC}"
        fi
    done
else
    echo -e "${YELLOW}⚠ No running execution found (may have completed already)${NC}"
fi
echo ""

# Test 2: Express Workflow with Empty Input (Streaming)
echo -e "${YELLOW}Test 2: Express Workflow (realtime) - Empty Input (Streaming)${NC}"
MESSAGE_ID=$(aws sqs send-message \
  --queue-url https://sqs.us-east-1.amazonaws.com/728093470684/ceap-workflow-realtime-queue \
  --message-body '{}' \
  --query 'MessageId' \
  --output text)

echo -e "${GREEN}✓ Empty message sent: $MESSAGE_ID${NC}"
echo "Waiting for execution..."
sleep 5

EXECUTION=$(aws stepfunctions list-executions \
  --state-machine-arn arn:aws:states:us-east-1:728093470684:stateMachine:Ceap-realtime-Workflow \
  --status-filter RUNNING \
  --max-results 1 \
  --query 'executions[0].executionArn' \
  --output text 2>/dev/null)

if [ "$EXECUTION" != "None" ] && [ -n "$EXECUTION" ]; then
    echo -e "${GREEN}✓ Execution started with empty input${NC}"
    
    # Wait for completion
    for i in {1..30}; do
        STATUS=$(aws stepfunctions describe-execution \
          --execution-arn "$EXECUTION" \
          --query 'status' \
          --output text 2>/dev/null)
        
        if [ "$STATUS" = "SUCCEEDED" ]; then
            echo -e "${GREEN}✓ Empty input processed successfully (streaming mechanism works)${NC}"
            break
        elif [ "$STATUS" = "FAILED" ]; then
            echo -e "${RED}✗ Execution failed with empty input${NC}"
            aws stepfunctions describe-execution --execution-arn "$EXECUTION"
            exit 1
        fi
        sleep 2
    done
else
    echo -e "${YELLOW}⚠ No running execution found${NC}"
fi
echo ""

# Test 3: Standard Workflow with Normal Input
echo -e "${YELLOW}Test 3: Standard Workflow (batch) - Normal Input${NC}"
MESSAGE_ID=$(aws sqs send-message \
  --queue-url https://sqs.us-east-1.amazonaws.com/728093470684/ceap-workflow-batch-queue \
  --message-body '{"test": "batch-data", "timestamp": "2026-02-01T15:00:00Z", "candidates": [{"id": 1, "score": 85}, {"id": 2, "score": 92}]}' \
  --query 'MessageId' \
  --output text)

echo -e "${GREEN}✓ Message sent: $MESSAGE_ID${NC}"
echo "Waiting for execution to start..."
sleep 5

EXECUTION=$(aws stepfunctions list-executions \
  --state-machine-arn arn:aws:states:us-east-1:728093470684:stateMachine:Ceap-batch-Workflow \
  --status-filter RUNNING \
  --max-results 1 \
  --query 'executions[0].executionArn' \
  --output text 2>/dev/null)

if [ "$EXECUTION" != "None" ] && [ -n "$EXECUTION" ]; then
    echo -e "${GREEN}✓ Execution started: $EXECUTION${NC}"
    echo -e "${YELLOW}Note: Standard workflow with Glue will take ~2 hours to complete${NC}"
    echo -e "${YELLOW}Monitor progress: aws stepfunctions describe-execution --execution-arn $EXECUTION${NC}"
else
    echo -e "${YELLOW}⚠ No running execution found${NC}"
fi
echo ""

# Test 4: Standard Workflow with Empty Input (Streaming)
echo -e "${YELLOW}Test 4: Standard Workflow (batch) - Empty Input (Streaming)${NC}"
MESSAGE_ID=$(aws sqs send-message \
  --queue-url https://sqs.us-east-1.amazonaws.com/728093470684/ceap-workflow-batch-queue \
  --message-body '{}' \
  --query 'MessageId' \
  --output text)

echo -e "${GREEN}✓ Empty message sent: $MESSAGE_ID${NC}"
echo "Waiting for execution..."
sleep 5

EXECUTION=$(aws stepfunctions list-executions \
  --state-machine-arn arn:aws:states:us-east-1:728093470684:stateMachine:Ceap-batch-Workflow \
  --status-filter RUNNING \
  --max-results 1 \
  --query 'executions[0].executionArn' \
  --output text 2>/dev/null)

if [ "$EXECUTION" != "None" ] && [ -n "$EXECUTION" ]; then
    echo -e "${GREEN}✓ Execution started with empty input${NC}"
    echo -e "${YELLOW}Note: Will take ~2 hours to complete${NC}"
else
    echo -e "${YELLOW}⚠ No running execution found${NC}"
fi
echo ""

# Summary
echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Test Summary${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""
echo "Tests executed:"
echo "  1. Express workflow - Normal input"
echo "  2. Express workflow - Empty input (streaming)"
echo "  3. Standard workflow - Normal input"
echo "  4. Standard workflow - Empty input (streaming)"
echo ""
echo "Monitor executions:"
echo "  Express:  aws stepfunctions list-executions --state-machine-arn arn:aws:states:us-east-1:728093470684:stateMachine:Ceap-realtime-Workflow"
echo "  Standard: aws stepfunctions list-executions --state-machine-arn arn:aws:states:us-east-1:728093470684:stateMachine:Ceap-batch-Workflow"
echo ""
echo "Check S3 outputs:"
echo "  Express:  aws s3 ls s3://ceap-workflow-realtime-728093470684/executions/ --recursive"
echo "  Standard: aws s3 ls s3://ceap-workflow-batch-728093470684/executions/ --recursive"
echo ""
