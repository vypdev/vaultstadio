# Collaboration authorization — issue #89

Contract: [SDD-0009§6.4](../../../specs/SDD-0009-real-time-collaboration.md#64-collaboration-authorization-repair--issue-89), SDD-0003§6.6 and SDD-0004§6.5. The contract was committed before code in `0e8535f`; implementation source is `7bb140e0f6006a3ffd2885557f816943f788f5c9`. Verification: 2026-10-27 UTC. The [machine-readable receipt](collaboration-authorization-89.json) includes source fingerprints, every collected new case and full denominators. Final-head remote CI/review/adoption must be recorded separately in the linked PR.

## Behavior and compatibility

The original production-DI/PostgreSQL regression returned201 to an unrelated account and persisted its membership. Both assertions were observed failing before the repair. All scoped application calls now require an explicit actor and canonical active-file ownership. Visibility, download shares, nonowner ADMIN and old unguarded membership confer no collaboration capability. Durable nonowner grants remain product work; valid owner behavior is retained.

Minimal session/participant/comment projections precede protected full-state access. Session/item/participant/comment IDs and comment path parents are bound. Cursor, selection, leave and socket join require the actor's own participant; comment deletion retains its author check after item ownership. Global presence is self-scoped and validates active pointers and returned identities. Disconnect does not implicitly clear another device's global presence.

Admission, updates and last-participant closure acquire the item lock before the session lock and recheck authority/liveness in the same transaction. A PostgreSQL lock-wait test transfers ownership while admission is blocked: the stale precheck cannot admit a participant. Other tests cover concurrent admissions/leaves, both join/leave orderings, rollback, cancellation and owner-scoped expiry.

Sockets register only after valid binding, have distinct connection IDs, reauthorize senders/recipients and broadcast success only after successful effects. Another actual failing regression exposed session rebinding after a socket opened; its fixed expected item is now checked at command/delivery boundaries. JSON serializers escape messages.401/403/404/400 and safe500 outcomes retain their meaning; cancellation remains control flow. Checked delivery boundaries do not imply an atomic distributed database/network transaction.

Save persists an **existing collaboration working snapshot**; missing state returns404. It does not commit canonical bytes or manufacture an initialized document. No schema migration or reassignment of existing content is performed. Internal actor signatures change; compatible model/serializer names and normal HTTP DTO fields remain. The former bogus save acknowledgement and heterogeneous operation maps become truthful snapshot outcomes and typed DTOs.

## Architecture and preserved work

Models/ports belong to `domain/collaboration`, retaining their qualified names for compatibility. The legacy core jar re-exports that domain module; compiled model classes have exactly one owner. The application module compiles against only domain/common, domain/storage and domain/collaboration project dependencies. It requires no legacy core, HTTP, SQL, DI or logging framework.

The facade delegates to focused access/session/document/comment/presence/stream helpers. Separate SQL adapters implement scope projections and atomic membership; production `CollaborationModule` wires them. HTTP mapping, socket binding, commands and transports are separate. The former670-line persistence facade delegates to focused implementations. Original12 service,35 API DTO/manager and11 model regressions remain collected; the model test moved byte-identically.

## Executed verification

| Scope | Cases | Failures / errors / skips |
|---|---:|---:|
| Complete unfiltered backend gate |2697|0 /0 /0|
| New collected cases against the2255-case baseline |442|0 /0 /0|
| Pre-code risk floor |391|fulfilled within the442 new cases|

The floor includes108 service and195 HTTP matrix executions,18 presence,24 socket,12 minimal-SQL, at least10 race/maintenance and24 additional fault cases. Admission/result-shape, live binding, suspension, empty-state and lock-wait checks expand this to442. These are matrix/contract executions, not442 product features. The local unfiltered Gradle gate can reuse unchanged tasks; final-head PR CI must independently reconcile backend/desktop/browser results.

Actual routing, production repository/service DI, migrated PostgreSQL and local bytes are used; only disposable session-token validation is substituted. Test-host sockets exercise delivery/state; a real Netty TCP upgrade verifies anonymous401 because the test-host cannot represent a rejected upgrade. Minimal adapter schemas omit full-model columns. Real SQL errors/cancellation and denied row/byte snapshots are verified, including old unauthorized membership.

Changed sources pass ktlint and strict Detekt with local `maxIssues=0`, an asserted exact source selection and the existing baseline unchanged. No repository threshold, baseline, Copilot control, protection or CI cadence was relaxed. Repository FQN and16-capability specification checks pass.

### Coverage with the complete denominator

All backend execution data covers **377/390 lines and183/207 branches** of the entire core/collaboration production module, including unchanged OT. No source is excluded to improve the figures; these are not whole-platform100% claims.

| Source | Lines | Branches |
|---|---:|---:|
| Access |75/75|54/54|
| Sessions |70/70|30/30|
| Documents |31/31|14/14|
| Facade |53/53|2/2|
| Comments |57/57|25/26|
| Presence |35/35|28/30|
| Streams |24/24|16/23|
| Inputs |11/11|none|
| Unchanged OT |21/34|14/28|

The retained diagnostic instrumentation aggregates all backend execution data; standard module reports alone have their own data scope. Remaining flow/presence/OT branches are not hidden. Broader coverage/inventory and R14 work remain #84/#112.

## Packaged loopback dogfooding

The actual distribution runs read-only under `/app` in the existing owned non-root Docker backend; all237 mounted files match its manifest. PostgreSQL was backed up before switching only that service. PostgreSQL/frontend containers, named volumes and all prior storage/plugin file hashes remain. The bc8ceed package/configuration is the immediate runnable rollback.

The completed HTTP replay records28 actual requests. A JDK client records nine owner socket frames: two independently bound devices, cursor/operation delivery, acknowledgement, ping, old-device leave and continued use of the second connection. A foreign socket closes1008 without receiving state, even with retained old unauthorized membership. Denied calls preserve the protected row snapshot. Replies persist the correct user/content, and previous version-history controls still return200/403.

The working state is seeded **explicitly** for the socket/edit/snapshot control. Its canonical download remains the original bytes. A separate cold upload still gives missing state/save404. The first private Python harness stopped after assuming the comments list was an object; persisted native frames and synthetic state were recovered without overwriting data, the response adapter was corrected and remaining/replayed checks completed. This was a harness failure, not an application fix. No complete GUI or cold-editor certification is claimed.

## Remaining acceptance and rollout

[#112](https://github.com/vypdev/vaultstadio/issues/112) owns the verified frontend/server route/auth/message mismatch, bootstrap, canonical save/version coordination, durable streams/replay/convergence and durable nonowner capabilities. Do not close R14 after this permission repair. Shares/pagination remain #90/#91; canonical version recovery is #109. Trusted-master setup rollout remains #111; VERIFICATION_SETUP staysfalse until its reviewed prerequisite is present.

From `backend/`, with Java17 and Docker:

```sh
./gradlew test :core:collaboration:jacocoTestReport :domain:collaboration:jacocoTestReport \
  --continue --no-parallel --max-workers=2 --no-daemon
```

Final-head CI/Copilot review, protected-branch adoption and master promotion remain pending. A reviewed rollback restores the prior authorization risk. No deployment, tag, release or master merge is established by this local evidence.
