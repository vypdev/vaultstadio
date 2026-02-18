# Documentation Structure

**Last updated**: 2026-02-16

This document describes how VaultStadio documentation is organized. It helps humans and AIs find the right document quickly.

---

## Principles

- **Single entry point**: [INDEX.md](INDEX.md) is the central navigation. Start here.
- **Layered by purpose**: Getting started → Architecture → API → Development → Operations.
- **Stable paths**: Links use relative paths from the repo root (e.g. `docs/architecture/ARCHITECTURE.md`).
- **English only**: All documentation is in English.

---

## Directory Layout

```
docs/
├── INDEX.md                    # Central navigation (start here)
├── PROJECT_OVERVIEW.md          # What the project offers (product + tech summary)
├── GLOSSARY.md                  # Terms and definitions
├── DOCS_STRUCTURE.md            # This file
│
├── getting-started/             # Setup and first run
│   ├── QUICK_START.md
│   ├── CONFIGURATION.md
│   └── TROUBLESHOOTING.md
│
├── architecture/                # System and frontend design
│   ├── ARCHITECTURE.md          # Backend and full system
│   ├── FRONTEND_ARCHITECTURE.md
│   ├── KNOWN_ISSUES.md
│   └── DOCUMENTATION_ANALYSIS.md
│
├── api/                         # REST API and Phase 6 protocols
│   ├── API.md                   # Full REST reference (with TOC)
│   └── PHASE6_ADVANCED_FEATURES.md
│
├── development/                 # Code quality, AI guidelines, testing
│   ├── CODE_QUALITY.md
│   ├── AI_CODING_GUIDELINES.md
│   ├── TESTING.md
│   └── TEST_COVERAGE_ACTION_PLAN.md
│
├── frontend/                    # Compose UI and components
│   ├── FRONTEND_COMPONENTS.md
│   ├── FRONTEND_FEATURES.md
│   ├── FRONTEND_DEVELOPMENT.md
│   └── FRONTEND_TESTING.md
│
├── plugins/                     # Plugin system and built-in plugins
│   ├── PLUGINS.md
│   ├── PLUGIN_DEVELOPMENT.md
│   ├── AI_INTEGRATION.md
│   └── METADATA_EXTRACTION.md
│
├── operations/                  # Deployment, security, monitoring
│   ├── DEPLOYMENT.md
│   ├── DOCKER_BUILD.md
│   ├── STORAGE_CONFIGURATION.md
│   ├── SECURITY.md
│   ├── BACKUP_RESTORE.md
│   ├── PERFORMANCE_TUNING.md
│   ├── MONITORING.md
│   └── HIGH_AVAILABILITY.md
│
└── migration/                   # Version upgrades
    └── MIGRATION.md
```

---

## Quick Links by Role

| Role | Start with | Then |
|------|------------|------|
| **New user** | [PROJECT_OVERVIEW.md](PROJECT_OVERVIEW.md), [getting-started/QUICK_START.md](getting-started/QUICK_START.md) | [architecture/ARCHITECTURE.md](architecture/ARCHITECTURE.md) |
| **Backend developer** | [architecture/ARCHITECTURE.md](architecture/ARCHITECTURE.md), [api/API.md](api/API.md) | [development/CODE_QUALITY.md](development/CODE_QUALITY.md) |
| **Frontend developer** | [architecture/FRONTEND_ARCHITECTURE.md](architecture/FRONTEND_ARCHITECTURE.md), [frontend/FRONTEND_FEATURES.md](frontend/FRONTEND_FEATURES.md) | [api/API.md](api/API.md) |
| **Plugin developer** | [plugins/PLUGINS.md](plugins/PLUGINS.md), [plugins/PLUGIN_DEVELOPMENT.md](plugins/PLUGIN_DEVELOPMENT.md) | [architecture/ARCHITECTURE.md](architecture/ARCHITECTURE.md) |
| **Operator / DevOps** | [getting-started/CONFIGURATION.md](getting-started/CONFIGURATION.md), [operations/DEPLOYMENT.md](operations/DEPLOYMENT.md) | [operations/SECURITY.md](operations/SECURITY.md), [operations/MONITORING.md](operations/MONITORING.md) |
| **AI assistant** | [INDEX.md](INDEX.md), [PROJECT_OVERVIEW.md](PROJECT_OVERVIEW.md), [development/AI_CODING_GUIDELINES.md](development/AI_CODING_GUIDELINES.md) | Context rules in `.cursor/rules/`; then architecture or api as needed |

---

## Cross-References

When adding or moving documents:

1. Update [INDEX.md](INDEX.md) so the new path appears in the correct section.
2. Use paths relative to the **repository root** in links from README, CONTRIBUTING, and `.cursor/rules/` (e.g. `docs/api/API.md`).
3. Use paths relative to **the current document** in links inside docs (e.g. from `architecture/ARCHITECTURE.md` to `../api/API.md` or `api/API.md` from INDEX).
4. Keep [DOCS_STRUCTURE.md](DOCS_STRUCTURE.md) in sync when adding new top-level sections.

---

## See Also

- [INDEX.md](INDEX.md) – Full document index and quick navigation
- [PROJECT_OVERVIEW.md](PROJECT_OVERVIEW.md) – What VaultStadio offers
- [GLOSSARY.md](GLOSSARY.md) – Term definitions
