# Storage ownership correction — issue #73

Verified source commit: `92a709de9d941e262e7d6f9889618a25d267c8d2`, 2026-10-25 UTC. Machine-readable counts: [storage-ownership-73.json](storage-ownership-73.json). This verifies the storage subset of R01; the platform audit remains [#71 / PR #72](https://github.com/vypdev/vaultstadio/pull/72).

## Behavior and compatibility

A foreign parent previously accepted folder creation and uploads. The core now authorizes the entire active folder chain before name lookup, rows, bytes or events. All ordinary item operations remain owner-only for PRIVATE/PUBLIC/SHARED. Separately validated share downloads retain their existing flow.

Breadcrumbs reject foreign ancestors without returning partial metadata. Cycles, more than 128 ancestors and moves into a source/descendant fail before writes. Get-or-create validates an existing result before reuse; move/copy/restore validate their destination.

Permanent delete now removes a leaf only. A nonempty folder returns HTTP 400 / `INVALID_OPERATION`; delete owned contents first. This deliberately prevents the migrated schema's implicit `ON DELETE CASCADE` from deleting descendants or orphaning their bytes. The adapter locks the parent, checks all children regardless of owner, and deletes in one transaction. No existing row is reassigned or purged automatically. Full recursive content lifecycle and compensation remain follow-up work.

## Executed verification

| Check | Result |
|---|---|
| All backend JVM suites | 1,964 tests; 0 failures, errors or skipped |
| Production HTTP routing, use cases, service, PostgreSQL and local bytes | 53 cases; ownership/visibility matrix, destination denial, unauthenticated rejection, owner folder/upload/download round trip |
| Migrated PostgreSQL leaf deletion | 5 collected JUnit cases: legacy own/foreign children, empty/missing replay, both insertion/delete race orders, typed SQL failure |
| `StorageAccessPolicy.kt` | 8/8 lines; 8/8 branches |
| `StorageHierarchy.kt` | 25/25 lines; 18/18 branches |
| `StorageDeletion.kt` | 15/15 lines; 4/4 branches |
| `StorageLeafDeletion.kt` | 17/17 lines; 5/6 branches |
| Complete backend denominator, no exclusions | 6,358/15,986 lines (39.77%); 964/5,335 branches (18.07%) |
| Exact Docker workflow JAR build (`:api:build -x test`), including unfiltered API KtLint | Pass after fixing the initial PR formatting failure |
| Detekt main, no-FQN, SDD catalog, CI Actionlint | Pass; existing Detekt warnings remain |

The unexecuted adapter branch is on the immediate/suspended completion comparison around `newSuspendedTransaction` (line 20, confirmed in bytecode). It is reported, not excluded. This is not a claim of 100% whole-platform coverage.

The integration harness installs the actual production routes, application storage DI module, security/error mapping, service, Exposed repository, Flyway migrations and LOCAL adapter. Only credential validation is replaced with exact disposable actor tokens. Tests assert status/body, unchanged rows/versions, bytes and events, plus an owner success control. They do not register replacement routes. The PostgreSQL test explicitly verifies the actual `ON DELETE CASCADE` constraint.

Race tests coordinate through real database locks, not fixed sleeps: an uncommitted child blocks the delete until commit, and a deletion held after its parent lock rejects a subsequent child insert with SQLSTATE 23503.

## Reproduction and CI enforcement

From `backend/`, with Java 17 and Docker available:

```sh
./gradlew test \
  :domain:storage:jacocoTestCoverageVerification \
  :core:storage:jacocoTestCoverageVerification \
  :domain:storage:jacocoTestReport \
  :core:storage:jacocoTestReport \
  detektMain
```

Application CI now includes the two storage test modules, verifies 100% line/branch coverage for the pure access policy and hierarchy policy, and uploads their XML coverage reports. Missing execution data fails closed. A local negative control supplied valid execution data from unrelated tests: the unchanged gate failed with 0% policy coverage against the required 100%; normal data passed. No Copilot setting or approval policy changed.

The complete aggregate uses the [audit init script](https://github.com/vypdev/vaultstadio/blob/a0d18a2fd560d25a5afafdebfd0a2910aa02c7c7/scripts/audit/aggregate-coverage.init.gradle); aggregate coverage is measured separately from the two scoped CI gates.

## Requirement traceability

| SDD-0004 requirement | Implementation | Executed tests |
|---|---|---|
| STO-AUTH-01 | `StorageAccessPolicy`, item lookup | `StorageAccessPolicyTest`, `StorageMutationAuthorizationTest`, `StorageItemAuthorizationIntegrationTest` |
| STO-AUTH-02–04 | `StorageHierarchy`, write/mutation helpers | `StorageParentAuthorizationTest`, `StorageFolderReuseAuthorizationTest`, `StorageOwnershipIntegrationTest` |
| STO-AUTH-05–06 | bounded owned ancestry and destination checks | `StorageHierarchyTest` |
| STO-AUTH-07 | `StorageDeletion`, atomic `StorageLeafDeletion` adapter | `StorageDeletionPreflightTest`, five `StorageLeafDeletionIntegrationTest` cases |
| STO-AUTH-08 | typed outcomes and ordered preflight | negative-effect assertions across parent/mutation/deletion tests and real rows/bytes/events assertions |

## Remaining work

Remote CI, Copilot review, native approval and protected-branch merges remain separate acceptance steps. Version-history ownership is tracked in [#74](https://github.com/vypdev/vaultstadio/issues/74); collaboration ownership, quotas/compensation, client startup and the rest of the [audit roadmap](https://github.com/vypdev/vaultstadio/blob/a0d18a2fd560d25a5afafdebfd0a2910aa02c7c7/docs/development/AUDIT_ROADMAP_2026-10-25.md) are not declared solved here. No deployment is included.
