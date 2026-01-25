#!/bin/bash

# Standalone test runner for CeapLambda tests
# This script runs the file-based tests without requiring full Gradle compilation

echo "Running CeapLambda existence tests..."
echo "======================================"
echo ""

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CEAP_LAMBDA_PATH="infrastructure/src/main/kotlin/com/ceap/infrastructure/constructs/CeapLambda.kt"
OLD_SOLICITATION_LAMBDA_PATH="infrastructure/src/main/kotlin/com/ceap/infrastructure/constructs/SolicitationLambda.kt"

PASS_COUNT=0
FAIL_COUNT=0

# Test 1: CeapLambda.kt file exists
echo "Test 1: CeapLambda.kt file should exist"
if [ -f "$PROJECT_ROOT/$CEAP_LAMBDA_PATH" ]; then
    echo "  ✓ PASS: CeapLambda.kt file exists at $CEAP_LAMBDA_PATH"
    ((PASS_COUNT++))
else
    echo "  ✗ FAIL: CeapLambda.kt file does not exist at $CEAP_LAMBDA_PATH"
    ((FAIL_COUNT++))
fi
echo ""

# Test 2: Old SolicitationLambda.kt file does not exist
echo "Test 2: Old SolicitationLambda.kt file should not exist"
if [ ! -f "$PROJECT_ROOT/$OLD_SOLICITATION_LAMBDA_PATH" ]; then
    echo "  ✓ PASS: SolicitationLambda.kt file does not exist (correctly renamed)"
    ((PASS_COUNT++))
else
    echo "  ✗ FAIL: SolicitationLambda.kt file still exists at $OLD_SOLICITATION_LAMBDA_PATH"
    ((FAIL_COUNT++))
fi
echo ""

# Test 3: CeapLambda class is defined
echo "Test 3: CeapLambda class should be defined"
if grep -q "class CeapLambda" "$PROJECT_ROOT/$CEAP_LAMBDA_PATH"; then
    echo "  ✓ PASS: CeapLambda class is defined"
    ((PASS_COUNT++))
else
    echo "  ✗ FAIL: CeapLambda class is not defined"
    ((FAIL_COUNT++))
fi
echo ""

# Test 4: CeapLambda extends Construct
echo "Test 4: CeapLambda should extend Construct"
if grep -q ": Construct(scope, id)" "$PROJECT_ROOT/$CEAP_LAMBDA_PATH"; then
    echo "  ✓ PASS: CeapLambda extends Construct"
    ((PASS_COUNT++))
else
    echo "  ✗ FAIL: CeapLambda does not extend Construct"
    ((FAIL_COUNT++))
fi
echo ""

# Test 5: Constructor has required parameters
echo "Test 5: Constructor should have required parameters"
REQUIRED_PARAMS=("scope: Construct" "id: String" "handler: String" "jarPath: String")
ALL_PARAMS_FOUND=true

for param in "${REQUIRED_PARAMS[@]}"; do
    if ! grep -q "$param" "$PROJECT_ROOT/$CEAP_LAMBDA_PATH"; then
        echo "  ✗ Missing parameter: $param"
        ALL_PARAMS_FOUND=false
    fi
done

if [ "$ALL_PARAMS_FOUND" = true ]; then
    echo "  ✓ PASS: All required constructor parameters are present"
    ((PASS_COUNT++))
else
    echo "  ✗ FAIL: Some required constructor parameters are missing"
    ((FAIL_COUNT++))
fi
echo ""

# Test 6: Constructor has optional parameters with defaults
echo "Test 6: Constructor should have optional parameters with defaults"
OPTIONAL_PARAMS=(
    "environment: Map<String, String> = emptyMap()"
    "tables: List<ITable> = emptyList()"
    "memorySize: Int = 512"
    "timeout: Duration = Duration.minutes(1)"
    "logRetention: RetentionDays = RetentionDays.ONE_MONTH"
)
ALL_OPTIONAL_FOUND=true

for param in "${OPTIONAL_PARAMS[@]}"; do
    if ! grep -q "$param" "$PROJECT_ROOT/$CEAP_LAMBDA_PATH"; then
        echo "  ✗ Missing optional parameter: $param"
        ALL_OPTIONAL_FOUND=false
    fi
done

if [ "$ALL_OPTIONAL_FOUND" = true ]; then
    echo "  ✓ PASS: All optional constructor parameters with defaults are present"
    ((PASS_COUNT++))
else
    echo "  ✗ FAIL: Some optional constructor parameters are missing"
    ((FAIL_COUNT++))
fi
echo ""

# Test 7: Correct package declaration
echo "Test 7: Should have correct package declaration"
if grep -q "package com.ceap.infrastructure.constructs" "$PROJECT_ROOT/$CEAP_LAMBDA_PATH"; then
    echo "  ✓ PASS: Correct package declaration"
    ((PASS_COUNT++))
else
    echo "  ✗ FAIL: Incorrect or missing package declaration"
    ((FAIL_COUNT++))
fi
echo ""

# Test 8: Has function property
echo "Test 8: Should have a function property"
if grep -q "val function: Function" "$PROJECT_ROOT/$CEAP_LAMBDA_PATH"; then
    echo "  ✓ PASS: Function property is defined"
    ((PASS_COUNT++))
else
    echo "  ✗ FAIL: Function property is not defined"
    ((FAIL_COUNT++))
fi
echo ""

# Summary
echo "======================================"
echo "Test Summary:"
echo "  Passed: $PASS_COUNT"
echo "  Failed: $FAIL_COUNT"
echo "  Total:  $((PASS_COUNT + FAIL_COUNT))"
echo "======================================"

if [ $FAIL_COUNT -eq 0 ]; then
    echo "✓ All tests passed!"
    exit 0
else
    echo "✗ Some tests failed"
    exit 1
fi
