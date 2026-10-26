# VaultStadio frontend testing guide

The frontend is a standalone Gradle build in `frontend/`. Its interaction contract is defined in [SDD-0013](../../specs/SDD-0013-frontend-kmp-and-platform-architecture.md), with password/privacy requirements in [SDD-0015](../../specs/SDD-0015-security-privacy-and-threat-model.md).

## Sources and responsibilities

| Location | Purpose |
|---|---|
| `frontend/composeApp/src/test/kotlin/` | Shared app, model, repository and use-case tests; inspect each case before classifying its integration scope |
| `frontend/composeApp/src/desktopTest/kotlin/com/vaultstadio/app/accessibility/` | Real production Compose component editing, focus, actions, state, localization and image-runtime contracts |
| `frontend/composeApp/src/wasmJsTest/kotlin/` | Real Compose viewport and production dialog dismissal/reopening regression |
| `frontend/core/form/src/wasmJsTest/kotlin/` | Native input/textarea restrictions, metadata, controlled state, selection, IME, layout and disposal contracts |
| `frontend/data/network/src/test/kotlin/` | Production client JSON factory and wire-format contracts |
| `frontend/core/resources/src/test/kotlin/` | Shared translation/resource contracts |
| `frontend/data/auth/src/test/kotlin/` | Authentication adapter/use-case contracts |
| `scripts/tests/test_frontend_accessibility_inventory.py` | Inventory drift and quality-report failure controls |

Module presence does not mean that it has tests. A `NO-SOURCE` task is not executed coverage. The existing suite also contains local replicas of behavior; their passing result does not establish that production works.

## Prerequisites and commands

Use the reviewed JetBrains Runtime SDK version in `frontend/.java-version` for desktop launch, packaging and interactive verification. Set `JAVA_HOME` to that SDK or use `-PdesktopJavaHome` for desktop runtime selection. JVM bytecode remains compatible with Java 17; Android/backend runtime policy is separate. Some desktop task selections also require the configured Android SDK.

Browser tests need an executable Chrome-compatible browser in `CHROME_BIN`. CI provisions its own pinned browser. From `frontend/`:

```sh
./gradlew desktopTest :core:ui:verifyInputSemanticsCoverage --continue --no-build-cache
./gradlew wasmJsBrowserTest --rerun --continue --no-build-cache
```

To exercise all projects with browser tests, use `./gradlew wasmJsBrowserTest --continue --no-build-cache` with the same prerequisite. Append `--rerun` to the relevant test task for fresh execution. `UP-TO-DATE` is not fresh evidence.

For a focused production-component suite:

```sh
./gradlew :composeApp:desktopTest --tests '*ActiveFieldInteractionTest*' --rerun
./gradlew :composeApp:desktopTest --tests '*ImageRuntimeCompatibilityTest*' --rerun
```

Filtered execution replaces that task's previous result files. Retain complete XML and coverage evidence first; rerun the complete task before publishing aggregate coverage. Each module writes JUnit XML to `build/test-results/<task>/` and HTML to `build/reports/tests/<task>/`. Check failures, errors, skips, collected count, source revision and actual task execution together.

From the repository root:

```sh
python3 scripts/check-frontend-accessibility.py
python3 -m unittest discover -s scripts/tests -p 'test_frontend_accessibility_inventory.py'
cd frontend
./gradlew :core:ui:detekt :core:form:detekt :core:resources:detekt :feature:auth:detekt :composeApp:ktlintDesktopTestSourceSetCheck --continue
cd ..
python3 scripts/check-frontend-accessibility.py --detekt-reports
python3 scripts/validate-specification-catalog.py
./scripts/check-no-fqn.sh
git diff --check
```

The actual-source Detekt tasks matter: typed `detektDesktopMain` tasks can report `NO-SOURCE` for custom shared source directories. The report checker rejects missing reports and findings in the new accessibility/authentication scope. Historical resource-interface debt remains explicit audit work.

The transitive WebSocket security patch also has a bounded local runtime smoke. After the frontend npm installation, run this from the repository root with Node.js:

```sh
node scripts/dogfood/websocket-runtime-smoke.mjs
```

It checks the installed version and exercises fragmented Unicode, rejection of an unsupported close-reason type, and a valid typed-array close over loopback. It does not replace application WebSocket authentication or collaboration tests.

## Meaningful interaction tests

Render the production component with observable state and controlled ports. Perform its supported action and assert the resulting value, callback, validation or navigation state. A copied widget, reimplemented calculation or constant assertion cannot establish production interaction.

The [maintained control inventory](../development/verification/accessibility-controls-80.json) records production feature fields, including three explicitly marked retained password-dialog fields without a current caller. Static checking verifies declarations and test cases, not runtime reachability. Every maintained field needs edit, clear and focus cases. Secure fields need masked output; disabled/read-only fields need negative cases. Controls need their own state/action evidence: checkboxes, switches, radios, menus, dropdowns, tabs, file selection/opening and dialog dismissal/reopening. Locale variants verify names and reversible visibility actions in all seven languages.

Keep suites and fixtures focused. Inject ports to control external effects while preserving production values, callbacks, edit semantics and validation. Domain policy stays free from Compose, DOM and transport dependencies.

## Real browser and desktop dogfooding

Component semantics do not prove that a native tool or browser can interact with the generated app. Build the distributable and optimized Wasm distribution, launch those exact artifacts and use a disposable local account through normal login. Record artifact hashes and the integration source revision.

Verify typing, Unicode, selection/replacement, deletion, focus transfer, login, file navigation and dialog cancellation/reopening. After login/logout and destination changes, verify that the accessible tree remains populated. A visible canvas alone does not establish operability.

For browser password fields, inspect the real focused editor. The three platform form components use actual HTML input/textarea elements through the public HtmlElementView API, with controlled state and release handling. Their native fields replace the unstable framework editor for all 29 inventoried declarations. The retired compatibility bridge remains in Git history; its 30 historical cases are excluded from current results. A keyboard hint is insufficient: the native type must be `password`, default accessible output must be protected, and editing must reach the production callback. Keep credentials/session material out of public evidence.

Isolate desktop preferences with the test-only [in-memory provider](../../scripts/dogfood/java/com/vaultstadio/audit/InMemoryPreferencesFactory.java) and confirm its marker before normal login. This does not establish production disk persistence. Do not seed a session or bypass authentication.

Record supported interaction methods and external tool limitations. Semantic focus plus paste can work while coordinate input is unavailable. Distinguish browser visibility/frame-clock effects from application failures and retain runtime/browser provenance.

## Coverage and CI

The `:core:ui:verifyInputSemanticsCoverage` gate requires execution data and compiled JVM helper classes, then enforces 100% lines and branches for that small scope. It does not measure Wasm DOM code, every feature or the whole frontend. Real-DOM and real-widget contracts verify the separate `:core:form` Wasm adapter; its compiled JVM renderer belongs in the full production coverage denominator.

For aggregates, report included modules, class directories, execution inputs, exclusions and uncovered lines/branches. Preserve existing gates while reviewing denominator changes. The [audit roadmap](../development/AUDIT_ROADMAP_2026-10-25.md) tracks broader coverage, legacy tests and unverified platforms.

Application [CI](../../.github/workflows/ci.yml) builds desktop/Android, runs production component/browser contracts, collects scoped coverage and checks inventory/quality. Copilot's separate verification environment has its own runtime prerequisites; application CI does not prepare that environment.

See [startup verification #76](../development/verification/FRONTEND_STARTUP_76.md) for its exercised paths and limits. Publish completed #80 evidence separately, distinguishing component tests, real UI actions, platform repeats and unresolved roadmap work.
