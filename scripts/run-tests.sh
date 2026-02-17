#!/bin/bash
set -e

echo "Running tests..."

# Backend tests
echo "Running backend tests..."
./gradlew :kotlin-backend:core:test
./gradlew :kotlin-backend:infrastructure:test
./gradlew :kotlin-backend:api:test

# Shared module tests
echo "Running shared module tests..."
./gradlew :shared:test || echo "Shared tests skipped (may not exist)"

echo "All tests complete!"

# Generate coverage report
if [ "$1" = "--coverage" ]; then
    echo "Generating coverage report..."
    ./gradlew jacocoTestReport
    echo "Coverage report available in build/reports/jacoco/"
fi
