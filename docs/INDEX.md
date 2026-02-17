# VaultStadio Documentation Index

**Last updated**: 2026-02-16

This is the central navigation for all VaultStadio documentation. Documents are organized by purpose and audience. For the directory layout and quick links by role, see [DOCS_STRUCTURE.md](DOCS_STRUCTURE.md).

---

## Start here

| I want to... | Read this |
|--------------|-----------|
| Understand what the project offers | [PROJECT_OVERVIEW.md](PROJECT_OVERVIEW.md) |
| Get started quickly | [getting-started/QUICK_START.md](getting-started/QUICK_START.md) |
| Look up a term | [GLOSSARY.md](GLOSSARY.md) |
| Understand the architecture | [architecture/ARCHITECTURE.md](architecture/ARCHITECTURE.md) |
| See API endpoints | [api/API.md](api/API.md) |
| Deploy to production | [operations/DEPLOYMENT.md](operations/DEPLOYMENT.md) |
| Develop a plugin | [plugins/PLUGIN_DEVELOPMENT.md](plugins/PLUGIN_DEVELOPMENT.md) |
| Contribute code | [../CONTRIBUTING.md](../CONTRIBUTING.md) |

---

## Documentation structure

### Getting started

| Document | Description | Audience |
|----------|-------------|----------|
| [getting-started/QUICK_START.md](getting-started/QUICK_START.md) | 5-minute setup guide with Docker, development, and TrueNAS options | All |
| [getting-started/CONFIGURATION.md](getting-started/CONFIGURATION.md) | Environment variables and settings reference | Operators |
| [getting-started/TROUBLESHOOTING.md](getting-started/TROUBLESHOOTING.md) | Common issues and solutions | All |

### Architecture and design

| Document | Description | Audience |
|----------|-------------|----------|
| [architecture/ARCHITECTURE.md](architecture/ARCHITECTURE.md) | System architecture, module structure, and design decisions | Developers |
| [architecture/FRONTEND_ARCHITECTURE.md](architecture/FRONTEND_ARCHITECTURE.md) | Compose Multiplatform patterns, ViewModel, navigation | Frontend Devs |
| [architecture/KNOWN_ISSUES.md](architecture/KNOWN_ISSUES.md) | Documented architectural limitations and technical debt | Developers |
| [architecture/DOCUMENTATION_ANALYSIS.md](architecture/DOCUMENTATION_ANALYSIS.md) | Suitability of docs for humans and AIs; recommendations | Maintainers |
| [GLOSSARY.md](GLOSSARY.md) | Terms and definitions | All |
| [DOCS_STRUCTURE.md](DOCS_STRUCTURE.md) | Directory layout and navigation by role | All |

### API and integration

| Document | Description | Audience |
|----------|-------------|----------|
| [api/API.md](api/API.md) | Complete REST API reference with examples and TOC | Developers |
| [api/PHASE6_ADVANCED_FEATURES.md](api/PHASE6_ADVANCED_FEATURES.md) | Versioning, Sync, WebDAV, S3, Federation, Collaboration | Developers |

### Development

| Document | Description | Audience |
|----------|-------------|----------|
| [development/CODE_QUALITY.md](development/CODE_QUALITY.md) | ktlint, detekt, FQN rules, pre-commit workflow | Developers |
| [development/PLATFORM_BUILD_REPORT.md](development/PLATFORM_BUILD_REPORT.md) | Build status and discrepancies per platform (backend, Desktop, Android, WASM, iOS) | Developers |
| [development/WASM_BUNDLE_OPTIMIZATION.md](development/WASM_BUNDLE_OPTIMIZATION.md) | WASM bundle size, lazy loading, and build optimizations | Frontend/Web |
| [development/AI_CODING_GUIDELINES.md](development/AI_CODING_GUIDELINES.md) | Guidelines for AI-assisted contributions | AI/Developers |
| [development/TESTING.md](development/TESTING.md) | Test strategy, coverage, and best practices | Developers |
| [../CONTRIBUTING.md](../CONTRIBUTING.md) | Contribution process and PR guidelines | Contributors |

### Plugins

