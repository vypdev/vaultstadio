# VaultStadio Backend Makefile
#
# Backend is a standalone Gradle project. Run from repo root:
#   make backend-build, make backend-run, make backend-test
# Or from backend/ directory: make build, make run, make test

.PHONY: help build run test test-coverage clean plugins db-migrate db-clean release lint lint-baseline lint-report docs

help:
	@echo "VaultStadio Backend - Commands"
	@echo ""
	@echo "From repo root: make backend-build, make backend-run, make backend-test"
	@echo "From backend/ directory:"
	@echo "  make build         - Build API and all modules"
	@echo "  make run           - Run API server"
	@echo "  make test          - Run tests"
	@echo "  make test-coverage - Jacoco reports"
	@echo "  make plugins       - Build plugin JARs"
	@echo "  make db-migrate    - Flyway migrate"
	@echo "  make db-clean      - Flyway clean"
	@echo "  make release       - shadowJar for API"
	@echo "  make lint          - detekt"
	@echo "  make clean         - Clean build artifacts"

build:
	./gradlew build -x test

run:
	./gradlew :api:run

test:
	./gradlew :core:test :api:test

test-coverage:
	./gradlew :core:jacocoTestReport \
		:api:jacocoTestReport \
		:infrastructure:jacocoTestReport \
		:plugins-api:jacocoTestReport \
		:plugins:image-metadata:jacocoTestReport \
		:plugins:video-metadata:jacocoTestReport \
		:plugins:fulltext-search:jacocoTestReport \
		:plugins:ai-classification:jacocoTestReport \
		--continue

plugins:
	./gradlew :plugins:image-metadata:pluginJar :plugins:video-metadata:pluginJar

db-migrate:
	./gradlew :api:flywayMigrate

db-clean:
	./gradlew :api:flywayClean

release:
	./gradlew :api:shadowJar

lint:
	./gradlew detektMain

lint-baseline:
	./gradlew detektBaseline

lint-report:
	./gradlew detektMain --continue
	@echo "Reports in build/reports/detekt/"

clean:
	./gradlew clean
	rm -rf .gradle
	rm -rf */build
	rm -rf plugins/*/build

docs:
	./gradlew dokkaHtml 2>/dev/null || echo "Dokka not configured; add plugin to backend root build if needed."
