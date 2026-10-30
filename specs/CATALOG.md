# Product capability specification catalog

> Generated from [`catalog.json`](./catalog.json). Do not edit this table by hand.
> Run `pnpm run generate:specifications` after changing catalog metadata.

This catalog answers which product contract owns a capability and where its
implementation, verification, workflow, and user-documentation evidence lives.
An **As-built baseline** records verified current behavior; it does not hide known
debt or convert unknown historic intent into a design decision.

| Capability ID | Status | Scope | Primary SDD | Evidence |
|---|---|---|---|---|
| `architecture-quality-and-product-contract` | Proposed | Coordinate the SDD portfolio, architectural ratchets, evidence policy, and ordered remediation of documentation and contract drift. | [Architecture Quality and Product Contract](./sdd-0000-architecture-quality-and-product-contract.md) + 1 companion | 12 paths · 2026-10-30 |
| `standalone-projects-and-clean-architecture` | As-built baseline | Record the verified backend/frontend project topology, dependency direction, composition ownership, and architectural constraints. | [Standalone Projects and Clean Architecture](./sdd-0001-standalone-projects-and-clean-architecture.md) | 11 paths · 2026-10-30 |
| `public-api-and-protocol-contract` | As-built baseline | Define the authoritative HTTP, OpenAPI, WebDAV, S3-compatible, and collaboration protocol surfaces. | [Public API and Protocol Contract](./sdd-0002-public-api-and-protocol-contract.md) | 12 paths · 2026-10-30 |
| `identity-authentication-and-authorization` | As-built baseline | Record user identity, JWT lifecycle, roles, permissions, and protocol-specific authentication boundaries. | [Identity, Authentication and Authorization](./sdd-0003-identity-authentication-and-authorization.md) | 8 paths · 2026-10-30 |
| `storage-domain-and-data-lifecycle` | As-built baseline | Define files, folders, metadata, ownership, quotas, trash, storage backends, and content lifecycle. | [Storage Domain and Data Lifecycle](./sdd-0004-storage-domain-and-data-lifecycle.md) | 15 paths · 2026-10-30 |
| `upload-and-transfer-protocols` | As-built baseline | Define normal, chunked, folder, multipart, WebDAV, and S3-compatible transfer lifecycles. | [Upload and Transfer Protocols](./sdd-0005-upload-and-transfer-protocols.md) | 7 paths · 2026-10-30 |
| `sharing-and-permissions` | As-built baseline | Define internal shares, public share tokens, permission inheritance, expiration, revocation, and shared-with-me behavior. | [Sharing and Permissions](./sdd-0006-sharing-and-permissions.md) | 13 paths · 2026-10-30 |
| `version-history-and-recovery` | As-built baseline | Define file versions, history, diffs, restore, retention, deletion, and system recovery boundaries. | [Version History and Recovery](./sdd-0007-version-history-and-recovery.md) | 9 paths · 2026-10-30 |
| `device-sync-and-conflicts` | As-built baseline | Define device registration, delta synchronization, conflict detection, replay, deactivation, and recovery. | [Device Sync and Conflict Resolution](./sdd-0008-device-sync-and-conflicts.md) | 6 paths · 2026-10-30 |
| `real-time-collaboration` | As-built baseline | Define collaboration sessions, WebSocket lifecycle, presence, operations, OT, comments, reconnect, and participant limits. | [Real-Time Collaboration](./sdd-0009-real-time-collaboration.md) | 9 paths · 2026-10-30 |
| `federation-and-trust` | Proposed | Define cross-instance identity, signed messages, federated shares, activities, trust, key lifecycle, and failure behavior. | [Federation and Trust](./sdd-0010-federation-and-trust.md) | 8 paths · 2026-10-30 |
| `plugin-runtime-and-extensibility` | As-built baseline | Define plugin discovery, lifecycle, API, endpoint exposure, configuration, isolation, permissions, and failure containment. | [Plugin Runtime and Extensibility](./sdd-0011-plugin-runtime-and-extensibility.md) | 10 paths · 2026-10-30 |
| `metadata-search-and-ai` | Proposed | Define metadata extraction, indexing/search, AI-assisted classification, provider boundaries, privacy, and readiness evidence. | [Metadata, Search and AI](./sdd-0012-metadata-search-and-ai.md) + 1 companion | 14 paths · 2026-10-30 |
| `frontend-kmp-and-platform-architecture` | As-built baseline | Define frontend domain/data/feature boundaries, navigation, state ownership, platform entrypoints, and user-facing capability mapping. | [Frontend KMP and Platform Architecture](./sdd-0013-frontend-kmp-and-platform-architecture.md) | 9 paths · 2026-10-30 |
| `deployment-operations-and-release` | As-built baseline | Define Docker, Helm, configuration, health, migrations, backup, CI, release, versioning, rollback, and operator evidence. | [Deployment, Operations and Release](./sdd-0014-deployment-operations-and-release.md) | 14 paths · 2026-10-30 |
| `security-privacy-and-threat-model` | Proposed | Define cross-cutting threat models, privacy rules, trust boundaries, abuse controls, logging constraints, and security evidence. | [Security, Privacy and Threat Model](./sdd-0015-security-privacy-and-threat-model.md) | 11 paths · 2026-10-30 |

