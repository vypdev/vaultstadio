# Project runtime rollout

Issue #85 owns operational acceptance of the setup from issue #79. SDD-0014 §6.7 defines the rollout contract. The maintainer authorized configuring the project dependencies and enabling `VERIFICATION_SETUP` when its prerequisites are ready. The effective value remains false while the trusted-main starter is still present.

## Existing implementation and trust

The reviewed project composite installs Java 17, Android 37/build tools 36 and Chrome 155.0.8059.84, prepares a private command environment and validates existing local Docker and Linux sandbox access. Its source is in `.github/actions/copilot-verification-setup/`. The optional project prerequisite declaration requires Java 17; Gradle manages the project's additional JavaScript toolchain. SDK and browser validation remain in the project composite.

The comment callbacks already use Copilot's native coordinator at `@v3`. The official CLI 3.10.5 preview preserves the existing project action and changed no files, Variables or Secrets. Do not hand-edit the generated callbacks or replace the configured main branch to bypass its trust boundary.

Current master `615f11a` contains the intentionally failing project-action starter. Develop `7cbceca` contains the reviewed implementation. Before activation, the configured MAIN_BRANCH must contain the reviewed action and its assets, and the actual operator prerequisites must be available. A passing hosted run or local fixture cannot establish those facts for the self-hosted runner.

## Deliberate manual verification

PR updates keep the complete application suites. The following manual choices add an environment check; they do not replace the ordinary test producers with skipped jobs or an information-only result:

```sh
# Collect read-only facts on the same runner route as official Copilot verification.
gh workflow run ci.yml --repo vypdev/vaultstadio --ref <reviewed-ref> -f project_environment=inventory

# Execute the full hosted Linux setup and configured/independent project suites.
# This is deliberately expensive; request it for relevant runtime changes.
gh workflow run ci.yml --repo vypdev/vaultstadio --ref <reviewed-ref> -f project_environment=complete
```

The existing CI entrypoint provides manual access to the project-owned reusable verification workflow, including while a newly introduced standalone workflow has not reached the default branch. Manual dispatch availability must be confirmed by GitHub; creating YAML alone does not establish that it runs.

Inventory uses the official `vypdev` group and `self-hosted/codex/Linux` route. It reports actual runner/OS/architecture, current Java/browser identities, Android 37 presence, existing local Docker availability and protected sandbox metadata. It does not install tools, start Docker, change host permissions or contact remote Docker endpoints. Child processes receive a small non-secret environment; raw command failures, provider output and environment dumps are not published. Docker daemon inspection uses empty private client credentials. The report explicitly states `inventoryOnly=true`, `projectSetupExecuted=false` and `readinessCertified=false`; missing prerequisites remain listed. A successful inventory job means facts were collected, not that setup is ready.

Full verification retains the hosted operator sandbox procedure, candidate composite, actual browser smoke, all four configured commands and complete independent suites. It remains manual/weekly; reusable invocation adds no heavy PR/push trigger. An inventory cannot substitute for that campaign or the actual immutable trusted-main protocol.

## Activation acceptance

1. Review the exact trusted-main action/assets and the native callback wiring.
2. Establish the actual runner's Docker and protected sandbox access. Missing tools can be installed through the reviewed project setup; operator prerequisites must not be bypassed, and unrelated host/credential state must be preserved.
3. Confirm the maintainer's conditional activation authorization and all prerequisite evidence, then update only the intended repository Variable.
4. Run an admitted official operation that genuinely needs verification. A no-op sync is not proof of runtime readiness.
5. Retain source/runtime identities, every configured command result, native post-step/private-profile cleanup, ref publication and unchanged protection evidence. A failure remains incomplete and is corrected through reviewed project-owned changes.

Official sync `40399399779` failed with missing Java and published no ref. The separately authorized recovery of issue #74 advanced its existing branch to the reviewed `7cbceca` parent, retaining 103 inherited commits and both private SDD drafts. That exception is not an operational setup pass. The local entrypoint/reporting gaps remain separate work in issue #84.
