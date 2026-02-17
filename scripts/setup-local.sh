#!/bin/bash
set -e

echo "Setting up VaultStadio for local development..."

# Create directories for local development
echo "Creating directories..."
mkdir -p data/storage
mkdir -p data/temp
mkdir -p data/plugins
mkdir -p docker/storage
mkdir -p docker/data

# Set permissions
echo "Setting permissions..."
chmod -R 755 data
chmod -R 755 docker/storage
chmod -R 755 docker/data

# Start PostgreSQL
echo "Starting PostgreSQL..."
docker-compose -f docker/docker-compose.yml up -d postgres

echo "Waiting for PostgreSQL to be ready..."
sleep 5

# Run migrations (if needed)
echo "Running database migrations..."
./gradlew :kotlin-backend:api:flywayMigrate 2>/dev/null || echo "Migrations skipped (may already be applied)"

echo ""
echo "Setup complete!"
echo ""
echo "To run the backend:"
echo "   ./gradlew :kotlin-backend:api:run"
echo ""
echo "To run the web frontend:"
echo "   ./gradlew :compose-frontend:composeApp:wasmJsBrowserRun"
echo ""
echo "To run the desktop app:"
echo "   ./gradlew :compose-frontend:composeApp:run"
echo ""
echo "Access:"
echo "   Backend API: http://localhost:8080"
echo "   API Docs: http://localhost:8080/swagger-ui"
echo ""
echo "To stop PostgreSQL:"
echo "   docker-compose -f docker/docker-compose.yml down"
