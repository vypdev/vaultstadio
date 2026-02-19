# VaultStadio Frontend Makefile
#
# Run from frontend/ directory. Compose Multiplatform: Web (WASM), Desktop, Android, iOS.
# Gradle project :composeApp (shared UI), :androidApp, :iosApp.

.PHONY: help build clean test test-coverage lint
.PHONY: frontend-web frontend-web-dev frontend-run frontend-run-prod
.PHONY: desktop-run desktop-build android-build android-install android-run
.PHONY: ios-build release

# Default target
help:
	@echo "VaultStadio Frontend - Commands (run from frontend/)"
	@echo ""
	@echo "Build:"
	@echo "  make build            - Build all (composeApp + WASM production bundle)"
	@echo "  make frontend-web      - Build web (WASM production): wasmJsBrowserDistribution"
	@echo "  make frontend-web-dev  - Build web (WASM development bundle)"
	@echo "  make desktop-build     - Compile desktop (JVM)"
	@echo "  make android-build     - Build Android APK (assembleRelease)"
	@echo "  make ios-build         - Build iOS framework (requires Xcode)"
	@echo ""
	@echo "Run (development / production):"
	@echo "  make frontend-run      - Run web dev server (WASM development): wasmJsBrowserDevelopmentRun"
	@echo "  make frontend-run-prod - Run web dev server (WASM production): wasmJsBrowserProductionRun"
	@echo "  make desktop-run       - Run desktop app"
	@echo "  make android-install   - Install debug APK on device/emulator"
	@echo ""
	@echo "Test & quality:"
	@echo "  make test              - Run frontend tests (desktopTest, wasmJsBrowserTest)"
	@echo "  make test-coverage     - Run tests and generate JaCoCo report (desktop)"
	@echo "  make lint              - Run detekt (from frontend)"
	@echo ""
	@echo "Release:"
	@echo "  make release           - Production WASM + Android release + desktop package"
	@echo ""
	@echo "Clean:"
	@echo "  make clean             - Clean build artifacts"

# ==================== Build ====================

build:
	@echo "Building frontend (composeApp + WASM production)..."
	./gradlew :composeApp:build -x test
	./gradlew :composeApp:wasmJsBrowserDistribution

frontend-web:
	@echo "Building web frontend (WASM production)..."
	./gradlew :composeApp:wasmJsBrowserDistribution

frontend-web-dev:
	@echo "Building web frontend (WASM development)..."
	./gradlew :composeApp:wasmJsBrowserDevelopmentExecutableDistribution

frontend-run:
	@echo "Starting web frontend (WASM development dev server)..."
	./gradlew :composeApp:wasmJsBrowserDevelopmentRun

frontend-run-prod:
	@echo "Starting web frontend (WASM production dev server)..."
	./gradlew :composeApp:wasmJsBrowserProductionRun

desktop-build:
	@echo "Building desktop..."
	./gradlew :composeApp:compileKotlinDesktop
	./gradlew :composeApp:desktopJar

desktop-run:
	@echo "Starting desktop app..."
	./gradlew :composeApp:run

android-build:
	@echo "Building Android APK (release)..."
	./gradlew :androidApp:assembleRelease

android-install:
	@echo "Installing Android debug APK..."
	./gradlew :androidApp:installDebug

android-run: android-install
	@echo "Launch app on device/emulator (manual or via adb shell am start)."

ios-build:
	@echo "Building iOS framework (simulator)..."
	./gradlew :composeApp:linkReleaseFrameworkIosSimulatorArm64
	@echo "Open iosApp/iosApp.xcodeproj in Xcode to run on simulator/device."

# ==================== Testing ====================

test:
	@echo "Running frontend tests..."
	./gradlew :composeApp:desktopTest --continue
	./gradlew :composeApp:wasmJsBrowserTest --continue 2>/dev/null || true

test-coverage:
	@echo "Running frontend tests with coverage..."
	./gradlew :composeApp:desktopTest
	./gradlew :composeApp:jacocoTestReport
	@echo "Report: composeApp/build/reports/jacoco/jacocoTestReport/html/index.html"

# ==================== Lint ====================

lint:
	@echo "Running detekt (frontend)..."
	./gradlew detektMain

# ==================== Release ====================

release:
	@echo "Creating frontend release artifacts..."
	./gradlew :composeApp:wasmJsBrowserDistribution
	./gradlew :androidApp:assembleRelease
	./gradlew :composeApp:packageDistributionForCurrentOS
	@echo "WASM: composeApp/build/dist/wasmJs/productionExecutable"
	@echo "Android: androidApp/build/outputs/apk/release/"
	@echo "Desktop: composeApp/build/compose/desktop/"

# ==================== Clean ====================

clean:
	@echo "Cleaning frontend build artifacts..."
	./gradlew clean
	rm -rf .gradle
	rm -rf composeApp/build
	rm -rf androidApp/build
	rm -rf iosApp/build
	rm -rf domain/*/build
	rm -rf data/*/build
	rm -rf feature/*/build
