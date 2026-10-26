# Frontend input and accessibility verification — issue #80

The local candidate supports assistant-operated text entry on the exercised Web and macOS paths. All 29 maintained field declarations use the platform form boundary. This is scoped input verification, not whole-platform readiness or merge approval. Contract: [SDD-0013 §6.1.1](../../../specs/SDD-0013-frontend-kmp-and-platform-architecture.md). Machine-readable results: [frontend-accessibility-80.json](frontend-accessibility-80.json).

## Source and artifact identity

- Managed implementation: `e132ea3f19a6f3d6919e58b438b3a3c84973a4c5`.
- Local verification merge: `e38a3881644914d9bd544c9ddbce507c2bcbddfe`, with develop `af97326397ee979294359c03edebc6439be97262` (#77). This local-only merge models the PR integration; it does not synchronize the Action-owned remote branch.
- Full verification: 2026-10-26 02:19:36–02:20:18 UTC, exit 0. Fresh desktop/browser execution was forced; package/compile tasks could reuse matching outputs. Configuration cache stored successfully.
- Actual macOS app: all **154 production JARs** match the verified distributable. The only audit packaging additions are the original non-seeded in-memory preferences provider and class-load evidence. Its loaded provider path/hash were checked before authentication. No token injection or production auth changes.
- Actual Web: immutable copy of the verified production distribution; served index returned HTTP 200 with matching SHA. All six asset hashes are in the JSON.
- Post-review verification: desktop `9c4ff42d28657c1b5e339278a7886a74fbc74185`; final Wasm/production Web `537e82e06435fad241d5ba7e4c6ea1d002633612`. The final Web copy was served and exercised again; all six HTTP asset digests match. The later sections record exact results and the companion JSON preserves the original campaign separately from these rechecks.
- Local GUI persistence readback: 2026-10-26 08:20:39 UTC. Backend is the previously verified #75 storage candidate in the dedicated disposable dogfood environment; this does not certify unimplemented backend roadmap items.

## Current implementation and compatibility decision

`:core:form` owns presentation adaptation, with small files separating specification, native control ownership, metadata, styling, reconciliation, measurement and renderer. Wasm uses actual HTML input/textarea elements through public `HtmlElementView`; other targets retain Material/Basic Compose fields. Features retain their production state/callbacks and domain/data dependency direction. No framework editor discovery, semantic-mirror mutation, swallowed rendering exception or auth backdoor remains.

Pins: Compose **1.13.0-beta01**, Kotlin **2.4.20**, Koin **4.2.2/compiler1.2.1**, Coil **3.6.3**, kotlinx-io **0.9.1**, ws **8.21.3**, Gradle **9.3.1**, AGP **9.1.1**, compile SDK **37**. Native packaging requires JetBrains Runtime **21.0.11**; bytecode remains JVM17. Android minimum/target behavior is unchanged.

**Maintainer decision required:** Compose is prerelease. Coil targets Skiko0.150.1 while Compose resolves0.156.0; the compatibility warning remains visible. Two actual Coil pipeline cases (valid synthetic PNG and invalid bytes) pass, but do not certify all image formats. No dependency forcing, exclusions, warning suppression, scanner weakening, Copilot edits or protection changes are used. The retained stable verification commit is `76c341f42ea5cf0d7d6ea4931f6fdda3d4734ecf`; it reproduces dialog-owner loss and is a source rollback reference, not a fully accepted functional fallback.

## Executed contracts

| Scope | Collected result | Meaning |
|---|---:|---|
| Complete desktop selection | 1,315 cases; 0 failures/errors/skips | All collected desktop modules, not just app tests; latest responsive recheck |
| Complete Wasm browser selection | 1,191 cases; 0 failures/errors/skips | Independent pure data/auth modules and actual browser compositions included; latest IME/startup recheck |
| Production component interaction suite | 151 initial cases + 12 responsive cases | 29×edit/clear/focus, 41 distinct controls, seven locale, 14 disabled, two primitive and production auth layout cases |
| Actual Coil pipeline | 2 cases | Valid PNG and invalid bytes with the resolved runtime |
| Native browser adapter/model | 41 cases | 22 editor, ten multiline, four layout/measurement, five common model cases |
| Real composed browser widget | 5 cases | Production profile validation/open/cancel/reopen, secure reveal/remask/dispose, sequential Unicode/caret, Basic multiline selection and viewport startup |
| Inventory checker | 21 cases | Failures for missing/mismatched declarations/cases, protected-renderer contract and lint evidence |
| Installed ws loopback | 3 cases | Fragmented Unicode echo, rejected invalid typed-array close and valid close |

Rows overlap the complete platform selections and MUST NOT be summed as unique tests. Five common model cases execute on both platforms. The 30 retired framework-bridge cases are historical, excluded from these current counts.

The [field inventory](accessibility-controls-80.json) covers **29 maintained declarations**, including three inactive password fields in the retained Profile ChangePasswordDialog. The routed Change Password screen is a separate component. Dormant legacy duplicate screens are outside this maintained inventory; static consistency does not prove route reachability.

The asynchronous browser runner was fault-tested with a deliberate failed assertion after suspension: the selected test and Gradle build failed. The source was restored byte-for-byte. An earlier invalid control failed before Gradle and is explicitly excluded from evidence. This prevents counting asynchronous tests that silently fail outside the runner.

## Actual GUI dogfooding

The assistant entered existing disposable credentials and submitted normal login with Enter in both Web and macOS. No human typing or seeded UI session was used. Password accessibility output did not expose the credential. On Web, the three routed change-password controls were inspected as `type=password`, with keyboard focus traversal; no new credential was entered or submitted.

Both actual apps completed profile selection/clear (Save disabled), Unicode replacement (Save enabled), cancellation, restored original value and reopening. Web completed two edit/cancel cycles without reload recovery, then continued navigation and sort-menu selection/dismissal. Native profile reopening and folder navigation also worked.

The assistant created separate Unicode-named folders from Web and desktop. Both were independently read back through authenticated local API calls, using a separate API-only login token. The existing synthetic file still contained 66 bytes with SHA256 `3a6e85352f08b63b0bb48c7c4d200e80c6cc98ab324f6c9ee98fef924fed34b3`. The desktop then opened its new empty folder. Original screenshots and sanitized GUI receipt are retained with the private verification evidence.

The search field also accepted typing/clearing without rendering errors. However, the live query `Web final` returned unrelated names after settling. Search correctness remains an unresolved product defect; this is not counted as a successful search journey. It is retained under R03/SDD-0012 for a focused follow-up.

The application selector stalled despite its requested timeout, and two native capture attempts returned capture errors; fresh observations recovered and subsequent bound-handle actions worked. These tool defects remain distinct from the now-successful field operations. Coordinate input was not needed for the accepted native journeys.

## Coverage with explicit denominators

| Compiled JVM production scope | Lines | Branches |
|---|---:|---:|
| New complete form module, no class exclusions | 192/194 = **98.97%** | 99/132 = **75.00%** |
| Existing narrow input-semantics helper | 3/3 = **100%** | 8/8 = **100%** |
| All compiled desktop production, including untested legacy classes | 6,148/21,079 = **29.17%** | 853/5,508 = **15.49%** |

Form coverage combines real app component interactions with model tests. Missing/nonempty execution data and compiled classes are checked before report generation. The existing helper's 100% gate is preserved; it never meant 100% of core UI or the product. Wasm native code has actual DOM/widget cases, not JVM line coverage. Kotlin2.4.20 changed the compiled branch denominator from the previous toolchain; no like-for-like branch-coverage improvement is claimed. Whole-project coverage remains substantial roadmap work.

## Reproduction and gates

Use the reviewed JBR21 runtime, provision Android platform37.0/build-tools36.0.0 and an executable Chrome installation. From `frontend/`:

```sh
./gradlew desktopTest --rerun wasmJsBrowserTest --rerun --continue --no-build-cache
./gradlew :core:form:jacocoTestReport :core:ui:verifyInputSemanticsCoverage :data:network:verifyInstantSerializerCoverage
./gradlew :composeApp:createDistributable :composeApp:wasmJsBrowserDistribution :androidApp:assembleDebug
./gradlew :core:form:compileKotlinIosArm64 :core:form:compileKotlinIosSimulatorArm64
./gradlew :core:ui:detekt :core:form:detekt :feature:auth:detekt :core:resources:detekt
./gradlew :composeApp:ktlintDesktopTestSourceSetCheck :composeApp:ktlintWasmJsTestSourceSetCheck
```

For the diagnostic aggregate, pass `-I ../scripts/audit/aggregate-coverage.init.gradle` and select fresh `desktopTest` before `auditJacocoAggregate` in the same invocation. Run the inventory/unit checks, specification validator, no-FQN check and Actionlint from the repository root. The final campaign selected all these Gradle tasks together; the private receipt retains the complete command and original XML. CI now selects complete desktop/browser suites, publishes form coverage, and uses verified official headless-browser artifacts; the initial workflow-level cache override was ineffective and is rejected below.

All three Android debug APK variants were generated. The new form module compiled for iosArm64 and iosSimulatorArm64. Neither proves execution on an Android/iOS device or that the pre-existing full iOS app is operational. Remote CI/Copilot review and approval must still be evaluated on the published head. Runtime setup #79 remains disabled; no deployment/release is authorized by these checks.

## Rejected candidates and retained diagnostics

| Observation | Rejected approach / correction |
|---|---|
| Same JARs: Corretto17 lacked automatic editable input; JBR21 worked | Pin the actually verified runtime combination; version and vendor both differ, so no isolated cause is asserted |
| Compose1.10.1 node-ID exception after normal login | Upstream stable fix removed that error, then real modal dismissal exposed single-owner loss in1.11.1/1.12.2 |
| Beta multi-owner dialogs worked, but password became unprotected contenteditable | Native owned password controls; privacy regression retained |
| Beta nonsecret select/replace exceeded DOM child length and cancelled the frame clock | All maintained fields now own native input/textarea; a filled accessibility mirror with unchanged model was explicitly rejected |
| Independent network/auth Wasm failed on bare import.meta from kotlinx-io0.8.2 | Explicit network-boundary0.9.1 and complete independent suites |
| CI Chrome cache hit returned a nonexistent root executable | Reserved RUNNER_* overrides are ignored; the pinned upstream macOS cache-hit code also assumes root/chrome. Use one verified private child-process installation and reuse its output. No shared-cache deletion or disabled test |
| First full native run passed tests but failed configuration-cache storage | Coverage input guard now reads declared task inputs; final complete run exited0 with cache stored |

Broader open work is tracked in the [audit roadmap](../AUDIT_ROADMAP_2026-10-25.md), especially authorization, downloads, complete platform support and legacy test/architecture debt. This increment does not mark those capabilities ready.

## CI browser correction after published-head verification

On published head `5dc08b1`, remote backend/tests, Android, plugins, lint, Security Scan and Trivy passed. The desktop compilation/package task also passed (221 executed tasks), then Frontend Build failed before browser tests: setup-chrome returned a non-executable cached root path. Coverage correctly remained skipped. This is not a fully green CI result.

The initial assignment to RUNNER_TOOL_CACHE through GITHUB_ENV did not isolate the JavaScript action: [GitHub reserves RUNNER_* variables](https://docs.github.com/en/actions/reference/workflows-and-actions/variables). Inspection of the exact official bundle also found that KnownGoodVersionInstaller.checkInstalledBrowser returns `root/chrome` on a cache hit, although its cold macOS install returns an app-bundle executable. A fresh actual local Chrome155.0.8059.84 install passed; replaying that same unchanged official bundle against its valid private cache failed with the root-path error. Thus a second Action invocation in one job is also invalid on this pinned macOS implementation.

The final correction in [setup_test_browser.py](../../../scripts/ci/setup_test_browser.py) installs the official **Chrome for Testing Headless Shell155.0.8059.84** directly into an owned job directory. The exact archive SHA256 is pinned for mac-arm64 and mac-x64; download size/time and unpacked size are bounded, unsafe/duplicate/symlink entries are rejected before extraction, executable modes are restored and the actual binary/version is checked before exporting outputs. It installs once and reuses that path for all browser tasks, without a shared cache, Node installer dependency or sandbox-disabling flags. Both official archive source identities were obtained; x64 is not executed on this ARM64 host.

Twelve collected bootstrap cases pass, including pinned-integrity, download/extraction budgets, path traversal, duplicate entries, symlinks, permissions, actual fixture-process version and preservation of a shared-cache marker. Fixture processes are not real-browser proof. Actual official artifact installation/version and browser contracts are separate evidence.

The rejected full-Chrome bootstrap did install/version-check successfully, but all four composed widget cases timed out. Mocha first aborted at its default2s; aligning its outer deadline with the already bounded45s coroutine contracts exposed actual10s state-wait timeouts. The original failures and XML are retained; they are not relabelled successful or attributed to an unproven GPU cause. The module now has a60s outer Mocha limit while preserving45s coroutine and10s state limits, every assertion and failure propagation. Headless Shell is the previously verified automatic-test renderer; actual Web/macOS GUI dogfooding remains separate.

Copilot's `/copilot sync-branch --no-agent` was requested for the managed branch. [Run40295176042](https://github.com/vypdev/vaultstadio/actions/runs/40295176042) prepared a clean merge of exact develop `af97326397ee979294359c03edebc6439be97262` into exact child `5dc08b1db1e33b545e4bdc3f6dd7c42ebc9fcb5b`, then failed `make test` because JAVA_HOME/java were absent and aborted without pushing. After the maintainer explicitly authorized this exact recovery, candidate `280792b8288b6c17024839bd82c08c2df67ca2fc` was published by normal fast-forward push to the existing Action-owned branch, retaining both histories. The recorded recovery and verified Git bundle preserve the prior state. Runtime setup79 remains disabled; this authorization does not include merging PR81 or future exceptional branch synchronizations.

Final local browser recheck: source `b0f70393ff714603021591f49e94003d292972f6`, 2026-10-26 09:13:31–09:14:27 UTC, exit0. The complete fresh XML selection contains1,188 cases with zero failures/errors/skips, including all four actual composed widgets, using the freshly installed official Headless Shell. Stale XML left by retired no-source suites is excluded by execution timestamps, not added to current counts. A deliberate failed assertion after actual suspension also failed the selected test/build under the60s outer deadline; original test bytes were restored, then this complete positive run rebuilt and passed again. The installer’s actual binary SHA256 matches the previous fully verified shell binary. Twelve bootstrap failure controls,21 inventory controls, Actionlint and the specification validator pass.

On published recovery head `280792b`, the frontend encountered a separate runner prerequisite error: Homebrew Python3.14.7 failed TLS issuer validation before downloading the archive. TLS was not bypassed. The downloader now uses `/usr/bin/curl` with normal native macOS trust, HTTPS-only transport/redirects, TLS1.2 minimum, transfer/time limits and the same required SHA256 before extraction. Fourteen bootstrap controls pass, including a real local HTTPS server with an untrusted temporary certificate that curl correctly rejects. A real official download succeeds and produces the identical verified Headless Shell binary; product/browser-test sources and assertions are unchanged. Global trust stores remain untouched. Fresh published-head CI is still required.

## Responsive review counterexample and complete revalidation

Copilot reviewed `cdd8ac3f5f3793a859974c3c0945c7b0f239dce6` and reported that `Modifier.width(400.dp)` overrides narrower parent constraints. The [official width API](https://developer.android.com/reference/kotlin/androidx/compose/foundation/layout/width.modifier) distinguishes this preferred width from `requiredWidth`. The [production-form regression](../../../frontend/composeApp/src/desktopTest/kotlin/com/vaultstadio/app/accessibility/AuthResponsiveInteractionTest.kt) measures unclipped horizontal bounds for every rendered field, submit button and mode switch, then edits/clears the actual email model and invokes production blank-submit validation. Its fixture throws if authentication is unexpectedly called.

Login and registration pass at 280/320/360/400/800dp, plus 320dp with fontScale1.5: **12 cases**. These are actual shared production widgets rendered on desktop; they are not Android/iOS device execution or a complete responsive-design certification.

Two temporary production mutations prove the assertions detect regressions:

| Source | Cases / failures | Observed result |
|---|---:|---|
| Original `width(400.dp)` | 12 / 0 | Controls fit, stay bounded and retain editing/validation |
| Forced `requiredWidth(400.dp)` | 12 / 2 | At280dp, the two forms' controls extend from -20dp to300dp; Gradle fails |
| Exact review suggestion `fillMaxWidth().widthIn(max = 400.dp)` | 12 / 2 | At800dp, the two forms' controls become720dp wide; Gradle fails |

The original production file was restored byte-for-byte after each completed build (SHA256 `f0366e669fae501672f974333418567cdaf3e22d0027b95cd07fad6677456ecc`). No layout change is needed for this alleged defect. The detailed upstream report is [vypdev/copilot#720](https://github.com/vypdev/copilot/issues/720); Copilot itself and its protections are unchanged.

The restored complete root desktop run on `9c4ff42d28657c1b5e339278a7886a74fbc74185` passed **1,315 tests** in257 fresh XML suites, zero failures/errors/skips, with packaging, form/helper coverage gates, auth Detekt and desktop-test ktlint. UTC2026-10-26 09:47:18–09:48:00, exit0. All154 packaged production JAR hashes match the retained GUI-verified package. The12 new cases extend the earlier1,303 desktop selection; the separate1,188-case browser run is unchanged. Current desktop aggregate coverage is6,148/21,079 lines and853/5,508 branches; the complete form module remains192/194 lines and99/132 branches. Raw positive and negative XML and execution receipts are retained privately.

A separate complete backend `test --rerun`, both storage coverage gates and diagnostic aggregate passed on `cdd8ac3`: **1,976 tests** across24 modules with tests, zero failures/errors/skips, UTC2026-10-26 09:37:26–09:39:33. The selection covers all backend test tasks; modules without test sources do not contribute cases. All compiled backend production coverage is6,370/15,998 lines (**39.82%**) and980/5,354 branches (**18.30%**), without exclusions. This independent local result does not substitute for latest-head remote CI or establish whole-platform readiness.

## IME correction, startup and SDK review evidence

The next review correctly found that a multiline textarea with an IME callback consumed plain Enter. The added regression failed before the fix (one failure among41 form browser cases). The editor now invokes IME actions only for editable single-line controls outside composition. A second regression proves that a single-line composition session blocks Enter even if the key event omits its composition flag, then allows ordinary Enter after composition ends. Synthetic keydown checks default prevention; the separate input event checks delivered newline text, without claiming synthetic events perform browser-native insertion.

Final source `537e82e06435fad241d5ba7e4c6ea1d002633612` passed the complete **1,191-case Wasm selection** in253 fresh suites, zero failures/errors/skips, UTC2026-10-26 10:03:48–10:08:43, exit0. Production Web packaging and selected actual-source ktlint/Detekt also complete. The initial fix emitted a Detekt complexity diagnostic despite Gradle returning0; the final availability predicate removes that diagnostic without suppressions or gate changes. Legacy compiler/webpack warnings remain visible. The41 form cases include22 editor, ten multiline, four measurement and five model cases; the five real composition cases include the new viewport startup regression.

Two other findings were disproven against primary sources and executed evidence:

- [vypdev/copilot#721](https://github.com/vypdev/copilot/issues/721): Google's [official SDK catalog](https://dl.google.com/android/repository/repository2-3.xml), retrieved2026-10-26, contains `platforms;android-37.0`; the proposed bare `platforms;android-37` is absent. The actual isolated Android installation/build succeeds. Keep the verified package ID.
- [vypdev/copilot#722](https://github.com/vypdev/copilot/issues/722): exact [Compose1.13.0-beta01 sources](https://repo.maven.apache.org/maven2/org/jetbrains/compose/ui/ui/1.13.0-beta01/ui-1.13.0-beta01-sources.jar) call `clearNodeChildren` inside `ComposeViewport` initialization, removing the initial loader. The new real framework startup case verifies removal after exported content appears. Actual packaged-page DOM also has no loading element and only the Compose component inside its viewport. Keep framework-owned cleanup.

The final six-asset Web distribution was served byte-for-byte and exercised again through normal UI actions: logout, assistant-typed existing credentials, Enter login, masked password output, profile clear/Unicode with disabled/enabled Save, cancel/reopen with the original value visually confirmed, and subsequent file navigation. No profile save or credential change was submitted. Input values are redacted by the observation interface, so direct DOM-value comparisons were discarded; original-value restoration is supported by the retained screenshot and exact model-value component assertions. The folder's AX-targeted double-click was rejected as covered by canvas on two fresh states; a visible coordinate double-click opened the existing folder. This automation limitation is recorded, not presented as a successful AX action. No reload was used to recover dialog editing.

[CI40300565272 on `cdd8ac3`](https://github.com/vypdev/vaultstadio/actions/runs/40300565272) completed all seven technical jobs successfully, including frontend browser contracts on x64, backend, Android, plugins, lint, security and coverage. [Docker40300565249](https://github.com/vypdev/vaultstadio/actions/runs/40300565249) also passed; release was correctly skipped. These are prior-head results. Fresh technical CI, review and maintainer compatibility/approval decisions remain required for the newly published IME correction.
