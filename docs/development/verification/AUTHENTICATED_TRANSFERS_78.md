# Authenticated transfer acceptance — issue #78

The [pre-code risk inventory](authenticated-transfers-78-plan.json) traces 72 risk families to executed behavioral tests and separately recorded [rendered application evidence](authenticated-transfers-78-gui.json). Implementation starts after the Action-created branch and SDD commit `379f3b803e26ed0e94902d4ead686998c76aa694`. Local verification, actual GUI observations, final-ref PR CI, Copilot review and maintainer adoption have separate evidence; none substitutes for another.

## Behavior and architecture

The client previously requested an unregistered item-download route, and the rendered file actions did not complete an authenticated save journey. It now sends bearer headers to `GET /api/v1/storage/download/{itemId}` and JSON selections to the existing `POST /api/v1/storage/batch/download-zip`. Tokens are read per request and never embedded in download, preview or thumbnail URLs. Ordinary API prefixes are canonicalized once and identifiers remain encoded path segments. Anonymous capability-share authorization retains its own existing real HTTP/SQL/stream regression suite.

`FilesDownloadController` owns fresh metadata, bounded bytes, cancellation, duplicate dispatch prevention, explicit retry and screen disposal through domain ports. Details, grid, list, selected-file and folder actions use that operation. Browser handoff and actual native disk publication have distinct outcomes. Failed HTTP bodies never become saved files. Localized safe messages hide raw provider diagnostics in all seven maintained locales.

Archive policy has moved from the route into an application use case and `ArchiveSelection`; the core defines its writer port and infrastructure implements real bounded ZIP encoding. The complete metadata selection is checked for ownership, safe paths, cycles, repeated/stalled pages, ambiguous roots and resource bounds before content opens. Each subsequently opened file is checked against its canonical owner/version/name/parent/storage key/size/checksum; changed facts close the stream and return 409. A foreign, missing, changed or failed item cannot silently produce a partial successful ZIP. All ZIP bytes exist within the bound before HTTP 200.

The response scope reads original binary chunks through a public typed Ktor body plugin, checking actual bytes as well as Content-Length, truncation and cancellation. The previous suite caught an introduced empty-body cancellation regression, which was repaired without weakening its assertions. Real Wasm fetch failure exposed a second issue that MockEngine had not represented: Ktor wraps its public `JsError` in a non-Exception error. Platform-specific classification now maps that exact transport failure while preserving caller cancellation and fatal/unknown errors. Five actual default-browser-engine regressions cover metadata, file, archive, multipart and chunk calls.

Upload ownership is split into focused processing and task collaborators. Empty files are valid; unreadable or size-contradictory content is visible failure. A failed/cancelled upload cannot emit completion. Retry retains the original bytes, name, MIME and target parent even after navigation; stale callbacks cannot change a newer attempt. Chunk count/size agreement is checked, one upload can be cancelled independently, and known server sessions get bounded best-effort cleanup. A failed folder creation cannot redirect content into an unintended partial parent.

Desktop saving uses an event-queue chooser and same-directory temporary file, atomic publication and owned temporary cleanup. The foreign callback boundary completes the suspended caller on ordinary dialog failures, including unexpected exceptions and disposal failures; fatal errors are not converted into network/save success. Browser saving creates actual binary Blobs, removes temporary anchors and revokes object URLs after a bounded handoff, with disposal/failure tests. Native chooser callbacks are additionally tested on the actual AWT event queue, including a modal cancellation loop. Existing profile-export callbacks still produce valid JSON through the real Web download and native Save File dialogs.

Domain policy contains no UI, Ktor, AWT or filesystem dependency. Platform details remain in adapters; the route and composition host remain thin. Existing large files are not certified as fully decomposed: this increment extracts transfer responsibility rather than expanding `FilesViewModel` into a new coordinator for everything.

## Verification

