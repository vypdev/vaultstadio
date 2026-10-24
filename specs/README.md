# VaultStadio Product Specification Standard

The `specs/` directory contains implementation-driving Software Design Documents (SDDs).
An SDD is an integrated product and engineering contract: it describes observable behavior,
architecture, configuration, failure recovery, security, tests, documentation, and acceptance.
It is not a retrospective design story and it is not a replacement for API reference pages.

## Document states

- **Draft** — incomplete working document; not an implementation contract.
- **Proposed** — behavior or architecture is not yet implemented or still has blocking decisions.
- **As-built baseline** — current behavior has been audited; observed behavior, intentional contract,
  debt, unknown rationale, and proposed improvements are separated explicitly.
- **Ready for implementation** — decisions and acceptance criteria are resolved and implementation may start.
- **Implemented** — the contract and its evidence are current at the catalog verification commit.
- **Superseded** — replaced by a newer SDD; retain the link and reason.
- **Rejected** — considered and deliberately not adopted; record the decision.

An as-built baseline does not claim that the implementation is complete, correct, or production-ready.
It records what exists and what maintainers agree to preserve until a later change updates the contract.

## Required metadata

Every SDD MUST declare:

- status, date, catalog capability ID, and last verified commit/date;
- owners, scope, related issues/PRs, required review gates, and open decisions;
- exact evidence paths for code, workflows, tests, and user documentation.

## Required contract sections

Every SDD MUST include the following sections, or explicitly state why a section is not applicable:

1. Executive summary.
2. Problem, current behavior, and evidence.
3. Actors, surfaces, and terminology.
4. Goals, non-goals, and fixed invariants.
5. Current versus proposed product journey.
6. Functional behavior and state model.
7. User-facing configuration.
8. Clean Architecture design and executable constraints.
9. UI/UX and content contract where the capability has a user or operator surface.
10. Failure, recovery, retry, idempotency, and cleanup.
11. Security, permissions, privacy, and untrusted-input rules.
12. Observability and operational UX.
13. Compatibility, migration, rollout, and rollback.
14. Testing strategy with a numeric, risk-derived minimum budget.
15. Documentation and discoverability.
16. Observable acceptance scenarios.
17. Requirement traceability from contract to implementation, test, and docs.
18. Implementation or maintenance sequence.
19. Definition of Done.
20. References and decisions.

Use `MUST`, `SHOULD`, and `MAY` deliberately. Every `MUST` needs an acceptance scenario and
evidence target. Visual diagrams MUST have a textual equivalent. Do not hide a concern by deleting
the section; mark it not applicable and explain why.

## Catalog and verification

`catalog.json` is the machine-readable source of capability ownership. `CATALOG.md` is its
human-readable generated view. A capability may have one primary SDD and companion SDDs, but the
catalog MUST identify one owning contract and MUST list the code, tests, workflows, and documentation
that provide evidence.

Run from the repository root:

```bash
python3 scripts/validate-specification-catalog.py
```

The validator checks catalog IDs, SDD paths, required metadata/headings, duplicate ownership, and
referenced evidence paths. It is intentionally deterministic and does not require network access,
Gradle, Docker, AI providers, or credentials.

A behavior-changing implementation MUST update its owning SDD, catalog evidence, tests, and user
Documentation in the same change. A documentation-only correction MUST still update the SDD when it
changes the stated contract or evidence status.

## Scope rules

- SDDs are organized by product capability and cross-cutting contract, not by every Gradle module or HTTP route.
- This repository uses SDDs as the primary product/engineering specification lifecycle. It does not
  introduce a parallel PRD/ADR lifecycle in this first baseline.
- Existing architecture and feature documents remain useful evidence or user references, but they are
  not authoritative when they contradict a current SDD.
- RepoWise and Graphify are advisory evidence sources. Their signals do not replace caller audits,
  contract tests, or maintainer decisions.
- Never mark an SDD `Implemented` solely because a route, class, or mocked test exists. Distinguish
  modeled, unit-tested, integration-tested, and real external verification states.
