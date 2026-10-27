# Collaboration authorization — issue #89

Contract: [SDD-0009§6.4](../../../specs/SDD-0009-real-time-collaboration.md#64-collaboration-authorization-repair--issue-89), SDD-0003§6.6 and SDD-0004§6.5. The contract was committed before code in `0e8535f`; initial implementation source is `7bb140e`; the verified follow-up is `f35140d99c103b6826e1df005e224663fc4f0aee` (production writes in `b0c60a6`). Verification: 2026-10-27 UTC. The [machine-readable receipt](collaboration-authorization-89.json) includes source fingerprints and full denominators; its hash-linked [case inventory](collaboration-authorization-89-cases.json) retains every collected new case. Final-head remote CI/review/adoption must be recorded separately in the linked PR.

## Behavior and compatibility

The original production-DI/PostgreSQL regression returned201 to an unrelated account and persisted its membership. Both assertions were observed failing before the repair. All scoped application calls now require an explicit actor and canonical active-file ownership. Visibility, download shares, nonowner ADMIN and old unguarded membership confer no collaboration capability. Durable nonowner grants remain product work; valid owner behavior is retained.

Minimal session/participant/comment projections precede protected full-state access. Session/item/participant/comment IDs and comment path parents are bound. Cursor, selection, leave and socket join require the actor's own participant; comment deletion retains its author check after item ownership. Global presence is self-scoped and validates active pointers and returned identities. Disconnect does not implicitly clear another device's global presence.

Admission, updates and last-participant closure acquire the item lock before the session lock and recheck authority/liveness in the same transaction. A PostgreSQL lock-wait test transfers ownership while admission is blocked: the stale precheck cannot admit a participant. Other tests cover concurrent admissions/leaves, both join/leave orderings, rollback, cancellation and owner-scoped expiry.

Sockets register only after valid binding, have distinct connection IDs, reauthorize senders/recipients and broadcast success only after successful effects. Another actual failing regression exposed session rebinding after a socket opened; its fixed expected item is now checked at command/delivery boundaries. JSON serializers escape messages.401/403/404/400 and safe500 outcomes retain their meaning; cancellation remains control flow. Checked delivery boundaries do not imply an atomic distributed database/network transaction.

Save acknowledges an **existing persisted collaboration working snapshot** only after locking the canonical owner and checking the exact version/content/timestamp under the document lock; missing state returns404. A stale snapshot is rejected without recreating or overwriting working state. It does not commit canonical bytes or manufacture an initialized document. No schema migration or reassignment of existing content is performed. Internal actor signatures change; compatible model/serializer names and normal HTTP DTO fields remain. The former bogus save acknowledgement and heterogeneous operation maps become truthful snapshot outcomes and typed DTOs.

## Architecture and preserved work

Models/ports belong to `domain/collaboration`, retaining their qualified names for compatibility. The legacy core jar re-exports that domain module; compiled model classes have exactly one owner. The application module compiles against only domain/common, domain/storage and domain/collaboration project dependencies. It requires no legacy core, HTTP, SQL, DI or logging framework.

The facade delegates to focused access/session/document/comment/presence/stream helpers. Separate SQL adapters implement scope projections and atomic membership/document/comment writes; production `CollaborationModule` wires them. HTTP mapping, socket binding, commands and transports are separate. The former670-line persistence facade delegates to focused implementations. Original12 service,35 API DTO/manager and11 model regressions remain collected; the initial model-test move was byte-identical; its later ownership header leaves all executable assertions unchanged.

## Executed verification

| Scope | Cases | Failures / errors / skips |
|---|---:|---:|
| Complete unfiltered backend gate |2735|0 /0 /0|
| New collected cases against the2255-case baseline |480|0 /0 /0|
| Pre-code risk floor |391|fulfilled within the480 new cases|

The floor includes108 service and195 HTTP matrix executions,18 presence,25 socket,12 minimal-SQL, at least10 race/maintenance and24 additional fault cases. Admission/result-shape, live binding, suspension, empty-state and lock-wait checks expand this to480. These are matrix/contract executions, not480 product features. The local unfiltered Gradle gate can reuse unchanged tasks; final-head PR CI must independently reconcile backend/desktop/browser results.

Actual routing, production repository/service DI, migrated PostgreSQL and local bytes are used; only disposable session-token validation is substituted. Test-host sockets exercise delivery/state; a real Netty TCP upgrade verifies anonymous401 because the test-host cannot represent a rejected upgrade. Minimal adapter schemas omit full-model columns. Real SQL errors/cancellation and denied row/byte snapshots are verified, including old unauthorized membership.

Changed sources pass ktlint and strict Detekt with local `maxIssues=0`, an asserted exact source selection and the existing baseline unchanged. No repository threshold, baseline, Copilot control, protection or CI cadence was relaxed. Repository FQN and16-capability specification checks pass.

### Coverage with the complete denominator

All backend execution data covers **382/395 lines and183/207 branches** of the entire core/collaboration production module, including unchanged OT. No source is excluded to improve the figures; these are not whole-platform100% claims.

| Source | Lines | Branches |
|---|---:|---:|
| Access |75/75|54/54|
| Sessions |70/70|30/30|
| Documents |36/36|14/14|
| Facade |53/53|2/2|
| Comments |57/57|25/26|
| Presence |35/35|28/30|
| Streams |24/24|16/23|
| Inputs |11/11|none|
| Unchanged OT |21/34|14/28|

The retained diagnostic instrumentation aggregates all backend execution data; standard module reports alone have their own data scope. Remaining flow/presence/OT branches are not hidden. The whole backend denominator is8042/16414 lines and1555/5712 branches. The machine receipt also includes every changed infrastructure source; legacy unused adapters have low coverage. Standard module reports do not aggregate the API execution data that exercises infrastructure, so the Codecov patch figure has a different scope. No denominator or uncovered adapter is discarded. Broader coverage/inventory and R14 work remain #84/#112.

## Packaged loopback dogfooding

The actual b0c60a6 production distribution runs read-only under `/app` in the existing owned non-root Docker backend; all237 mounted files match its manifest. Production sources are byte-identical in the f35140d verification revision; its only later change separates a test helper. PostgreSQL was backed up before switching only that service. PostgreSQL/frontend containers, named volumes and all prior storage/plugin file hashes remain. The bc8ceed package/configuration is the retained secure runnable rollback; the previous7bb package is transient while this follow-up is reviewed.

The completed/resumed campaign records40 actual HTTP requests and nine native JDK owner frames: two independently bound devices, cursor/operation delivery, acknowledgement, ping, old-device leave and continued use of the second connection. Unknown top-level and nested operation fields are accepted; a supplied userId cannot forge the server's authenticated operation identity. A foreign socket closes1008 without receiving state, even with retained old unauthorized membership. Denied calls preserve the protected row snapshot. Reply identity, resolve persistence and deletion of both the comment and its replies are checked in the real database; previous version-history controls still return200/403.

Working state is seeded **explicitly** for the socket/edit/snapshot control; canonical bytes remain original. A separate cold upload still gives state/save404. The probe initially assumed a resolvedAt DTO and delete200; the actual routes return a message acknowledgment and204. The original frames/journal and synthetic state were preserved, the adapter was corrected, persisted effects were verified directly, and the remaining controls resumed without replaying the document operation. These were probe assumptions, not application defects. No complete GUI or cold-editor certification is claimed.

## Remaining acceptance and rollout

[#112](https://github.com/vypdev/vaultstadio/issues/112) owns the verified frontend/server route/auth/message mismatch, bootstrap, canonical save/version coordination, durable streams/replay/convergence and durable nonowner capabilities. Do not close R14 after this permission repair. Shares/pagination remain #90/#91; canonical version recovery is #109. Trusted-master setup rollout remains #111; VERIFICATION_SETUP staysfalse until its reviewed prerequisite is present.

From `backend/`, with Java17 and Docker:

```sh
./gradlew test :core:collaboration:jacocoTestReport :domain:collaboration:jacocoTestReport \
  --continue --no-parallel --max-workers=2 --no-daemon
```

Final-head CI/Copilot review, protected-branch adoption and master promotion remain pending. A reviewed rollback restores the prior authorization risk. No deployment, tag, release or master merge is established by this local evidence.

## Immutable comparison follow-up

GitHub reports the original model-test move as renamed with0additions/deletions/changes and no patch; the exact old/new blobs are both028d97dcf4ad24ac1ec94fea892494def1912c48. Copilot Commit correctly retains progress but rejects that representation as missing evidence (run40437333668/job121969044688, validation.evidence-unavailable). The specific pure-rename reproduction was added to [upstream#726](https://github.com/vypdev/copilot/issues/726#issuecomment-6432187474).

The domain ownership is now explicit in the test header, giving GitHub a real textual delta. Everything from the package directive onward, including the original eleven test cases, is byte-identical. Those11 cases and strict domain quality pass after the note. No production behavior, Copilot control, exclusion, approval policy or synthetic success was introduced. The first head's technical CI passed; its incomplete evidence assessment is not represented as a successful Copilot review. The new pinned comparison and final-head CI/review must still be checked.

## Transactional follow-up and actual regressions

The pre-code addenda d0c9e94/84c0dbb/6df1d51 define snapshot integrity, transactional owner checks and decoder compatibility. The original save race genuinely returned success while overwriting version2 with version1. Two stale document prechecks and four stale comment prechecks also genuinely returned success after a committed ownership transfer; an otherwise valid ping with an extension returned a format error. Their raw failing cases are retained separately from successful verification.

Document writes now lock/recheck the item, then lock/check the exact expected working state before inserting an operation. Conflicting transformed writes cannot overwrite one another. Comment create/update/resolve/reply/delete carry the actor and expected parent into the transaction; deletion and updates retain author checks. Mutation result reads stay inside the transaction. Typed failures and cancellation remain intact.

The follow-up adds38 executions: five snapshot persistence cases, six stale write-authority cases, fourteen document adapter/concurrency cases, twelve comment adapter cases and one socket extension case. All480 new cases plus retained regressions pass in the complete2735-case backend command. Strict Detekt uses maxIssues0 with the existing baseline unchanged. One prior full execution passed every test but failed a complex test method; the final command passes after that helper was separated. It explicitly reuses its owned private test HOME, allowing164 unchanged tasks to remain up to date; raw552XML and actual execution metadata are retained.

The previous remote revision c07383b passed2697backend+1315desktop+1191browser=5203 cases, with no failures/errors/skips. Copilot Commit succeeded once the rename had a genuine textual patch; [upstream#726 records the verified workaround](https://github.com/vypdev/copilot/issues/726#issuecomment-6432581270). These results are historical and do not certify this follow-up head. Its exact-head CI/review/adoption are required separately.

### Reviewable receipt representation

At42d9702 GitHub omitted the patch for the added2999-line/109259-byte receipt even though its immutable blob6287cdb24b6f3b22a169e8d9f1bd33c040a5fe77 is complete. Copilot Commit blocked on evidence availability (run40442166032). The schemaVersion2 receipt separates its case inventory from source/coverage/operational facts. Every original field and all480 cases are preserved; the inventory is linked by path/count/SHA256 and both reviewable files retain their implementation identity. This is a documentation split; no control or test scope changed. The final pinned comparison must provide both patches.
