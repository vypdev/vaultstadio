# VaultStadio Makefile
#
# Convenient commands for development, testing, and deployment.

.PHONY: help install build test clean dev docker-build docker-push release lint lint-baseline lint-report

# Default target
help:
	@echo "VaultStadio - Development Commands"
	@echo ""
	@echo "Development:"
	@echo "  make install       - Install dependencies"
	@echo "  make build         - Build all modules"
	@echo "  make test          - Run all tests"
	@echo "  make dev           - Start development servers"
	@echo "  make clean         - Clean build artifacts"
	@echo ""
	@echo "Backend:"
	@echo "  make backend-build - Build backend only"
	@echo "  make backend-run   - Run backend server"
	@echo "  make backend-test  - Run backend tests"
	@echo ""
	@echo "Frontend:"
	@echo "  make frontend-web  - Build web frontend"
	@echo "  make frontend-run  - Run web frontend (dev)"
	@echo "  make desktop-run   - Run desktop app"
	@echo "  make android-build - Build Android APK"
	@echo ""
	@echo "Docker:"
	@echo "  make docker-build  - Build Docker images"
	@echo "  make docker-up     - Start Docker containers"
	@echo "  make docker-down   - Stop Docker containers"
	@echo "  make docker-logs   - View container logs"
	@echo "  make docker-push   - Push images to registry"
	@echo ""
	@echo "Plugins:"
	@echo "  make plugins-build - Build all plugins"
	@echo ""
	@echo "Code Quality:"
	@echo "  make lint          - Run detekt code analysis"
	@echo "  make lint-baseline - Generate detekt baseline"
	@echo ""
	@echo "Release:"
	@echo "  make release       - Create a release build"

# ==================== Installation ====================

install:
	@echo "Installing dependencies..."
	./gradlew build --refresh-dependencies -x test

# ==================== Build ====================

build:
	@echo "Building all modules..."
	./gradlew build -x test

backend-build:
	@echo "Building backend..."
	./gradlew :kotlin-backend:api:build -x test

frontend-web:
	@echo "Building web frontend..."
	./gradlew :compose-frontend:composeApp:wasmJsBrowserDistribution

android-build:
	@echo "Building Android APK..."
	./gradlew :compose-frontend:androidApp:assembleRelease

plugins-build:
	@echo "Building plugins..."
	./gradlew :kotlin-backend:plugins:image-metadata:pluginJar
	./gradlew :kotlin-backend:plugins:video-metadata:pluginJar

# ==================== Testing ====================

test:
	@echo "Running all tests..."
	./gradlew test

backend-test:
	@echo "Running backend tests..."
	./gradlew :kotlin-backend:core:test
	./gradlew :kotlin-backend:api:test

test-coverage:
	@echo "Running tests with coverage..."
	./gradlew test jacocoTestReport

# ==================== Development ====================

dev:
	@echo "Starting development servers..."
	docker-compose -f docker/docker-compose.yml up -d postgres
	@echo "Waiting for PostgreSQL..."
	sleep 5
	./gradlew :kotlin-backend:api:run &
	./gradlew :compose-frontend:composeApp:wasmJsBrowserRun

backend-run:
	@echo "Starting backend server..."
	./gradlew :kotlin-backend:api:run

frontend-run:
	@echo "Starting web frontend (dev)..."
	./gradlew :compose-frontend:composeApp:wasmJsBrowserRun

desktop-run:
	@echo "Starting desktop app..."
	./gradlew :compose-frontend:composeApp:run

# ==================== Docker ====================

docker-build:
	@echo "Building Docker images..."
	docker-compose -f docker/docker-compose.yml build

docker-up:
	@echo "Starting Docker containers..."
	docker-compose -f docker/docker-compose.yml up -d

docker-down:
	@echo "Stopping Docker containers..."
	docker-compose -f docker/docker-compose.yml down

docker-logs:
	@echo "Viewing container logs..."
	docker-compose -f docker/docker-compose.yml logs -f

docker-rebuild:
	@echo "Rebuilding and starting containers..."
	docker-compose -f docker/docker-compose.yml up --build -d

docker-push:
	@echo "Pushing Docker images..."
	docker-compose -f docker/docker-compose.yml push

# ==================== Cleanup ====================

clean:
	@echo "Cleaning build artifacts..."
	./gradlew clean
	rm -rf .gradle
	rm -rf kotlin-backend/*/build
	rm -rf compose-frontend/*/build
	rm -rf shared/build

docker-clean:
	@echo "Cleaning Docker resources..."
	docker-compose -f docker/docker-compose.yml down -v --rmi local
	docker system prune -f

# ==================== Release ====================

release:
	@echo "Creating release build..."
	./gradlew clean
	./gradlew build
	./gradlew :kotlin-backend:api:shadowJar
	./gradlew :compose-frontend:composeApp:wasmJsBrowserDistribution
	./gradlew :compose-frontend:androidApp:assembleRelease
	./gradlew :compose-frontend:composeApp:packageDistributionForCurrentOS
	@echo "Release artifacts created in build directories"

# ==================== Database ====================

db-migrate:
	@echo "Running database migrations..."
	./gradlew :kotlin-backend:api:flywayMigrate

db-clean:
	@echo "Cleaning database..."
	./gradlew :kotlin-backend:api:flywayClean

# ==================== Linting & Code Quality ====================

lint:
	@echo "Running detekt code analysis..."
	./gradlew detektMain

lint-baseline:
	@echo "Generating detekt baseline..."
	./gradlew detektBaseline

lint-report:
	@echo "Running detekt with HTML report..."
	./gradlew detektMain --continue
	@echo "Reports generated in build/reports/detekt/"

# ==================== Documentation ====================

docs:
	@echo "Generating documentation..."
	./gradlew dokkaHtml

# ==================== Helm ====================

helm-install:
	@echo "Installing Helm chart..."
	helm install vaultstadio ./helm/vaultstadio

helm-upgrade:
	@echo "Upgrading Helm release..."
	helm upgrade vaultstadio ./helm/vaultstadio

helm-uninstall:
	@echo "Uninstalling Helm release..."
	helm uninstall vaultstadio

helm-template:
	@echo "Rendering Helm templates..."
	helm template vaultstadio ./helm/vaultstadio
