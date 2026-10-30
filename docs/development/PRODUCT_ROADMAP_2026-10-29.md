# Product roadmap from the 30 accepted decisions

This is the current planning overlay for [issue #124](https://github.com/vypdev/vaultstadio/issues/124),
the [discovery index](../../specs/product-discovery-2026-10-29.md) and the existing capability SDDs.
It extends the [original audit roadmap](AUDIT_ROADMAP_2026-10-25.md) without replacing historical
evidence. A milestone is a dependency group of bounded issues/PRs, not one large implementation PR.
Numbers indicate planning order; independent work need not wait for unrelated optional features.

## Starting evidence and practical limits

- Planning baseline: develop `3eb3c063de9b8e951f747d5851eda8f0b986bd35`, after PR #123.
  [Post-merge CI 40784074395](https://github.com/vypdev/vaultstadio/actions/runs/40784074395)
  passes 6,606 executions: 3,388 backend, 1,664 desktop and 1,554 Wasm, with no failures,
  errors or skipped cases. The [#121 closing evidence](https://github.com/vypdev/vaultstadio/issues/121#issuecomment-6483108588)
  records final artifact verification and adoption. Older candidate receipts remain historical.
- [#121 measured coverage](verification/folder-selection-121.json) is 9,433/17,047 backend lines
  and 2,142/6,215 branches; desktop is 7,846/22,186 lines and 1,624/6,183 branches across the
  complete 44/52 module populations. This is partial coverage. Wasm executions are separate
  from JVM line coverage, and strong focused policy coverage does not imply whole-product coverage.
- [Transfer](verification/AUTHENTICATED_TRANSFERS_78.md) and
  [directory-selection](verification/FOLDER_SELECTION_121.md) evidence establishes bounded Web/macOS
  journeys, real bytes and retained failures/limitations. Physical positive Swing directory selection,
  other desktop systems and mobile acceptance remain outstanding. Current selection limits include
  32 MiB, 256 combined entries and 16 segments; a future 100 GB target does not remove them by prose.
- Public single-file read-only sharing and owner-scoped security repairs do not implement shared
  spaces, writable links or collaborative editing. Search still has explicit unavailable/partial
  paths. E2EE, broad multi-server topology, HA and automatic full/selective backups are targets.
- The original detached SDD drafts, local database/content and executable rollback remain protected.
  This roadmap neither promotes master nor changes Copilot, protections, workflow cadence or setup.

## Immediate bounded queue

These issues were checked open during this documentation change. Their existing official scopes
remain authoritative; broader product targets must not be smuggled into their repairs.

| Order | Existing issue | Concrete acceptance before advancing |
|---|---|---|
| 1 | [#116 Web API origin](https://github.com/vypdev/vaultstadio/issues/116) | Operator-configured fixed primary, production same-origin/override behavior, consistent HTTP/download/share/WebSocket URL boundaries, invalid-config safety, full regression suites and actual packaged Web journeys on two origins. Do not claim federation or repair the independent #112 collaboration protocol by URL normalization. |
| 2 | [#82 filename/suggestion search](https://github.com/vypdev/vaultstadio/issues/82) and [#115 advanced/content search](https://github.com/vypdev/vaultstadio/issues/115) | Literal/Unicode matching, authorized dataset/counts, filters and real engine/unavailable outcomes. Actual UI queries must match retained fixtures; filenames cannot masquerade as content search. Separate each issue's bounded implementation/review. |
| 3 | [#109 version lifecycle](https://github.com/vypdev/vaultstadio/issues/109) | Normal mutation creates immutable history; binary historical download and restore preserve prior recoverable bytes, quota and references under failure/concurrency. Metadata-only download is not acceptance. |
| 4 | [#112 collaboration](https://github.com/vypdev/vaultstadio/issues/112) | Real authenticated client/server protocol, explicit content authority, convergence/reconnect and canonical persisted effects. Existing owner checks stay effective; socket connectivity alone is insufficient. |
| 5 | [#111 trusted setup](https://github.com/vypdev/vaultstadio/issues/111) | Review setup onto the trusted branch through its own authorized lifecycle, then verify native setup and official synchronization. Keep `VERIFICATION_SETUP=false` until those prerequisites and activation are explicitly handled; it is not a reason to publish manually managed branches. |

Coverage of risky services/adapters and responsibility splits continues alongside each repair,
using the complete production denominator. Keep tests and Copilot on each PR update, with the
already reviewed lighter cadence for heavy scans/build/runtime jobs. Do not dispatch redundant
full campaigns solely because documentation changed or to obtain the same coverage twice.

## Implementation milestones

<a id="m0"></a>

### M0 — Reliable fixed-installation journeys and preserved baseline

Owns PD-001. Complete the immediate queue above in bounded changes, starting with #116.
The fixed-primary login/file journey is the first slice; unified/grouped multi-server views
depend on M4 and must remain explicitly pending until then. Keep an inventory of actual package,
source, platform, database/byte state and known failures. Exit per slice: a reproduced defect,
verified corrected real journey and regression tests, final-ref CI/review and authorized adoption.

<a id="m1"></a>

### M1 — Identity lifecycle and explicit authority

Owns PD-008/025/026/027. Depends on preserved M0 authentication/resource behavior, not on
completion of every M0 follow-up. Split admission/bootstrap and email verification, local/SSO
identity linking, MFA enrollment/recovery and permission audit into reviewed increments.
Resolve provider protocols, role mapping, first/last-administrator recovery and migration of
existing accounts before enforcement. Exit: real local/controlled-provider journeys, verification/
factor replay negatives, current-session revocation and zero implicit administrator content access.

<a id="m2"></a>

### M2 — Spaces, encryption boundaries and durable lifecycle policy

Owns PD-004/005/006/007/012/013/014/015. Depends on M1 authority contracts; version retention
also depends on #109's actual byte lifecycle. Order sub-deliveries as explicit space/membership
model, policy/charging types and inheritance, consistent retention/quota effects, encryption modes,
then owner/trusted recovery. Existing personal owner checks remain active during migration.
Crypto design and review precede E2EE implementation; no home-made protocol is implied.

Exit each slice with controlled-clock retention boundaries, concurrent quota reservations,
failure/restore byte invariants, per-space grant tests and migration/rollback. E2EE exit additionally
requires actual client/ciphertext/key evidence and reviewed recovery abuse cases; server mocks do
not establish server inability to decrypt. Expose mode capabilities and unresolved migrations.

<a id="m3"></a>

### M3 — Bidirectional sync, offline state and conflict resolution

Owns PD-009/010/011. Depends on M2 authority, history and storage lifecycle; protect server-processable
sync first, then prove mode/platform-specific E2EE/offline behavior. Define durable change identity,
replay/cursors and policy revisions before placeholders/cache adapters and three conflict strategies.
Exit: two real clients, interrupted/replayed requests, no lost local edits, pinned subtree availability,
deactivated-device rejection, compatible merge and preserve-both fallback, retained latest-received
loser history and administrator override behavior. Expand background/mobile differences under M8.

<a id="m4"></a>

### M4 — Additional servers, placement and protection

Owns PD-002/003/020/021/022/030 and completes PD-001's multi-server views. Depends on M2 resource,
permission and accounting contracts; use M3 change/conflict contracts wherever data is synchronized.
Separate managed enrollment from independent peer trust, then capability assignments, automatic/
permitted user placement, resource/failure-domain discovery, redundancy/repair and degraded behavior.
An independent peer's ungranted storage is never administrative capacity of the primary.

Exit with two actual installations and declared disk/server failure domains: grant/revoke tests,
verified placement/replica hashes, insufficient-resource refusal, default under-protected write
blocking, explicit reduced-durability outcomes and repair/restart evidence. Single-disk/same-host
fixtures cannot certify whole-server protection; production fault campaigns need suitable resources.

<a id="m5"></a>

### M5 — Scoped public writing and collaboration permissions

Owns PD-017/018. Depends on M1 identity and M2 spaces/quota/encryption boundaries; collaborative
links additionally depend on #112 and relevant M3 conflict behavior. Keep existing read-only public
links compatible. Specify permission lattice and verified guest invitations before upload-delivery,
editing and folder scopes. Exit: real read/write/upload-only journeys, per-link/admin-policy matrix,
identity-required default, no surrounding/delivered-content leakage, revocation during active work
and mode-specific E2EE behavior. Do not infer history or collaboration grants from a download link.

<a id="m6"></a>

### M6 — Private, authorized content/OCR/semantic search

Owns PD-024/029. Depends on #82/#115's honest search outcomes and M2 mode/permission contracts;
does not require optional federation or HA. Add local engines, extraction/index lifecycle and
consent policies in distinct increments, then external providers only behind both required grants.
Resolve languages/formats, index freshness and quality/latency corpora. Exit with controlled known
matches/nonmatches, authorization and revocation checks, actual engine evidence, zero external
content sends without both grants, and cancellation/reindex recovery. No server E2EE plaintext index.

<a id="m7"></a>

### M7 — Recoverable operations and optional primary HA

Owns PD-023/028. Automatic consistent single-installation backups and full/selective restore start
after M2; they need not wait for optional multi-server HA. Extend backup scope only after M4 grants/
topology exist. Primary HA is a separate later slice with suitable infrastructure and a defined
metadata replication/fencing/hand-over design. Choose schedules, destination/key custody, restore
authority and measurable RPO/RTO before claiming recovery objectives.

Exit: restore verified snapshots into isolated targets, corruption and interrupted-restore controls,
ACL/E2EE-preserving selective recovery, retained previous backup and independent-installation scope.
HA exit also needs real primary loss/partition, one logical endpoint, prevented split-brain effects
and documented recovery. A redundant file or a successful backup job is not either restore proof or HA.

<a id="m8"></a>

### M8 — Platform acceptance and verified scale

Owns PD-016/019 and applies incrementally to each implemented capability; it is not a final cleanup
after all features. Add per-capability Web/Windows/macOS/Linux/Android/iOS packages and real journey
evidence as they become supported, disclosing unavoidable limits. Fix existing native/mobile gaps
without reclassifying shared or simulated tests as hardware acceptance.

Before 100 GB/10-million-file claims, agree units/count population, hardware, concurrency,
latency/memory objectives and a resource-bounded campaign/cleanup plan. Stage increasing loads and
restart/failure tests; final acceptance still requires the actual target volume and byte identity.
Do not fill this development machine's disk merely to change a readiness label. Exit: independently
recorded platform journeys and representative measured target-scale results with reproducible inputs.

## Quality and verification gates for every implementation slice

1. Resolve that slice's blocking decisions in its owning SDD, not by hidden adapter/UI defaults.
   Expand the 90 seed families into a pre-code risk inventory; map each requirement to observable
   assertions, permission combinations, boundary values, concurrency and retained failure state.
2. Reproduce the defect or missing contract where applicable. Keep pure policies inward-facing,
   use cases focused and ports semantic; inspect callers/imports and split collaborators by actual
   responsibility. File-size/growth and dependency-graph signals prompt review, not score-driven moves.
3. Verify pure policy, use cases, real adapter/HTTP/database/byte effects and actual packaged UI at
   the appropriate boundaries. Keep deterministic failures, cancellation, replay and retry. External
   engines/identity/federation use owned isolated endpoints/data; real outside-service proof is separate.
4. Run complete applicable backend/desktop/Wasm suites and existing coverage gates on the final
   candidate. Publish exact source/tree/task/runtime and original reports, full-denominator line/branch
   changes, focused misses and platform limitations. Do not hide misses with exclusions, lower gates
   or counts inflated by mocks/multiplatform repetitions. Global targets need separate reviewed changes.
5. Complete applicable native CI and actual Copilot review; resolve findings and inspect original
   artifacts before requesting an exact administrative merge exception if required. Action refs stay
   `@vX`; keep reviewed per-PR cancellation/cadence, protections and managed-branch ownership.
6. Preserve original drafts, user content/database and one executable rollback; make persistent-data
   migration/recovery explicit. After authorized adoption verify CI and local/remote equality, and update
   source-bound evidence/status without relabeling earlier failures or wider targets as successful.

## Definition of this documentation delivery

#124 is complete only after all 30 source answers match their single owners, default/authority/open
decisions and planned scenarios; catalog/links/diff validate; applicable CI and actual review are
handled; and publication/adoption is recorded truthfully. Documentation checks add no executed
product tests and implement no target capability. Future milestones require their own admitted
issues rather than a burst of automatically started work or unreviewed changes to Copilot.
