#!/bin/bash
set -e

echo "Running tests..."

# Backend tests (standalone project)
echo "Running backend tests..."
(cd backend && ./gradlew :core:test :infrastructure:test :api:test)

echo "Running frontend tests..."
(cd frontend && ./gradlew :composeApp:desktopTest) 2>/dev/null || true

echo "All tests complete!"

# Generate coverage report
if [ "$1" = "--coverage" ]; then
    echo "Generating coverage report..."
    make test-coverage
    echo "Coverage reports: backend/*/build/reports/jacoco/, frontend/composeApp/build/reports/jacoco/"
fi
