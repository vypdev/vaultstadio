# Bounded storage selections — issue #91

The verified implementation is **1499c6b0437d4a451f9b5cbf1596d1727a2e1c4e**, based on adopted developc9f1ac5. SDD-0002§6.5/6.5.1, SDD-0004§6.6 and SDD-0012§6.4/6.5 were committed before their code, in818a751 and385daf4. Verification date:2026-10-27 UTC. The [receipt](storage-selection-91.json) binds a separate [case inventory](storage-selection-91-cases.json) and [complete coverage/source detail](storage-selection-91-coverage.json). Final PR CI, Copilot review and protected adoption are separate pending acceptance steps.

## Observed defect and resulting contract

The original native package03aa855 reproduced negative limit→500DATABASE_ERROR; negative offset and malformed/zero limit silently succeeded. Limit1/offset2147483647 returned negative page−2147483648. The original unknown sort already returned400 and remains a required passing behavior.

After authentication, invalid selections now return a bounded400VALIDATION_ERROR before parent/metadata lookup, query/count, plugins, bytes or events. Direct core folder/search/recent calls validate semantic bounds before lookup too. REST limits are1..1000 and offsets0..2147483647. Missing values alone use defaults; malformed query strings, overflow, signs, whitespace, empty or duplicate known keys do not. Query leading zeros are supported; JSON supplied values must be actual Int-range integers, not null/string/fraction/exponent. Known unsupported GET selection controls are rejected, including when a suggestion prefix is too short to search.

Folder defaults remain100/0/name/asc; fields name,size,created_at,updated_at,type and orders asc/desc are ASCII case-insensitive. Search defaults50/0, recent20, suggestions10 and metadata50. SQL adds stable ID ascending as the final tie-breaker, retaining type/name secondary ordering. Owner, exact parent, trash/starred/type predicates and typed provider errors remain. Authentication precedes validation; a valid foreign parent remains403 and a valid missing parent404.

Page/totalPages are Long through domain/server/shared KMP DTOs and models. Safe division/remainder avoids overflow in ceil(total/limit); hasMore adds in Long. Limit1/offset2147483647 now returns page2147483648. Ordinary JSON fields/numbers retain their values. Consumers must support the documented int64 extremes. Raw protocol query constructors remain separate; existing nonpositive-limit page1/totalPages0 conventions remain for legacy callers, without a blanket REST constraint or data migration.

Metadata results remain a bounded owner-authorized projection with page1/totalPages0 or1. Only forbidden/missing canonical records are omitted; provider failure resolving a record remains an explicit typed error. Advanced invalid dates produce safe400 without echoing input. The unavailable content-search option returns501CONTENT_SEARCH_UNAVAILABLE once, without selecting storage or plugins. The old native package returned a successful warning map, even with negative limit; this is now explicitly rejected rather than certified as implemented search.

## Architecture and retained scope

A pure domain value object owns limits and failure identity. API owns query cardinality/lexical adaptation and JSON parsing; core maps pure failures to the existing semantic exception port. The new policy/orchestration imports no HTTP/SQL/DI/provider implementation. Two dependency-direction guards run inside the existing source/workflow contract job. No new workflow, heavy trigger or SHA-based Action reference is introduced; Copilot and branch protections are unchanged.

The old435-line storage route file and350-line search route file become small composition facades. Focused listing/read/mutation/trash/transfer, basic/advanced/metadata/suggestion handlers and models keep original qualified DTO and route identities. SQL selection is separated from the existing repository facade. Its expected database/mapping failures preserve safe context and the original private cause; cancellation propagates unchanged and unexpected/fatal failures are not blanket converted. Real download verification confirms the input stream remains open until deferred transmission and closes afterwards.

