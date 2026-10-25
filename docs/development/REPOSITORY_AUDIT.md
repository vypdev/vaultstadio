# VaultStadio Repository Audit

**Review date**: 2026-09-20 UTC
**Repository**: `vypdev/vaultstadio`
**Branch**: `master`
**Reviewed commit**: `3e76042f1a47eabcf92ce0967c756aa3ab79b9de`
**Review mode**: Read-only evidence collection; no source or workflow changes were made.

## Scope and limitations

This baseline covers repository shape, documentation and design records, Cursor rules, CI and security workflow claims, local build/test evidence, RepoWise, Graphify, and GitHub metadata visible from the authenticated account. It does not prove production deployment health, external consumers, or the health of the self-hosted macOS runners. Those areas require separate live-system evidence.

The findings below distinguish current checkout evidence from historical documents. This document is a review baseline, not an approval to perform broad refactoring or dependency upgrades.

## Executive recommendation

Prioritize a **documentation-contract and verification slice** before architectural refactoring:

1. Reconcile documentation and Cursor rules with the current `backend/` and `frontend/` project layout.
2. Introduce a small, explicit Software Design Decision (SDD) index so proposals, current decisions, and historical analyses cannot be confused.
3. Add a deterministic documentation/link validator and run it in CI.
4. Re-run the backend test matrix on a runner with Docker and sufficient memory, then record the result against an immutable commit.
5. Keep RepoWise and Graphify as advisory navigation/evidence tools. Do not refactor solely to improve their scores or graph shape.

The smallest reversible next action is to repair the stale paths and links, then publish one SDD describing the current standalone Gradle topology and its dependency-direction rules.

## Verified repository baseline

| Area | Evidence | Assessment |
|---|---|---|
| Repository state | Public, not archived, default branch `master`, 0 stars, 0 open issues, 17 pull requests; `master` is clean at the reviewed SHA | Active codebase with limited public adoption signal; product value and consumers are not established by this audit |
| Recent activity | Latest commit on 2026-09-17: `chore: remove destructive clean rebuild script`; preceding substantive-looking activity is concentrated in February 2026 | Recent pushes do not by themselves prove current product health |
| Project layout | `backend/settings.gradle.kts` and `frontend/settings.gradle.kts` are independent Gradle projects; both have their own wrapper and settings file | This is the current build boundary and should be the source of truth |
| Documentation | 83 non-code files were skipped by the code-only Graphify run; documentation is broad but contains historical and proposal material mixed with current guides | Strong breadth, weak freshness/status contract |
| Docker | `docker compose -f docker/docker-compose.yml config --quiet` succeeded locally with a warning that the top-level `version` attribute is obsolete | Syntax is currently renderable; warning should be removed in a separate Compose cleanup |
| Frontend tests | `JAVA_HOME=.../jdk-17 ./gradlew :composeApp:desktopTest --no-daemon --continue` succeeded: Gradle 9.1.0, 189 actionable tasks | Positive desktop test evidence, with many deprecation/opt-in warnings |
| Backend tests | Full `./gradlew test --no-daemon --continue` failed with 6 task failures. A focused infrastructure run completed 288 tests and failed 2 Testcontainers initialization tests because Docker was unavailable | Do not classify this as an application-wide test failure; it is a mixed environment/resource and infrastructure-test blocker |
| GitHub Actions | The latest listed `CI` and `Docker Build` runs on `master` (created 2026-09-17) were cancelled; earlier scheduled Security runs include both success and failure | Current remote CI health is not green evidence; runner and cancellation causes need investigation |

## Documentation and SDD findings

### P0 — Documentation paths no longer match the checkout

A local relative-link audit checked 213 links in `docs/` and found 13 unresolved targets. The most consequential examples are:

- `docs/architecture/ARCHITECTURE.md` describes `kotlin-backend/`, `compose-frontend/`, and `docker/docker-compose.kotlin.yml`, while the checkout contains `backend/`, `frontend/`, and `docker/docker-compose.yml`.
- `docs/development/TESTING.md`, `docs/frontend/FRONTEND_TESTING.md`, and `docs/development/PLATFORM_BUILD_REPORT.md` still publish commands and paths using `kotlin-backend` and `compose-frontend`.
- `docs/DOCUMENTATION_ANALYSIS.md` links as if it were inside `docs/architecture/`, and still recommends resolving an `ACTION_PLAN.md` that is absent from the repository.
- `docs/getting-started/QUICK_START.md` contains unresolved references to the Phase 6 API document.

