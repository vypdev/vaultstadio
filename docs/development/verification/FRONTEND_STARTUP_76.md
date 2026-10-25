# Frontend startup verification — issue #76

Verified on 2026-10-25 against source `4fcc7782c4a031465cab7c8b25f8ae98e059f3c7`. This report covers the startup correction in SDD-0013 §6.4 and the corresponding SDD-0002 transport contract. It does not certify the whole platform as operational.

## Changes and architectural boundaries

The custom contextual Instant serializer now owns the descriptor name `VaultStadio.InstantIso8601`. The previous `kotlin.time.Instant` name collides with the serialization library's built-in descriptor during construction of the production Json factory. ISO-8601 string values, precision and DTO schemas remain unchanged. The correction stays in the network adapter; no domain or feature workaround was introduced.

The production Nginx configuration assigns the Wasm MIME type through `types`, eliminating the duplicated `Content-Type` response header. Missing Wasm files still return 404 and ordinary SPA routes still resolve to the HTML entry point.

Desktop packaging used to require three icon files that do not exist in the repository. It now uses jpackage's default icon until real branding assets are versioned. CI builds the actual macOS distributable so this failure is detectable. Dependencies and Copilot files/settings are unchanged.

## Executed verification

| Verification | Result | Scope |
|---|---|---|
| Entire desktop test task selection | 1,142 tests, 245 suites; zero failures, errors or skips | All projects selected by the root frontend `desktopTest` task |
| Production Json serializer cases | 15 unique cases, executed on JVM and Wasm | Initialization, built-in/contextual coexistence, scalar and collection round trips, epoch, pre-epoch, UTC offsets, nanoseconds, nullable/nested/unknown fields, malformed/type/missing/null rejection |
| Actual Nginx HTTP probe | 4/4 pass | GET and HEAD each have one `application/wasm` header and correct body behavior; missing Wasm 404; SPA fallback 200 |
| Serializer coverage gate | Pass: 5/5 lines, 4/4 methods, 19/19 instructions | Production serializer class; it has no branch counter |
| Desktop distributable / production Wasm | Both generated successfully | Java 17 on this macOS machine; executable app and optimized browser assets |
| Static checks | Pass | `detektDesktopMain`, network `ktlintCheck`, specification catalog, no-FQN and Actionlint |

The 15 Wasm executions repeat the same 15 serializer cases on another platform; they are not 15 additional unique behaviors. The complete network module has 16/172 covered lines. The aggregate report across all compiled desktop modules, without class exclusions in the audit report, has 4,024/20,748 covered lines (19.39%) and 359/12,782 covered branches (2.81%). The scoped 100% serializer gate must not be presented as whole-module or whole-frontend coverage.

Negative controls were retained: the original serializer failed all 14 then-existing factory cases before the fix; the original Nginx configuration failed both successful Wasm MIME assertions. The coverage gate also failed correctly with only 4/5 serializer lines covered before the real collection-serialization case exercised the remaining descriptor API. No coverage threshold was lowered.

The first combined build hit an internal Compose/JVM inline-compilation error in generated/incremental state. A clean of the composeApp generated outputs rebuilt the same production source successfully; the subsequent complete published-candidate run also passed. The missing-icon packaging failure was independently reproduced and corrected in source.

The first remote frontend job (`40221813627`, job `121254805611`) then exposed a missing runner prerequisite: Karma could not find Chrome at its default macOS application path. CI now provisions a dedicated browser using the commit-pinned `browser-actions/setup-chrome` action and passes its output as `CHROME_BIN`; the job verifies the executable and records its actual version before running the mandatory tests. This is an environment correction, not a test skip. The new remote run must pass before merge.

## Real local dogfooding

The production Wasm distribution ran behind the real Nginx configuration on a dedicated loopback origin. A new PostgreSQL database and new local-content volumes held only synthetic data. The backend executable was the separately retained, verified #75 candidate (`260489a0131419cc1c1231d952681c81d067c802`); its standalone distribution SHA256 is recorded in the companion JSON. This is a deliberate combined frontend/backend test, not evidence that #76 contains the storage fix.

Personally observed in the browser:

