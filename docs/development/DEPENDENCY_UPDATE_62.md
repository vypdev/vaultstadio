# Consolidated dependency update (#62)

This PR implements the 15 proposals recorded in [issue #62](https://github.com/vypdev/vaultstadio/issues/62)
on its Action-managed branch. Changes apply to the standalone backend/frontend
projects and active workflows. The issue preserves the complete artifact list and
source commits; this table records how every proposal is reconciled.

| Source PR | Proposal | Result in this PR |
| --- | --- | --- |
| #18 | Temurin JRE 17 to 25 | Backend runtime uses `25-jre`; PR image builds verify its Java version and application health against an isolated PostgreSQL container. |
| #24 | Kotlin and kotlinx group (11 entries) | Backend compiler/serialization plugin `2.3.10`; both projects use coroutines `1.10.2` and serialization `1.10.0`. Backend datetime uses `0.7.1-0.6.x-compat`. Preserve frontend Kotlin `2.3.20-Beta1` and datetime `0.7.1`. |
| #25 | Ktor group (23 entries) | Both catalogs use `3.4.0`; explicitly include `ktor-server-routing-openapi-jvm` for the Swagger API exposed by Ktor 3.4. |
| #27 | Testing group (6 entries) | JUnit Jupiter `6.0.3`, MockK `1.14.9`, Testcontainers `1.21.4`; preserve the already-updated backend Testcontainers version. Declare the JUnit Platform launcher at test runtime. |
| #28 | Koin group (7 entries) | Backend `4.1.1`; preserve frontend `4.2.0-RC1` and compiler plugin `0.3.0`, which are newer than the proposal. |
| #29 | AndroidX test JUnit extension | Both catalogs use `1.3.0`; the separate JUnit 4 coordinate used by Android local tests is unchanged. |
| #30 | MinIO | Both catalogs use `8.6.0`. |
| #31 | Flyway | Both catalogs use `12.0.1`; include the existing PostgreSQL integration and startup checks. |
| #32 | Exposed group (4 entries) | Both catalogs use `1.0.0`; migrate actual backend imports, predicates, pagination and timestamp adapters. |
| #33 | AndroidX lifecycle (2 entries) | Both catalogs use `2.10.0`; preserve the separate JetBrains multiplatform lifecycle version. |
| #35 | Gradle builder image 9.5 | Superseded by #64; no second image target is retained. |
| #64 | Gradle builder image 9.8 | Both builders use `9.8-jdk17`. Existing project wrappers still select Gradle 8.10 for backend and 9.1.0 for frontend; preserve native frontend build placement and published image platforms. |
| #65 | Checkout Action v7 | Active workflows consistently use `actions/checkout@v7`; preserve credential persistence choices and default fork protections. |
| #66 | Codecov Action v7 | Use `codecov/codecov-action@v7` with existing report discovery, authentication and error policy. |
| #67 | Release Action v3 | Existing Docker and Android release steps use `softprops/action-gh-release@v3`. This dependency update does not invoke a release. |

Catalog declarations shared between projects are synchronized where applicable,
including entries not currently consumed by the frontend. Newer frontend Kotlin,
Koin, Compose and AGP versions are preserved. KSP and Koin annotation entries that
are not applied by these builds remain untouched; no obsolete root Gradle build
is restored.

## Compatibility changes

- Remove the unused backend context-receivers compiler flag, which is unsupported
  by the newer Kotlin compiler. No context receiver declarations exist in the backend.
- Keep the backend Instant model through the datetime compatibility artifact and
  explicit conversions in cron scheduling. Exposed's `xTimestamp` retains the SQL
  mapping; see [Exposed compatibility](EXPOSED_UPGRADE.md).
- Align the JUnit test launcher with Jupiter 6 instead of relying on the older
  launcher bundled by Gradle. Preserve every existing test and coverage gate.
- The candidate runtime smoke test uses a private Docker network, no published
  host ports and cleanup limited to IDs/tag created by the job.

## Validation

Local verification of the combined candidate on JDK 17:

- Backend: `test` passes 1,756 tests with zero failures, errors or skips, including
  727 API tests and 318 real PostgreSQL/storage infrastructure tests.
- Backend: `ktlintFormat`, `detektMain`, `jacocoTestReport` and `:api:installDist`
  pass. Fourteen backend coverage XML reports are generated. Verification used
  a 4 GiB Gradle heap, two workers and in-process Kotlin compilation.
- Frontend: `desktopTest`, `:composeApp:jacocoTestReport` and
  `:androidApp:assembleDevDebug` pass; 1,127 desktop tests have zero failures,
  errors or skips. `detektDesktopMain` passes with the existing rules.
- Docker client isolation: all three existing contract tests pass.
- All active workflow YAML files parse. Actionlint 1.7.12 reports no new findings
  compared with develop when configured with the existing `codex` runner label;
  its three existing `concurrency.queue` parser limitations remain unchanged.
- Full frontend `ktlintFormat` reports eight pre-existing file-naming/line-length
  violations in untouched common tests. No lint rule or gate is disabled, and
  unrelated formatter-only edits are excluded from this PR.
- Both Docker images and the JRE 25 startup smoke are being verified against the
  candidate. Their result and the remote CI status are recorded in the PR.

Existing results from the closed source PRs are reference material and are not
claimed as results for this combined candidate.

## Upstream migration references

- [JUnit 6.0.3 release notes](https://docs.junit.org/6.0.3/release-notes.html)
- [Ktor 3.4 changes](https://ktor.io/docs/whats-new-340.html)
- [Exposed 1.0 migration](https://www.jetbrains.com/help/exposed/migration-guide-1-0-0.html)
- [Checkout v7 changes](https://github.com/actions/checkout/blob/v7/README.md)
- [Codecov v7 changes](https://github.com/codecov/codecov-action/blob/v7/README.md)