The clean implementation `ae5f22a8eef9de54b357ba429765c432fd84775e` passed **6,386 product executions** in 506.33 seconds: **3,388 backend, 1,563 desktop and 1,435 Wasm**, with zero failures, errors and skipped cases. This is 295 additional platform executions over the adopted #119 population. There are 36 meaningful operation cases per JVM/Wasm and 23 direct archive path-policy cases. See [machine results](authenticated-transfers-78.json).

| Complete compiled population | Lines | Branches |
|---|---|---|
| backend (44 modules) | 9,433 / 17,047 (55.34%) | 2,140 / 6,215 (34.43%) |
| frontend (52 modules) | 7,488 / 21,812 (34.33%) | 1,454 / 5,972 (24.35%) |

| Critical source, including generated/nested classes | Lines | Branches |
|---|---|---|
| BoundedZipArchiveWriter.kt | 68 / 68 (100.00%) | 31 / 32 (96.88%) |
| ArchiveDownload.kt | 16 / 16 (100.00%) | 36 / 50 (72.00%) |
| ArchiveDownloadRoutes.kt | 15 / 15 (100.00%) | 3 / 9 (33.33%) |
| ArchiveSelection.kt | 53 / 53 (100.00%) | 54 / 56 (96.43%) |
| DownloadArchiveUseCase.kt | 25 / 25 (100.00%) | 20 / 23 (86.96%) |
| TransportCancellation.kt | 13 / 13 (100.00%) | 16 / 20 (80.00%) |
| StorageDownloadApi.kt | 54 / 56 (96.43%) | 22 / 26 (84.62%) |
| DesktopDownloadSaver.kt | 29 / 31 (93.55%) | 16 / 18 (88.89%) |
| DesktopDownloadDialog.kt | 12 / 23 (52.17%) | 5 / 12 (41.67%) |
| DownloadArchiveUseCaseImpl.kt | 3 / 3 (100.00%) | — |
| UploadWork.kt | 17 / 17 (100.00%) | 3 / 4 (75.00%) |
| UploadManager.kt | 196 / 200 (98.00%) | 92 / 114 (80.70%) |
| UploadEntryProcessor.kt | 49 / 49 (100.00%) | 27 / 36 (75.00%) |
| FilesDownloadController.kt | 94 / 94 (100.00%) | 83 / 118 (70.34%) |

The critical `isSafeArchiveSegment` method independently covers **5 / 5 (100.00%) lines and 22 / 22 (100.00%) branches** through ordinary tests. The same source file also contains generated model paths, whose real missed counts remain included. Root/source-file line counters are consolidated; class line counters must not be summed. Raw branch counters include coroutine/default/generated code. Actual native chooser GUI observations are separate from unit instrumentation. The defensive zero-read/yield branch, composition factories and remaining legacy modules retain their missed counts.

Original clean command, receipts, JUnit XML, executed class/JaCoCo inputs and canonical reports are retained in the final evidence archive, SHA256 `928bbd1e8cfa9ad44dc5c333120c7966a509a71ce2519a756dfe7169f25ce9b5`. The [machine result](authenticated-transfers-78.json) retains earlier positive, red and fixture diagnostic hashes.

All applicable backend and desktop compiled module outputs, including untested code, remain in the canonical JVM population. Common tests on JVM and Wasm count as repeated platform executions, not distinct requirements. Mobile-specific compilation/runtime and hardware acceptance remain outside this certificate. No coverage exclusions, lowered gates, scanner suppression or workflow-cadence changes were introduced.

