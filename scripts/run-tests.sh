#!/bin/bash
set -euo pipefail

repository_root="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
operation=test
if [ "$#" -eq 1 ] && [ "$1" = '--coverage' ]; then
    operation=coverage
elif [ "$#" -ne 0 ]; then
    echo 'Usage: scripts/run-tests.sh [--coverage]' >&2
    exit 2
fi
exec python3 "$repository_root/scripts/verification/run.py" "$operation"