This does not complete filename/prefix matching (#82), global advanced filtering/counts or real content indexing (#115). Existing advanced filters still project after a bounded page; documentation states that limitation. Wider storage/protocol/coverage work remains on the roadmap. The hardcoded Web API origin discovered during GUI testing is recorded in #116; it was not silently bundled into this input-validation repair.

## Executed verification

| Scope | Cases | Failures / errors / skips |
|---|---:|---:|
| Complete unfiltered backend selector |3247|0 /0 /0|
| Complete Desktop JVM selector |1324|0 /0 /0|
| Complete browser Wasm selector |1200|0 /0 /0|
| Genuinely new backend cases |266|0 /0 /0|
| Shared production-serializer contracts |8 per platform|0 /0 /0|
| New architecture guards |2|0 /0 /0|

Total application executions:5771. Baselines2981/1316/1192 remain collected. Numeric Long expectation adaptations keep existing assertions and do not inflate the new-case count. The266 new backend cases comprise75 pure policy/parser,39 core call-order/failure,119 actual HTTP/production composition/PostgreSQL,16 SQL ordering/ownership,8 arithmetic and9 database-boundary cases. Parameterized executions count cases, not distinct features. This exceeds the pre-code160+8 floor without counting static getters or assertion-only conversions.

HTTP uses production repository/service/application modules, serialization, authentication/error handling and migrated PostgreSQL plus local bytes. Only disposable credential validation and plugin seams are mocked; repositories/backends are spies over actual implementations. Invalid input proves no downstream selection/effect. Positive controls retain real ownership/type errors, provider failures, exact bounds, defaults, empty/beyond-end data, duplicate keys, typed JSON/null/coercion rejection, metadata privacy and safe unavailable search. SQL manually specified expected ID sequences cover all five fields in both directions over unchanged tied datasets.

Changed Kotlin passes ktlint and strict Detekt maxIssues0 with an asserted exact source selection and unchanged baseline. Existing storage and frontend serializer/input-semantics coverage gates pass. Catalog/FQN/source contracts, all47 setup controls,15 browser-bootstrap controls,34 Node page tests and5 actual Nginx privacy/origin tests pass. Initial sandbox attempts blocked sockets/Docker; those exact local controls were repeated with the required local access, without changing tests. Full reachable-history plus merge-only-blob Gitleaks8.30.1/TruffleHog3.99.7 scans find no new secrets;17/19 prior exact reviewed nonsecrets remain unchanged.

Tests ran before the implementation commit against recorded working sources. All41 tested Kotlin hashes were then compared with the exact1499c6b Git blobs, and the raw execution HEAD385daf4 is retained honestly. No post-commit execution is invented. The browser source remained unchanged after its successful full selector; the JVM was separately rerun completely with verified Temurin17.

### Coverage and limitations

Whole backend diagnostic aggregate: **8958/16877 lines and1934/6042 branches**. Every changed infrastructure source and legacy denominator remains included; no new exclusions or baseline relaxation improve the figures.

| Source | Lines | Branches |
|---|---:|---:|
|StorageSelection.kt|10/10|6/6|
|StorageSelections.kt|40/40|4/4|
|PagedResult.kt|9/9|8/8|
|StorageDatabaseResult.kt|13/13|none emitted|
|StorageItemSelection.kt|52/52|20/23|
|StorageSelectionParameters.kt|42/56|53/62|

The generic inline receiver has uncovered declaration probes while actual route callsites are exercised. Query helpers also retain missing branch probes; they are not all certified unreachable. Nullable raw owner/trash SQL branches and a suspend transaction branch remain uncovered in that source. The coverage detail preserves these exact lines and every changed source's counters. Full portfolio coverage remains partial under #84.

### Preserved failures

Initial test compilation found fixture import/type/visibility errors; strict lint exposed long legacy mixed handlers and broad SQL exception catches. The handlers and provider boundary were extracted rather than suppressing the findings. A later compile caught incorrect infix parentheses during that extraction; real SQL tests and the final full suite verify the corrected predicates. Original logs are retained.

The first complete frontend command passed all1200 Wasm cases but its desktop JVM aborted natively in Corretto17.0.9 ARM64, in assembler_aarch64.hpp (`Field too big for insn`). Original fatal/replay diagnostics are retained in the hash-bound crash archive. The complete1324-case JVM selector then passed with the official, checksum-verified Temurin17.0.21+9 SDK; each fork asserted its actual launcher. No test was filtered, JIT mode disabled, CI gate changed or user's installed JDK removed. The native crash's underlying cause is not independently established. This does not certify a native Desktop/mobile GUI journey.

## Actual packaged dogfooding

The package uses the same pinned nonroot Java25 backend, Nginx and PostgreSQL images, read-only executable mounts and loopback8080/18086. All238 backend and6 frontend file hashes match the generated manifests. A PostgreSQL dump was taken before replacement; its container ID, volumes and all pre-existing storage/plugin file hashes remain. Only disposable fixture rows/bytes were added; no role was broadened. The03aa855 package is the immediate runnable rollback.

The real HTTP campaign passes46 scenarios/69 requests including setup: five uploads with exact byte hashes, all ten sort/direction combinations through three pages, malformed/duplicate/unsupported values, int64 extremes, JSON null/string errors, authentication/foreign parent distinctions, explicit501 and recovery to a valid default. These are HTTP claims, separate from GUI.

Actual Chrome GUI testing enters the disposable email/password autonomously, logs in, opens the new folder and selects Size. It displays delta0bytes, alpha1, epsilon/beta2 and gamma3 in the SQL-stable order. The UI exposes enum sort controls, not freeform numeric pagination input.

To test actual browser denial/recovery, a temporary loopback-only proxy appended an invalid duplicate limit solely for the new disposable folder. The browser received400 and showed `Validation failed for field: limit`. Removing the proxy and restoring the original backend port, then clicking the real Refresh control, produced backend folder/breadcrumb200 and the same five ordered files, with the error cleared. Original screenshots, AX state, HTTP logs and fault/restoration journals are retained. This is clearly labeled wire fault injection, not a claim that a user typed an unavailable numeric field. The original Nginx config is restored byte-for-byte and the temporary container is removed; all package/data/health checks pass again.

The first fault attempt at the frontend Nginx did not intercept Web's direct localhost:8080 API calls and is not counted as a GUI400. Its restoration is recorded. That observation exposed the fixed Web API URL now tracked by #116. Search completion, external S3, native Desktop/mobile GUI, master promotion and full platform acceptance remain unverified separate work. No deploy, tag, release or setup activation is implied.