## Evidence map

### `architecture-quality-and-product-contract` — Architecture Quality and Product Contract

- Owner: VaultStadio maintainers
- Last verified: 2026-10-30
- Specifications: [`specs/sdd-0000-architecture-quality-and-product-contract.md`](./sdd-0000-architecture-quality-and-product-contract.md) · [`specs/product-discovery-2026-10-29.md`](./product-discovery-2026-10-29.md)
- Workflows: Not applicable for this capability.
- Entrypoints: [`scripts/validate-specification-catalog.py`](../scripts/validate-specification-catalog.py)
- Core code: [`scripts/specification_catalog.py`](../scripts/specification_catalog.py)
- Tests: [`scripts/tests/test_specification_catalog.py`](../scripts/tests/test_specification_catalog.py)
- User documentation: [`docs/development/REPOSITORY_AUDIT.md`](../docs/development/REPOSITORY_AUDIT.md) · [`docs/architecture/ARCHITECTURE.md`](../docs/architecture/ARCHITECTURE.md) · [`docs/architecture/BACKEND_CLEAN_ARCHITECTURE_PROPOSAL.md`](../docs/architecture/BACKEND_CLEAN_ARCHITECTURE_PROPOSAL.md) · [`docs/development/REPOSITORY_AUDIT_2026-10-25.md`](../docs/development/REPOSITORY_AUDIT_2026-10-25.md) · [`docs/development/audits/2026-10-25/verification-summary.json`](../docs/development/audits/2026-10-25/verification-summary.json) · [`docs/development/PRODUCT_ROADMAP_2026-10-29.md`](../docs/development/PRODUCT_ROADMAP_2026-10-29.md) · [`docs/development/verification/SDD_CONSUMER_ADOPTION_145.md`](../docs/development/verification/SDD_CONSUMER_ADOPTION_145.md) · [`docs/development/verification/sdd-owner-path-migration-178.json`](../docs/development/verification/sdd-owner-path-migration-178.json) · [`docs/development/verification/SDD_OWNER_PATHS_178.md`](../docs/development/verification/SDD_OWNER_PATHS_178.md)

### `standalone-projects-and-clean-architecture` — Standalone Projects and Clean Architecture

- Owner: VaultStadio maintainers
- Last verified: 2026-10-30
- Specifications: [`specs/sdd-0001-standalone-projects-and-clean-architecture.md`](./sdd-0001-standalone-projects-and-clean-architecture.md)
- Workflows: Not applicable for this capability.
- Entrypoints: [`backend/settings.gradle.kts`](../backend/settings.gradle.kts) · [`frontend/settings.gradle.kts`](../frontend/settings.gradle.kts)
- Core code: [`backend/build.gradle.kts`](../backend/build.gradle.kts) · [`frontend/build.gradle.kts`](../frontend/build.gradle.kts)
- Tests: [`scripts/tests/test_selection_architecture.py`](../scripts/tests/test_selection_architecture.py) · [`scripts/tests/test_verification_tooling.py`](../scripts/tests/test_verification_tooling.py)
- User documentation: [`docs/architecture/ARCHITECTURE.md`](../docs/architecture/ARCHITECTURE.md) · [`docs/architecture/CLEAN_ARCHITECTURE_REVIEW.md`](../docs/architecture/CLEAN_ARCHITECTURE_REVIEW.md) · [`docs/architecture/FRONTEND_ARCHITECTURE.md`](../docs/architecture/FRONTEND_ARCHITECTURE.md) · [`docs/development/REPOSITORY_AUDIT_2026-10-25.md`](../docs/development/REPOSITORY_AUDIT_2026-10-25.md) · [`docs/development/audits/2026-10-25/verification-summary.json`](../docs/development/audits/2026-10-25/verification-summary.json)

### `public-api-and-protocol-contract` — Public API and Protocol Contract

