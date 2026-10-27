# Critical backend and desktop coverage — issue #119

This is the preserved 6,081-case checkpoint at `92404e45e596e7832e7bfb59d9d5b2fb697054fe`. A later private incoming-body probe reproduced four transfer-cancellation failures; shared cancellation refinement and new full-candidate acceptance are in progress. Local complete verification passed; final-ref PR CI, Copilot review and adoption remain separate steps. The [machine results](critical-coverage-119.json) and [98 risk families mapped to actual tests](critical-coverage-119-scenarios.json) distinguish executed cases, repeated platforms and outstanding capabilities. Documentation commits after this implementation must leave production and tests unchanged; PR CI verifies its own final ref.

## Scope and repairs

The pre-code contracts were committed in `7aad7e5`, `e839345`, `64fe6ba`, `5599f83` and `cd212f8`, using the existing SDD-0003/0004/0013/0014 format. The official Action created the linked #119 branch before implementation. Copilot and its generated files are unchanged.

Eight empty auth-route tests mounted no production application and contained no behavioral assertions. They are replaced with real handlers, DI, BCrypt, migrated PostgreSQL and owned filesystem bytes. Registration validates the supported email shape; malformed JSON returns a safe 400; inactive identities cannot authorize an old session; failed logout persistence cannot claim success. Refresh uses a conditional SQL update against the previous hash: two concurrent consumers have exactly one winner, and logout during an already-read refresh cannot yield an unpersisted credential pair. Direct SQL controls exercise every session-provider error, expiry cleanup, identity lookup and revocation without fabricated state.

The production HTTP factory accepts an owned engine/logger for testing. Reified wrappers supply codecs to one executed response path; typed missing data cannot masquerade as Unit success, and body/form Unit commands work. Cancellation remains control flow through request and error-body decoding, while a real configured timeout remains a network failure. Tests caught and repaired a JVM protected-receiver regression across modules during that refactor. The logger test uses the actual public `/api/v1/share/{token}` route and metadata/view/download/password-query variants; its earlier plural fixture was corrected before final acceptance.

Storage tests preserve binary multipart content, relative paths, paging/sort/identity, token rotation and transfer errors. Folder progress follows streamed bytes; cancellation propagates and a missing override cannot manufacture `Bearer null`. A real loopback HTTP server with the default CIO engine verifies multipart Content-Type and bytes independently of MockEngine.

Files state is split into focused request, info-panel and batch-outcome collaborators. Older loads/pages/activity cannot overwrite newer scope, cancellation releases owned progress, paths search later pages and stop on empty/repeated pages, and duplicate pagination is guarded. Failed optimistic moves retain their cause through refresh. Partial or contradictory batch results preserve failed/uncertain selection; the single-item move path applies that policy too. Successful mutations and retry paths remain covered.

## Complete executions and populations

`make test-coverage` completed **6,081 product executions** in 455.03 seconds: **3,296 backend**, **1,455 desktop** and **1,330 Wasm**, all with zero failures, errors and skipped cases. This is an increase of 308 over the adopted #84 baseline; eight placeholder cases were retired. Common cases running on both JVM and Wasm are repeated platform executions, not distinct scenarios. Ninety-six risk families have behavioral examples, NET119-24 verifies the owned test boundary, and FILE119-32 explicitly keeps separate roadmap work.

The default inventory still discovers all applicable modules and test tasks rather than a hand-maintained list. Modules without test sources add no passed cases. Android/iOS-specific tests and hardware remain outside this certificate, as documented in [Testing](../TESTING.md). JVM coverage includes every main class output from all 44 backend and 52 desktop modules, including untested code. No source/class exclusions or threshold reductions were introduced. Compiler-generated/inlined inputs and JaCoCo report classes are different populations.

| Full population | Adopted baseline | This implementation |
|---|---|---|
| Backend lines | 8,958 / 16,877 (53.08%) | 9,246 / 16,903 (54.70%) |
| Backend branches | 1,934 / 6,042 (32.01%) | 1,995 / 6,059 (32.93%) |
| Desktop JVM lines | 6,153 / 21,079 (29.19%) | 6,955 / 21,172 (32.85%) |
| Desktop JVM branches | 853 / 5,508 (15.49%) | 1,180 / 5,622 (20.99%) |

| Focused module | Baseline lines | Current lines | Current branches |
|---|---|---|---|
| backend:core:auth | 199 / 283 (70.32%) | 243 / 289 (84.08%) | 87 / 138 (63.04%) |
| backend:infrastructure | 1,500 / 3,358 (44.67%) | 1,632 / 3,371 (48.41%) | 336 / 895 (37.54%) |
| frontend:data:network | 21 / 172 (12.21%) | 141 / 184 (76.63%) | 50 / 72 (69.44%) |
| frontend:data:storage | 80 / 545 (14.68%) | 285 / 556 (51.26%) | 60 / 82 (73.17%) |
| frontend:feature:files | 4 / 461 (0.87%) | 480 / 531 (90.40%) | 217 / 256 (84.77%) |

