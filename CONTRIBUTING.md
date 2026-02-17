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
# Run frontend
./gradlew :compose-frontend:composeApp:run

# Build for web
./gradlew :compose-frontend:composeApp:wasmJsBrowserRun
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
3. **Run Tests**: Ensure all tests pass
4. **Update CHANGELOG**: Document your changes
5. **Submit PR**: Create pull request with description

### PR Checklist

- [ ] Code follows style guidelines (ktlint, detekt, no FQN in types)
- [ ] Tests added/updated
- [ ] Documentation updated
- [ ] All tests pass
- [ ] No detekt issues

## Testing

Before submitting a PR:

```bash
# Run all backend tests
./gradlew :kotlin-backend:api:test

# Run core module tests
./gradlew :kotlin-backend:core:test

# Run all tests
./gradlew test

# Run with coverage
./gradlew jacocoTestReport
```

## Project Structure

```
vaultstadio/
├── kotlin-backend/          # Backend modules
│   ├── core/               # Domain logic, models, services
│   ├── plugins-api/        # Plugin SDK
│   ├── api/                # Ktor REST API
│   └── infrastructure/     # DB, storage implementations
├── compose-frontend/        # Compose Multiplatform
└── docker/                  # Docker configurations
```

## Documentation

- Update README.md for user-facing changes
- Update docs/api/API.md for API changes
- Add KDoc comments for public APIs
- Document complex logic with inline comments

## Questions?

Open an issue for questions or discussions.
