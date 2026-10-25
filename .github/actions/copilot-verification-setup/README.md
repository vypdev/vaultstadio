# VaultStadio verification setup

Copilot 3.10.0 generated `action.yml` as an intentionally failing starter.
`VERIFICATION_SETUP` remains `false`: this starter must not be enabled.

The configured verification commands (`make test`, `make test-coverage`,
`make lint`, and `git diff --check`) need Java 17, Gradle wrapper dependencies,
the Android SDK and a working Docker daemon for Testcontainers. The current
Copilot Linux runner does not provide Docker. Installing Java alone would not
make verification ready.

Before enabling, replace the starter with a reviewed composite action, declare
matching runtimes in `.copilot/verification-prerequisites.json`, and prove all
configured commands in a fresh private HOME on the actual verification runner.
Merge the reviewed action onto trusted `master` before setting
`VERIFICATION_SETUP=true`. Setup failure must keep publication blocked.

Future Copilot upgrades preserve this action and its assets. The local Copilot
CLI cannot execute composite actions while this option is enabled.
