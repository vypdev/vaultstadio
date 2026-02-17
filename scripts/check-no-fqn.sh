#!/usr/bin/env bash
# Check that Kotlin source files do not use fully qualified names (FQN) in type positions
# when a simple import would suffice. FQNs in types hurt readability and violate project style.
#
# Pattern: type positions like "param: com.foo.Bar" or "List<io.ktor.X>" instead of using imports.
# We exclude: package declarations, import statements, and string/comment content.
set -e

ROOT_DIR="${1:-$(cd "$(dirname "$0")/.." && pwd)}"
cd "$ROOT_DIR"

FAILED=0

# Find FQN in type-like positions: ": package.path.Type" or "<package.path.Type>"
# Exclude import/package lines and common false positives (e.g. in strings).
while IFS= read -r line; do
  file="${line%%:*}"
  rest="${line#*:}"
  line_no="${rest%%:*}"
  content="${rest#*:}"
  # Skip import and package lines
  if echo "$content" | grep -qE '^\s*(import|package)\s'; then
    continue
  fi
  # Skip if it's clearly inside a string or comment
  if echo "$content" | grep -qE '^\s*[*/]|"\s*$|'\''\s*$'; then
    continue
  fi
  echo "FQN in type position (use import instead): $file:$line_no"
  echo "  $content"
  FAILED=1
done < <(
  grep -rn -E ':\s*(com|io|kotlinx|java|org|androidx)\.[a-zA-Z0-9.]+\b' \
    --include='*.kt' \
    compose-frontend kotlin-backend \
    2>/dev/null | grep -v '/build/\|/bin/\|/out/' || true
)

# Also check generics: <com. or <io. etc.
while IFS= read -r line; do
  file="${line%%:*}"
  rest="${line#*:}"
  line_no="${rest%%:*}"
  content="${rest#*:}"
  if echo "$content" | grep -qE '^\s*(import|package)\s'; then
    continue
  fi
  echo "FQN in type position (use import instead): $file:$line_no"
  echo "  $content"
  FAILED=1
done < <(
  grep -rn -E '<(com|io|kotlinx|java|org|androidx)\.[a-zA-Z0-9.]+\b' \
    --include='*.kt' \
    compose-frontend kotlin-backend \
    2>/dev/null | grep -v '/build/\|/bin/\|/out/' || true
)

if [ "$FAILED" -eq 1 ]; then
  echo ""
  echo "Use short type names with an import at the top of the file instead of FQNs."
  echo "See docs/development/CODE_QUALITY.md and .cursor/rules for style."
  exit 1
fi
exit 0
