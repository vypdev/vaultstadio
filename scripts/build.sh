#!/bin/bash
set -e

echo "Building VaultStadio..."

# Build backend
echo "Building backend..."
./gradlew :backend:api:build -x test

# Build frontend (web WASM production)
echo "Building frontend..."
make -C frontend frontend-web

# Build Docker images (optional)
if [ "$1" = "--docker" ]; then
    echo "Building Docker images..."
    docker-compose -f docker/docker-compose.yml build
fi

echo "Build complete!"