| Document | Description | Audience |
|----------|-------------|----------|
| [plugins/PLUGINS.md](plugins/PLUGINS.md) | Built-in plugins overview and usage | All |
| [plugins/PLUGIN_DEVELOPMENT.md](plugins/PLUGIN_DEVELOPMENT.md) | How to create custom plugins | Plugin Devs |
| [plugins/AI_INTEGRATION.md](plugins/AI_INTEGRATION.md) | AI provider configuration (Ollama, LM Studio, OpenRouter) | Operators |
| [plugins/METADATA_EXTRACTION.md](plugins/METADATA_EXTRACTION.md) | Image, video, and document metadata details | Developers |

### Operations and deployment

| Document | Description | Audience |
|----------|-------------|----------|
| [operations/DEPLOYMENT.md](operations/DEPLOYMENT.md) | TrueNAS, Docker, Kubernetes deployment guides | Operators |
| [operations/DOCKER_BUILD.md](operations/DOCKER_BUILD.md) | Building Docker images | DevOps |
| [operations/STORAGE_CONFIGURATION.md](operations/STORAGE_CONFIGURATION.md) | Storage backend setup (local, S3) | Operators |
| [operations/SECURITY.md](operations/SECURITY.md) | Authentication, authorization, and security practices | Operators |
| [operations/BACKUP_RESTORE.md](operations/BACKUP_RESTORE.md) | Backup strategies and restore procedures | Operators |
| [operations/PERFORMANCE_TUNING.md](operations/PERFORMANCE_TUNING.md) | JVM, database, and application optimization | Operators |
| [operations/MONITORING.md](operations/MONITORING.md) | Metrics, logging, and observability setup | Operators |
| [operations/HIGH_AVAILABILITY.md](operations/HIGH_AVAILABILITY.md) | HA configurations and clustering | Operators |

### Frontend development

| Document | Description | Audience |
|----------|-------------|----------|
| [architecture/FRONTEND_ARCHITECTURE.md](architecture/FRONTEND_ARCHITECTURE.md) | Architecture overview and patterns | Frontend Devs |
| [frontend/FRONTEND_COMPONENTS.md](frontend/FRONTEND_COMPONENTS.md) | UI component library documentation | Frontend Devs |
| [frontend/FRONTEND_FEATURES.md](frontend/FRONTEND_FEATURES.md) | Feature implementation details | Frontend Devs |
| [frontend/FRONTEND_DEVELOPMENT.md](frontend/FRONTEND_DEVELOPMENT.md) | Development workflow and tools | Frontend Devs |
| [frontend/FRONTEND_TESTING.md](frontend/FRONTEND_TESTING.md) | Frontend testing strategies | Frontend Devs |

### Migration and upgrades

| Document | Description | Audience |
|----------|-------------|----------|
| [migration/MIGRATION.md](migration/MIGRATION.md) | Version migration guides | Operators |

---

## Cursor rules (AI context)

For AI-assisted development, context-aware rules are in `.cursor/rules/`:

| Rule file | Applies to | Purpose |
|-----------|------------|---------|
| `vaultstadio.mdc` | Always | Index of all rules |
| `vaultstadio-core.mdc` | Always | Language, structure, coding standards |
| `vaultstadio-commands.mdc` | Always | Build, run, test, Docker commands |
| `vaultstadio-quality.mdc` | Always | ktlint, detekt, FQN checks |
| `vaultstadio-backend.mdc` | `kotlin-backend/**` | Ktor, Koin, API conventions |
| `vaultstadio-frontend.mdc` | `compose-frontend/**` | Compose MP, ViewModel, expect/actual |
| `vaultstadio-api-reference.mdc` | API/shared files | Endpoint reference |
| `vaultstadio-plugins.mdc` | `plugins/**` | Plugin development |
| `vaultstadio-docker-deploy.mdc` | `docker/**`, `helm/**` | Deployment context |

---

## Document maintenance

When updating documentation:

1. **Keep INDEX.md updated** when adding or removing documents
2. **Cross-reference** related documents with links (use paths relative to repo root from README/CONTRIBUTING/rules; relative to current doc inside docs/)
3. **Use consistent formatting**: H1 for title, H2 for sections, tables for references
4. **Date sensitive content**: Mark version-specific information clearly; add "Last updated" to key docs
5. **Write in English**: All documentation must be in English
6. **Keep DOCS_STRUCTURE.md in sync** when changing the directory layout

---

## See also

- [README.md](../README.md) – Project overview and quick reference
- [CHANGELOG.md](../CHANGELOG.md) – Version history
- [LICENSE](../LICENSE) – MIT License
