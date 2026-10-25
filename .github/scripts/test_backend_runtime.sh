#!/usr/bin/env bash
# Verify the loaded PR image on its declared JRE, including application startup.
set -euo pipefail
image="${1:?Expected the loaded candidate image}"
[[ "$image" =~ ^[a-zA-Z0-9][a-zA-Z0-9_.:/@-]*$ ]] || exit 2
root="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.." && pwd)"
expected="$(sed -nE 's/^FROM eclipse-temurin:([0-9]+)-jre([@[:space:]].*)?$/\1/p' "$root/docker/Dockerfile.backend")"
[[ "$expected" =~ ^[0-9]+$ ]] || { echo 'Runtime JRE declaration is ambiguous.' >&2; exit 1; }
state="$(mktemp -d "${RUNNER_TEMP:-${TMPDIR:-/tmp}}/vault-runtime.XXXXXX")"
chmod 700 "$state"
network_id=''
database_id=''
application_id=''
cleanup() {
  [[ -z "$application_id" ]] || docker rm -f "$application_id" >/dev/null 2>&1 || true
  [[ -z "$database_id" ]] || docker rm -f "$database_id" >/dev/null 2>&1 || true
  [[ -z "$network_id" ]] || docker network rm "$network_id" >/dev/null 2>&1 || true
  rm -rf "$state"
}
trap cleanup EXIT
trap 'exit 130' INT
trap 'exit 143' TERM

docker run --rm --pull=never --network none "$image" \
  java -XshowSettings:properties -version > "$state/java-version" 2>&1
actual="$(awk '$1 == "java.specification.version" && $2 == "=" { print $3 }' "$state/java-version")"
[[ "$actual" == "$expected" ]] || { echo 'Loaded candidate JRE differs from its Dockerfile.' >&2; exit 1; }
echo "Candidate JRE $actual verified."

scope="vault-smoke-${state##*/}"
network_id="$(docker network create --label vypdev.verification=runtime "$scope")"
database_id="$(docker run -d --network "$network_id" --network-alias database \
  --memory 512m --cpus 1 -e POSTGRES_DB=runtime_test \
  -e POSTGRES_USER=runtime_test -e POSTGRES_PASSWORD=runtime_test postgres:16-alpine)"
ready=false
for _ in $(seq 1 30); do
  if docker exec "$database_id" pg_isready -U runtime_test -d runtime_test >/dev/null 2>&1; then ready=true; break; fi
  sleep 2
done
[[ "$ready" == true ]] || { echo 'Private smoke database did not become ready.' >&2; docker logs --tail 100 "$database_id" >&2 || true; exit 1; }

application_id="$(docker run -d --pull=never --network "$network_id" \
  --memory 768m --cpus 2 \
  -e DATABASE_URL=jdbc:postgresql://database:5432/runtime_test \
  -e DATABASE_USER=runtime_test -e DATABASE_PASSWORD=runtime_test \
  -e 'JAVA_OPTS=-Xms64m -Xmx384m' \
  -e JWT_SECRET=runtime-smoke-only-not-a-production-secret "$image")"
deadline=$(( $(date +%s) + 300 ))
while (( $(date +%s) < deadline )); do
  if docker exec "$application_id" curl --fail --silent --show-error --connect-timeout 1 --max-time 2 http://127.0.0.1:8080/health > "$state/health" 2>/dev/null; then
    echo 'Candidate application started and passed its health endpoint on the declared JRE.'
    exit 0
  fi
  if [[ "$(docker inspect --format '{{.State.Running}}' "$application_id" 2>/dev/null || true)" != true ]]; then break; fi
  sleep 2
done
echo 'Candidate runtime application failed its startup/health smoke.' >&2
docker logs --tail 100 "$application_id" >&2 || true
exit 1