| Critical source (includes its generated/nested classes) | Lines | Branches |
|---|---|---|
| UserService.kt | 235 / 281 (83.63%) | 69 / 120 (57.50%) |
| UserInputValidation.kt | 8 / 8 (100.00%) | 18 / 18 (100.00%) |
| AuthRoutes.kt | 76 / 76 (100.00%) | 15 / 34 (44.12%) |
| ExposedSessionRepository.kt | 125 / 125 (100.00%) | 13 / 24 (54.17%) |
| BaseApi.kt | 69 / 84 (82.14%) | 34 / 44 (77.27%) |
| HttpClientFactory.kt | 43 / 43 (100.00%) | 8 / 8 (100.00%) |
| StorageApi.kt | 155 / 159 (97.48%) | 60 / 68 (88.24%) |
| FilesLoadingCoordinator.kt | 33 / 33 (100.00%) | 18 / 18 (100.00%) |
| FilesLoader.kt | 108 / 111 (97.30%) | 55 / 62 (88.71%) |
| FilesBatchOutcome.kt | 15 / 15 (100.00%) | 21 / 26 (80.77%) |
| FilesInfoState.kt | 31 / 31 (100.00%) | 13 / 20 (65.00%) |
| FilesViewModel.kt | 289 / 304 (95.07%) | 110 / 130 (84.62%) |

Root/source-file line counters are consolidated; summing class line counters would double-count overlapping lines. The two inlined `Comparisons.kt` lines shared across backend modules are kept as a separate shared population rather than counted in both modules. Branch/instruction counters remain additive and reconcile to the canonical reports. BaseApi retains the thin reified wrappers and their missed compiler-generated paths; these are reported rather than excluded. Complete coverage is not claimed for UserService administration/password paths, other infrastructure, screens or the whole platform.

The verifier's 132 controls pass separately, with 586/597 lines and 244/256 branches covered. The existing storage, serializer and input-semantics gates and actual `make lint` pass; legacy advisory Detekt findings and kotlinx-datetime compatibility deprecations remain disclosed. Ktlint 1.5.0 formats the changed Kotlin files without changing lint configuration. Source/SDD/accessibility, architecture, browser bootstrap, setup, workflow/cadence, anonymous-share Node and real Nginx contracts were also executed. Actionlint 1.7.12 reports only three unsupported `queue: max` keys in unchanged generated workflows; that syntax is [valid GitHub Actions configuration](https://docs.github.com/en/actions/how-tos/write-workflows/choose-when-workflows-run/control-workflow-concurrency). This tool limitation is not suppressed or represented as a passing lint run.

## Evidence and limitations

Original command output, matching suite receipts, raw JUnit XML, JVM execution inputs and canonical XML are preserved in the final archive, SHA256 `73400ad3a7c19e4275627084dcbfdf8745487a6ab816f7bf3a7f15e2d72057da`. The machine result lists the separate red-phase archive hashes. Those phases preserve source and actual failures before repair; the refactor regression and corrected logger fixture are not mislabeled as baseline product behavior. Tests that directly use domain ports or MockEngine are explicitly distinct from real PostgreSQL/HTTP/bytes tests.

The original two detached SDD drafts retain their exact hashes. Normal commits/pushes preserve history on the official managed branch. Existing dogfood data, package 1499c6b and rollback 03aa855 remain intact; read-only health checks returned 200. That old package health is not evidence that these new changes have been GUI-dogfooded. Final-ref CI and its coverage upload must pass independently. Master, protections and VERIFICATION_SETUP=false remain unchanged.

## Remaining work

1. [#78](https://github.com/vypdev/vaultstadio/issues/78): the client still downloads from `/storage/item/{id}/download`, while the server implements `/storage/download/{id}`. The real server owner/foreign-owner tests use the server route; the MockEngine binary test deliberately does not certify URL compatibility. Fix the complete client/server download/upload journey and rendered application next.
2. [#82](https://github.com/vypdev/vaultstadio/issues/82) and [#115](https://github.com/vypdev/vaultstadio/issues/115): search semantics, filters before pagination and response ownership need their own acceptance. Current basic search-port tests cannot close those issues.
3. [#109](https://github.com/vypdev/vaultstadio/issues/109), [#112](https://github.com/vypdev/vaultstadio/issues/112), [#116](https://github.com/vypdev/vaultstadio/issues/116) and [#111](https://github.com/vypdev/vaultstadio/issues/111): recovery, full collaboration, Web API origin and trusted runtime-setup acceptance retain their contracts.
4. Continue measured coverage of remaining services/adapters and rendered desktop journeys, then the architecture roadmap. UserService still combines several account responsibilities; this increment does not claim to remove every legacy large file. The former 429-line SQL file is now three focused adapters; FilesViewModel delegates request, activity and partial-result ownership. Search/share lifecycle cancellation, unresolved-path navigation ownership and activity error presentation are not fully certified by this scope.

The [audit roadmap](../AUDIT_ROADMAP_2026-10-25.md) remains the platform-wide plan. Broader operational readiness and promotion to master are not implied by this coverage increment.
