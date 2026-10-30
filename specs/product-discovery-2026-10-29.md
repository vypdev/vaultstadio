# Accepted discovery decisions — 2026-10-29

[Issue #124](https://github.com/vypdev/vaultstadio/issues/124) reconciles the maintainer's
30 answers into the existing SDD portfolio before the focused #116 implementation resumes.
The [verbatim Spanish answers](discovery/2026-10-29/answers.json) preserve their order and source keys. The
recorded source snapshot hash identifies the original retained local questionnaire, not the
different byte representation of this normalized registry.

This directory is provenance and an index, **not a second PRD, ADR or specification lifecycle**.
Each requirement has exactly one owner below. Its owning SDD §6 contains the normative
interpretation and blocking technical decisions; §7 records authority/default/inheritance;
§16 contains three concrete future acceptance families; §17 links ownership and evidence state.
Companion SDDs reference that owner instead of redefining the requirement.

All 30 decisions are **accepted targets, proposed for implementation**. None is proof that
the target is implemented or ready. Existing catalog statuses, historical verification dates,
failed scenarios, bounded successes and original evidence remain intact.

## Ownership and planned verification

`P`, `N` and `F` mean positive, denied/invalid and failure/recovery respectively. The linked
SDDs specify the observable inputs, effects and retained state, not just a test-name placeholder.
There are **90 planned acceptance families**, not 90 new executed tests or a sufficient final
implementation budget. Each feature must expand its actual actor, policy, boundary,
concurrency, cryptographic and platform risks before becoming ready for implementation.

| Decision | Owning contract | Planned acceptance | Roadmap |
|---|---|---|---|
| PD-001 Primary installation and views | [SDD-0000](sdd-0000-architecture-quality-and-product-contract.md#pd-001) | PD-001-P/N/F | M0 |
| PD-002 Two server models | [SDD-0010](sdd-0010-federation-and-trust.md#pd-002) | PD-002-P/N/F | M4 |
| PD-003 Administrative enrollment | [SDD-0010](sdd-0010-federation-and-trust.md#pd-003) | PD-003-P/N/F | M4 |
| PD-004 Personal/shared spaces | [SDD-0004](sdd-0004-storage-domain-and-data-lifecycle.md#pd-004) | PD-004-P/N/F | M2 |
| PD-005 Server-processable/E2EE modes | [SDD-0015](sdd-0015-security-privacy-and-threat-model.md#pd-005) | PD-005-P/N/F | M2 |
| PD-006 Mode chosen per space | [SDD-0015](sdd-0015-security-privacy-and-threat-model.md#pd-006) | PD-006-P/N/F | M2 |
| PD-007 Owner/trusted recovery | [SDD-0015](sdd-0015-security-privacy-and-threat-model.md#pd-007) | PD-007-P/N/F | M2 |
| PD-008 No administrator content bypass | [SDD-0003](sdd-0003-identity-authentication-and-authorization.md#pd-008) | PD-008-P/N/F | M1 |
| PD-009 On-demand/offline sync | [SDD-0008](sdd-0008-device-sync-and-conflicts.md#pd-009) | PD-009-P/N/F | M3 |
| PD-010 Three conflict strategies | [SDD-0008](sdd-0008-device-sync-and-conflicts.md#pd-010) | PD-010-P/N/F | M3 |
| PD-011 Conflict inheritance | [SDD-0008](sdd-0008-device-sync-and-conflicts.md#pd-011) | PD-011-P/N/F | M3 |
| PD-012 Trash retention | [SDD-0004](sdd-0004-storage-domain-and-data-lifecycle.md#pd-012) | PD-012-P/N/F | M2 |
| PD-013 Version retention | [SDD-0007](sdd-0007-version-history-and-recovery.md#pd-013) | PD-013-P/N/F | M2 |
| PD-014 Quota categories | [SDD-0004](sdd-0004-storage-domain-and-data-lifecycle.md#pd-014) | PD-014-P/N/F | M2 |
| PD-015 Quota exhaustion | [SDD-0004](sdd-0004-storage-domain-and-data-lifecycle.md#pd-015) | PD-015-P/N/F | M2 |
| PD-016 Scale target | [SDD-0000](sdd-0000-architecture-quality-and-product-contract.md#pd-016) | PD-016-P/N/F | M8 |
| PD-017 Link capabilities | [SDD-0006](sdd-0006-sharing-and-permissions.md#pd-017) | PD-017-P/N/F | M5 |
| PD-018 Identity for public writes | [SDD-0006](sdd-0006-sharing-and-permissions.md#pd-018) | PD-018-P/N/F | M5 |
| PD-019 Platform parity | [SDD-0013](sdd-0013-frontend-kmp-and-platform-architecture.md#pd-019) | PD-019-P/N/F | M8 |
| PD-020 Feasible redundancy | [SDD-0004](sdd-0004-storage-domain-and-data-lifecycle.md#pd-020) | PD-020-P/N/F | M4 |
| PD-021 Disk/server failure domains | [SDD-0004](sdd-0004-storage-domain-and-data-lifecycle.md#pd-021) | PD-021-P/N/F | M4 |
| PD-022 Degraded operation | [SDD-0004](sdd-0004-storage-domain-and-data-lifecycle.md#pd-022) | PD-022-P/N/F | M4 |
| PD-023 Primary HA | [SDD-0014](sdd-0014-deployment-operations-and-release.md#pd-023) | PD-023-P/N/F | M7 |
| PD-024 External processing consent | [SDD-0012](sdd-0012-metadata-search-and-ai.md#pd-024) | PD-024-P/N/F | M6 |
| PD-025 Invitation-first onboarding | [SDD-0003](sdd-0003-identity-authentication-and-authorization.md#pd-025) | PD-025-P/N/F | M1 |
| PD-026 Local and SSO identity | [SDD-0003](sdd-0003-identity-authentication-and-authorization.md#pd-026) | PD-026-P/N/F | M1 |
| PD-027 MFA defaults | [SDD-0003](sdd-0003-identity-authentication-and-authorization.md#pd-027) | PD-027-P/N/F | M1 |
| PD-028 Automatic backups/restore | [SDD-0014](sdd-0014-deployment-operations-and-release.md#pd-028) | PD-028-P/N/F | M7 |
| PD-029 Content/OCR/semantic search | [SDD-0012](sdd-0012-metadata-search-and-ai.md#pd-029) | PD-029-P/N/F | M6 |
| PD-030 Placement/user choice | [SDD-0004](sdd-0004-storage-domain-and-data-lifecycle.md#pd-030) | PD-030-P/N/F | M4 |

## Interpretation and interacting policies

- **Terminology:** primary, managed storage node and independent installation describe different
  authorities. A single community can have personal files and family/team spaces; the occasional
  answer mentioning an organization does not select a separate multi-tenant product model.
- **Defaults and exceptions:** where the answers permit space/vault overrides, the explicit
  administrator override wins over the general default and absence inherits it. Link options and
  user placement preferences remain inside their governing policy. Persistence, validation,
  effective revision for pending work and migration are blocking implementation decisions.
- **Choice references:** the conflict answer's “point 2” means safe compatible-format merge with
  preserve-both/manual fallback; the alternatives are preserve-both/manual and latest-received
  current plus retained history. Quota's “second” means eligible-retention cleanup then block;
  the alternatives are block without admission-driven purge and bounded temporary overage.
  The version answer means 30 days **and at least** 10 historical versions, with current separate.
- **Retention versus quota:** a version is eligible only outside both time and minimum-count
  protection and any explicit hold/reference. Quota pressure does not authorize deletion of
  active content or protected history. Indefinite retention remains meaningful under every quota
  strategy. Physical-versus-logical charging and replica overhead are still open.
- **Encryption versus processing:** no server decryption of E2EE is implied by a local search
  engine, external consent, backup, administrator role or recovery request. External content
  processing needs both administrator enablement and express space-responsible authorization.
  Metadata leakage and any client-side E2EE indexing need explicit design.
- **Authority versus convenience:** adding a server, selecting it for upload, joining a space,
  knowing a public link or holding ADMIN cannot bypass the owning content grant. Revocation and
  policy changes during queued/offline work must be expanded in the applicable acceptance matrix.
- **Availability versus recovery:** disk protection, server protection, primary HA and backup
  restore have different failure domains. One disk does not give product redundancy; two disks
  on one host do not give server-loss protection. Independent peer authority remains independent.
- **Targets versus evidence:** 100 GB, 10 million files, E2EE, MFA/SSO, HA and parity require their
  own executed campaigns. Existing unit/HTTP/GUI receipts retain their exact source, package,
  platform, sizes and limitations. Documentation validation establishes none of those targets.

## Next work and unresolved decisions

The [ordered roadmap](../docs/development/PRODUCT_ROADMAP_2026-10-29.md) preserves the
existing focused issues and describes implementation/evidence dependencies. Technical choices
are recorded with their owning requirement rather than silently resolved here. Future issues
must expand the scenarios and resolve their own blocking decisions before implementation;
there is no need to reopen all 30 product questions or launch all milestones at once.


## Canonical owner-path migration — issue #178

This discovery record moved to the flat canonical owner path required by the
published SDD reader. The provenance directory referenced above still retains
the unchanged verbatim answers. Accepted requirements, logical ownership,
90 planned families and implementation/readiness limits remain unchanged.