These are not cosmetic problems: a contributor or AI following the documented command can invoke a nonexistent Gradle project or miss the intended source file.

### P0 — Current frontend architecture is contradicted by historical examples

The checkout contains `frontend/composeApp/src/main/kotlin/com/vaultstadio/app/VaultStadioRoot.kt` and no `AppViewModel.kt` source file, while several current-looking documents still describe `AppViewModel`, `compose-frontend`, and a legacy screen layout. `docs/architecture/KNOWN_ISSUES.md` correctly records the migration to per-screen ViewModels, but the surrounding examples and testing sections still encode the old model.

The next docs slice should update examples only after confirming the current source entrypoints and test locations. Do not preserve a retired architecture as if it were an alternative supported path.

### P0 — Cursor rule links and globs need an explicit path contract

The `.cursor/rules/*.mdc` files contain 34 relative links that resolve as missing when interpreted relative to their file locations. They also contain stale globs such as `kotlin-backend/**` and `compose-frontend/**`. Some links may have been intended as repository-root references for an IDE, but that convention is undocumented and is not portable to normal Markdown renderers.

Choose one contract and validate it:

- use correct file-relative links such as `../../docs/...` for rendered Markdown; or
- use plain code paths for IDE-only references and document that they are not Markdown links.

### P1 — SDD/ADR structure is missing

The repository has architecture reviews, proposals, issue lists, and roadmap sections, but no explicit `SDD`, `ADR`, or decision-record directory was found. As a result, documents such as `BACKEND_CLEAN_ARCHITECTURE_PROPOSAL.md`, `CLEAN_ARCHITECTURE_REVIEW.md`, and the large architecture roadmap do not expose a consistent status, owner, decision date, supersession relationship, or acceptance criteria.

Proposed structure:

```text
docs/architecture/decisions/
  README.md
  SDD-0001-standalone-gradle-projects.md
  SDD-0002-backend-layer-direction.md
  SDD-0003-frontend-decompose-and-viewmodels.md
  SDD-0004-storage-backend-capabilities.md
```

Each SDD should include: status (`proposed`, `accepted`, `superseded`, or `rejected`), date, scope, decision, alternatives, consequences, source evidence, validation command, and links to superseding decisions. Existing proposals should be classified before being converted; do not silently rewrite historical analysis into an accepted decision.

### P1 — Rule contradictions should be removed, not explained away

`.cursor/rules/vaultstadio-core.mdc` says S3 is planned and not implemented, while the checkout contains `S3StorageBackend` production classes and tests. The same rule set says the architecture roadmap is Phase 7 while the main architecture document marks Phase 6 complete and contains a future Phase 7 section. These statements need one authoritative status vocabulary.

### P1 — CI and security documentation overstates enforcement

The workflow configuration contains several fail-open or non-blocking paths:

- `security.yml` uses `|| true` for dependency/build commands and `continue-on-error: true` for Trivy, TruffleHog, and Gitleaks.
- `ci.yml` sets Codecov `fail_ci_if_error: false`.
- `docker.yml` allows the frontend build and frontend image publication path to continue on error.

This may be intentional while the self-hosted infrastructure is unstable, but documentation must label these checks as advisory instead of implying that they are release gates. Hardening them is a separate operational change requiring runner evidence and an explicit failure policy.

### P0 — Onboarding and published metadata still use a placeholder repository

The following tracked files still point to `github.com/yourusername/vaultstadio`: `README.md`, `docs/getting-started/QUICK_START.md`, and `helm/vaultstadio/Chart.yaml` (home, source, and icon metadata). A new contributor or operator can copy an invalid clone URL, and Helm metadata points away from the actual project.

Replace the placeholder with the canonical repository URL and add a validator that rejects `yourusername` in user-facing metadata.

### P0 — API documentation has no single authoritative contract

