# Public file access — issue #90

Contract: [SDD-0006§21](../../../specs/sdd-0006-sharing-and-permissions.md#21-revision-r05--public-file-access-90-pre-implementation-contract). Contract revisions were committed before their implementation: f22cbed, 283fd81, 2ed57b0, bf2dfdb and f5e9edb. The verified implementation is **03aa8552424d703db127be1d97e08c066598168d**, based on develop eac7185. Verification date: 2026-10-27 UTC. The [receipt](public-share-access-90.json) links separate, hash-bound [case inventory](public-share-access-90-cases.json) and [coverage/source detail](public-share-access-90-coverage.json). Remote final-head CI, Copilot review and adoption remain separate acceptance steps.

## Executed behavior and compatibility

The old packaged endpoint reproduced the audited defect: two metadata requests returned400 while consuming a two-download allowance; the next request returned403. Another real proxy request generated an absolute URL without port18086. Both failures are retained as original observations.

Anonymous metadata is now an explicit five-field DTO: name, size, mimeType, hasPassword and expiresAt. It exposes no owner/item IDs, storage keys, password hashes or recipients. Inspecting information never opens content or increments a counter. Public links authorize only access to a live, owned, nontrashed file through the public API; they do not authenticate private storage, versions or collaboration.

Token identity is case-sensitive. Missing files/content, changed ownership, invalid counters and folder links fail closed. Expiry equality is denied. Creation validates expiration1..365days (default7, explicit null supported), positive optional download limits, and strict UTF-8 passwords of1..72bytes. Unpaired surrogates are rejected. No schema migration or silent repair of legacy rows is performed.

The machine interface accepts `X-Share-Password` as canonical unpadded Base64url of strict UTF-8. Query passwords are rejected. This intentional replacement of the former query interface keeps passwords out of URLs and supports Unicode in Fetch headers. The anonymous page sends UTF-8 passwords in a bounded native POST form and streams attachments without full-file JavaScript buffering. Generated links retain the configured/request API origin, public port and token case. The bundled proxy preserves incoming authority; the operator still owns trusted forwarded-header configuration.

The SQL adapter locks the canonical item before its link, reads a fresh clock after waiting, rechecks identity/credentials/current content and atomically admits at most the remaining allowance. A stale lifecycle update cannot overwrite a newer count. Content is opened and a bounded prefix checked before admission; failed preparation consumes nothing. After admission an interrupted/partial transfer can consume its allowance, which the page explains explicitly. The implementation does not promise a distributed transaction with a byte provider or confirmation that a browser saved a file.

Streams close on preparation/admission/cancellation/response failure and after deferred Ktor transmission. Exact declared size is enforced; extra bytes are not forwarded, short/failed streams surface a safe failure. Filename handling retains Unicode via RFC5987 with a safe ASCII fallback and removes controls/path separators. Public request logging redacts capabilities; SQL failures do not log credentials, hashes or provider diagnostics.

## Architecture and CI scope

Pure decisions and ports live in domain/share. The focused core/share-access module depends only on domain projects and coroutine/value libraries; it imports no HTTP, SQL, provider, DI or legacy core implementation. HTTP descriptions/statuses live at the API boundary. Separate policy, orchestration, atomic SQL, password, prepared-content, stream, page, owner-listing/mutation and response adapters replace the old mixed access path. The unrelated activity adapter was extracted without losing behavior. Legacy package-qualified models remain compatible through narrow composed repository ports.

Three architectural guards run in the existing source/workflow contract job. That same job runs34 Node built-in page tests using official setup-node@v7. Existing application tests remain the per-PR commit gate. Five isolated Nginx privacy/origin tests also run in that existing contract job. No heavy security/Docker/runtime workflow or higher trigger frequency was added, no action uses a commit SHA, and no Copilot/protection/verification-setup setting was changed.

## Verification results

| Scope | Cases | Failures / errors / skips |
|---|---:|---:|
| Complete unfiltered backend selector |2981|0 /0 /0|
| Desktop JVM |1316|0 /0 /0|
| Browser Wasm |1192|0 /0 /0|
| New backend cases relative to2740 |241|0 /0 /0|
| Node page contracts |34|0 /0 /0|
| Architecture contracts |3|0 /0 /0|
| Real proxy privacy/origin tests |5|0 /0 /0|

The original eleven legacy access scenarios were migrated to the new orchestration module and remain collected. They are excluded from the new-case count. The pre-code floor was100 genuinely new cases across pure policy, orchestration, actual HTTP/DI/PostgreSQL and atomic SQL. Matrix executions are cases, not distinct product features. Native local gates can reuse unchanged Gradle tasks through the explicitly retained owned test HOME; final PR CI independently executes its selectors.

Changed Kotlin sources pass ktlint and strict Detekt with maxIssues0, an exact asserted source selection and the existing baseline unchanged. Storage coverage gates, FQN and16-capability catalog checks pass. Raw XML, source hashes, command/execution records and coverage XML are retained in the source-bound archive identified by the receipt. Secrets are scanned over complete reachable history and merge-only blobs by Gitleaks8.30.1 and TruffleHog3.99.7. Two new synthetic example matches were reviewed only at their exact immutable locations; no rule/path-wide exclusion or scanner threshold changed.

### Coverage and remaining denominator

Whole backend: **8558/16755 lines and1823/6001 branches**.

| Source | Lines | Branches |
|---|---:|---:|
|PublicSharePolicy.kt|28/28|70/70|
|PublicShareService.kt|36/36|19/20|
|ShareContentReaderAdapter.kt|32/32|14/16|
|VerifiedShareInputStream.kt|23/23|18/24|

The service retains one unobserved branch at the suspend/Arrow lookup boundary; it is not certified unreachable. Provider closed/bounds/late-transfer defensive branches remain visible. The receipt includes every changed infrastructure production source, including low-covered legacy adapters.

No sources are excluded to improve these figures. API execution is included in the diagnostic aggregate; standard per-module reports and Codecov patch coverage have different data scopes. Legacy adapters and defensive branches remain in the denominator. Broader coverage work remains #84.

### Failures found and corrected

Actual regression failures included early closure of a deferred response stream (expected17bytes, received0), raw Unicode Fetch-header rejection and ASCII loss of a Unicode attachment filename. Tests now retain and assert the corrected boundaries. An initial full backend run also found two obsolete URL expectations and four fixture DI omissions; application storage/version modules were supplied without relaxing authorization assertions. The late-I/O fake was corrected to honor zero-length InputStream reads, ensuring it represents failure after admission rather than during preparation.

Packaged GUI dogfood then found a native-form403 that metadata/HTTP tests had missed. Fetch specifies that no-referrer changes a navigate-mode POST Origin to null. The configured CORS policy correctly rejected it. The page now uses strict-origin in both header and meta, sending only the origin in Referer and never the token path/query. Machine responses/assets retain no-referrer. Null and foreign origins remain denied; the CORS allowlist was not broadened. Three production-CORS/PostgreSQL cases and actual browser repetition verify this repair. See https://fetch.spec.whatwg.org/#append-a-request-origin-header .

The first referrer-header repair failed its page assertion because the initial header still selected no-referrer; a shared helper now emits one consistent policy. Real proxy logs then exposed unredacted capabilities. The proxy now redacts the public path and omits query/Referer while retaining method/status/byte count; public upstream diagnostics cannot echo the raw request. The isolated proxy test initially exposed that a nested location needs its own proxy_pass; the corrected production config passes all five tests, including a real upstream transport failure. Original failures remain retained.

The installed Chrome-for-Testing155.0.8059.84 failed Karma capture after60seconds and its independent native DOM probe also timed out, reporting a CoreVideo display-link error. This is an observed runtime failure, not a proven root cause. The complete1192-case browser selector then passed with the official same-version chrome-headless-shell, with provenance/archive hashes retained. No browser case was filtered, CI gate skipped or product launcher altered.

## Packaged local dogfooding and preservation

The fresh043b544 campaign completes42 real HTTP requests: typed anonymous information, missing/wrong/Unicode password, zero metadata count, binary and empty downloads, eight contenders for one allowance (one200/seven410), expiry, owner revocation, wrong token case and rejection of a capability as a general JWT. The browser campaign on that package verifies missing/wrong/correct password, plain/protected native downloads, exact byte hashes and Unicode filename, exhausted cap, expired/revoked/missing link and recovery to a valid link.

After proxy repair,03aa855 repeats actual plain and Unicode-protected native downloads and cap exhaustion. Both saved21-byte attachments match the source digest. Real backend/proxy logs contain no fixture capabilities or passwords; proxy access statuses remain redacted and present. PostgreSQL verifies the protected count is exactly1. The separate five-test Nginx campaign also covers normalized encoding, query/Referer markers and an upstream transport failure; it runs in about2seconds locally in the existing CI contract job without an application/Docker build.

The actual package runs read-only with the existing nonroot Java25 backend image, pinned Nginx/PostgreSQL images and loopback ports8080/18086. Compilation/tests use Java17. Mounted backend/web manifests and Nginx configuration are checked against generated files. PostgreSQL is backed up before replacing backend/frontend; its container and named volumes remain. Every pre-existing storage/plugin byte hash is preserved. Only disposable fixture files/links were added; expiry of one new link was explicitly seeded in SQL, rather than claimed as a user-facing instant-expiry feature. No account role was broadened. Current package and paired previous runnable rollback are retained; reverting an executable restores its known defects and needs operator review.

This certifies the scoped local file/public-link path and its recorded denial/recovery cases. It does not certify external S3, folder grants, shared-with-me/inheritance, mobile/native desktop GUI or all product functionality. Internal/durable grants remain product work; canonical version recovery is #109, complete collaboration #112, trusted-master setup #111 and pagination #91. The wider sharing capability remains partial under its As-built baseline catalog status. No master promotion, deployment, tag or release is implied by local evidence.

## Protected adoption — PR #114

The maintainer authorized the administrative merge of exact head4e6d707825a6f13a6765b804e005a4e3e5938a07. [PR#114](https://github.com/vypdev/vaultstadio/pull/114) merged into developc9f1ac505c09c03d6c3eec9d1f4f940e26bdbce0 on2026-10-27. Both protection snapshots are identical before/after. The automatic [post-merge CI40493531498](https://github.com/vypdev/vaultstadio/actions/runs/40493531498) passes2981backend+1316desktop+1192browser=5489 executions with zero failures/errors/skips across1079XML. Its checkoutc090d421765a347f4ddaad14d18b2cf078f788a3 has parents master615f11a/developc9f1ac5 and retains the verified tree9a49fbe1874e2539819eed33aa6fb56ff65943c7. Issue#90 adoption is complete; earlier pending-adoption text is historical. Master remains615f11a, VERIFICATION_SETUP=false, and#91/full portfolio acceptance remain separate work.
