# Web API origin verification — issue #116

Local implementation checkpoint: `35ce4076f88f030237a886f628dbb829ed51e21f`, 2026-10-29.
Final PR CI, Copilot review and protected adoption are separate gates. This report does not certify the whole platform or promote master.

## Contract and implementation

The pre-code contract was committed at `4ae1fd8`, extended for private media/exact CORS at `9926d78`, and refined for acknowledged clipboard writes at `0c039ad`. The [38-family plan](api-origin-116-plan.json) remains a plan; [executed traceability](api-origin-116-results.json) identifies its actual evidence.

The operator-owned `/vaultstadio-config.json` is loaded and validated before application/client creation. The shipped `{}` selects the serving origin plus `/api`; an explicit absolute HTTP(S) API root can preserve a reverse-proxy prefix. Loading is bounded by bytes and deadline, refuses redirects and credentials, and uses one immutable startup snapshot. Invalid configuration has safe native error/reload controls. See the [operator runbook](../../operations/WEB_API_CONFIGURATION.md).

Shared data/network adapters preserve the configured authority and API prefix across Ktor requests, generated storage/share/version locations and socket locations. Automatic bearer attachment uses the current token and configured authority; public/version URLs do not acquire account tokens. The inventory records 113 literal `/api/v1` construction sites in 14 production data files, with one production `HttpClient` factory. These are reviewed construction sites, not 113 independently executed endpoint journeys.

The actual Coil transport uses the configured authenticated client and bypasses unscoped image caches. Text preview uses the existing authenticated domain download port. Public GET and rejected-POST pages resolve their assets at both URL depths without widening CSP. Backend CORS retains the configured scheme, host and port and admits the required Cache-Control preflight header.

Real dogfooding found that Copy link previously displayed confirmation without writing a clipboard. Shares presentation now depends on a neutral domain output port; platform adapters perform IO, and only acknowledged completion displays success. Tests cover timeout, denial, failure, retry, duplicate submission, cancellation and stale completion. The browser fallback restores focus/selection and is used only when the asynchronous API is absent.

The legacy 690-line preview file was split into focused components and a domain-backed text loader. The largest changed production Kotlin file is 268 lines. This does not claim that every existing file in the repository meets the size guideline.

## Complete local verification

`make test-coverage` exited 0 on the clean implementation commit, with five successful source-bound receipts and unchanged coverage populations/gates:

| Actual test executions | Passed | Failures / errors / skipped |
| --- | ---: | --- |
| Backend JVM | 3,397 | 0 / 0 / 0 |
| Frontend desktop JVM | 1,729 | 0 / 0 / 0 |
| Frontend Wasm browser | 1,636 | 0 / 0 / 0 |
| Total product executions | **6,762** | **0 / 0 / 0** |

The common tests execute on both frontend targets; the total is executions, not distinct behavioral requirements. Separately, all 54 Node public-page cases and the 12 contributor/tooling contract groups (18 commands) pass. `make lint` exits 0. Official frontend detekt leaves many common source sets NO-SOURCE; the additional local selected-source inspection is not a claim that the official task covers them all. Exact-file formatting avoids changing unrelated existing tests.

| Unfiltered JVM production population | Classes | Lines covered / total | Branches covered / total |
| --- | ---: | ---: | ---: |
| Backend, 44 projects | 2,196 | 9,446 / 17,060 | 2,146 / 6,221 |
| Frontend, 52 projects | 1,902 | 8,159 / 22,453 | 1,866 / 6,451 |

These global percentages remain approximately 55.37% / 36.34% line coverage; passing gates is not maximum platform coverage. Critical new sources have more focused evidence:

| Source | Lines covered / total | Branches covered / total |
| --- | ---: | ---: |
| ApiRootSyntax | 46 / 46 | 123 / 132 |
| WebConfigurationShape | 22 / 22 | 24 / 26 |
| WebApiConfiguration | 14 / 15 | 27 / 28 |
| ApiEndpoint | 22 / 23 | 24 / 28 |
| ApiEndpointPlugin | 21 / 21 | 8 / 8 |
| WebStartup | 11 / 11 | 4 / 4 |
| PrivateImageLoader | 5 / 5 | — |
| TextPreviewLoader | 12 / 12 | 2 / 2 |
| DirectShareLinkClipboard | 11 / 11 | — |
| SharesViewModel | 76 / 80 | 20 / 26 |

Wasm reader/clipboard/DOM assertions are actual browser tests, not JVM coverage measurements. The desktop AWT clipboard adapter has no measured positive native execution in this campaign. Android and iOS simulator-target compilation both execute successfully; compilation does not certify device clipboard or transfer interactions.

## Real application evidence

One seven-file production Web package from `35ce407` was served unchanged on two independent origins:

| Profile | Web origin | Effective API |
| --- | --- | --- |
| A: shipped `{}` | `http://127.0.0.1:18116` | same origin `/api` |
| B: explicit override | `http://localhost:18117` | `http://127.0.0.1:18117/gateway/api` |

Profile B deliberately rejects root `/api`, so silently dropping the prefix cannot pass. Both profiles use backend package `ccff15a`; its entire backend Git tree is identical to `35ce407` (`e458727b9ce8a51c8f5257aeaba21a8cc731cfdc`). The database and storage are the existing local dogfood environment, with a disposable account and synthetic files only.

