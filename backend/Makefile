# VaultStadio Backend Makefile
#
# Backend is part of the root Gradle project. Run these from repo root for correct paths:
#   make backend-build
#   make backend-run
#   make backend-test
# Or from backend/ directory: make -f Makefile run (invokes root Makefile).

ROOT := $(dir $(lastword $(MAKEFILE_LIST)))..

.PHONY: help build run test test-coverage clean plugins

help:
	@echo "VaultStadio Backend - Commands"
	@echo ""
	@echo "Backend is part of the root project. From repo root run:"
	@echo "  make backend-build   - Build backend (:backend:api)"
	@echo "  make backend-run     - Run backend server"
	@echo "  make backend-test    - Run backend tests"
	@echo "  make plugins-build   - Build plugin JARs"
	@echo "  make db-migrate      - Run Flyway migrations"
	@echo ""
	@echo "From backend/ directory (delegates to root):"
	@echo "  make build   - same as backend-build"
	@echo "  make run     - same as backend-run"
	@echo "  make test    - same as backend-test"

build:
	$(MAKE) -C $(ROOT) backend-build

run:
	$(MAKE) -C $(ROOT) backend-run

test:
	$(MAKE) -C $(ROOT) backend-test

test-coverage:
	$(MAKE) -C $(ROOT) test-coverage

plugins:
	$(MAKE) -C $(ROOT) plugins-build

clean:
	$(MAKE) -C $(ROOT) clean
