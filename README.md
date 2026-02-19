# VaultStadio

<p align="center">
  <strong>Self-Hosted Storage Platform with Plugin Architecture</strong>
</p>

<p align="center">
  A modern, extensible file storage solution inspired by Google Drive, built for self-hosting on TrueNAS and other platforms.
</p>

<p align="center">
  <a href="#quick-start">Quick Start</a> •
  <a href="docs/PROJECT_OVERVIEW.md">Overview</a> •
  <a href="docs/architecture/ARCHITECTURE.md">Architecture</a> •
  <a href="docs/api/API.md">API</a> •
  <a href="docs/INDEX.md">Documentation</a>
</p>

---

## Features

### Core
- **Multi-Platform**: Web (WASM), Android, iOS, Desktop – built with Compose Multiplatform
- **Plugin Architecture**: Extend functionality without modifying core code
- **Secure Sharing**: Password-protected links with expiration and download limits
- **Modern Stack**: Kotlin, Ktor, Compose Multiplatform, PostgreSQL

### Advanced (Phase 6)
- **File Versioning**: Complete version history with restore and diff
- **Sync Protocol**: Multi-device synchronization with conflict resolution
- **Real-time Collaboration**: Multi-user editing with OT algorithm
- **Federation**: Cross-instance sharing
- **WebDAV & S3**: Mount as network drive or use AWS CLI

## Tech Stack

| Layer | Technology |
|-------|------------|
| Backend | Kotlin, Ktor, Coroutines, Arrow |
| Database | PostgreSQL, Exposed ORM, Flyway |
| Frontend | Compose Multiplatform |
| Shared | Kotlin Multiplatform |
| Container | Docker, Helm, Kubernetes |

## Quick Start

### Docker Compose (Recommended)

```bash
git clone https://github.com/yourusername/vaultstadio.git
cd vaultstadio

# Configure
cp docker/.env.example docker/.env
# Edit docker/.env: set POSTGRES_PASSWORD and VAULTSTADIO_JWT_SECRET

# Start
docker-compose -f docker/docker-compose.yml up -d

# Access: http://localhost (Web UI), http://localhost:8080 (API)
```

### Development

```bash
# Start PostgreSQL
docker-compose -f docker/docker-compose.yml up -d postgres

# Run backend (from repo root)
./gradlew :backend:api:run
# Or: make backend-run

# Run frontend (from repo root via Make, or from frontend/ with Gradle)
make desktop-run              # Desktop
make frontend-run             # Web (WASM development dev server)
make frontend-run-prod        # Web (WASM production dev server)
# Or: cd frontend && ./gradlew :composeApp:run
#     cd frontend && ./gradlew :composeApp:wasmJsBrowserDevelopmentRun
```

See [QUICK_START.md](docs/getting-started/QUICK_START.md) for TrueNAS deployment and more options.

## Project Structure

```
vaultstadio/
├── backend/            # Ktor backend (root Gradle project :backend:*)
├── frontend/            # Compose Multiplatform (standalone Gradle project)
├── docker/              # Docker Compose, Dockerfiles
├── helm/                # Kubernetes/TrueNAS Helm charts
└── docs/                # Documentation (see docs/INDEX.md)
```

## Documentation

| Document | Description |
|----------|-------------|
| [docs/INDEX.md](docs/INDEX.md) | **Start here** – Documentation navigation |
| [docs/PROJECT_OVERVIEW.md](docs/PROJECT_OVERVIEW.md) | What the project offers |
| [docs/getting-started/QUICK_START.md](docs/getting-started/QUICK_START.md) | Setup guide |
| [docs/architecture/ARCHITECTURE.md](docs/architecture/ARCHITECTURE.md) | System design |
| [docs/api/API.md](docs/api/API.md) | API reference |
| [docs/architecture/KNOWN_ISSUES.md](docs/architecture/KNOWN_ISSUES.md) | Known limitations |
| [CONTRIBUTING.md](CONTRIBUTING.md) | Contribution guidelines |

## Commands

```bash
# Build
make build                    # Backend + frontend
make backend-build            # Backend only
make frontend-web             # Frontend WASM production bundle

# Run
make backend-run              # Backend server
make frontend-run              # Web (WASM dev server)
make frontend-run-prod         # Web (WASM production dev server)
make desktop-run              # Desktop app

# Test
make test                     # Backend + frontend tests
make backend-test             # Backend only
make frontend-test            # Frontend only

# Quality (run before commit)
./gradlew ktlintFormat && ./scripts/check-no-fqn.sh && ./gradlew detekt

# Docker
make docker-up                # Start all
make docker-down              # Stop all
```

See [docs/INDEX.md](docs/INDEX.md) for complete command reference.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines. Key points:

1. Format with `ktlintFormat`
2. Check with `detekt` and `check-no-fqn.sh`
3. Add tests for new features
4. Write in English

## License

MIT License – see [LICENSE](LICENSE)

---

<p align="center">
  Made with Kotlin
</p>