The repository contains a generated-looking OpenAPI YAML with 12 path entries, while `docs/api/API.md` documents a much larger surface including Sync and Collaboration endpoints. A source scan found 218 route-like declarations, but that count also includes non-route `get()` calls and must not be treated as an exact endpoint count.

Choose one of these explicit contracts:

1. generate or validate OpenAPI from the implemented route contract and publish the generated artifact; or
2. label the YAML as partial, state that source routes are authoritative, and add a coverage matrix for documented versus specified endpoints.

Until then, Swagger/OpenAPI and `docs/api/API.md` can disagree while both appear authoritative.

### P1 — Release workflows reference a different project shape

`.github/workflows/release_workflow.yml` and `.github/workflows/hotfix_workflow.yml` contain steps named for `package.json` and `dist/`/`build/` publication, but the checkout has no root `package.json`, `build/`, or `dist/`. The Kotlin project publishes through the backend and frontend Gradle projects instead. These workflows need a deliberate classification as retired, experimental, or migrated before being treated as release gates.

### P1 — Internal documentation violates the English-only repository rule

The repository rules require English, but the audit found Spanish prose in `docs/architecture/BACKEND_CLEAN_ARCHITECTURE_PROPOSAL.md` and multiple lines in `docs/development/PLATFORM_BUILD_REPORT.md`. This should be handled as a documentation cleanup slice, with technical claims revalidated rather than mechanically translated.

### P0 — The published Swagger URL is inconsistent with the implementation

`backend/api/src/main/kotlin/com/vaultstadio/api/config/Swagger.kt` configures Swagger at `/swagger`, while `README.md`, `docs/getting-started/QUICK_START.md`, `docs/operations/DEPLOYMENT.md`, and `docs/api/API.md` advertise `/swagger-ui`. Standardize the documented URL or change the implementation after an explicit API decision; do not leave both paths presented as supported.

### P0 — Security workflow references a nonexistent Dockerfile

`.github/workflows/security.yml` references `docker/Dockerfile.backend.kotlin`, but the checkout contains `docker/Dockerfile.backend`. The command is also fail-open via `|| true`, so the missing file can be hidden from the workflow result. This should be corrected or the scan step should be retired, then its failure policy should be made explicit.

### P1 — Release and product versions have no single source of truth

Verified version values diverge across the repository: `backend/build.gradle.kts` uses `1.0.0-SNAPSHOT`, `helm/vaultstadio/Chart.yaml` uses `2.0.0`, `CHANGELOG.md` lists `2.2.0` as the latest release, and the release/hotfix workflow defaults use `1.0.0`. Establish and document the authoritative version source before changing release automation or publishing artifacts.

### P1 — The documentation index is incomplete and contains stale analysis

`docs/INDEX.md` and `docs/DOCS_STRUCTURE.md` omit several tracked documents, including the file/folder analysis, frontend task-cycle documentation, directory-structure analysis, frontend modularisation document, platform build report, and WASM optimization document. `docs/DOCUMENTATION_ANALYSIS.md` also makes claims contradicted by the current tree, including saying that `PROJECT_OVERVIEW.md` and `GLOSSARY.md` are absent. The index and analysis should be refreshed together, with historical conclusions explicitly labelled.

### P1 — CI and release onboarding requirements are inconsistent

The repository uses JDK 17 in CI, backend build configuration, and Dockerfiles, while `CONTRIBUTING.md` and Quick Start require JDK 21+. The backend and frontend also use different Gradle wrapper versions. `docs/frontend/FRONTEND_TESTING.md` describes release-tag CI behavior that is not present in `.github/workflows/ci.yml`. Publish one supported toolchain matrix and a dedicated CI/release runbook before treating workflow behavior as a contributor contract.

## RepoWise evidence

`repowise 0.42.0 health --no-workspace --format json --refactoring-targets` completed against the reviewed checkout. It produced 20 targets. The highest-priority signals included:

