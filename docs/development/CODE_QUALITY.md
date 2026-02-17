# Code Quality: ktlint and detekt

VaultStadio uses **ktlint** for formatting and **detekt** for static analysis. They are complementary: run formatting first, then quality checks.

## Workflow

1. **Format** code with ktlint (fixes style automatically).
2. **Analyse** with detekt (complexity, naming, bugs, style semantics).

```bash
# 1. Format Kotlin code (fixes indentation, line length, trailing whitespace, etc.)
./gradlew ktlintFormat

# 2. Run static analysis (quality rules; detekt runs after ktlintCheck when you run detekt)
./gradlew detekt
```

When you run `./gradlew detekt` or `./gradlew check`, **ktlintCheck** runs first, then detekt. This keeps the order consistent: formatting is checked before quality.

## ktlint (formatting)

- **Role**: Enforce code style (indent, line length, trailing whitespace, final newline, etc.).
- **Config**: [.editorconfig](.editorconfig) at project root.
- **Tasks**:
  - `ktlintFormat` – fix formatting in place.
  - `ktlintCheck` – fail if formatting is wrong (used in CI).

**Main settings** (in `.editorconfig`):

- `indent_size = 4`, `max_line_length = 120` (aligned with detekt).
- No wildcard imports (explicit imports only).
- Code style: `intellij_idea` (Kotlin conventions).

## No fully qualified names (FQN) in type positions

Do **not** use fully qualified type names in signatures or bodies when an import would suffice.

- **Bad**: `fun foo(param: com.vaultstadio.core.domain.service.StorageBackend)` or `val x: kotlinx.datetime.Instant`
- **Good**: Add `import com.vaultstadio.core.domain.service.StorageBackend` (and similar) at the top, then use `StorageBackend`, `Instant`, etc.

**Check**: Run `./scripts/check-no-fqn.sh` to detect FQNs in type positions. CI runs this before detekt.

## detekt (quality)

- **Role**: Complexity, naming, potential bugs, style semantics (e.g. braces, redundant code).
- **Config**: [config/detekt/detekt.yml](config/detekt/detekt.yml).
- **Task**: `detekt` – report issues; no auto-fix (use `autoCorrect` in config if you enable it later).

**Formatting rules** in detekt (MaxLineLength, NoTabs, TrailingWhitespace, NewLineAtEndOfFile) are kept in sync with ktlint (120 chars, spaces, trim) as a fallback. ktlint remains the primary formatter.

## Quick reference

| Goal              | Command / step                          |
|-------------------|-----------------------------------------|
| Format before commit | `./gradlew ktlintFormat`             |
| Check formatting  | `./gradlew ktlintCheck`                 |
| Check no FQN in types | `./scripts/check-no-fqn.sh`          |
| Run quality checks | `./gradlew detekt` (runs ktlintCheck first) |
| Format + quality  | `./gradlew ktlintFormat detekt`          |

## CI

In CI, run in this order:

1. `./gradlew ktlintCheck` – fail if formatting is wrong.
2. `./gradlew detekt` – fail if quality rules are violated (and optionally `ignoreFailures = false` in detekt config once the codebase is clean).

## See also

- [.editorconfig](.editorconfig) – ktlint/editor formatting.
- [config/detekt/detekt.yml](config/detekt/detekt.yml) – detekt rules and comments.
- [CONTRIBUTING.md](../CONTRIBUTING.md) – coding standards and PR process.