- Owner: VaultStadio maintainers
- Last verified: 2026-10-30
- Specifications: [`specs/sdd-0002-public-api-and-protocol-contract.md`](./sdd-0002-public-api-and-protocol-contract.md)
- Workflows: Not applicable for this capability.
- Entrypoints: [`backend/api/src/main/kotlin/com/vaultstadio/api/config/Routing.kt`](../backend/api/src/main/kotlin/com/vaultstadio/api/config/Routing.kt)
- Core code: [`backend/api/src/main/kotlin/com/vaultstadio/api/routes/storage/StorageRoutes.kt`](../backend/api/src/main/kotlin/com/vaultstadio/api/routes/storage/StorageRoutes.kt)
- Tests: [`backend/api/src/test/kotlin/com/vaultstadio/api/routes/StorageRoutesTest.kt`](../backend/api/src/test/kotlin/com/vaultstadio/api/routes/StorageRoutesTest.kt)
- User documentation: [`docs/api/API.md`](../docs/api/API.md) · [`docs/development/REPOSITORY_AUDIT_2026-10-25.md`](../docs/development/REPOSITORY_AUDIT_2026-10-25.md) · [`docs/development/audits/2026-10-25/verification-summary.json`](../docs/development/audits/2026-10-25/verification-summary.json) · [`docs/development/verification/STORAGE_SELECTION_91.md`](../docs/development/verification/STORAGE_SELECTION_91.md) · [`docs/development/verification/storage-selection-91.json`](../docs/development/verification/storage-selection-91.json) · [`docs/development/verification/storage-selection-91-cases.json`](../docs/development/verification/storage-selection-91-cases.json) · [`docs/development/verification/storage-selection-91-coverage.json`](../docs/development/verification/storage-selection-91-coverage.json) · [`docs/development/verification/STORAGE_SELECTION_91_REVIEW.md`](../docs/development/verification/STORAGE_SELECTION_91_REVIEW.md) · [`docs/development/verification/storage-selection-91-review.json`](../docs/development/verification/storage-selection-91-review.json)

### `identity-authentication-and-authorization` — Identity, Authentication and Authorization

- Owner: VaultStadio maintainers
- Last verified: 2026-10-30
- Specifications: [`specs/sdd-0003-identity-authentication-and-authorization.md`](./sdd-0003-identity-authentication-and-authorization.md)
- Workflows: Not applicable for this capability.
- Entrypoints: [`backend/api/src/main/kotlin/com/vaultstadio/api/routes/auth/AuthRoutes.kt`](../backend/api/src/main/kotlin/com/vaultstadio/api/routes/auth/AuthRoutes.kt)
- Core code: [`backend/core/auth/src/main/kotlin/com/vaultstadio/core/domain/service/UserService.kt`](../backend/core/auth/src/main/kotlin/com/vaultstadio/core/domain/service/UserService.kt)
- Tests: [`backend/core/auth/src/test/kotlin/com/vaultstadio/core/domain/service/UserServiceTest.kt`](../backend/core/auth/src/test/kotlin/com/vaultstadio/core/domain/service/UserServiceTest.kt) · [`backend/api/src/test/kotlin/com/vaultstadio/api/config/SecurityTest.kt`](../backend/api/src/test/kotlin/com/vaultstadio/api/config/SecurityTest.kt)
- User documentation: [`docs/operations/SECURITY.md`](../docs/operations/SECURITY.md) · [`docs/development/REPOSITORY_AUDIT_2026-10-25.md`](../docs/development/REPOSITORY_AUDIT_2026-10-25.md) · [`docs/development/audits/2026-10-25/verification-summary.json`](../docs/development/audits/2026-10-25/verification-summary.json) · [`docs/development/PRODUCT_ROADMAP_2026-10-29.md`](../docs/development/PRODUCT_ROADMAP_2026-10-29.md)

### `storage-domain-and-data-lifecycle` — Storage Domain and Data Lifecycle

- Owner: VaultStadio maintainers
- Last verified: 2026-10-30
- Specifications: [`specs/sdd-0004-storage-domain-and-data-lifecycle.md`](./sdd-0004-storage-domain-and-data-lifecycle.md)
- Workflows: Not applicable for this capability.
- Entrypoints: [`backend/api/src/main/kotlin/com/vaultstadio/api/routes/storage/StorageRoutes.kt`](../backend/api/src/main/kotlin/com/vaultstadio/api/routes/storage/StorageRoutes.kt)
- Core code: [`backend/core/storage/src/main/kotlin/com/vaultstadio/core/domain/service/StorageService.kt`](../backend/core/storage/src/main/kotlin/com/vaultstadio/core/domain/service/StorageService.kt) · [`backend/infrastructure/src/main/kotlin/com/vaultstadio/infrastructure/persistence/StorageItemSelection.kt`](../backend/infrastructure/src/main/kotlin/com/vaultstadio/infrastructure/persistence/StorageItemSelection.kt)
- Tests: [`backend/core/storage/src/test/kotlin/com/vaultstadio/core/domain/service/StorageServiceTest.kt`](../backend/core/storage/src/test/kotlin/com/vaultstadio/core/domain/service/StorageServiceTest.kt)
- User documentation: [`docs/operations/STORAGE_CONFIGURATION.md`](../docs/operations/STORAGE_CONFIGURATION.md) · [`docs/operations/BACKUP_RESTORE.md`](../docs/operations/BACKUP_RESTORE.md) · [`docs/development/REPOSITORY_AUDIT_2026-10-25.md`](../docs/development/REPOSITORY_AUDIT_2026-10-25.md) · [`docs/development/audits/2026-10-25/verification-summary.json`](../docs/development/audits/2026-10-25/verification-summary.json) · [`docs/development/verification/STORAGE_SELECTION_91.md`](../docs/development/verification/STORAGE_SELECTION_91.md) · [`docs/development/verification/storage-selection-91.json`](../docs/development/verification/storage-selection-91.json) · [`docs/development/verification/storage-selection-91-cases.json`](../docs/development/verification/storage-selection-91-cases.json) · [`docs/development/verification/storage-selection-91-coverage.json`](../docs/development/verification/storage-selection-91-coverage.json) · [`docs/development/verification/STORAGE_SELECTION_91_REVIEW.md`](../docs/development/verification/STORAGE_SELECTION_91_REVIEW.md) · [`docs/development/verification/storage-selection-91-review.json`](../docs/development/verification/storage-selection-91-review.json) · [`docs/development/PRODUCT_ROADMAP_2026-10-29.md`](../docs/development/PRODUCT_ROADMAP_2026-10-29.md)

