# Version ownership and retained content — issue #74

Contract: [SDD-0007 §6.4](../../../specs/sdd-0007-version-history-and-recovery.md#64-owner-scoped-version-operations--issue-74) and [SDD-0003 §6.5](../../../specs/sdd-0003-identity-authentication-and-authorization.md#65-historical-file-authorization--issue-74). Local verification: 2026-10-27 UTC (review follow-up), Java 17.0.9. The [machine-readable receipt](version-ownership-74.json) records exact source hashes and collected test names; the linked PR records the final commit and remote results.

## Behavior and compatibility

An unrelated user could retrieve another owner's history and filename. Every one of the twelve public service operations now requires an explicit actor and checks the current canonical item owner. PRIVATE/PUBLIC/SHARED, previous version creator, a shared download recipient and a nonowner administrator do not grant historical access. Version-ID deletion first queries only the owning item ID, then authorizes, then binds the complete row to that item and requested ID. No actor-less fallback remains.

History names come from the authorized item. Mismatched IDs, item IDs, version numbers, mixed histories and invalid latest flags fail before content effects. Missing versions map to 404, cross-user requests to 403, anonymous requests to 401, and typed provider failures to a safe 500 without keys or provider diagnostics. Existing JSON response field names remain; heterogeneous response maps are replaced with serializable DTOs so owner controls can complete successfully.

Two failing owner-sequence tests exposed deletion of bytes still used by a restored version or the current file. A separate minimal boolean reference port now protects such content. Bulk cleanup completes every reference lookup before deleting any bytes; a late reference-query failure leaves all bytes intact. The guard also preserves content referenced by another item's current pointer without revealing its metadata. It is conservative and sequential: atomic concurrent mutation, orphan reclamation and compensation after partial failure remain [#109 / R07](https://github.com/vypdev/vaultstadio/issues/109).

The version-download endpoint still returns metadata. Restoring a version creates a historical row with persisted `restoredFrom`; this change does not implement canonical-file byte restoration, automatic snapshots on upload or quota accounting. Those explicit R07 limits prevent treating this permission repair as complete recovery support.

## Architecture

`FileVersionService` is an explicit-actor facade. Focused `VersionAccess`, `VersionReads`, `VersionMutations`, `VersionDiffGenerator` and `VersionLineDiff` own separate decisions. Domain ports contain no HTTP, SQL or provider types; Exposed adapters implement minimal scope/reference queries; `VersionModule` wires the actual production service. Routes are split into history, detail and mutation handlers with shared DTO/error mapping. The former service test monolith is replaced by focused regression and fault suites, preserving its original 31 cases.

## Executed local verification

| Scope | Executed cases | Failures / errors / skips |
|---|---:|---:|
| Complete core/version suite | 197 | 0 / 0 / 0 |
| Version use cases and actual HTTP composition | 140 | 0 / 0 / 0 |
| Real PostgreSQL scope/reference adapters | 11 | 0 / 0 / 0 |
| Total selected affected suites | **348** | **0 / 0 / 0** |

The 72 service matrix executions cover twelve operations × three visibilities × owner/unrelated caller. The 91 HTTP executions cover seven surfaces × three visibilities × owner/unrelated/shared recipient/nonowner admin, plus seven anonymous cases. These are matrix executions, not 163 unique product features. Additional cases cover missing actors/items, stale/mixed row identities, SQL failures, cancellation, stream read/close failures, Unicode diff semantics, latest-version constraints and retained-content sequences.

HTTP fixtures use production routing, use cases, version DI, migrated PostgreSQL and local file bytes; only session-token validation is replaced with disposable actor identities. Rejected requests compare actual row snapshots and byte hashes. Adapter tests use minimal tables that make accidental full-row projection fail. Two original destructive-content assertions were observed failing before the guard and passing afterward.

The complete core/version report, without exclusions, covers **272/272 lines and 90/90 branches**. Both new SQL adapters cover **7/7 lines each**; JaCoCo reports no branches for these adapter classes. This is scoped evidence, not 100% whole-platform coverage. Eight other backend core modules still lack standard XML coverage reports; see [#84](https://github.com/vypdev/vaultstadio/issues/84).

Changed Kotlin sources in all five affected modules passed strict Detekt using the existing configuration with only `build.maxIssues` changed locally from `-1` to `0`, existing baseline unchanged, and an asserted exact changed-file selection. Changed-file ktlint passed. No repository baseline, quality threshold or required check was weakened. Repository-wide legacy Detekt adoption remains separate debt: its current unlimited issue budget can make a green task advisory.

## Reproduction

With Java 17 and Docker, from `backend/`:

```sh
./gradlew :core:version:test --rerun \
  :infrastructure:test --tests '*Version*AdapterTest' --rerun \
  :api:test --tests '*version*' --tests '*Version*' --rerun \
  :core:version:jacocoTestReport :infrastructure:jacocoTestReport \
  --continue --no-parallel --max-workers=2 --build-cache --no-daemon
```

A complete backend run and the independent desktop/browser suites remain the PR CI's responsibility. The initial PR CI on `e49c8d8` passed 2,240 backend + 1,315 desktop + 1,191 browser executions (4,746 total), zero failures/errors/skips, with an identical merge-preview/candidate tree. The retention review follow-up requires its own final-head CI. Raw final selected-suite XML, coverage, exact inputs, strict-quality logs and the original two failing XML assertions are retained as verification evidence.

## Traceability and rollout

| Requirement | Production responsibility | Executed suites |
|---|---|---|
| VER-OWNER / GRANT / SCOPE | explicit actor, shared storage owner policy, minimal item-scope port | OwnershipMatrix, AuthorizationFault, OwnershipHttp, ScopeAdapter |
| VER-BIND / EFFECTS | canonical item and row binding before effects | BindingFault, OwnershipHttp, HttpBoundary |
| VER-CONTENT | preflight reference predicate and conservative byte retention | RetainedContent, ContentReferenceAdapter, HttpBoundary |
| VER-ERROR | typed failures, stream ownership, safe HTTP mapping | ProviderFailure, HttpBoundary, six use-case regression suites |
| VER-ARCH | pure ports, focused service helpers and production composition | complete affected suites and source/module review |

No schema migration, data reassignment or automatic purge is performed. Consumers must pass the actor to the changed internal service/use-case signatures. Roll back through a reviewed revert; reverting restores the former authorization risk and does not recover content deleted by pre-existing behavior. Keep disposable-data verification isolated. Final PR110 CI passes4761 cases and Copilot Review has no open findings. The exact user-authorized administrative exception merged bc8ceed into develop0363822; automatic post-merge CI again passes4761 with the same tested tree and unchanged protections. Native self-approval was not fabricated; master promotion remains pending. Collaboration, shares and pagination findings (#89–#91), full recovery (#109) and the broader audit roadmap remain open.

## Packaged loopback dogfooding

The application distribution built from `e3b5ce2` was mounted read-only on the existing non-root Java runtime in the owned local Docker project. All 237 mounted files matched the host package manifest. Fifteen actual HTTP requests reused the preserved disposable owner/unrelated accounts and existing PostgreSQL data: both logins and owner history/download succeed, anonymous history returns401, and seven foreign item/history/detail/download-metadata/diff/restore/cleanup surfaces return403. Owner metadata and downloaded bytes remain identical. Every existing storage/plugin file hash is unchanged, and the PostgreSQL/frontend containers and named volumes were preserved. The previous distribution/configuration and a pre-change database backup remain available for rollback. This is local API/package evidence, not a new complete GUI or historical byte-restoration certification.

The first PR secret scan correctly blocked an unreviewed detector match. Private redacted investigation identified the public SHA256 of `VersionAccess.kt` in the source-input receipt. It was independently recomputed from source bytes. The exact original immutable commit/path/line is recorded as a reviewed non-secret using the existing mechanism; no rule/path-wide exception was added. The receipt now puts path and digest in separate fields to avoid an incidental credential-shaped same-line expression. Full reachable-history scans still include the original occurrence and report reviewed matches separately.

## Retention review follow-up

Copilot identified that metadata retention left unreferenced historical blobs behind. One actual failing assertion reproduced this before the service change. Retention now retains the bound pre-deletion rows, validates returned IDs, checks references for every removed row before any blob deletion and deletes each unreferenced key once. Nine new service cases cover reclamation, current/retained-version references, duplicate keys, empty policy, policy failure, reference failure, storage failure and a late reference failure. A new real HTTP sequence restores history and applies retention while both retained-version and canonical-file bytes remain readable; all three owner visibility controls also assert physical reclamation of the unreferenced old blob.

Historical checkpoint `0fbb0af`: 343 selected cases passed (196 core,139 API,8 SQL adapters). Strict API quality passed in that campaign; one test-only wrapping violation was then formatted, followed by a fresh strict core pass and196 core tests. Production/API/adapter inputs were unchanged during that formatting correction. Whole core/version coverage is271/271 lines and90/90 branches, without exclusions.

The metadata-first repository port can commit row deletion before a later reference/provider error. Such errors remain typed failures, never successful cleanup; reference preflight preserves all bytes on query failure, but there is no durable rollback/outbox or atomic cross-provider transaction. Concurrent mutation, compensation and recovery of that partial state remain #109. This limitation is tested and explicit.

## Full-history batch cleanup follow-up

A second genuine assertion failure reproduced two soon-to-be-deleted versions retaining each other's shared blob. The pure reference port now accepts the full bound deletion-ID set. Core deduplicates keys, preflights all queries and excludes the complete authorized batch while preserving external-version/current-item references. Five new executions cover restored-history purge, full-batch SQL exclusion, an external surviving version, empty exclusion and actual HTTP/service/PostgreSQL purge retaining canonical bytes. The latest single frozen-source campaign passes348 cases (197core,140API,11SQL adapters), strict quality in all four updated modules, and packaging. The unchanged application module retains its prior strict pass.

The latest core/version denominator is 272/272lines and90/90branches, without exclusions. Metadata-first retention compensation and concurrent GC remain#109; ordinary sequential batch reclamation is now implemented. The stale count in SDD-0003 was corrected alongside the owning receipt.

Two SQL reviewer claims were checked against the exact packaged Exposed1.0.0 JAR: `ExposedSQLException extends java.sql.SQLException`. Both real DROP TABLE regressions return typed DatabaseException; cancellation remains outside the targeted catch. The valid catches remain unchanged. Evidence and detailed reproduction were reported in [vypdev/copilot#736](https://github.com/vypdev/copilot/issues/736). No broad exception catch, baseline or scanner exclusion was added.

## Adoption checkpoint — PR110

Final source bc8ceed was merged into develop03638226aaa339bc83aeb4363d390367fb561903 after the explicit maintainer exception. PR CI40426022867 and automatic post-merge CI40427381586 each reconcile2255backend+1315desktop+1191browser, zero failures/errors/skips; both integration trees match57664a0be9b4e2bb6d9d205ecfdfea18d2e65b83. Earlier pending statements are superseded for this develop adoption. Protections remain intact. master615f11a and the#89–#91 promotion blockers remain; broader recovery is#109.
