#!/bin/bash
set -e

echo "Running tests..."

# Backend tests
echo "Running backend tests..."
./gradlew :kotlin-backend:core:test
./gradlew :kotlin-backend:infrastructure:test
./gradlew :kotlin-backend:api:test

echo "All tests complete!"

# Generate coverage report
if [ "$1" = "--coverage" ]; then
    echo "Generating coverage report..."
    make test-coverage
    echo "Coverage reports: kotlin-backend/*/build/reports/jacoco/, compose-frontend/composeApp/build/reports/jacoco/"
fi