### `upload-and-transfer-protocols` — Upload and Transfer Protocols

- Owner: VaultStadio maintainers
- Last verified: 2026-10-30
- Specifications: [`specs/sdd-0005-upload-and-transfer-protocols.md`](./sdd-0005-upload-and-transfer-protocols.md)
- Workflows: Not applicable for this capability.
- Entrypoints: [`backend/api/src/main/kotlin/com/vaultstadio/api/routes/storage/ChunkedUploadRoutes.kt`](../backend/api/src/main/kotlin/com/vaultstadio/api/routes/storage/ChunkedUploadRoutes.kt) · [`backend/api/src/main/kotlin/com/vaultstadio/api/routes/storage/WebDAVRoutes.kt`](../backend/api/src/main/kotlin/com/vaultstadio/api/routes/storage/WebDAVRoutes.kt) · [`backend/api/src/main/kotlin/com/vaultstadio/api/routes/storage/S3Routes.kt`](../backend/api/src/main/kotlin/com/vaultstadio/api/routes/storage/S3Routes.kt)
- Core code: [`backend/core/common/src/main/kotlin/com/vaultstadio/core/domain/service/MultipartUploadManager.kt`](../backend/core/common/src/main/kotlin/com/vaultstadio/core/domain/service/MultipartUploadManager.kt)
- Tests: [`backend/api/src/test/kotlin/com/vaultstadio/api/routes/ChunkedUploadRoutesTest.kt`](../backend/api/src/test/kotlin/com/vaultstadio/api/routes/ChunkedUploadRoutesTest.kt)
- User documentation: [`docs/development/REPOSITORY_AUDIT_2026-10-25.md`](../docs/development/REPOSITORY_AUDIT_2026-10-25.md) · [`docs/development/audits/2026-10-25/verification-summary.json`](../docs/development/audits/2026-10-25/verification-summary.json)

### `sharing-and-permissions` — Sharing and Permissions

- Owner: VaultStadio maintainers
- Last verified: 2026-10-30
- Specifications: [`specs/sdd-0006-sharing-and-permissions.md`](./sdd-0006-sharing-and-permissions.md)
- Workflows: Not applicable for this capability.
- Entrypoints: [`backend/api/src/main/kotlin/com/vaultstadio/api/routes/share/ShareRoutes.kt`](../backend/api/src/main/kotlin/com/vaultstadio/api/routes/share/ShareRoutes.kt)
- Core code: [`backend/application/share/src/main/kotlin/com/vaultstadio/application/di/ApplicationShareKoin.kt`](../backend/application/share/src/main/kotlin/com/vaultstadio/application/di/ApplicationShareKoin.kt) · [`backend/application/share/src/main/kotlin/com/vaultstadio/application/usecase/share/AccessShareUseCase.kt`](../backend/application/share/src/main/kotlin/com/vaultstadio/application/usecase/share/AccessShareUseCase.kt) · [`backend/application/share/src/main/kotlin/com/vaultstadio/application/usecase/share/CreateShareUseCase.kt`](../backend/application/share/src/main/kotlin/com/vaultstadio/application/usecase/share/CreateShareUseCase.kt)
- Tests: [`backend/core/share/src/test/kotlin/com/vaultstadio/core/domain/service/ShareServiceTest.kt`](../backend/core/share/src/test/kotlin/com/vaultstadio/core/domain/service/ShareServiceTest.kt)
- User documentation: [`docs/api/API.md`](../docs/api/API.md) · [`docs/development/REPOSITORY_AUDIT_2026-10-25.md`](../docs/development/REPOSITORY_AUDIT_2026-10-25.md) · [`docs/development/audits/2026-10-25/verification-summary.json`](../docs/development/audits/2026-10-25/verification-summary.json) · [`docs/development/verification/PUBLIC_SHARE_ACCESS_90.md`](../docs/development/verification/PUBLIC_SHARE_ACCESS_90.md) · [`docs/development/verification/public-share-access-90.json`](../docs/development/verification/public-share-access-90.json) · [`docs/development/verification/public-share-access-90-cases.json`](../docs/development/verification/public-share-access-90-cases.json) · [`docs/development/verification/public-share-access-90-coverage.json`](../docs/development/verification/public-share-access-90-coverage.json) · [`docs/development/PRODUCT_ROADMAP_2026-10-29.md`](../docs/development/PRODUCT_ROADMAP_2026-10-29.md)

