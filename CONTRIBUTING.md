# Contributing to VaultStadio

Thank you for your interest in contributing to VaultStadio!

## Code of Conduct

- Be respectful and inclusive
- Welcome newcomers
- Focus on constructive feedback

## Development Setup

1. Fork the repository
2. Clone your fork
3. Set up development environment (see README.md)
4. Create a feature branch

### Prerequisites

- JDK 21+
- Gradle 8.10+
- Docker (for running tests with Testcontainers)

## Coding Standards

### Kotlin (Backend & Shared)

- Follow Kotlin coding conventions
- Use type inference where appropriate
- Maximum line length: 120 characters
- **Formatting**: ktlint (run `ktlintFormat` before commit)
- **Quality**: detekt (run after formatting)

```bash
# 1. Format code (fixes style)
./gradlew ktlintFormat

# 2. Run static analysis (detekt runs ktlintCheck first)
./gradlew detekt

# 3. Check no FQN in type positions (use imports instead)
./scripts/check-no-fqn.sh
```

See [docs/development/CODE_QUALITY.md](docs/development/CODE_QUALITY.md) for the full ktlint + detekt workflow. Do not use fully qualified type names in signatures or bodies when an import would suffice.

### AI-Assisted Contributions

If you use AI (e.g. Cursor) to contribute, follow [docs/development/AI_CODING_GUIDELINES.md](docs/development/AI_CODING_GUIDELINES.md): senior-level quality, tests for non-trivial logic, KDoc for public APIs, and for structural changes explain the reason, risks, and how to validate.

### Compose Multiplatform (Frontend)

- Follow Compose best practices
- Use Material 3 components
- Keep composables small and focused

```bash
# Run frontend (from repo root via Make, or from frontend/ with Gradle)
make desktop-run    # Desktop
make frontend-run   # Web (WASM development dev server)
make frontend-run-prod  # Web (WASM production dev server)

# Or from frontend/ directory:
# ./gradlew :composeApp:run
# ./gradlew :composeApp:wasmJsBrowserDevelopmentRun
# ./gradlew :composeApp:wasmJsBrowserProductionRun
```

## Commit Messages

Use clear, descriptive commit messages following conventional commits:

```
feat: Add file search functionality
fix: Resolve metadata extraction issue
docs: Update API documentation
test: Add tests for storage service
refactor: Simplify authentication flow
```

## Pull Request Process

1. **Update Documentation**: Update relevant documentation
2. **Add Tests**: Add tests for new features
3. **Run Tests**: Ensure all tests pass (`./gradlew test` or `make test`)
4. **Run Coverage (recommended)**: Run `make test-coverage` and check that new code is covered (CI uploads to Codecov; PR status check uses a 1% threshold)
5. **Update CHANGELOG**: Document your changes
6. **Submit PR**: Create pull request with description

### PR Checklist

- [ ] Code follows style guidelines (ktlint, detekt, no FQN in types)
- [ ] Tests added/updated
- [ ] Documentation updated
- [ ] All tests pass
- [ ] No detekt issues
- [ ] **Coverage:** Run `make test-coverage` before opening the PR; ensure new code has tests (Codecov status check enforces 1% threshold on patch coverage)

## Testing

Before submitting a PR:

```bash
# Run backend tests (from repo root)
./gradlew :backend:api:test
./gradlew :backend:core:test
# Or: make backend-test

# Run all tests (backend + frontend)
make test

# Run with coverage
make test-coverage
```

### Coverage

- **Before opening a PR**, run `make test-coverage` to generate backend and frontend coverage reports. This matches what CI runs and uploads to Codecov. The Codecov status check on PRs uses a **1% threshold** (patch coverage); avoid dropping coverage for new or changed code.
- **Goal:** Keep coverage ≥ 80% for new code where feasible. Avoid removing tests without adding equivalent coverage elsewhere.
- **Local HTML reports:** Backend: `backend/<module>/build/reports/jacoco/test/html/index.html`. Frontend: `frontend/composeApp/build/reports/jacoco/jacocoTestReport/html/index.html`.
- **Strategy and commands:** [docs/development/TESTING.md](docs/development/TESTING.md).
- **Current snapshot and per-module targets:** [docs/development/TEST_COVERAGE_ACTION_PLAN.md](docs/development/TEST_COVERAGE_ACTION_PLAN.md).

## Project Structure

```
vaultstadio/
├── backend/                 # Backend (root Gradle project :backend:*)
│   ├── core/               # Domain logic, models, services
│   ├── plugins-api/        # Plugin SDK
│   ├── api/                # Ktor REST API
│   └── infrastructure/     # DB, storage implementations
├── frontend/                # Compose Multiplatform (standalone Gradle project)
└── docker/                  # Docker configurations
```

## Documentation

- Update README.md for user-facing changes
- Update docs/api/API.md for API changes
- Add KDoc comments for public APIs
- Document complex logic with inline comments

## Questions?

Open an issue for questions or discussions.