The unchanged Python verifier source passed its 132 controls with the exact CI coverage procedure on `4a725d4`; final-ref CI executes those tools again. Existing storage, Instant serializer and input-semantics coverage gates, source/SDD/accessibility, architecture, public-share Node/Nginx, setup/bootstrap and cadence controls are recorded independently. Actual `make lint` preserves existing advisory findings; any new advice from this change is resolved or specifically explained at its foreign callback boundary. Actionlint 1.7.12 rejects exactly three unchanged generated Copilot `queue: max` fields; this is [valid GitHub Actions syntax](https://docs.github.com/en/actions/how-tos/write-workflows/choose-when-workflows-run/control-workflow-concurrency). Its nonzero result remains disclosed, without changing Copilot or suppressing a workflow check.

## Real application acceptance

The candidate used the real backend and migrated local PostgreSQL, synthetic account data, actual file pickers, actual download events and native saved files. Sign-in used normal forms, without seeded tokens. Native fields were filled autonomously using explicit focus and keyboard input; earlier AX `setValue` failures are not represented as successful input. Test-only package identity and an unseeded in-memory PreferencesFactory isolate desktop preferences; production JARs retain their built bytes.

| Rendered journey | Observed result |
|---|---|
| Normal Web login | Actual sign-out/sign-in and loaded files |
| Binary upload/details download | 65,537 original bytes, SHA256 `237356e18b503616912abb8ffaed3a72591e397d4ac294c4637917d48a3f529d` |
| Unicode and empty files | Original Unicode name and 46-byte UTF-8 payload; genuine zero-byte file |
| Grid and list menus | Actual menu actions produce the same binary hash; grid opened by long press |
| Selection and folder ZIP | Three exact selected file entries; six recursive entries including empty directory; extracted bytes and paths checked |
| Network failure and retry | Visible safe failure, no file on failure, explicit retry returns the original hash |
| In-progress cancellation/repeated click | Explicit first-byte-held body; cancellation produces no download or stale completion; retry succeeds |
| Failed upload/retry after navigation | Local synthetic 503, failed 0/1 status, then retry completes to the original parent and returns the exact 129-byte hash |
| Native login, save and cancel | Actual login, Unicode Save File result, normal selector cancellation with no file or temporary |
| Native upload/download | Actual Open File selector, list refresh, completed status and 321-byte saved file with SHA256 `9a3f2af84b24c845767ceb07914fb4a53a1c62a351f1d8cd393f51067181f237` |
| Legacy profile export | Valid JSON from actual Web download and native Save File; private synthetic account contents retained outside Git |

Positive journeys UI02–08 were initially observed on `a207b1356f6a43c2fb3f25f432639551fa92f4c6`; new normal login, real fetch/cancellation/retry, failed-upload retry and native journeys were observed on `4a725d4464864eea65311aac76b9d5d6cdb48975`. These sources remain explicit in the receipts rather than being relabeled as observations of a later commit. The final dialog correction was separately observed on `4128d0323ab8d81de537e846553dd77dbe3a6b1e`: autonomous normal login, real Save File publication of the same 321-byte binary, and selector cancellation preserving that existing destination with no new files or temporary parts (UI15–16).

The initial absent-origin CORS login failure was a local fixture configuration omission. One initial archive 404 came from a test fixture that omitted the registered batch route. Both are kept separate from reproduced production defects. Nginx `limit_rate` did not actually hold the response and is not counted as cancellation evidence; the replacement held-body fixture was independently timed before the real cancellation. Temporary fault proxies are removed and the original direct backend port is restored.

## Review and CI refinements

Copilot identified a real drive-prefix archive gap after the earlier complete acceptance snapshot. Four real root/nested file/folder creation-and-archive HTTP controls returned ZIP 200 before repair; direct policy and writer controls independently failed, for 14 preserved red failures. The shared archive segment policy now rejects `:` before preflight/content opening and again at the ZIP writer. Direct policy cases also cover ordinary Unicode, empty/navigation segments, both separators, control characters and exact 255/first 256 length. This establishes the stated drive-prefix/path invariant, not every Windows filename or extractor behavior. No vulnerable Windows extraction or Windows hardware execution was performed.

A separate original CI artifact records the unchanged maximum 32 MiB byte-equality case failing only on Mocha's outer 2,000 ms timer. The storage module now uses the same finite 60,000 ms Karma mechanism already used by composeApp. The full-size assertion, payload, browser cases and production transport deadlines are unchanged. Both the direct storage suite and complete verification pass; this is a harness budget repair, not evidence of an application byte mismatch or a lower supported size.

The updated live backend uses source `cf7812b`, with identical production code to the final test-only refinement. Through the actual Web UI, an owned folder `C:` was created, selected alone and refused with “Nothing was saved”; the filesystem contained no new ZIP. A negative event-watcher timeout reset the CUA tool, so its promise result is explicitly not counted. The visible refusal and independent filesystem check remain recorded. Subsequent actual binary download returned the same 65,537 bytes/hash, and a valid recursive folder ZIP contained all six original paths/bytes including an empty directory. Current client and server sources remain distinct in the GUI receipts. PostgreSQL, all 30 existing storage files and original 22 hashes were preserved when replacing only the backend package.

The maintainer confirmed that Copilot's temporary develop→master→develop retargeting is intentional issue/PR linking. [copilot#759](https://github.com/vypdev/copilot/issues/759) requests installation-generated agent guidance or investigation of equivalent native linkage without retargeting; it does not call the intended linking step a defect. [copilot#760](https://github.com/vypdev/copilot/issues/760) separately reports supplemental PNG evidence handling and actionable unsupported-path diagnostics. Neither report changes Copilot or grants merge authority.

## Bounds, preservation and remaining work

The client supports at most **32 MiB per file/ZIP** and buffers bounded chunks plus final bytes; peak allocation exceeds the payload length and this is not unlimited streaming. Archives permit **24 MiB aggregate input, 32 MiB encoded output, 256 entries including directories and 16 folder levels**. They fail visibly at these bounds. Metadata revalidation is not a global filesystem transaction or arbitrary same-size provider-corruption detector. Chunk cleanup is best effort rather than a server rollback/expiry guarantee. Android/iOS return an explicit unsupported save capability; neither is certified here. Browser handoff does not promise that the operating system persisted a file.

Original data/plugins hashes, database identity and backup, original detached SDD drafts and Git history are protected. Normal commits/pushes preserve the official branch. The current runnable package and immediate rollback have named consumers. Master, branch protections, Copilot and `VERIFICATION_SETUP=false` remain unchanged. Full-history Gitleaks/TruffleHog results retain the existing reviewed non-secret matches and require zero new findings.

1. Finish final-ref PR CI and Copilot review, then obtain any required maintainer approval before adoption. A local successful suite cannot bypass protected approval.
2. [#121](https://github.com/vypdev/vaultstadio/issues/121): implement an actual Web directory picker and hierarchy contract. The existing Wasm picker selects ordinary files and fabricates flat relative paths. In #78 it produces a truthful unreadable-entry failure instead of a fake empty upload; whole-directory selection is not certified. The tested recursive ZIP uses folders created through the real UI and ordinary file uploads.
3. [#82](https://github.com/vypdev/vaultstadio/issues/82)/[#115](https://github.com/vypdev/vaultstadio/issues/115): actual search/filter/pagination and response ownership; [#116](https://github.com/vypdev/vaultstadio/issues/116): reviewed configurable Web API origin. The candidate still uses the existing loopback origin.
4. [#109](https://github.com/vypdev/vaultstadio/issues/109)/[#112](https://github.com/vypdev/vaultstadio/issues/112)/[#111](https://github.com/vypdev/vaultstadio/issues/111): recovery, full collaboration and trusted setup activation retain their separate acceptance. Continue measured critical service/adapter coverage and decomposition using the [platform audit roadmap](../AUDIT_ROADMAP_2026-10-25.md), then certify mobile and other desktop systems on their actual runtimes.

This certificate covers the stated authenticated transfer scope and observed macOS/Web paths; it does not certify every platform feature or production deployment.

![Final native candidate after actual Save File, matching 321-byte payload](https://github.com/vypdev/vaultstadio/blob/5321a3b6596b2b73cb4a1cd6b5208c76d59edf2b/docs/development/verification/assets/transfer78-native-save.png?raw=true)

The original screenshot remains in the published evidence commit `5321a3b`, linked above and retained in local original observations. It is outside the final text diff because GitHub omits its binary patch and Copilot progress reports `validation.evidence-unavailable`; evidence is preserved rather than discarded.