### `version-history-and-recovery` — Version History and Recovery

- Owner: VaultStadio maintainers
- Last verified: 2026-10-30
- Specifications: [`specs/sdd-0007-version-history-and-recovery.md`](./sdd-0007-version-history-and-recovery.md)
- Workflows: Not applicable for this capability.
- Entrypoints: [`backend/api/src/main/kotlin/com/vaultstadio/api/routes/version/VersionRoutes.kt`](../backend/api/src/main/kotlin/com/vaultstadio/api/routes/version/VersionRoutes.kt)
- Core code: [`backend/application/version/src/main/kotlin/com/vaultstadio/application/di/ApplicationVersionKoin.kt`](../backend/application/version/src/main/kotlin/com/vaultstadio/application/di/ApplicationVersionKoin.kt) · [`backend/application/version/src/main/kotlin/com/vaultstadio/application/usecase/version/ApplyRetentionPolicyUseCase.kt`](../backend/application/version/src/main/kotlin/com/vaultstadio/application/usecase/version/ApplyRetentionPolicyUseCase.kt) · [`backend/application/version/src/main/kotlin/com/vaultstadio/application/usecase/version/CompareVersionsUseCase.kt`](../backend/application/version/src/main/kotlin/com/vaultstadio/application/usecase/version/CompareVersionsUseCase.kt)
- Tests: [`backend/api/src/test/kotlin/com/vaultstadio/api/application/usecase/version/GetVersionHistoryUseCaseTest.kt`](../backend/api/src/test/kotlin/com/vaultstadio/api/application/usecase/version/GetVersionHistoryUseCaseTest.kt)
- User documentation: [`docs/operations/BACKUP_RESTORE.md`](../docs/operations/BACKUP_RESTORE.md) · [`docs/development/REPOSITORY_AUDIT_2026-10-25.md`](../docs/development/REPOSITORY_AUDIT_2026-10-25.md) · [`docs/development/audits/2026-10-25/verification-summary.json`](../docs/development/audits/2026-10-25/verification-summary.json) · [`docs/development/PRODUCT_ROADMAP_2026-10-29.md`](../docs/development/PRODUCT_ROADMAP_2026-10-29.md)

### `device-sync-and-conflicts` — Device Sync and Conflict Resolution

- Owner: VaultStadio maintainers
- Last verified: 2026-10-30
- Specifications: [`specs/sdd-0008-device-sync-and-conflicts.md`](./sdd-0008-device-sync-and-conflicts.md)
- Workflows: Not applicable for this capability.
- Entrypoints: [`backend/api/src/main/kotlin/com/vaultstadio/api/routes/sync/SyncRoutes.kt`](../backend/api/src/main/kotlin/com/vaultstadio/api/routes/sync/SyncRoutes.kt)
- Core code: [`backend/core/src/main/kotlin/com/vaultstadio/core/domain/model/Sync.kt`](../backend/core/src/main/kotlin/com/vaultstadio/core/domain/model/Sync.kt)
- Tests: [`backend/api/src/test/kotlin/com/vaultstadio/api/routes/SyncRoutesTest.kt`](../backend/api/src/test/kotlin/com/vaultstadio/api/routes/SyncRoutesTest.kt)
- User documentation: [`docs/development/REPOSITORY_AUDIT_2026-10-25.md`](../docs/development/REPOSITORY_AUDIT_2026-10-25.md) · [`docs/development/audits/2026-10-25/verification-summary.json`](../docs/development/audits/2026-10-25/verification-summary.json) · [`docs/development/PRODUCT_ROADMAP_2026-10-29.md`](../docs/development/PRODUCT_ROADMAP_2026-10-29.md)

### `real-time-collaboration` — Real-Time Collaboration

