# VaultStadio verification setup

Copilot generated the original intentionally failing starter. This directory now
contains the project-owned implementation reviewed through #79/#83 and the #85
rollout candidate. `VERIFICATION_SETUP` remains `false` until the reviewed action
and assets reach trusted `MAIN_BRANCH` and operational prerequisites are ready.

The configured verification commands (`make test`, `make test-coverage`,
`make lint`, and `git diff --check`) need Java 17, Gradle wrapper dependencies,
the Android SDK and a working Docker daemon for Testcontainers. Inventories on
two current Copilot Linux runners found no Docker CLI; the second also found no
standard Docker socket. Installing Java alone does not establish readiness.

The composite prepares Java 17, Android SDK 37 and the reviewed Chrome version.
Its Docker adapter resolves an existing current-account local Unix endpoint
without reading user client credentials. The proposed native callback route is
the existing Docker-capable macOS ARM64 group; Linux candidate verification
retains its protected operator-provisioned browser sandbox. No component starts
Docker, connects to a remote daemon or weakens sandbox/host permissions.

The prerequisite declaration requires Java 17. All configured commands must be
proven in a fresh private HOME on the actual selected runner. `chrome-path`
exports the exact selected executable for independent smoke/provenance checks;
macOS must not assume the application is named `chrome` in PATH.

Follow the [runtime rollout runbook](../../../docs/development/verification/RUNTIME_ROLLOUT_85.md)
for manual candidate verification, trusted-main review, conditional activation
and actual official verification. Candidate success does not certify the native
trusted-main protocol. Setup failure must keep publication blocked.

Future Copilot upgrades preserve this action and its assets. The local Copilot
CLI cannot execute composite actions while this option is enabled.