- Fresh application startup reached a visible usable login form.
- An invalid password was rejected and the expected error appeared.
- The disposable account authenticated and reached My Files.
- Creating a folder, opening it, selecting a synthetic file through the upload chooser and uploading it all succeeded.
- The UI displayed the 66-byte upload as completed. The production API independently listed the same folder/file and returned exactly the uploaded bytes. SHA256: `3a6e85352f08b63b0bb48c7c4d200e80c6cc98ab324f6c9ee98fef924fed34b3`.

The generated macOS application was launched directly. Its real login form and empty-form validation appeared without the previous serialization/Koin initialization crash. Before interacting, the process log confirmed `in-memory Java preferences active`. No real desktop preference node or session was replaced.

**Desktop interactive login is verified, with human assistance for text entry.** Native automation could not enter values into the Compose fields, so the maintainer supplied the disposable values and left submission to the assistant. The assistant activated Log in, observed HTTP 200 from the real native client, verified My Files and the correct account/folder, then opened the folder through its accessible node and observed the uploaded file. This exercises the real packaged UI and authentication path; no token-seeded session workaround was executed. Autonomous text entry remains an explicit follow-up in #80. Disk persistence across restart is not established by the in-memory test provider.

## Confirmed remaining blockers

The browser details-panel Download action currently computes a URL without invoking a download adapter. Clicking it produced no download and no backend download request; reading the production handler confirmed the no-op. The successful authenticated API byte comparison above is explicitly separate from this failed GUI flow. Batch/grid/list download entry points and authenticated download transport require a focused follow-up.

The Action's requested branch synchronization failed in its Linux verification job because `JAVA_HOME` and `java` were absent. The failure is documented by runs `40219787333` and `40219788305`. Existing verification commands remain intact; the project runtime must be provisioned through the supported repository setup before retrying official synchronization. No contributor-side remote branch synchronization was performed.

Android/iOS runtime behavior, Windows/Linux packages, real desktop disk persistence, complete feature flows, remote API configuration, responsive/accessibility behavior after dialogs, and the low aggregate coverage remain open work in the audit roadmap. This PR does not close those items.

## Reproduce the verification

From `frontend/`, with Java 17, the project Android SDK configuration and a browser available:

```sh
./gradlew desktopTest :data:network:wasmJsBrowserTest \
  :data:network:verifyInstantSerializerCoverage :data:network:jacocoTestReport \
  :composeApp:createDistributable :composeApp:wasmJsBrowserDistribution \
  detektDesktopMain :data:network:ktlintCheck --continue --no-build-cache
```

From the repository root, with a local Docker daemon:

```sh
python3 scripts/check-frontend-wasm-mime.py
python3 scripts/validate-specification-catalog.py
./scripts/check-no-fqn.sh
```

The companion JSON records the actual Nginx image/configuration digests and built client artifact hashes. Private raw JUnit XML, complete aggregate coverage, execution metadata, logs and GUI screenshots were retained outside Git; credentials and local session tokens are not published.

For safe desktop GUI verification, compile `scripts/dogfood/java/com/vaultstadio/audit/InMemoryPreferencesFactory.java` into a helper JAR with the same JDK used to launch the app. Run its `main` first with `-Xbootclasspath/a:<helper.jar>` and `-Djava.util.prefs.PreferencesFactory=com.vaultstadio.audit.InMemoryPreferencesFactory`; it refuses an unconfigured/default provider before writing its probe. Launch the generated executable with these same options through `JAVA_TOOL_OPTIONS`, confirm the in-memory marker in its private log, and use only disposable local credentials. This test provider validates isolation, not the production Preferences persistence implementation.

The subsequent CI browser installation succeeded with Chrome for Testing 156.0.8078.47, but Karma timed out on all three capture attempts. Local controlled runs with explicit launch markers reproduced that capture failure for 156, while the same 15 cases passed with confirmed Chrome for Testing 155.0.8059.86. CI now pins that verified browser version instead of a moving stable channel. The same sandbox and assertions remain enabled. This records an observed launcher compatibility limitation; it does not claim that every Chrome 156 run fails or that the application's serialization assertions failed. The replacement remote CI result remains required.

The first pin, `155.0.8059.86`, was downloadable for the local ARM control but absent from the official known-good manifest used by setup-chrome on macOS Intel; CI rejected it before running tests. The corrected pin `155.0.8059.84` is listed for both macOS architectures and Linux. A fresh forced run through a launch-recording wrapper executed all 15 Wasm tests successfully (0 failures/errors/skips, 17.57 s). This controls browser provenance without skipping assertions or assuming every Chrome 156 launch fails.