- Owner: VaultStadio maintainers
- Last verified: 2026-10-30
- Specifications: [`specs/sdd-0009-real-time-collaboration.md`](./sdd-0009-real-time-collaboration.md)
- Workflows: Not applicable for this capability.
- Entrypoints: [`backend/api/src/main/kotlin/com/vaultstadio/api/routes/collaboration/CollaborationRoutes.kt`](../backend/api/src/main/kotlin/com/vaultstadio/api/routes/collaboration/CollaborationRoutes.kt) · [`backend/api/src/main/kotlin/com/vaultstadio/api/routes/collaboration/CollaborationWebSocket.kt`](../backend/api/src/main/kotlin/com/vaultstadio/api/routes/collaboration/CollaborationWebSocket.kt)
- Core code: [`backend/core/collaboration/src/main/kotlin/com/vaultstadio/core/domain/service/CollaborationService.kt`](../backend/core/collaboration/src/main/kotlin/com/vaultstadio/core/domain/service/CollaborationService.kt)
- Tests: [`backend/core/collaboration/src/test/kotlin/com/vaultstadio/core/domain/service/CollaborationAdmissionContractTest.kt`](../backend/core/collaboration/src/test/kotlin/com/vaultstadio/core/domain/service/CollaborationAdmissionContractTest.kt)
- User documentation: [`docs/development/REPOSITORY_AUDIT_2026-10-25.md`](../docs/development/REPOSITORY_AUDIT_2026-10-25.md) · [`docs/development/audits/2026-10-25/verification-summary.json`](../docs/development/audits/2026-10-25/verification-summary.json) · [`docs/development/verification/COLLABORATION_AUTHORIZATION_89.md`](../docs/development/verification/COLLABORATION_AUTHORIZATION_89.md) · [`docs/development/verification/collaboration-authorization-89.json`](../docs/development/verification/collaboration-authorization-89.json) · [`docs/development/verification/collaboration-authorization-89-cases.json`](../docs/development/verification/collaboration-authorization-89-cases.json)

### `federation-and-trust` — Federation and Trust

- Owner: VaultStadio maintainers
- Last verified: 2026-10-30
- Specifications: [`specs/sdd-0010-federation-and-trust.md`](./sdd-0010-federation-and-trust.md)
- Workflows: Not applicable for this capability.
- Entrypoints: [`backend/api/src/main/kotlin/com/vaultstadio/api/routes/federation/FederationRoutes.kt`](../backend/api/src/main/kotlin/com/vaultstadio/api/routes/federation/FederationRoutes.kt)
- Core code: [`backend/core/federation/src/main/kotlin/com/vaultstadio/core/domain/service/FederationService.kt`](../backend/core/federation/src/main/kotlin/com/vaultstadio/core/domain/service/FederationService.kt)
- Tests: [`backend/api/src/test/kotlin/com/vaultstadio/api/routes/FederationRoutesTest.kt`](../backend/api/src/test/kotlin/com/vaultstadio/api/routes/FederationRoutesTest.kt)
- User documentation: [`docs/api/PHASE6_ADVANCED_FEATURES.md`](../docs/api/PHASE6_ADVANCED_FEATURES.md) · [`docs/operations/SECURITY.md`](../docs/operations/SECURITY.md) · [`docs/development/REPOSITORY_AUDIT_2026-10-25.md`](../docs/development/REPOSITORY_AUDIT_2026-10-25.md) · [`docs/development/audits/2026-10-25/verification-summary.json`](../docs/development/audits/2026-10-25/verification-summary.json) · [`docs/development/PRODUCT_ROADMAP_2026-10-29.md`](../docs/development/PRODUCT_ROADMAP_2026-10-29.md)

### `plugin-runtime-and-extensibility` — Plugin Runtime and Extensibility

- Owner: VaultStadio maintainers
- Last verified: 2026-10-30
- Specifications: [`specs/sdd-0011-plugin-runtime-and-extensibility.md`](./sdd-0011-plugin-runtime-and-extensibility.md)
- Workflows: Not applicable for this capability.
- Entrypoints: [`backend/api/src/main/kotlin/com/vaultstadio/api/plugins/PluginManager.kt`](../backend/api/src/main/kotlin/com/vaultstadio/api/plugins/PluginManager.kt) · [`backend/api/src/main/kotlin/com/vaultstadio/api/routes/plugin/PluginRoutes.kt`](../backend/api/src/main/kotlin/com/vaultstadio/api/routes/plugin/PluginRoutes.kt)
- Core code: [`backend/application/plugin/src/main/kotlin/com/vaultstadio/application/di/ApplicationPluginKoin.kt`](../backend/application/plugin/src/main/kotlin/com/vaultstadio/application/di/ApplicationPluginKoin.kt) · [`backend/application/plugin/src/main/kotlin/com/vaultstadio/application/usecase/plugin/DisablePluginUseCase.kt`](../backend/application/plugin/src/main/kotlin/com/vaultstadio/application/usecase/plugin/DisablePluginUseCase.kt) · [`backend/application/plugin/src/main/kotlin/com/vaultstadio/application/usecase/plugin/EnablePluginUseCase.kt`](../backend/application/plugin/src/main/kotlin/com/vaultstadio/application/usecase/plugin/EnablePluginUseCase.kt)
- Tests: [`backend/plugins-api/src/test/kotlin/com/vaultstadio/plugins/lifecycle/PluginLifecycleTest.kt`](../backend/plugins-api/src/test/kotlin/com/vaultstadio/plugins/lifecycle/PluginLifecycleTest.kt)
- User documentation: [`docs/plugins/PLUGINS.md`](../docs/plugins/PLUGINS.md) · [`docs/plugins/PLUGIN_DEVELOPMENT.md`](../docs/plugins/PLUGIN_DEVELOPMENT.md) · [`docs/development/REPOSITORY_AUDIT_2026-10-25.md`](../docs/development/REPOSITORY_AUDIT_2026-10-25.md) · [`docs/development/audits/2026-10-25/verification-summary.json`](../docs/development/audits/2026-10-25/verification-summary.json)

