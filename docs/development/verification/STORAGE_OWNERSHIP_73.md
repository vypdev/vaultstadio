# Storage ownership correction — issue #73

Verified source commit: `260489a0131419cc1c1231d952681c81d067c802`, 2026-10-25 UTC. Machine-readable counts and collected case names: [storage-ownership-73.json](storage-ownership-73.json). This verifies the storage subset of R01; the platform audit remains [#71 / PR #72](https://github.com/vypdev/vaultstadio/pull/72).

## Behavior and compatibility

A foreign parent previously accepted folder creation and uploads. Core now authorizes the entire active folder chain before name lookup, rows, bytes or events. All ordinary item operations remain owner-only for PRIVATE/PUBLIC/SHARED. Separately validated share downloads retain their existing flow.

Breadcrumbs reject foreign ancestors without partial metadata. Cycles, more than 128 ancestors and moves into a source/descendant fail before writes. Get-or-create validates an existing result before reuse; move/copy/restore validate their destination.

Permanent delete removes a leaf only. A nonempty folder returns HTTP 400 / `INVALID_OPERATION`; delete owned contents first. No existing row is reassigned or purged automatically.

The atomic `deleteLeaf` port revalidates the owner and checks all children under a row lock. Core supplies content deletion using that locked snapshot's key. The adapter retains the lock through the callback and metadata deletion, including suspension; core emits success using the returned snapshot only after completion. This fixes Copilot's finding that an inserted child could cause rejection after bytes were already deleted. It also avoids deleting a stale key read before the lock.

This is not a transaction across PostgreSQL and external storage. The per-item database lock lasts through provider I/O; a database commit failure after irreversible content deletion still needs the durable compensation/outbox lifecycle planned in R02. That limitation is explicit in SDD-0004 rather than hidden by a distributed-atomicity claim.

## Executed verification

| Check | Result |
|---|---|
| All backend JVM suites | 1,970 tests; 0 failures, errors or skipped |
| Production HTTP routes/use cases/service/PostgreSQL/local bytes | 53 cases; owner/visibility matrix, destination denial, anonymous rejection and owner folder/upload/download round trip |
| Migrated PostgreSQL leaf/callback integration | 10 collected cases across two focused classes: own/foreign children, empty/missing replay, both race orders with real file bytes, typed SQL/callback errors, locked owner, cancellation and suspended callback lock retention |
| `StorageDeletion.kt` | 12/12 lines; 8/9 branches (88.89%) |
| `StorageHierarchy.kt` | 25/25 lines; 18/18 branches (100.0%) |
| `StorageAccessPolicy.kt` | 8/8 lines; 8/8 branches (100.0%) |
| `StorageLeafDeletion.kt` | 27/27 lines; 14/16 branches (87.5%) |
| `StorageItemMapping.kt` | 19/19 lines; no branches |
| Complete backend denominator, no exclusions | 6,370/15,998 lines (39.82%); 978/5,354 branches (18.27%) |
| API build and packaging with unfiltered API KtLint | Pass, with tests enabled; includes the Docker workflow JAR checks |
| Detekt main, no-FQN, SDD catalog, CI Actionlint | Pass; existing Detekt warnings remain |

All new policy sources above have 100% line coverage. Raw uncovered deletion branches remain reported without exclusions; the access and hierarchy policies achieve the enforced 100% line/branch budget. This is not a claim of 100% whole-platform coverage.

The integration harness installs actual production routes, the application storage DI module, security/error mapping, service, Exposed repository, Flyway migrations and LOCAL adapter. Only credential validation uses exact disposable actor tokens. Tests assert status/body, unchanged rows/versions, bytes and events, plus an owner success control. No replacement routes are registered. The PostgreSQL test verifies the actual `ON DELETE CASCADE` constraint.

Race tests coordinate through real database locks. If a child commits first, deletion rejects it and original file bytes remain. If deletion locks first, insertion fails with SQLSTATE 23503. A callback deliberately suspended after the leaf check retains the row lock: a separate connection's `FOR KEY SHARE NOWAIT` fails with 55P03 until completion. Cancellation preserves metadata and releases the lock. A delayed provider test proves no successful event is published before content deletion finishes.

## Reproduction and CI enforcement

From `backend/`, with Java 17 and Docker available:

```sh
./gradlew test :api:build \
  :domain:storage:jacocoTestCoverageVerification \
  :core:storage:jacocoTestCoverageVerification \
  :domain:storage:jacocoTestReport \
  :core:storage:jacocoTestReport \
  detektMain
```

Application CI includes both storage test modules, verifies 100% line/branch coverage for the access and hierarchy policies, and uploads their XML reports. Missing execution data fails closed. A local negative control supplied valid execution data from unrelated tests: the unchanged gate failed at 0% against 100%; normal data passed. No Copilot setting or approval policy changed.

The complete aggregate uses the [audit init script](https://github.com/vypdev/vaultstadio/blob/a0d18a2fd560d25a5afafdebfd0a2910aa02c7c7/scripts/audit/aggregate-coverage.init.gradle), separately from the two scoped CI gates.

## Requirement traceability

| SDD-0004 requirement | Implementation | Executed tests |
|---|---|---|
| STO-AUTH-01 | `StorageAccessPolicy`, item lookup | `StorageAccessPolicyTest`, `StorageMutationAuthorizationTest`, `StorageItemAuthorizationIntegrationTest` |
| STO-AUTH-02–04 | `StorageHierarchy`, write/mutation helpers | `StorageParentAuthorizationTest`, `StorageFolderReuseAuthorizationTest`, `StorageOwnershipIntegrationTest` |
| STO-AUTH-05–06 | bounded owned ancestry and destination checks | `StorageHierarchyTest` |
| STO-AUTH-07 | core callback and atomic leaf port/adapter | `StorageDeletionPreflightTest`, `StorageLeafDeletionIntegrationTest`, `StorageAtomicDeletionIntegrationTest` |
| STO-AUTH-08 | typed outcomes and ordered preflight | negative-effect assertions and real rows/bytes/events assertions across parent/mutation/deletion tests |

## Remaining work

Remote CI, Copilot review, native approval and protected-branch merges remain separate acceptance steps. Version-history ownership is [#74](https://github.com/vypdev/vaultstadio/issues/74), client startup is [#76](https://github.com/vypdev/vaultstadio/issues/76). Collaboration ownership, quotas/compensation and the remaining [audit roadmap](https://github.com/vypdev/vaultstadio/blob/a0d18a2fd560d25a5afafdebfd0a2910aa02c7c7/docs/development/AUDIT_ROADMAP_2026-10-25.md) are not declared solved. No deployment is included.
