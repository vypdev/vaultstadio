# Web directory selection verification — issue #121

The [pre-code inventory](folder-selection-121-plan.json) records 68 risk families and nine rendered journeys. SDD-0005 section6.5 and SDD-0013 section6.8 were committed before their implementations in `b23219b` and the subsequent UI refinement in `a06f27d`. [Machine results](folder-selection-121.json) and [separate GUI evidence](folder-selection-121-gui.json) retain exact sources and scope. This is a local verification checkpoint; CI, review, native-dialog acceptance and adoption remain open.

## Behavior and ownership

Web Upload folder previously opened an ordinary multiple-file input and replaced relative paths with basenames. It now requests a real directory-capable input, reads public `File.webkitRelativePath`, and preserves the selected root and represented nested paths. Unsupported browsers produce an explicit capability result. Empty files are valid; the file-list API cannot enumerate empty directories, so the menu and empty-result feedback disclose their omission.

The domain defines neutral file metadata, a cancellable source port and the selection policy. Before any file body read or remote mutation, it rejects absolute/ambiguous/navigation/control/drive-prefixed paths, basename disagreement, duplicates, file/directory collisions and resource violations. Bounds are32MiB aggregate,256 combined file/implicit-directory entries,16 path segments and255 UTF-16 units per segment. Valid Unicode, case and spaces are preserved without normalization.

Preparation reads sources sequentially and checks actual against declared bytes. The application controller owns cancellation, duplicate activation, stale-result disposal, safe localized errors and explicit retry to the original destination. Queueing is reported as queueing; actual transfer completion remains the upload queue's responsibility. DOM inputs, listeners and FileReaders belong to focused browser adapters and are released on completion, failure or cancellation. Ordinary and large-file upload shapes remain intact, with deferred chunk reads.

Production responsibilities were split into small policy, reader, controller, DOM dialog, binary reader, directory adapter and presentation files. `FilePicker.wasmJs.kt` shrinks from282 to51 lines. The domain has no browser, UI, Ktor or filesystem dependency. This increment does not claim that every legacy ViewModel, native picker or unrelated module is fully decomposed.

## Reproduced failures and UI refinement

The original browser regression ran four cases before implementation: two asserted the wrong input mode/flattened path and failed; two binary/empty-file controls passed. Original XML, source inputs and commands remain archived with SHA256 `7baafefb01df4d5da77758107e8e58a8828803ad50707fc5334a1b99249f4797`. Initial new-test typing/import errors were fixture compilation corrections, not production red cases.

Real production-package dogfooding then found the framework's native search input above the upload menu. A separate pre-code refinement specified the correction; a targeted renderer assertion failed before the runtime change, archived as SHA256 `055b249ea88574086df25885e5fbde68fe649b4640a04ca215293bd2e70ec893`. No framework-private DOM state is modified. While upload/sort popups are open, the real background search control is removed and its measured layout space retained. Closing restores the controlled query and an editable input. Folder preparation/error feedback now occupies normal layout above search. Six actual browser renderer cases, four desktop component cases and the separately observed packaged UI cover the refinement, including increased font scale and detached-listener disposal. This does not promise caret position retention across unmounting.

## Complete tests and coverage

Clean production/test source `d18d72c579d8f0dc011cfd9b866a2004a8c30340` passed `make test-coverage` in436.56s: **6,569 executions —3,388 backend,1,635 desktop and1,546 Wasm— with zero failures, errors or skipped cases**. Raw saved JUnit XML was independently recounted. There are119 distinct new methods and183 repeated platform executions over the adopted6,386 baseline. These are distinct from the68 risk families. Existing folder creation, sibling ownership and queue retry tests are reused explicitly in the matrix.

The original5,816-file archive contains source/SDK/command receipts, raw XML, executed production classes, execution data and canonical reports; CRC validation and SHA256 `aed871e09dcbd3cd7670c0e418646acac25bb8d3823cdcd9e65b8b6ee1a4b7e4` were checked. Earlier full6,559 execution evidence belongs to core source `8320a43`, before the UI correction, and is not relabeled as the final result.

