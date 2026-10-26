# CI cadence and test evidence

Issue #106 implements SDD-0014 §6.6. Copilot workflows and branch protection are unchanged.

| Workflow | Automatic cadence | Manual use |
|---|---|---|
| CI | Every opened/reopened/updated PR to develop/master/main, including drafts | Complete application tests and coverage on the selected ref |
| Secret scanning | Every PR update; Sunday 00:00 UTC | Both offline scanners over the selected complete reachable history |
| Security | Tuesday 03:17 UTC | Full CodeQL and backend container analysis |
| Project runtime verification | Wednesday 03:23 UTC | Native Linux bootstrap, configured Make commands and independent complete suites |
| Extended verification | Friday 03:31 UTC | Kotlin lint, desktop distribution and Android debug APK |
| Docker Build | Version-tag pushes `v*` | Build-only candidate validation, backend JRE/startup smoke, frontend Wasm MIME/build |
| Docker client contract | PRs changing the Docker credential adapter or its contracts | Native credential isolation controls |
| Copilot | Existing issue, commit, PR and lifecycle events | Existing commands and approval process |

Schedules execute the **default branch**. They become effective there after promotion; merging to develop alone does not activate its version of a schedule. Select develop or the exact candidate ref manually when that is the intended scope. A new manual-only workflow may not be registered until its definition reaches the default branch. No tag, release or deployment is created by this CI change.

Manual example after the workflow is available:

```sh
gh workflow run ci.yml --repo vypdev/vaultstadio --ref develop
gh workflow run security.yml --repo vypdev/vaultstadio --ref develop
gh workflow run project_runtime_verification.yml --repo vypdev/vaultstadio --ref develop
gh workflow run extended_verification.yml --repo vypdev/vaultstadio --ref develop
gh workflow run docker.yml --repo vypdev/vaultstadio --ref develop
```

These are deliberate alternatives; do not dispatch all of them for every source update. A manual Docker run never logs into the registry, pushes images or creates a release, even when a tag is selected. Existing **tag-push** release publication is retained and still requires the maintainer's authorization to create the tag.

## What runs for each PR commit

CI has four jobs instead of seven: backend, frontend, source/workflow contracts and strict Codecov upload. Secret detection remains a separate hosted Linux job. Complete backend root `test` includes plugins and every other module; separate plugin packaging is unnecessary to compile and execute their tests. Frontend root `desktopTest` and `wasmJsBrowserTest` run once each, without a preceding selected subset or desktop distribution build. Root `jacocoTestReport` selects every configured module report, and all existing storage, input semantics and serializer coverage thresholds remain required.

Gradle `--rerun` applies to the selected test task, whereas `--rerun-tasks` also recompiles its dependency graph. Test tasks are forced to execute for each new PR head; unchanged compilation/dependencies may be reused. Same-repository PR jobs may write their ref-scoped Gradle cache. Fork code does not execute on shared self-hosted runners; those jobs remain visibly skipped and cannot be claimed as verified. Native Linux runtime verification retains its separate uncached protocol and operator sandbox requirements.

Raw backend/frontend test XML and coverage are uploaded; missing reports fail the upload. Tests, coverage gates and Codecov failures are not suppressed. Codecov still validates its pinned CLI through its isolated verification keyring. Workflow and PR/ref concurrency cancel superseded application/scanner runs without cancelling independent PRs. There are no changed-path test filters and no duplicate develop/master push CI.

The source-contract job keeps accessibility inventory, frontend MIME behavior, FQN style, SDD catalog, browser installer, Codecov keyring and runtime adapter controls. Full Kotlin lint and actual packaging remain explicit weekly/manual checks. The previous `gradlew dependencies || true` job is retired: it listed dependencies, suppressed errors and was not a vulnerability scan. The redundant advisory filesystem Trivy job is removed from PR CI; CodeQL/container analysis and both actual secret detectors remain available at their declared cadence.

## Baseline and how to compare

Successful samples from 2026-10-26, each a single observation:

| Run / job | Execution | Largest costs |
|---|---:|---|
| CI 40361704214, frontend | 17m58s | Distribution 6m38s; browser step 6m38s |
| Same CI, lint | 9m54s | Backend Detekt 6m34s |
| Same CI, backend | 3m12s | Selected tests 1m36s; prebuild 1m |
| Docker 40344821201, backend | 12m01s | Image build 10m44s |
| Security 40353446268, CodeQL | 44m35s | Traced backend 30m25s; frontend 10m18s |
| Same Security, container | 30m26s | Image 9m54s; scan 11m17s; post 8m32s |
| Runtime 40378245743 | 34m51s | Make test 16m44s plus independent complete suites |

CI's seven jobs consumed 2,479 job-seconds in that sample. Created-to-start latency includes runner waiting **and job dependencies**; Codecov's wait is not exclusively runner queue time. Compare durations, source/runtime identity, cold/warm cache state and actual executed XML separately. Removing triggers measures fewer launches; it does not establish a faster individual test suite.

The preserved pre-change native result for develop `0ada608` contains 1,978 backend, 1,315 desktop and 1,191 browser executions, 1,026 XML files and zero failures/errors/skips. Those 4,484 executions are a comparison baseline, **not** a passing result for a later CI candidate. Current source may legitimately add tests. The CI/Copilot results on the final candidate must be recorded in its PR.

Local contract verification:

```sh
python3 -m unittest discover -s scripts/tests -p test_ci_cadence.py -v
python3 scripts/validate-specification-catalog.py
actionlint .github/workflows/ci.yml .github/workflows/security.yml \
  .github/workflows/docker.yml .github/workflows/extended_verification.yml \
  .github/workflows/project_runtime_verification.yml \
  .github/workflows/secret_scanning.yml .github/workflows/docker_client_contract.yml
```

The cadence controls include executing actual workflow test command strings with an intentionally failing wrapper. A separate Gradle 8.10 cacheable fixture demonstrated that root `test --rerun` executes both subprojects without recompiling them, and that `--continue` preserves a nonzero failure while executing the other suite. Neither control substitutes for real application tests. Issue #84 still owns suppressed/incomplete local Make/run-tests entry points; this change does not certify or repair them. Master promotion and known product authorization defects remain separate work.
