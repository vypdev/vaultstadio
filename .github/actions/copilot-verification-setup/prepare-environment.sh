#!/usr/bin/env bash
# VFY-ENV-01: private runtime output, literal arguments, no candidate or credential edits.
set -euo pipefail

verification_fail() {
    printf 'Project verification setup: %s\n' "$1" >&2
    exit 78
}

[[ -n "${HOME:-}" && -d "$HOME" && ! -L "$HOME" ]] || verification_fail 'a real private HOME directory is required'
case "$HOME" in
    /*) ;;
    *) verification_fail 'HOME must be absolute' ;;
esac
case "$HOME" in
    *$'\n'*|*$'\r'*) verification_fail 'HOME cannot contain line separators' ;;
esac
[[ -n "${GITHUB_PATH:-}" && -f "$GITHUB_PATH" && ! -L "$GITHUB_PATH" ]] || verification_fail 'the runtime PATH output channel is required'
[[ -n "${JAVA_HOME:-}" && "$JAVA_HOME" == /* && -x "$JAVA_HOME/bin/java" ]] || verification_fail 'Java 17 executable is required'
verification_java_version=$("$JAVA_HOME/bin/java" -version 2>&1) || verification_fail 'Java could not start'
case "$verification_java_version" in
    *'version "17.'*|*'version "17"'*) ;;
    *) verification_fail 'Java major version 17 is required' ;;
esac
[[ -n "${ANDROID_HOME:-}" && "$ANDROID_HOME" == /* && -s "$ANDROID_HOME/platforms/android-37.0/android.jar" ]] || verification_fail 'Android SDK platform 37.0 is required'
[[ -n "${CHROME_BIN:-}" && "$CHROME_BIN" == /* && -f "$CHROME_BIN" && -x "$CHROME_BIN" ]] || verification_fail 'an executable Chrome browser is required'
verification_chrome_version=$("$CHROME_BIN" --version 2>&1) || verification_fail 'Chrome could not start'
verification_chrome_identity='^(Google Chrome( for Testing)?|Chromium) 155\.0\.8059\.84[[:space:]]*$'
[[ "$verification_chrome_version" =~ $verification_chrome_identity ]] || verification_fail 'Chrome version 155.0.8059.84 is required'

verification_make=$(command -v make) || verification_fail 'make is required'
[[ "$verification_make" == /* && -x "$verification_make" ]] || verification_fail 'make must resolve to an executable path'
verification_docker=$(command -v docker) || verification_fail 'the runner must already provide Docker CLI and daemon access'
verification_docker_host=${DOCKER_HOST:-unix:///var/run/docker.sock}
case "$verification_docker_host" in
    unix:///*) ;;
    *) verification_fail 'Docker must use an existing local Unix socket' ;;
esac
[[ -S "${verification_docker_host#unix://}" ]] || verification_fail 'the local Docker socket is unavailable'
/usr/bin/env -u DOCKER_TLS_VERIFY -u DOCKER_CERT_PATH -u DOCKER_AUTH_CONFIG \
    DOCKER_CONTEXT='' DOCKER_CONFIG="$HOME/.docker" "$verification_docker" \
    --host "$verification_docker_host" info --format '{{.ServerVersion}}' >/dev/null 2>&1 \
    || verification_fail 'the local Docker daemon is unavailable to this runner'

verification_root="$HOME/.vaultstadio-verification"
[[ ! -L "$verification_root" ]] || verification_fail 'the private runtime output root must not be a symlink'
mkdir -p "$verification_root"
verification_tools=$(mktemp -d "$verification_root/commands.XXXXXX")
verification_wrapper="$verification_tools/make"
{
    printf '#!/usr/bin/env bash\nset -euo pipefail\n'
    printf 'export JAVA_HOME=%q\n' "$JAVA_HOME"
    printf 'export ANDROID_HOME=%q\n' "$ANDROID_HOME"
    printf 'export ANDROID_SDK_ROOT=%q\n' "$ANDROID_HOME"
    printf 'export CHROME_BIN=%q\n' "$CHROME_BIN"
    printf 'export CHROME_DEVEL_SANDBOX=%q\n' "${CHROME_DEVEL_SANDBOX:-}"
    printf 'export DOCKER_HOST=%q\n' "$verification_docker_host"
    printf 'export DOCKER_CONFIG=%q\n' "$HOME/.docker"
    printf 'export DOCKER_CONTEXT=\n'
    printf 'unset DOCKER_TLS_VERIFY DOCKER_CERT_PATH DOCKER_AUTH_CONFIG\n'
    printf 'export TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock\n'
    printf 'export GRADLE_USER_HOME=%q\n' "$HOME/.gradle"
    printf 'exec %q "$@"\n' "$verification_make"
} > "$verification_wrapper"
chmod 700 "$verification_wrapper"
printf '%s\n' "$verification_tools" >> "$GITHUB_PATH"
printf '%s\n' 'Verified Java 17, Android platform 37.0 and Chrome 155.0.8059.84.'
printf '%s\n' 'Project verification runtime prepared; official native verification is still required.'