| Full compiled population | Covered lines | Covered branches |
|---|---|---|
| Backend,44 modules |9,433 /17,047 (55.34%)|2,140 /6,215 (34.43%)|
| Frontend,52 modules |7,725 /22,089 (34.97%)|1,583 /6,147 (25.75%)|

| Critical source | Lines | Branches |
|---|---|---|
| FolderSelection.kt |33 /33|48 /48|
| FolderSelectionReader.kt |16 /16|6 /6|
| FolderUploadController.kt |61 /61|44 /52|
| FolderUploadCopy.kt |88 /88|11 /11|

Whole-population coverage remains partial. Coroutine/defensive branches keep their actual missed counts; thin composition wiring and legacy desktop picker code remain incomplete under JVM instrumentation. Real Wasm/GUI evidence is separate and does not become JVM JaCoCo coverage. No exclusions, lowered thresholds or fabricated tests were added. Actual browser File/FileReader tests compare every byte at32MiB, two files summing exactly32MiB, and bound+1 refusal before reading.

`make lint`, existing storage authorization/path gates, Instant serializer and input semantics gates pass. Twelve contract groups pass: accessibility/MIME/specification/no-FQN, inventory, browser bootstrap, Codecov keyring, CI cadence, share/selection architecture, verification tooling and managed setup. The exact head's full history scan has zero secret findings; the unchanged17/19 reviewed nonsecrets remain recorded. No workflow, Copilot, branch protection or setup activation is changed.

## Actual application verification and remaining native check

The copied production Wasm package runs against the real local backend/PostgreSQL at `http://127.0.0.1:18090`. Normal UI login uses a disposable USER account and no seeded session. Actual directory operations use the browser FileChooser API over owned on-disk fixtures, not a fabricated FileList. Download buttons produce actual browser events and saved files; bytes and SHA256 are checked after saving. Independent authenticated API reads verify server facts separately.

Eight rendered journeys have bounded passing evidence: normal login; root/nested Unicode/empty-file upload and downloads; disclosed empty directories and no-mutation empty selection; fresh selection after cancellation; ordinary single/multiple round trips; large-picker chunked round trip; unsafe-name refusal followed by explicit valid retry; and popup/search/inline-feedback behavior. All10 files created through the GUI retain their original sizes and SHA256. Saved browser downloads include binary65,793bytes, Unicode42bytes, empty0bytes, ordinary321/22bytes and large10,486,017bytes. The account contains exactly10 files and5 represented folders after those planned operations.

**GUI05 remains partial:** the real application Cancel button ends preparation; subsequently delivering the old chooser's selected tree causes no queue or remote mutation, and a fresh chooser remains usable. A physical OS/Finder dialog Cancel click was not observed. Opening without a chooser event watcher and sending Escape through the tab did not demonstrate OS cancellation. Broad inspection of the Codex window was rejected by automatic review because it could show unrelated private content; it was not performed. Downloads were subsequently verified through the safer browser-only API. This checkpoint does not equate browser API selection or a synthetic cancel event with physical Finder interaction. Keep the PR in draft until that planned acceptance is completed or explicitly resolved by the maintainer.

Two tool calls exceeded their requested time bounds during chooser/download orchestration; those elapsed waits are not application performance measurements. Later split chooser and download calls completed normally. Firefox/Safari, Windows/Linux desktop hardware, mobile runtime and native desktop folder enumeration remain unexecuted here. The existing native folder reader's behavior is not certified by this Web repair.

## Preservation and next steps

All30 preexisting storage payloads, the original PostgreSQL instance and both detached #74 SDD drafts are protected. The private database dump is retained outside Git. The only backend environment change adds the exact local18090 CORS origin, preserving its code/image/mounts and the existing allowlist; an unapproved18091 origin still receives403. Current Web, prior runnable18088 rollback, native package and backend rollback are retained. No database reset, destructive cleanup, master promotion or protection change is part of this increment.

Complete GUI05 native acceptance, verify the final PR's technical CI/original artifacts and resolve actual Copilot findings before adoption. The next functional roadmap priorities remain #116 for configurable Web API origin, then #82/#115 search/filter behavior, with #109/#112/#111 and broader coverage/decomposition retaining their own acceptance. This bounded result does not certify the whole platform as operational.
