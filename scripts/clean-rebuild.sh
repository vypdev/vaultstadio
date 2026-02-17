#!/bin/bash
# =============================================================================
# clean-rebuild.sh
# =============================================================================
# Cleans Docker resources, data directories, and rebuilds containers from scratch.
#
# Usage:
#   ./scripts/clean-rebuild.sh [--keep-storage]
#
# Options:
#   --keep-storage  Keep the storage directory but clean everything else
#
# =============================================================================

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Project root directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

# Parse arguments
KEEP_STORAGE=false
for arg in "$@"; do
    case $arg in
        --keep-storage)
            KEEP_STORAGE=true
            shift
            ;;
    esac
done

echo -e "${BLUE}=======================================${NC}"
echo -e "${BLUE}  VaultStadio - Clean & Rebuild Script  ${NC}"
echo -e "${BLUE}=======================================${NC}"
echo ""

# Step 1: Stop and remove containers
echo -e "${YELLOW}[1/6] Stopping and removing containers...${NC}"
cd "$PROJECT_ROOT"
docker-compose -f docker/docker-compose.yml down -v --rmi all 2>/dev/null || true
echo -e "${GREEN}✓ Containers stopped and removed${NC}"
echo ""

# Step 2: Docker system prune
echo -e "${YELLOW}[2/6] Cleaning Docker system (prune)...${NC}"
docker system prune -af --volumes
echo -e "${GREEN}✓ Docker system cleaned${NC}"
echo ""

# Step 3: Clean Gradle build
echo -e "${YELLOW}[3/6] Cleaning Gradle build...${NC}"
./gradlew clean 2>/dev/null || true
rm -rf .gradle
rm -rf kotlin-backend/*/build
rm -rf compose-frontend/*/build
rm -rf shared/build
echo -e "${GREEN}✓ Gradle build cleaned${NC}"
echo ""

# Step 4: Clean data directory
echo -e "${YELLOW}[4/6] Cleaning data directory...${NC}"
if [ -d "$PROJECT_ROOT/docker/data" ]; then
    rm -rf "$PROJECT_ROOT/docker/data"/*
    echo -e "${GREEN}✓ Data directory cleaned${NC}"
else
    mkdir -p "$PROJECT_ROOT/docker/data"
    echo -e "${GREEN}✓ Data directory created${NC}"
fi
echo ""

# Step 5: Clean storage directory (unless --keep-storage is specified)
echo -e "${YELLOW}[5/6] Cleaning storage directory...${NC}"
if [ "$KEEP_STORAGE" = true ]; then
    echo -e "${YELLOW}⚠ Skipping storage cleanup (--keep-storage flag)${NC}"
else
    if [ -d "$PROJECT_ROOT/docker/storage" ]; then
        find "$PROJECT_ROOT/docker/storage" -mindepth 1 -delete 2>/dev/null || true
        echo -e "${GREEN}✓ Storage directory cleaned${NC}"
    else
        mkdir -p "$PROJECT_ROOT/docker/storage"
        echo -e "${GREEN}✓ Storage directory created${NC}"
    fi
fi
echo ""

# Step 6: Rebuild and start containers
echo -e "${YELLOW}[6/6] Rebuilding and starting containers...${NC}"
cd "$PROJECT_ROOT"
docker-compose -f docker/docker-compose.yml build --no-cache
docker-compose -f docker/docker-compose.yml up -d

echo ""
echo -e "${GREEN}=======================================${NC}"
echo -e "${GREEN}  Clean & Rebuild Complete!           ${NC}"
echo -e "${GREEN}=======================================${NC}"
echo ""
echo -e "Frontend: ${BLUE}http://localhost${NC}"
echo -e "Backend:  ${BLUE}http://localhost:8080${NC}"
echo -e "Swagger:  ${BLUE}http://localhost:8080/swagger-ui${NC}"
echo ""

# Show container status
echo -e "${YELLOW}Container status:${NC}"
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
