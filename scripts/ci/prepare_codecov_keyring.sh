#!/usr/bin/env bash
set -euo pipefail

: "${RUNNER_TEMP:?RUNNER_TEMP is required}"
: "${GITHUB_ENV:?GITHUB_ENV is required}"

# Never share the runner account's keybox database or modify its locks/agents.
keyring_dir="$(mktemp -d "$RUNNER_TEMP/vaultstadio-codecov-gnupg.XXXXXX")"
chmod 700 "$keyring_dir"
printf 'GNUPGHOME=%s\n' "$keyring_dir" >> "$GITHUB_ENV"
