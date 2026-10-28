# Web directory selection verification — issue #121

The [pre-code inventory](folder-selection-121-plan.json) records 76 risk families and nine rendered journeys. SDD-0005 section6.5 and SDD-0013 section6.8 were committed before their implementations in `b23219b` and the subsequent UI refinement in `a06f27d`; the action-specific failure-copy contract preceded its implementation in `21415fc`. [Machine results](folder-selection-121.json) and [separate GUI evidence](folder-selection-121-gui.json) retain exact sources and scope. This is a local verification checkpoint. The native acceptance follow-up closes the earlier draft gate; the review refinements below require fresh final-ref CI/review and explicit adoption.

## Behavior and ownership

Web Upload folder previously opened an ordinary multiple-file input and replaced relative paths with basenames. It now requests a real directory-capable input, reads public `File.webkitRelativePath`, and preserves the selected root and represented nested paths. Unsupported browsers produce an explicit capability result. Empty files are valid; the file-list API cannot enumerate empty directories, so the menu and empty-result feedback disclose their omission.

The domain defines neutral file metadata, a cancellable source port and the selection policy. Before any file body read or remote mutation, it rejects absolute/ambiguous/navigation/control/drive-prefixed paths, basename disagreement, duplicates, file/directory collisions and resource violations. Bounds are32MiB aggregate,256 combined file/implicit-directory entries,16 path segments and255 UTF-16 units per segment. Valid Unicode, case and spaces are preserved without normalization.

Preparation reads sources sequentially and checks actual against declared bytes. The application controller owns cancellation, duplicate activation, stale-result disposal, safe localized errors and explicit retry to the original destination. Queueing is reported as queueing; actual transfer completion remains the upload queue's responsibility. DOM inputs, listeners and FileReaders belong to focused browser adapters and are released on completion, failure or cancellation. Ordinary and large-file upload shapes remain intact, with deferred chunk reads.

Production responsibilities were split into small policy, reader, controller, DOM dialog, binary reader, directory adapter and presentation files. `FilePicker.wasmJs.kt` shrinks from282 to51 lines. The domain has no browser, UI, Ktor or filesystem dependency. This increment does not claim that every legacy ViewModel, native picker or unrelated module is fully decomposed.

## Reproduced failures and UI refinement

The original browser regression ran four cases before implementation: two asserted the wrong input mode/flattened path and failed; two binary/empty-file controls passed. Original XML, source inputs and commands remain archived with SHA256 `7baafefb01df4d5da77758107e8e58a8828803ad50707fc5334a1b99249f4797`. Initial new-test typing/import errors were fixture compilation corrections, not production red cases.

Real production-package dogfooding then found the framework's native search input above the upload menu. A separate pre-code refinement specified the correction; a targeted renderer assertion failed before the runtime change, archived as SHA256 `055b249ea88574086df25885e5fbde68fe649b4640a04ca215293bd2e70ec893`. No framework-private DOM state is modified. While upload/sort popups are open, the real background search control is removed and its measured layout space retained. Closing restores the controlled query and an editable input. Folder preparation/error feedback now occupies normal layout above search. Six actual browser renderer cases, four desktop component cases and the separately observed packaged UI cover the refinement, including increased font scale and detached-listener disposal. This does not promise caret position retention across unmounting.

## Copilot review refinements

The actual review of `a23287c` raised three findings. The status now explicitly names **Chrome on macOS**. File/large-file picker failures now retain their originating action and use a pure, localized file-selection mapping across all seven maintained languages; folder capability and preparation-budget text remain specific to folders. Eight additional semantic tests ran red first:16 platform executions,14 assertion failures and2 unchanged-folder controls (original SHA256 `3cc5ff10b0e78ced50457b5f23717051cd03ba08a2c1e74b1563af407db0f79a`). The focused green run passes35 desktop/browser cases (original SHA256 `1e091f343dabea065058d22d930929b8fce15eb49c9d64e4fbd84e92487a53d4`).

