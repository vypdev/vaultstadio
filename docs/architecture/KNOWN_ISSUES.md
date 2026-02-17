# Known Issues & Technical Debt

This document tracks known architectural limitations and technical debt.

**Last Updated**: 2026-01-30

---

## Status: All Issues Resolved

All previously identified architectural issues have been resolved:

| Issue | Resolution Date | Solution |
|-------|-----------------|----------|
| Monolithic AppViewModel | 2026-01-30 | Split into 17 per-screen ViewModels |
| No Navigation Library | 2026-01-30 | Implemented Decompose navigation |
| Missing S3StorageBackend | 2026-01-30 | Implemented full S3-compatible backend |
| Duplicate DB Initialization | 2026-01-30 | Removed unused DatabaseFactory.kt |
| Inconsistent Error Handling | 2026-01-30 | Created RouteExtensions.kt |
| Federation Security | 2026-01-30 | Added key generation script |
| Legacy App.kt | 2026-01-30 | Replaced with VaultStadioRoot.kt |
| Hardcoded Strings | 2026-01-30 | Added to i18n system |
| Transaction Management | 2026-01-30 | Created TransactionManager |
| Missing Documentation | 2026-01-30 | Created 4 new guides |

## Current Architecture

The frontend now uses a clean Decompose-based architecture:

```
VaultStadioRoot.kt          # Entry point
├── navigation/
│   ├── RootComponent.kt    # Auth/Main split
│   └── MainDestination.kt  # Navigation destinations
├── feature/
│   ├── */Component.kt      # Decompose components
│   ├── */ViewModel.kt      # State management
│   └── */Content.kt        # UI adapters
└── ui/screens/             # Screen implementations
```

## Reporting New Issues

When identifying new technical debt:

1. Create an issue in the project tracker
2. Update this document with the new entry
3. Include: location, impact, proposed solution
4. Assign priority: Critical, High, Medium, Low

See [CONTRIBUTING.md](../CONTRIBUTING.md) for guidelines.