### `metadata-search-and-ai` — Metadata, Search and AI

- Owner: VaultStadio maintainers
- Last verified: 2026-10-30
- Specifications: [`specs/sdd-0012-metadata-search-and-ai.md`](./sdd-0012-metadata-search-and-ai.md) · [`specs/filename-search-and-suggestions.md`](./filename-search-and-suggestions.md)
- Workflows: Not applicable for this capability.
- Entrypoints: [`backend/api/src/main/kotlin/com/vaultstadio/api/routes/metadata/SearchRoutes.kt`](../backend/api/src/main/kotlin/com/vaultstadio/api/routes/metadata/SearchRoutes.kt) · [`backend/api/src/main/kotlin/com/vaultstadio/api/routes/metadata/MetadataRoutes.kt`](../backend/api/src/main/kotlin/com/vaultstadio/api/routes/metadata/MetadataRoutes.kt)
- Core code: [`backend/infrastructure/src/main/kotlin/com/vaultstadio/infrastructure/persistence/StorageItemSelection.kt`](../backend/infrastructure/src/main/kotlin/com/vaultstadio/infrastructure/persistence/StorageItemSelection.kt)
- Tests: [`backend/api/src/test/kotlin/com/vaultstadio/api/routes/SearchRoutesTest.kt`](../backend/api/src/test/kotlin/com/vaultstadio/api/routes/SearchRoutesTest.kt)
- User documentation: [`docs/plugins/AI_INTEGRATION.md`](../docs/plugins/AI_INTEGRATION.md) · [`docs/development/REPOSITORY_AUDIT_2026-10-25.md`](../docs/development/REPOSITORY_AUDIT_2026-10-25.md) · [`docs/development/audits/2026-10-25/verification-summary.json`](../docs/development/audits/2026-10-25/verification-summary.json) · [`docs/development/verification/STORAGE_SELECTION_91.md`](../docs/development/verification/STORAGE_SELECTION_91.md) · [`docs/development/verification/storage-selection-91.json`](../docs/development/verification/storage-selection-91.json) · [`docs/development/verification/storage-selection-91-cases.json`](../docs/development/verification/storage-selection-91-cases.json) · [`docs/development/verification/storage-selection-91-coverage.json`](../docs/development/verification/storage-selection-91-coverage.json) · [`docs/development/verification/STORAGE_SELECTION_91_REVIEW.md`](../docs/development/verification/STORAGE_SELECTION_91_REVIEW.md) · [`docs/development/verification/storage-selection-91-review.json`](../docs/development/verification/storage-selection-91-review.json) · [`docs/development/PRODUCT_ROADMAP_2026-10-29.md`](../docs/development/PRODUCT_ROADMAP_2026-10-29.md)

### `frontend-kmp-and-platform-architecture` — Frontend KMP and Platform Architecture

- Owner: VaultStadio maintainers
- Last verified: 2026-10-30
- Specifications: [`specs/sdd-0013-frontend-kmp-and-platform-architecture.md`](./sdd-0013-frontend-kmp-and-platform-architecture.md)
- Workflows: Not applicable for this capability.
- Entrypoints: [`frontend/composeApp/src/main/kotlin/com/vaultstadio/app/VaultStadioRoot.kt`](../frontend/composeApp/src/main/kotlin/com/vaultstadio/app/VaultStadioRoot.kt)
- Core code: [`frontend/feature/files/src/main/kotlin/com/vaultstadio/app/feature/files/FilesViewModel.kt`](../frontend/feature/files/src/main/kotlin/com/vaultstadio/app/feature/files/FilesViewModel.kt)
- Tests: [`frontend/data/network/src/commonTest/kotlin/com/vaultstadio/app/data/network/Api116AuthorityTest.kt`](../frontend/data/network/src/commonTest/kotlin/com/vaultstadio/app/data/network/Api116AuthorityTest.kt)
- User documentation: [`docs/architecture/FRONTEND_ARCHITECTURE.md`](../docs/architecture/FRONTEND_ARCHITECTURE.md) · [`docs/architecture/FRONTEND_MODULARISATION_AND_STANDALONE_BUILDS.md`](../docs/architecture/FRONTEND_MODULARISATION_AND_STANDALONE_BUILDS.md) · [`docs/frontend/FRONTEND_TESTING.md`](../docs/frontend/FRONTEND_TESTING.md) · [`docs/development/REPOSITORY_AUDIT_2026-10-25.md`](../docs/development/REPOSITORY_AUDIT_2026-10-25.md) · [`docs/development/audits/2026-10-25/verification-summary.json`](../docs/development/audits/2026-10-25/verification-summary.json) · [`docs/development/PRODUCT_ROADMAP_2026-10-29.md`](../docs/development/PRODUCT_ROADMAP_2026-10-29.md)

