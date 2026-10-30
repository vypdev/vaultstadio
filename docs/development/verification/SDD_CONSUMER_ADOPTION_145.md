# Supported SDD consumer adoption — #145

This change adopts Copilot's supported version-1 catalog and populated SDD
standard/template so #82 can re-enter its existing specification gate after
reviewed adoption. It changes consumer documentation and validation. It does not
implement filename search or complete the broader local-setup work in #145.
That remaining setup, user-journey and roadmap acceptance is retained in
[#165](https://github.com/vypdev/vaultstadio/issues/165), admitted by actual
hosted and Linux worker run 41095957980 and linked under roadmap #127.

## Observed compatibility failure

Baseline develop is `46225cd47b10d248277f41310575d32e78203ca8`. The previous
consumer validator reports `specification_catalog=PASS capabilities=16`.
Actual [#82 run 41081742618](https://github.com/vypdev/vaultstadio/actions/runs/41081742618)
reports `configuration.unsupported` / `invalid-metadata`; its
[canonical card](https://github.com/vypdev/vaultstadio/issues/82#issuecomment-6526711333)
confirms no SDD or branch was published. Native validity and SDK publication
compatibility are different facts. No SDK code or gate is changed here.

## Meaning of the migrated metadata

| Field/surface | Meaning |
|---|---|
| Primary capability IDs/owners/SDDs | The same 16 owners and contracts; original SDD content remains an unchanged prefix. |
| Inventory status | Four `proposed`, twelve `as-built-baseline`; none becomes `implemented`. |
| Document readiness | Existing primary-SDD status and unresolved decisions; not inferred from catalog validity. |
| Capability `lastVerified` | Actual structural inventory review date, 2026-10-30. No runtime test or whole-capability acceptance claim. |
| `structuralVerification` | Reviewed baseline and explicitly limited file/role/ownership inspection. |
| `legacyVerification` | Exact previous status/date, including three original null values; not refreshed runtime evidence. |
| Original root fields and flat `evidence` | Historical checkpoint and inventory retained with their original meaning. |
| Typed role arrays | Real regular contained files selected for the declared role. Tests may be scoped or mocked; registration does not mean execution. |
| Discovery companion | The unchanged accepted-answer index is registered under governance; all 30 PD requirement owners remain unchanged. |

As-built appendices visualize the existing section-5 journey, show concrete
section-9 contract content, and tabulate the original section-14 base floor.
They do not certify new UI, replace later matrices (including version-history
163 and collaboration 391 floors), lower budgets or resolve known failures.
The original dirty SDD drafts in the separate issue74 worktree are untouched.

## Contributor workflow

From the repository root, with Python 3.9 or newer:

```bash
python3 scripts/validate-specification-catalog.py --write
python3 scripts/validate-specification-catalog.py
python3 -m unittest discover -s scripts/tests -p 'test_specification_catalog.py' -v
git diff --check
```

Default validation is offline and read-only. Explicit `--write` updates only the
generated `specs/CATALOG.md` after validating the catalog and prerequisites.
The generated project view exposes structural versus historical verification.
If an Action SDD commit emits Copilot's generic catalog view, regenerate this
project view with the first command before contributor verification. The JSON
publication profile, primary ownership, SDK validation and branch gate remain
the supported profile throughout; regeneration adds no runtime evidence.

## Verification and limits

The pre-code contract and risk-derived 22-control floor are in
[SDD-0000's migration appendix](../../../specs/sdd-0000-architecture-quality-and-product-contract.md#consumer-sdd-publication-migration--issue-145-2026-10-30).
Migration preservation and actual validation results will be recorded with their
candidate source and original reports before requesting merge. A planned control
or registered test file is not an executed test result.

Executed local migration controls: **44 tests, zero failures/errors/skips**.
The original immutable-prefix/owner/decision/date checks pass. Consumer validation
passes all 16 capabilities. The installed compiled SDK recognizes `sdk-v1`,
reports no catalog errors, passes standard/template prerequisites, and rejects
the original catalog with the original `invalid-metadata` reason.

| Changed production Python module | Covered lines | Covered branches |
|---|---:|---:|
| Catalog validation policy | 165 / 167 | 101 / 104 |
| Catalog rendering | 25 / 25 | 8 / 8 |
| Actual CLI, including subprocess controls | 35 / 35 | 11 / 12 |
| **Complete three-module denominator** | **225 / 227** | **120 / 124** |

Coverage.py 7.17.0 measured these three production modules without exclusions or
threshold changes. The unexercised OS-error fallback and four branch alternatives
remain visible in the original report. This is tooling coverage, not backend,
frontend, platform or application coverage. The
[source-bound validation receipt](sdd-consumer-adoption-145-validation.json)
records file hashes, exact denominators and compiled SDK module/bundle identities.
The existing CI `quality` contract step explicitly runs the catalog tests. Its
job graph, triggers, permissions, cadence and major Action references are unchanged;
generated Copilot workflows are untouched. The missing-history control distinguishes
a new capability without a prior checkpoint from an explicit original null.
The review correction rejects empty required primary sections, including bare
subheadings, accepts an explicit not-applicable explanation and retains populated
historical section 21. Metadata and twenty empty headings cannot establish a
valid contract.

Full configured application verification and native PR CI are separate gates;
these local controls do not imply either has completed.

The independent compatibility check reads only the installed CLI bundle's four
unmodified policy modules (catalog compatibility, filesystem validation, path
policy and prerequisite validation). It never launches the CLI entrypoint,
doctor dispatch, model, GitHub mutation or application. Candidate acceptance
requires that compiled SDK policy and the consumer command both pass.

Historical runtime receipts, partial coverage, failed dogfood results and open
decisions remain authoritative. Neither structural validation nor existing
6800-test baseline evidence proves #82 works. Re-enter #82 after the reviewed
consumer change is adopted; its Action must publish its SDD and exact managed
branch before product implementation.

## Recovery

### Native scanner checksum review

The first native security run (41093209961) correctly blocked one unreviewed
`ProtocolsIO` match. It is the original SHA-256 integrity checksum for SDD-0005
in the preservation manifest, introduced at commit
`e81da9c26ba72274eee16b62b56b8af3ab7201b5`, line 469. An independent `git show`
of the historical source and SHA-256 recomputation proves the value is source
integrity data. It is never an authentication input.

The existing reviewed-nonsecret mechanism records only that immutable
tool/version/commit/path/line coordinate. A control recomputes the source checksum
and rejects copied coordinates with a different commit, path or line. Both real
scanners, their complete-history scope and all unreviewed-finding failures remain
required; the original failed run is retained. No provider verification occurs.

### Consumer rollback

Revert the focused consumer change through the normal reviewed workflow to
restore the previous native standard and explicit compatibility block. No
application schema/data changes, credential replacement, setup activation,
protection edits, release or deployment are part of this migration. PR #86 stays
an unmerged draft. No administrative exception for this PR has been authorized.
