#!/bin/bash
# Wrapper script to run the workflow CDK app
cd "$(dirname "$0")"
exec ./gradlew runWorkflowApp --quiet --console=plain