- `frontend/data/collaboration/.../CollaborationWebSocket.kt`: critical nested-complexity signal;
- `backend/api/.../CollaborationWebSocket.kt`: high nested-complexity signal;
- `backend/core/collaboration/.../CollaborationOT.kt`: high nested-complexity signal;
- `backend/core/version/.../FileVersionService.kt`: high-complexity `generateLineDiff`;
- `backend/api/.../AdminRoutes.kt`: high large-method signal;
- `frontend/.../KeyboardShortcuts.kt`: high-complexity signal.

RepoWise also created a local `.repowise/` directory during the read-only run; it was removed and the checkout was verified clean. The repository should not commit generated RepoWise state unless a separate, documented evidence policy requires it. These findings are triage signals. Before changing a target, audit callers, semantic ownership, composition boundaries, focused tests, and failure contracts.

## Graphify evidence

`graphify 0.9.46 extract --code-only --no-cluster` completed outside the repository:

- 1,252 code files scanned;
- 11,261 nodes and 30,209 raw edges written to an external graph;
- 4 SQL files contributed nothing because `tree_sitter_sql` was unavailable;
- 2 Kotlin files had syntax errors and may be partially extracted: `backend/api/.../ApiResponse.kt` and `backend/api/.../PluginManager.kt`;
- 2 files were skipped as potentially sensitive (`secrets.yaml` and `serviceaccount.yaml`).

The multigraph diagnostic reported 5,714 dangling endpoint edges and 218 relation-variant endpoint groups. The output is useful for navigation and candidate inspection, but it is not proof of architectural cycles or of a healthy graph. The code-only mode intentionally skipped 83 documentation files. If dependency direction becomes a release criterion, add a deterministic directed-import/SCC check over production source and report Graphify limitations beside the result. Do not commit graph output or run LLM community labeling as part of a normal documentation check.

## Prioritized remediation backlog

| Priority | Change | Scope | Exit evidence |
|---|---|---|---|
| P0 | Replace stale project paths, Docker filenames, AppViewModel examples, and dead links | Documentation and Cursor rules only | Link/path validator reports zero unresolved repository paths; all examples resolve in the current checkout |
| P0 | Add a docs contract validator | Small script plus CI invocation | Deterministic report for Markdown links, known project roots, stale module names, and required SDD metadata |
| P1 | Create SDD index and classify existing architecture documents | Documentation only | Every architecture proposal/review has an explicit status and source-of-truth relationship |
| P1 | Repair test and coverage command documentation | `docs/development/TESTING.md`, frontend docs, coverage plan | Commands match `backend/settings.gradle.kts`, `frontend/settings.gradle.kts`, and CI task names |
| P1 | Decide whether security/coverage/Docker checks are blocking | Workflow policy and operator evidence | Deliberate fail-open exceptions are documented with owners and removal criteria |
| P2 | Re-run RepoWise/Graphify on a frozen clean SHA | External reports only | Tool versions, command lines, SHA, warnings, and output locations are recorded without generated artifacts in Git |
| P2 | Address the first two or three verified code hotspots | Focused code slices | Caller-complete tests, typecheck/lint, full relevant suite, and independent review; no metric-only refactor |

## Explicit non-findings and open questions

- This audit does not classify the repository as obsolete. Consumer, deployment, package, and production-use evidence was not established.
- RepoWise scores do not prove defects, and Graphify dangling edges do not prove broken runtime dependencies.
- The backend test result does not prove that the two repository implementations are broken: the focused failures occurred during Testcontainers initialization because Docker was unavailable.
- The self-hosted macOS runner, release publication, GHCR images, and TrueNAS/Helm deployment were not validated from this Linux checkout.
- The current supported product scope for Phase 6/7 is not resolved by documentation alone; it needs an owner-approved product decision.

## Proposed decision gate

Before the next implementation block, accept or reject this narrow sequence:

1. **Docs contract**: reconcile paths, links, stale architecture vocabulary, and Cursor globs.
2. **SDD baseline**: record the standalone Gradle topology and the current backend/frontend dependency direction.
3. **Verification**: add the deterministic docs validator, then run backend tests on a Docker-capable, adequately provisioned runner.
4. **Only then** select one code hotspot from RepoWise, with a focused contract and test plan.

No broad dependency upgrade, repository archival, architecture rewrite, or graph-driven mass refactor is justified by this audit alone.
