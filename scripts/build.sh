#!/bin/bash
set -e

echo "Building VaultStadio..."

# Build backend
echo "Building backend..."
./gradlew :kotlin-backend:api:build -x test

# Build frontend (web)
echo "Building frontend..."
./gradlew :compose-frontend:composeApp:wasmJsBrowserDistribution

# Build Docker images (optional)
if [ "$1" = "--docker" ]; then
    echo "Building Docker images..."
    docker-compose -f docker/docker-compose.yml build
fi

echo "Build complete!"
