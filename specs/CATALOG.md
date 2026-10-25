# VaultStadio SDD Catalog

Generated from `specs/catalog.json`. The JSON catalog is authoritative.

| Capability ID | Status | Owning SDD | Last verified |
|---|---|---|---|
| `architecture-quality-and-product-contract` | Proposed | [SDD-0000-architecture-quality-and-product-contract.md](./SDD-0000-architecture-quality-and-product-contract.md) | not applicable |
| `standalone-projects-and-clean-architecture` | As-built baseline | [SDD-0001-standalone-projects-and-clean-architecture.md](./SDD-0001-standalone-projects-and-clean-architecture.md) | 2026-09-20 |
| `public-api-and-protocol-contract` | As-built baseline | [SDD-0002-public-api-and-protocol-contract.md](./SDD-0002-public-api-and-protocol-contract.md) | 2026-09-20 |
| `identity-authentication-and-authorization` | As-built baseline | [SDD-0003-identity-authentication-and-authorization.md](./SDD-0003-identity-authentication-and-authorization.md) | 2026-09-20 |
| `storage-domain-and-data-lifecycle` | As-built baseline | [SDD-0004-storage-domain-and-data-lifecycle.md](./SDD-0004-storage-domain-and-data-lifecycle.md) | 2026-09-20 |
| `upload-and-transfer-protocols` | As-built baseline | [SDD-0005-upload-and-transfer-protocols.md](./SDD-0005-upload-and-transfer-protocols.md) | 2026-09-20 |
| `sharing-and-permissions` | As-built baseline | [SDD-0006-sharing-and-permissions.md](./SDD-0006-sharing-and-permissions.md) | 2026-09-20 |
| `version-history-and-recovery` | As-built baseline | [SDD-0007-version-history-and-recovery.md](./SDD-0007-version-history-and-recovery.md) | 2026-09-20 |
| `device-sync-and-conflicts` | As-built baseline | [SDD-0008-device-sync-and-conflicts.md](./SDD-0008-device-sync-and-conflicts.md) | 2026-09-20 |
| `real-time-collaboration` | As-built baseline | [SDD-0009-real-time-collaboration.md](./SDD-0009-real-time-collaboration.md) | 2026-09-20 |
| `federation-and-trust` | Proposed | [SDD-0010-federation-and-trust.md](./SDD-0010-federation-and-trust.md) | not applicable |
| `plugin-runtime-and-extensibility` | As-built baseline | [SDD-0011-plugin-runtime-and-extensibility.md](./SDD-0011-plugin-runtime-and-extensibility.md) | 2026-09-20 |
| `metadata-search-and-ai` | Proposed | [SDD-0012-metadata-search-and-ai.md](./SDD-0012-metadata-search-and-ai.md) | not applicable |
| `frontend-kmp-and-platform-architecture` | As-built baseline | [SDD-0013-frontend-kmp-and-platform-architecture.md](./SDD-0013-frontend-kmp-and-platform-architecture.md) | 2026-09-20 |
| `deployment-operations-and-release` | As-built baseline | [SDD-0014-deployment-operations-and-release.md](./SDD-0014-deployment-operations-and-release.md) | 2026-09-20 |
| `security-privacy-and-threat-model` | Proposed | [SDD-0015-security-privacy-and-threat-model.md](./SDD-0015-security-privacy-and-threat-model.md) | not applicable |

## Catalog rules

- Each capability has one owning SDD path.
- `As-built baseline` records observed behavior and known debt; it is not a completeness claim.
- `Proposed` documents contain open decisions and cannot be treated as implemented.
- Evidence paths are checked by `python3 scripts/validate-specification-catalog.py`.