- Normal UI login, file listing, upload and actual saved downloads pass. A fresh final-package A upload/download preserves 28 bytes; binary A preserves 2,048 bytes; authenticated B preserves 611 bytes.
- Actual directory selection on B uploads all three nested files, including a zero-byte file. Native browser ZIP saves on A and B contain the same five expected file members, with valid CRCs and exact original byte hashes.
- The actual copied clipboard value equals the displayed prefixed public link. No previous clipboard contents were retained or published.
- Anonymous public downloads work; password-protected downloads on both origins preserve the expected 611 bytes. Wrong-password UI feedback and successful correction are verified on B. Protected fixtures are created through normal API login/setup because the current owner UI does not expose those constraint fields; that setup is not owner-UI acceptance.
- Consuming the last allowance through an explicit HTTP fixture after UI metadata load makes both real browser forms reject the POST. The deeper download pages retain styling, scripts and working metadata reattempts. Separate HTTP checks verify wrong-password POST 403, all four GET/POST-relative asset fetches, CSP/referrer policy and actual allowed/denied CORS preflights.
- Mutating B's operator file after startup does not change the live application's API. Reloading with the invalid file shows the safe error and makes **zero additional backend requests**. Restoring the file in place and using native Reload recovers the authenticated application.
- The real `:composeApp:wasmJsBrowserDevelopmentRun` entrypoint also passes normal UI login/listing through the repository's `/api` proxy. An external test init changes only the loopback binding, port and automatic browser opening. Two incorrect initial test-init attempts are retained; the working long-running server was intentionally stopped after the smoke, without claiming a finite successful Gradle exit.

Chrome's extension could not assign upload paths while its file-URL access was disabled; the in-app browser completed the normal chooser flow. No browser security setting was changed. Offscreen Log out initially scrolled without activation; a fresh visual coordinate completed logout. Neither observation is presented as an application defect.

## Retained originals and preservation

The local audit evidence contains raw XML, execution data, class files, coverage XML, input hashes, commands and representative screenshots. Archives were checked for CRC integrity:

| Evidence under `audit/evidence/` | SHA-256 |
| --- | --- |
| `issue116-full-35ce407/originals.zip` — 5,932 files | `6b11659b31d639708d80de3f0f0d181fdd8c180b545a55e433fe7e9bb3ce0ef7` |
| `issue116-runtime-supplement-35ce407/originals.zip` — lint, dev-server and preservation | `7daf1e8cf0328b824faf5fb1958b1259b120c6b3a473d6f17a14793e88fcb80f` |
| `issue116-full-ccff15a/originals.zip` — earlier complete checkpoint and lock reconciliation | `af3fe7a4df656977826aff01c8426b7b7bec8b092ef015b725dfa5f9fcfd4274` |
| `issue116-initial-red-4ae1fd8/originals.zip` | `9df4cc07a3eae54947ea37b169096a7c02572b0b88ee22585c625d3a29c3b77b` |
| `issue116-cors-red-9926d78/originals.zip` — original scheme/header failures | `a3b12b2ee96852dcf33948cdc28abf40539c919b8804a44517c10ee6a7d218a4` |
| `issue116-clipboard-fallback-red-0c039ad/originals.zip` — two genuine selection-restoration failures subsequently fixed | `876a9b6fd10e5cc6aade61c6daad2eb27cca25f32db0c5f9bd8da8e142eec6a9` |

The five final receipts agree on the clean source commit. The initial ccff15a concurrent lint/coverage lock failure remains historical evidence; this final campaign runs the exclusive stages sequentially. The initial Wasm class-name filter that selected no cases is not counted as passing coverage.

All seven original containers/images and four volumes remain present. All 43 preexisting non-database file paths and bytes are unchanged; six synthetic files were added. The existing database has normal runtime/session/test-fixture changes, not cold-file identity. Its pre-candidate logical backup is retained privately (SHA-256 `1b69282afc9333c6b8fceb88dc5b860ee3e319b37614d8cfb51cd5d9ccbde70d`). Both original detached SDD drafts retain their exact hashes. No reset, restore, drop, force-push or prune was used.

Full-history gitleaks and trufflehog report no unreviewed secret findings, retaining the existing reviewed non-secret matches. Credentials, bearer tokens, raw capability URLs, database dumps and runtime environment files stay outside Git and public reports.

## Remaining boundaries

Historical-version URLs currently lead to metadata, not historical binary delivery (#109). Collaboration protocol interoperability remains #112. The current file-screen preview action is a no-op; audio/video/PDF interaction and the full-body buffering behind the text display cap remain separate gaps. Android/iOS transfer support and actual native clipboard/device journeys are not certified here. Full platform readiness, federation, the accepted 100-GB/10-million-file targets and the broader product roadmap need their own implementation and evidence.

The official Copilot branch synchronization failed before publication on upstream `evidence-shape` compatibility ([vypdev/copilot#779](https://github.com/vypdev/copilot/issues/779)). Its exact fast-forward recovery was explicitly authorized and recorded. The installed catalog, Copilot, protections, workflows, master and `VERIFICATION_SETUP=false` are unchanged. Final-ref GitHub checks/review and an exact protected merge decision remain pending until recorded separately.
