# Project runtime rollout

Issue #85 owns operational acceptance of the setup from issue #79. SDD-0014 §6.7 defines the rollout contract. The maintainer authorized configuring the project dependencies and enabling `VERIFICATION_SETUP` when its prerequisites are ready. The effective value remains false while the trusted-main starter is still present.

## Existing implementation and trust

The reviewed project composite installs Java 17, Android 37/build tools 36 and Chrome 155.0.8059.84, prepares a private command environment and validates existing local Docker and Linux sandbox access. Its source is in `.github/actions/copilot-verification-setup/`. The optional project prerequisite declaration requires Java 17; Gradle manages the project's additional JavaScript toolchain. SDK and browser validation remain in the project composite.

On macOS, the same project-owned Headless Shell installer serves ordinary CI and trusted setup. It verifies the official archive's existing pinned SHA-256, extracts a fresh owned directory and checks the actual executable/version without using the shared browser cache. Setup places it under its private profile and caps the browser transfer at 120 seconds with a 150-second subprocess ceiling; ordinary CI keeps its separate transfer budget. Linux retains the existing browser Action and protected sandbox. Both expose the exact selected executable to provenance/smoke checks; a PATH alias is not required. The setup step retains the official five-minute total budget, and a slow prerequisite remains a failure.

The comment callbacks already use Copilot's native coordinator at `@v3`. The official CLI 3.10.5 preview preserves the existing project action and changed no files, Variables or Secrets. This candidate changes only the supported literal `runs-on` input of the two native comment callbacks to the existing `vypdev` group with `self-hosted/macOS/ARM64` labels. Their events, guards, permissions, named Secrets, coordinator reference and other inputs remain identical. Other Copilot workflows retain their existing routing. Keep the setup CLI runner mode `retain` when preserving these role-specific placements; do not replace the configured main branch to bypass its trust boundary.

Actual inventories on `ubuntu-runner-3` and `ubuntu-runner-1` found no Java, Android SDK, browser or Docker CLI; the second found no standard Docker socket either. The proposed route already executes the ordinary PostgreSQL/Testcontainers suites. Before the composite installs runtimes, its Docker adapter resolves only an explicitly supplied local Unix endpoint or current-account standard sockets. It probes with empty temporary client credentials, reads no user Docker configuration and exports the verified endpoint through an owned regular output channel. A remote endpoint, unavailable daemon or replaced output file fails setup.

Current master `615f11a` contains the intentionally failing project-action starter. Develop `7cbceca` contains the reviewed implementation. Before activation, the configured MAIN_BRANCH must contain the reviewed action and its assets, and the actual operator prerequisites must be available. A passing hosted run or local fixture cannot establish those facts for the self-hosted runner.

## Deliberate manual verification

PR updates keep the complete application suites. The following manual choices add an environment check; they do not replace the ordinary test producers with skipped jobs or an information-only result:

```sh
# After the helper reaches trusted main, collect read-only facts on the runner route.
gh workflow run ci.yml --repo vypdev/vaultstadio --ref <reviewed-ref> -f project_environment=inventory

# Execute the full hosted Linux setup and configured/independent project suites.
# This is deliberately expensive; request it for relevant runtime changes.
gh workflow run ci.yml --repo vypdev/vaultstadio --ref <reviewed-ref> -f project_environment=complete

# Execute the same candidate on the proposed native macOS ARM64 route.
gh workflow run ci.yml --repo vypdev/vaultstadio --ref <reviewed-ref> -f project_environment=complete -f project_environment_runner=macos-arm64
```

The existing CI entrypoint provides manual access to the project-owned reusable verification workflow, including while a newly introduced standalone workflow has not reached the default branch. Manual dispatch availability must be confirmed by GitHub; creating YAML alone does not establish that it runs.

Diagnostic-only inventory obtains its implementation from the trusted default branch, with credentials persistence disabled and the resolved source SHA recorded. It fails while that branch lacks the helper; it never falls back to the dispatch candidate. The earlier bootstrap inventories were explicit executions of the reviewed candidate. Full candidate verification continues to collect its own pre-setup inventory from the deliberately selected candidate whose setup/tests are being executed.

Inventory uses the existing `vypdev` group and `self-hosted/codex/Linux` route by default; `project_environment_runner=macos-arm64` selects the proposed route. It reports actual runner/OS/architecture, effective UID/container marker, current Java/browser identities, Android 37 presence, standard Docker socket metadata, CLI/daemon availability and protected sandbox metadata. A missing Docker CLI leaves daemon availability unknown; socket presence alone does not certify an accessible daemon. It does not install tools, start Docker, change host permissions or contact remote Docker endpoints. Child processes receive a small non-secret environment; raw command failures, provider output and environment dumps are not published. Docker daemon inspection uses empty private client credentials. The report explicitly states `inventoryOnly=true`, `projectSetupExecuted=false` and `readinessCertified=false`; missing prerequisites remain listed. A successful inventory job means facts were collected, not that setup is ready.

Full verification defaults to hosted Ubuntu and retains its signed operator sandbox procedure. The explicit macOS candidate uses Chrome's native sandbox. Both placements record an inventory before setup, create a private HOME, clear inherited Docker endpoint/context values, execute the candidate composite, require actual sandboxed browser startup, and run all four configured commands plus complete independent suites. This deliberately tests endpoint discovery after HOME isolation. It remains manual/weekly; reusable invocation adds no heavy PR/push trigger. An inventory cannot substitute for that campaign or the actual immutable trusted-main protocol.

## Activation acceptance

1. Review the exact trusted-main action/assets and the native callback wiring.
2. Establish the actual runner's Docker and protected sandbox access. Missing tools can be installed through the reviewed project setup; operator prerequisites must not be bypassed, and unrelated host/credential state must be preserved.
3. Confirm the maintainer's conditional activation authorization and all prerequisite evidence, then update only the intended repository Variable.
4. Run an admitted official operation that genuinely needs verification. A no-op sync is not proof of runtime readiness.
5. Retain source/runtime identities, every configured command result, native post-step/private-profile cleanup, ref publication and unchanged protection evidence. A failure remains incomplete and is corrected through reviewed project-owned changes.

Official sync `40399399779` failed with missing Java and published no ref. The separately authorized recovery of issue #74 advanced its existing branch to the reviewed `7cbceca` parent, retaining 103 inherited commits and both private SDD drafts. That exception is not an operational setup pass. The local entrypoint/reporting gaps remain separate work in issue #84.
