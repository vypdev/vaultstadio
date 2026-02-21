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
	@echo "  make test-coverage - Run tests and generate jacoco reports (backend + frontend)"
	@echo "  make dev           - Start development servers"
	@echo "  make clean         - Clean build artifacts"
	@echo ""
	@echo "Backend:"
	@echo "  make backend-build - Build backend only"
	@echo "  make backend-run   - Run backend server"
	@echo "  make backend-test  - Run backend tests"
	@echo ""
	@echo "Frontend (run from root; frontend is standalone in frontend/):"
	@echo "  make frontend-web        - Build web frontend (WASM production)"
	@echo "  make frontend-web-dev    - Build web frontend (WASM development)"
	@echo "  make frontend-run        - Run web frontend (WASM dev server)"
	@echo "  make frontend-run-prod   - Run web frontend (WASM production dev server)"
	@echo "  make desktop-run        - Run desktop app"
	@echo "  make android-build      - Build Android APK"
	@echo "  make frontend-test      - Run frontend tests"
	@echo "  (See also: cd frontend && make help)"
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
	@echo "Installing dependencies (backend + frontend)..."
	$(MAKE) -C backend build
	$(MAKE) -C frontend build

# ==================== Build ====================

build:
	@echo "Building backend (standalone)..."
	$(MAKE) -C backend build
	@echo "Building frontend (standalone)..."
	$(MAKE) -C frontend build

backend-build:
	@echo "Building backend..."
	$(MAKE) -C backend build

frontend-web:
	@echo "Building web frontend (WASM production)..."
	$(MAKE) -C frontend frontend-web

frontend-web-dev:
	@echo "Building web frontend (WASM development)..."
	$(MAKE) -C frontend frontend-web-dev

android-build:
	@echo "Building Android APK..."
	$(MAKE) -C frontend android-build

plugins-build:
	@echo "Building plugins..."
	$(MAKE) -C backend plugins

# ==================== Testing ====================

test:
	@echo "Running backend tests..."
	$(MAKE) -C backend test
	@echo "Running frontend tests..."
	$(MAKE) -C frontend test

backend-test:
	@echo "Running backend tests..."
	$(MAKE) -C backend test

frontend-test:
	@echo "Running frontend tests..."
	$(MAKE) -C frontend test

test-coverage:
	@echo "Running tests with coverage (backend + frontend, same as CI/Codecov)..."
	$(MAKE) -C backend test-coverage
	$(MAKE) -C frontend test-coverage
	@echo "Coverage reports:"
	@echo "  Backend:  backend/*/build/reports/jacoco/test/"
	@echo "  Frontend: frontend/composeApp/build/reports/jacoco/jacocoTestReport/"
	@echo "Validate codecov config: curl -s --data-binary @codecov.yml https://codecov.io/validate"

# ==================== Development ====================

dev:
	@echo "Starting development servers..."
	docker-compose -f docker/docker-compose.yml up -d postgres
	@echo "Waiting for PostgreSQL..."
	sleep 5
	$(MAKE) -C backend run &
	$(MAKE) -C frontend frontend-run

backend-run:
	@echo "Starting backend server..."
	$(MAKE) -C backend run

frontend-run:
	@echo "Starting web frontend (WASM dev server)..."
	$(MAKE) -C frontend frontend-run

frontend-run-prod:
	@echo "Starting web frontend (WASM production dev server)..."
	$(MAKE) -C frontend frontend-run-prod

desktop-run:
	@echo "Starting desktop app..."
	$(MAKE) -C frontend desktop-run

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
	$(MAKE) -C backend clean
	$(MAKE) -C frontend clean 2>/dev/null || true

docker-clean:
	@echo "Cleaning Docker resources..."
	docker-compose -f docker/docker-compose.yml down -v --rmi local
	docker system prune -f

# ==================== Release ====================

release:
	@echo "Creating release build..."
	$(MAKE) -C backend clean
	$(MAKE) -C backend build
	$(MAKE) -C backend release
	$(MAKE) -C frontend release
	@echo "Release artifacts created in build directories"

# ==================== Database ====================

db-migrate:
	@echo "Running database migrations..."
	$(MAKE) -C backend db-migrate

db-clean:
	@echo "Cleaning database..."
	$(MAKE) -C backend db-clean

# ==================== Linting & Code Quality ====================

lint:
	@echo "Running detekt (backend + root)..."
	$(MAKE) -C backend lint
	@echo "Run frontend lint from frontend/: cd frontend && ./gradlew detektMain"

lint-baseline:
	@echo "Generating detekt baseline..."
	$(MAKE) -C backend lint-baseline

lint-report:
	@echo "Running detekt with HTML report..."
	$(MAKE) -C backend lint-report
	@echo "Reports generated in backend/build/reports/detekt/"

# ==================== Documentation ====================

docs:
	@echo "Generating documentation..."
	$(MAKE) -C backend docs

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
