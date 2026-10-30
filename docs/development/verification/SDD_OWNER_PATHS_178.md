# Supported SDD owner paths — issue #178

The consumer catalog now uses the flat lowercase owner paths accepted by the
unchanged published workspace reader. All 16 capability IDs, 17 primary/companion
owners, contracts, historical verification values and verbatim discovery answers
remain preserved. Product backend/frontend execution paths are unchanged.

## Original failure and scoped reader verification

Baseline develop `a72344dddb8232ca496ecf0ad614d940ec05c858` follows #164. Native #82
attempts [41108456778](https://github.com/vypdev/vaultstadio/actions/runs/41108456778)
and [41109113102](https://github.com/vypdev/vaultstadio/actions/runs/41109113102)
published no SDD or branch. The second reached companion analysis, then the
workspace reader rejected the uppercase existing owner path before drafting.

The actual unchanged SDK 3.10.6 workspace adapter, from published revision
`f5e5a4cf9a80248994469cd88ad55eecab5b95ca`, independently rejects all 17 original
owner paths at that immutable baseline. It reads all 17 corrected owner documents
at candidate `b2199edacbfa4c93fa60ae68c7dba6beebc97bc9`. Five invalid-path controls
remain denied. The receipt records each actual owner blob and SDK module digest.
This invokes only snapshot/owner reads through Git show: no CLI entrypoint,
provider, model, remote branch operation or SDK modification.

These are one integration family with 17 owner observations, separately from the
focused Python controls. They establish reader acceptance for those source blobs;
model drafting and native publication remain #82's subsequent gates.

## Preservation and actual focused controls

The [reversible migration record](sdd-owner-path-migration-178.json) includes the
original catalog, source Git blob identities, exact reference edits and the
scoped verification receipt. Independent comparison with actual baseline Git
objects reconstructs all 17 owners and 13 other documents exactly; the managed
catalog view reproduces from both the original and mapped catalogs. Only the
explicit governance migration contract and discovery location note are appended.

The original 44 catalog controls remain and 25 additional path/preservation
controls pass: **69 tests, zero failures/errors/skips**. They cover strict path
boundaries, complete metadata/evidence, original owner reconstruction, accepted
discovery and verbatim answers, primary ordering, colliding maps, text/reference
tampering, exact-case live links, missing-owner partial migration and rollback in
owned temporary fixtures. Historical preservation also permits supported additive
companions, evidence and distinct future capabilities while rejecting original
evidence removal or replacement of the primary owner. Every addition still
requires normal catalog validation. Exact equality of this path migration is
independently checked against actual original Git data; long-term preservation
does not freeze the entire catalog against future authorized work.

The first run exposed manifest row ordering; that
record was corrected to follow the already preserved catalog order. The
independent readback also separated generated catalog output from literal
reference edits. Original failures remain retained with the local evidence.

Real coverage across the three existing production tooling modules, including CLI
subprocesses, is **226/228 executable lines and 120/124 branches**, with no excluded
lines or lower thresholds. This is scoped tooling coverage, separate from product
application coverage and test execution. Test helpers are not product modules.

## Compatibility, rollout and rollback

Canonical file locations change; the 30 accepted decisions and 90 planned
acceptance families retain their existing logical owners and readiness limits.
The source answers JSON, dated preservation receipt and exact reviewed-nonsecret
historical commit/path/line coordinates remain byte-identical. Paths in dated
source receipts continue to refer to their original immutable source; the map
explains the current canonical locations.

Publish the complete path/catalog/reference set together. Rollback restores that
set through a reviewed forward commit and can regenerate the original catalog
view. Original Git blobs and source receipts remain retained; no destructive reset
or user-data operation is needed.

All four unchanged configured contributor commands and native CI/security/review
must independently finish on the actual candidate. Their original source-bound
artifacts and PR checks provide that evidence; it is not inferred from the focused
controls. No product API/schema migration, runtime/provider configuration, SDK
internal code, protected setting, setup flag, release, tag or deployment changes.
Keep `VERIFICATION_SETUP=false`, draft #86 and unrelated local SDD drafts.

After explicitly authorized adoption, retry the same #82 with its confirmed
filename/prefix/case/accent/ñ decisions and full planned acceptance matrix. No
filename-search implementation or whole-portfolio readiness is claimed here.
#165 retains its separate original documentation/setup acceptance.


## Native review: exact-case owner links

Copilot review of candidate `b1339309c2933c3bcacd2284936f48db1fa8a0ae`
identified mixed-case owner links that bypassed the exact-path classification.
Three added regression controls fail against that original implementation and
pass after recognizing both canonical and historical owner spellings with
case-insensitive comparison, then requiring exact canonical inventory membership.
The controls cover all 16 mixed-case primaries, percent-encoded reference links
and mixed-case nested historical discovery links. Exact canonical live links and
remote immutable historical links continue through the existing full inventory
audit. The original 63 controls remain; that review’s focused total was 66.


## Native review: reserved specification owners

The next native review identified the lowercase `specs/catalog.md` alias of
the generated `specs/CATALOG.md` on case-insensitive filesystems. The consumer
owner boundary now reserves the generated catalog, normative README/template
and JSON catalog paths case-insensitively. Three added controls fail before the
fix, including an actual `--write` invocation in a valid disposable SDD fixture.
The corrected command rejects the reserved owner before writing, and its complete
file snapshot remains byte-identical. All 66 previous controls remain; the
current focused total is 69. This consumer constraint leaves the SDK unchanged.