The raw-JavaScript parameter finding is refuted by the generated Kotlin2.4.20 import module, whose functions declare `(empty)`, `(size,count)`, `(size)` and `(file,size)` as formal arguments. All15 affected actual browser cases pass on analyzed source `a23287c`, including full32MiB reads. The [official Kotlin/Wasm contract](https://kotlinlang.org/docs/wasm-js-interop.html#kotlin-functions-with-javascript-code) supports package-level parameterized `= js(...)` bridges. Generated module, Wasm, sources, original XML and command receipts are archived (SHA256 `f64696ff46d1c0fcb34f21d1464b5ed3aca41851313e451a5cdb3612f345fc73`); [upstream issue764](https://github.com/vypdev/copilot/issues/764) and the PR reply contain the evidence. Valid fixtures are retained; the final review state is verified separately.

After packaging `4d3fb74`, an additional actual UI journey selected a four-byte owned file with mode000 through ordinary Upload files. The real FileReader refusal displayed “The selected files could not be read. Nothing was added. Select the files again.” The before/after API listings are identical:13 files,8 folders, including IDs and parent relations. No queue item or upload was created. The fixture was restored to0600. This uses the production package and an on-disk failure, with no injected error/session/application state; it supplements the nine earlier directory/native journeys rather than changing their source attribution.

## Complete tests and coverage

Clean production/test source `4d3fb740f3ae093135159a7d0c06752f136f0c38` passed `make test-coverage` in478.60s: **6,585 executions —3,388 backend,1,643 desktop and1,554 Wasm— with zero failures, errors or skipped cases**. All1,196 raw saved JUnit XML reports were independently recounted. There are127 distinct new methods and199 repeated platform executions over the adopted6,386 baseline. These are distinct from the76 risk families. Existing folder creation, sibling ownership and queue retry tests are reused explicitly in the matrix.

The original5,821-file archive contains source/SDK/command receipts, raw XML, executed production classes, execution data and canonical reports; CRC validation and SHA256 `560facd5c0369556a6064ffd69d55b3de9e3d24e938a10483cc6cf5bcdf02d83` were checked. Earlier full6,559 execution evidence belongs to core source `8320a43`; the6,569 campaign and original native journeys belong to `d18d72c`. Their sources remain explicit.

| Full compiled population | Covered lines | Covered branches |
|---|---|---|
| Backend,44 modules |9,433 /17,047 (55.34%)|2,140 /6,215 (34.43%)|
| Frontend,52 modules |7,766 /22,129 (35.09%)|1,595 /6,159 (25.90%)|

| Critical source | Lines | Branches |
|---|---|---|
| FolderSelection.kt |33 /33|48 /48|
| FolderSelectionReader.kt |16 /16|6 /6|
| FolderUploadController.kt |61 /61|44 /52|
| FolderUploadCopy.kt |88 /88|11 /11|
| UploadSelectionProblemCopy.kt |38 /38|12 /12|

Whole-population coverage remains partial. Coroutine/defensive branches keep their actual missed counts; thin composition wiring and legacy desktop picker code remain incomplete under JVM instrumentation. Real Wasm/GUI evidence is separate and does not become JVM JaCoCo coverage. No exclusions, lowered thresholds or fabricated tests were added. Actual browser File/FileReader tests compare every byte at32MiB, two files summing exactly32MiB, and bound+1 refusal before reading.

`make lint`, existing storage authorization/path gates, Instant serializer and input semantics gates pass. Twelve contract groups pass: accessibility/MIME/specification/no-FQN, inventory, browser bootstrap, Codecov keyring, CI cadence, share/selection architecture, verification tooling and managed setup. The exact head's full history scan has zero secret findings; the unchanged17/19 reviewed nonsecrets remain recorded. No workflow, Copilot, branch protection or setup activation is changed.

## Actual application verification and native acceptance

The copied production Wasm package from `4d3fb74` runs against the real local backend/PostgreSQL at `http://127.0.0.1:18090`. Normal UI login uses a disposable USER account and no seeded session. Actual directory operations use the browser FileChooser API over owned on-disk fixtures, not a fabricated FileList. Download buttons produce actual browser events and saved files; bytes and SHA256 are checked after saving. Independent authenticated API reads verify server facts separately.

All nine rendered journeys have bounded passing evidence, including the separate native cancellation below: normal login; root/nested Unicode/empty-file upload and downloads; disclosed empty directories and no-mutation empty selection; fresh selection after cancellation; ordinary single/multiple round trips; large-picker chunked round trip; unsafe-name refusal followed by explicit valid retry; and popup/search/inline-feedback behavior. All13 files created through the GUI retain their original sizes and SHA256. Saved browser downloads include binary65,793bytes, Unicode42bytes, empty0bytes, ordinary321/22bytes and large10,486,017bytes. The account contains exactly13 files and8 represented folders after those planned operations.

**GUI05 native acceptance is complete.** In an explicitly authorized one-tab Chrome window, the assistant opened the physical macOS Open panel from the native application menu and navigated to the owned tree through Go to Folder. Native application Escape closed that selector. The production page returned from Preparing folder to No files selected; an independent read confirmed exactly the same10 files,5 folders, IDs and parent relations as before cancellation. Earlier application-Cancel/stale-result checks remain separate evidence.

The positive physical journey then selected the actual tree directory row, clicked the OS Upload button and the native Chrome confirmation for exactly3 files. The production queue completed3/3 in the new native121 destination. Listing and authenticated downloads verify root/nested Unicode/empty-file content, and all13 accumulated GUI payloads match their original byte counts and hashes. A Chrome details download from this physically selected tree saved the65,793-byte binary with the original SHA256. Its event watcher timed out despite the actual file being saved; that watcher failure is retained and is not represented as a successful event or a product byte failure.

The initial host/shared-window inspection was blocked because it could reveal unrelated content. The user authorized the exact test window/dialog and moving only the owned test tab into an isolated Chrome window. Native source observations and selected-tree/three-file-confirmation AX originals are retained separately from browser-page PNGs. Native ScreenCaptureKit capture failed with an audio/video stream error, so no native-dialog PNG is claimed. An unintended foreground-window diagnostic was immediately discarded and was never published or archived.

Two tool calls exceeded their requested time bounds during chooser/download orchestration; those elapsed waits are not application performance measurements. Later split chooser and download calls completed normally. Firefox/Safari, Windows/Linux desktop hardware, mobile runtime and native desktop folder enumeration remain unexecuted here. The existing native folder reader's behavior is not certified by this Web repair.

## GitHub CI checkpoint

Documentation head `0df291f` passed [CI40725080774](https://github.com/vypdev/vaultstadio/actions/runs/40725080774), including Codecov. All five original artifact ZIP digests and CRCs were verified, and1,194 saved XML reports independently reproduce3,388backend/1,635desktop/1,546Wasm executions with zero failures/errors/skips. Canonical reports retain all44/52 compiled modules and match the exact expected Git tree. Copilot Review was neutral/skipped while the PR was draft; it is not a favorable code review. The native-acceptance follow-up `a23287c` also passes [CI40729907715](https://github.com/vypdev/vaultstadio/actions/runs/40729907715):6,569 executions and the same5-artifact/1,194-XML verification. Its actual Copilot review produced the three findings addressed above. The new review-refinement candidate still requires its own final-ref CI/artifact verification and actual Copilot reassessment before adoption.

## Preservation and next steps

All30 preexisting storage payloads, the original PostgreSQL instance and both detached #74 SDD drafts are protected. The private database dump is retained outside Git. The only backend environment change adds the exact local18090 CORS origin, preserving its code/image/mounts and the existing allowlist; an unapproved18091 origin still receives403. Current Web, prior runnable18088 rollback, native package and backend rollback are retained. No database reset, destructive cleanup, master promotion or protection change is part of this increment.

Verify the final PR's technical CI/original artifacts and resolve actual Copilot findings before adoption. The next functional roadmap priorities remain #116 for configurable Web API origin, then #82/#115 search/filter behavior, with #109/#112/#111 and broader coverage/decomposition retaining their own acceptance. This bounded result does not certify the whole platform as operational.