### `deployment-operations-and-release` — Deployment, Operations and Release

- Owner: VaultStadio maintainers
- Last verified: 2026-10-30
- Specifications: [`specs/sdd-0014-deployment-operations-and-release.md`](./sdd-0014-deployment-operations-and-release.md)
- Workflows: [`.github/workflows/ci.yml`](../.github/workflows/ci.yml) · [`.github/workflows/docker.yml`](../.github/workflows/docker.yml) · [`.github/workflows/security.yml`](../.github/workflows/security.yml) · [`.github/workflows/release_workflow.yml`](../.github/workflows/release_workflow.yml) · [`.github/workflows/hotfix_workflow.yml`](../.github/workflows/hotfix_workflow.yml)
- Entrypoints: [`scripts/verification/run.py`](../scripts/verification/run.py)
- Core code: [`scripts/verification/tooling_report.py`](../scripts/verification/tooling_report.py)
- Tests: [`scripts/tests/test_verification_tooling.py`](../scripts/tests/test_verification_tooling.py) · [`scripts/tests/test_secret_scan_adapter.py`](../scripts/tests/test_secret_scan_adapter.py)
- User documentation: [`docs/operations/DEPLOYMENT.md`](../docs/operations/DEPLOYMENT.md) · [`docs/operations/BACKUP_RESTORE.md`](../docs/operations/BACKUP_RESTORE.md) · [`docs/development/REPOSITORY_AUDIT_2026-10-25.md`](../docs/development/REPOSITORY_AUDIT_2026-10-25.md) · [`docs/development/audits/2026-10-25/verification-summary.json`](../docs/development/audits/2026-10-25/verification-summary.json) · [`docs/development/PRODUCT_ROADMAP_2026-10-29.md`](../docs/development/PRODUCT_ROADMAP_2026-10-29.md)

### `security-privacy-and-threat-model` — Security, Privacy and Threat Model

- Owner: VaultStadio maintainers
- Last verified: 2026-10-30
- Specifications: [`specs/sdd-0015-security-privacy-and-threat-model.md`](./sdd-0015-security-privacy-and-threat-model.md)
- Workflows: [`.github/workflows/security.yml`](../.github/workflows/security.yml)
- Entrypoints: [`backend/api/src/main/kotlin/com/vaultstadio/api/config/Security.kt`](../backend/api/src/main/kotlin/com/vaultstadio/api/config/Security.kt)
- Core code: [`scripts/ci/run_secret_scan.py`](../scripts/ci/run_secret_scan.py) · [`scripts/ci/secret_findings.py`](../scripts/ci/secret_findings.py)
- Tests: [`backend/api/src/test/kotlin/com/vaultstadio/api/config/SecurityTest.kt`](../backend/api/src/test/kotlin/com/vaultstadio/api/config/SecurityTest.kt) · [`scripts/tests/test_secret_scanners_real.py`](../scripts/tests/test_secret_scanners_real.py)
- User documentation: [`docs/operations/SECURITY.md`](../docs/operations/SECURITY.md) · [`docs/plugins/AI_INTEGRATION.md`](../docs/plugins/AI_INTEGRATION.md) · [`docs/development/REPOSITORY_AUDIT_2026-10-25.md`](../docs/development/REPOSITORY_AUDIT_2026-10-25.md) · [`docs/development/audits/2026-10-25/verification-summary.json`](../docs/development/audits/2026-10-25/verification-summary.json) · [`docs/development/PRODUCT_ROADMAP_2026-10-29.md`](../docs/development/PRODUCT_ROADMAP_2026-10-29.md)

## Maintenance contract

1. Read the relevant SDD before changing a catalogued capability.
2. Change the SDD, catalog evidence, tests, and user documentation together when
   behavior or an architecture boundary changes.
3. Use repository-relative paths in `catalog.json`; each path is validated and every
   top-level product SDD must have exactly one capability owner.
4. Run `pnpm run validate:specifications` in local and CI validation.
