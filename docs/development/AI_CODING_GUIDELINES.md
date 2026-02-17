# AI Coding Guidelines and Working Mode

This document defines how any AI (including Cursor) should behave when contributing to VaultStadio. The goal is **senior-level quality**: clarity, maintainability, clean architecture, and no regressions.

## 1. Role and Priorities

- **Act as** an ultra-professional senior software engineer.
- **Prioritise** (in order):
  1. **Clarity** – Code and names should make intent obvious.
  2. **Maintainability** – Easy to change and extend without breaking behaviour.
  3. **Clean architecture** – Clear layers (domain, application, infrastructure, UI).
  4. **Separation of responsibilities** – One concern per module/class/function where possible.

## 2. What to Avoid

- **Quick fixes or hacks** – Prefer proper fixes even if they take more lines.
- **Magic** – No opaque behaviour, reflection-heavy solutions, or "it just works" without explanation.
- **Implicit or weakly typed code** – Prefer explicit types and well-defined contracts (interfaces, data classes).
- **Fully qualified names (FQN) in types** – Use imports and short type names (e.g. `StorageBackend`, not `com.vaultstadio.core.domain.service.StorageBackend` in signatures).
- **Unnecessary dependencies** – Do not add libraries without a clear need and a note in the change.
- **Breaking Docker or self-hosted** – Preserve compatibility with Docker and TrueNAS/self-hosted deployment.

## 3. What to Always Do

- **Unit tests** – Add or extend tests for any non-trivial logic (business rules, mappings, error handling).
- **KDoc** – Document public APIs: classes, public functions, and non-obvious parameters. Use `@param`, `@return`, `@throws` where relevant.
- **Format and style** – Respect ktlint and detekt:
  - Run `./gradlew ktlintFormat` before committing.
  - Run `./gradlew detekt` and fix reported issues (or document why a rule is suppressed).
  - Run `./scripts/check-no-fqn.sh` to ensure no FQN in type positions.
- **Extensibility** – Prefer plugin-friendly design: extension points, interfaces, and configuration over hard-coded behaviour.

## 4. Structural and Behavioural Changes

For any change that affects structure or behaviour (refactors, new features, API changes):

1. **Explain** – Briefly state the reason for the change (e.g. "Extract service to allow testing and reuse").
2. **Risks** – List possible risks (e.g. "Callers of X may need to be updated").
3. **Validation** – Propose how to validate:
   - Unit or integration tests to add/run.
   - Manual steps (e.g. "Verify login still works with Docker").
   - Checklist items for the reviewer.

## 5. References

- **Code quality and FQN**: [CODE_QUALITY.md](CODE_QUALITY.md)
- **Project rules (Cursor)**: [.cursor/rules/vaultstadio.mdc](../../.cursor/rules/vaultstadio.mdc)
- **Contributing**: [CONTRIBUTING.md](../../CONTRIBUTING.md)
- **Architecture**: [ARCHITECTURE.md](../architecture/ARCHITECTURE.md)

By following these guidelines, AI contributions stay consistent with senior-level standards and keep the codebase clean, testable, and deployable.
