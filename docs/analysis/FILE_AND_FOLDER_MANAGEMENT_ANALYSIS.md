# File and Folder Management – Analysis

This document analyses how VaultStadio handles **file/folder uploads** (including heavy loads and progress) and **content visualization** (folders, nesting, paths, breadcrumbs).

---

## 1. Upload

### 1.1 Backend capacity for large files (e.g. 64 GB)

| Mechanism | Location | Suitable for 64 GB? | Notes |
|-----------|----------|---------------------|--------|
| **Chunked upload** | `ChunkedUploadRoutes.kt` | **Yes** | Init → upload chunks (1–100 MB) → complete. Chunks stored in temp dir, then merged and sent to `StorageService.uploadFile()`. Designed for large files. |
| **Simple POST** | `StorageRoutes.kt` POST `/upload` | **No** | Reads entire body with `part.streamProvider().readBytes()` → full file in memory. Only for small files. |
| **Folder upload** | `FolderUploadRoutes.kt` POST `/upload-folder` | **No** | Each part is `readBytes()` in memory. No chunking; large files or many files will cause memory issues. |

**Conclusion:** The backend is **prepared for 64 GB per file** only via **chunked upload** (`/storage/upload/init`, `/storage/upload/{id}/chunk`, `/storage/upload/{id}/complete`). The simple upload and folder upload are not suitable for very large files.

### 1.2 Frontend upload flow

- **`UploadFileUseCase`** only calls `storageRepository.uploadFile(fileName, fileData, mimeType, parentId, onProgress)` with **`ByteArray`** → all uploads go through in-memory file data.
- **`StorageRepository`** exposes both:
  - `uploadFile(..., ByteArray, ...)` (used by the use case), and
  - Chunked APIs: `initChunkedUpload`, `uploadChunk`, `getUploadStatus`, `completeChunkedUpload`, `cancelChunkedUpload`.
- The **chunked API is never used** from the app layer; only the single `uploadFile(ByteArray)` path is used.
- **`UploadDialog`** exists and supports multiple items, per-file progress, and a global progress bar, but it is **not used** anywhere in the main UI (e.g. not opened from `FilesContent`). The FAB in Files only opens “New folder”, not upload.
- **Drag-and-drop** in `VaultStadioRoot` only clears the drop event; it does **not** open the upload dialog or start any upload.

**Conclusion:** The frontend is **not** prepared for 64 GB: it only uses in-memory upload. Chunked upload exists in the repo and backend but is not wired into the UI. There is no visible upload entry point (no “Upload” button and no working drag-and-drop to upload).

### 1.3 Progress banner (e.g. bottom-right like Google Drive)

- There is **no** floating or persistent upload progress banner (e.g. bottom-right).
- Progress is only shown inside **`UploadDialog`** (modal), which is not currently shown from the Files screen.

**Conclusion:** No banner-style progress; no in-place progress when the dialog is not used.

### 1.4 Upload of full directories with per-file progress

- **Backend:** `FolderUploadRoutes.kt` accepts multipart with path-like part names, creates folders, and uploads each file in one shot (`readBytes()`). No per-file progress reporting; no chunking for large files.
- **Frontend:** `StorageRepository.uploadFolder(files, parentId, onProgress)` exists and calls the folder upload API. There is no UI that selects a directory, builds `FolderUploadFile` list, and calls this (and no use of chunked upload for large files inside folders).

**Conclusion:** Directory upload exists on the backend and in the repository, but there is no directory picker or UI that uses it, and no per-file progress in the UI. Large files inside folders are not supported (memory-bound).

---

## 2. Content visualization (folders and paths)

### 2.1 Folder and nesting display

- Listing is by **folder ID**: `GetFolderItemsUseCase` / `getItems(folderId, ...)` load children of a given folder. Nested structure is correct: each folder has `parentId`, and the tree is consistent.
- **`FilesContent`** shows a grid of items; folders are clickable and call `viewModel.navigateToFolder(item.id, item.name)`. So navigation into nested folders works; what’s missing is path and URL sync (see below).

### 2.2 Path of nested content (e.g. `files/test_folder/other_folder`)

- **URL path:** Routes support path segments, e.g. `/files/my_folder/other_folder` → `pathSegments = ["my_folder", "other_folder"]` (`AppRoute.pathParam = "path"`, `RouteMatch.pathSegments()`).
- **`FilesComponent`** receives `pathSegments` from the main stack config when the initial URL has segments (e.g. deep link or refresh on `/files/test_folder/other_folder`).
- **Critical gap:** Neither `FilesContent` nor `FilesViewModel` use `component.pathSegments` on load. The ViewModel always starts with `currentFolderId = null` and never resolves path segments to folder IDs. So:
  - The **path is not shown in the URL** when navigating by clicking folders (no `setPath` with the current path).
  - When opening or refreshing **`/files/test_folder/other_folder`**, the app does **not** open that folder; it shows root.

So the intended “path in the URL” exists in the route model but is not used to drive navigation or to update the URL when the user navigates.

### 2.3 Breadcrumbs and “where I am” (e.g. test_folder > other_folder)

- **Backend:** `GET /item/{itemId}/breadcrumbs` returns **ancestors** of the item (from `StorageService.getBreadcrumbs` → `storageItemRepository.getAncestors(itemId)`). So for the current folder it returns the list of parent folders **only**, not the root and not the current folder itself.
- **Example:** For `other_folder` (parent `test_folder`, root parent), the API returns something like `[test_folder]` (one ancestor). It does **not** return a “Home” root node or the current folder `other_folder`.
- **Frontend:** After `loadItems()`, when `currentFolderId != null`, the ViewModel calls `loadBreadcrumbs()` and sets `breadcrumbs = result.data`. So the bar shows exactly the API list (ancestors only).
- **`FilesViewModel.navigateToFolder`** sets **optimistic** breadcrumbs to `[Home, currentFolder]` (two entries). When `loadBreadcrumbs()` completes, it **replaces** that with the API list, which has **no** “Home” and **no** current folder name. So:
  - The breadcrumb bar can show only ancestor names (e.g. “test_folder”), without “Home” and without the current folder (“other_folder”).
- **`Breadcrumbs.kt`** renders `breadcrumbs` and uses `breadcrumb.id == null` for the “Home” label; API items have non-null `id`, so when using only API data, “Home” never appears. The current folder is never appended to the list.

**Conclusion:** Breadcrumbs do not reliably show “Home > test_folder > other_folder”. They show only the ancestor chain returned by the API, with no explicit “Home” and no current folder, so the user does not see a full path like `test_folder > other_folder` in the bar.

---

## 3. Summary table

| Topic | Status | Notes |
|-------|--------|--------|
| Backend: 64 GB per file | Supported via chunked upload | Simple POST and folder upload are in-memory only. |
| Frontend: 64 GB per file | Not supported | Only `ByteArray` upload is used; chunked API unused. |
| Upload progress in UI | Partial | Only inside `UploadDialog`; dialog not opened from Files. |
| Bottom-right progress banner | No | Not implemented. |
| Directory upload (backend) | Yes, but limited | In-memory per file; no progress; not for large files. |
| Directory upload (frontend UI) | No | No directory picker or flow using `uploadFolder`. |
| Folder nesting (data + list) | Correct | Tree and listing by folder ID work. |
| URL path (e.g. files/test_folder/other_folder) | Not wired | pathSegments not used to open folder; URL not updated on navigate. |
| Breadcrumbs (full path + current) | Incomplete | API returns ancestors only; no Home; current folder not in list. |

---

## 4. Recommended next steps

1. **Upload**
   - Add an upload entry point in Files (e.g. FAB or toolbar) that opens `UploadDialog`.
   - Wire drag-and-drop in `VaultStadioRoot` to the same upload flow (e.g. open dialog with dropped files).
   - For files above a size threshold (e.g. 100 MB), use chunked upload from the UI (init → upload chunks with progress → complete) instead of `uploadFile(ByteArray)`.
   - Optionally add a small floating progress panel (e.g. bottom-right) for background uploads, with progress and “minimize” so the user can keep browsing.

2. **Folder upload**
   - Add directory picker where the platform allows (e.g. web `webkitdirectory`, desktop).
   - Use `uploadFolder` and report per-file progress in the same dialog or in the floating panel.
   - For very large files inside folders, consider chunked upload per file or at least clear UX limits.

3. **Paths and breadcrumbs**
   - When the user navigates (e.g. into a folder), update the URL (e.g. `setPath`) so the path reflects current location (e.g. `/files/test_folder/other_folder`).
   - On load (or refresh), resolve `pathSegments` to folder IDs (e.g. by walking from root by name) and call `navigateToFolder` so deep links open the correct folder.
   - Fix breadcrumbs so the bar always shows **Home + ancestors + current folder**: e.g. prepend a Home crumb when the API list is used, and append the current folder name (and optionally id) after the ancestors.

This analysis is based on the codebase state at the time of writing; implementation details may have evolved in later commits.

---

## 5. Implementation (post-analysis)

The following was implemented to align with Google Drive–style behaviour:

- **Breadcrumbs:** Full path is shown: Home + ancestors (from API) + current folder. `currentFolderName` is kept in `FilesViewModel` and used when building breadcrumbs after `loadBreadcrumbs()`.
- **URL sync:** When navigating in Files mode, the browser URL is updated to `/files` or `/files/folderA/folderB` via `setPath(RoutePaths.toPath(..., pathParams))` in `FilesContent`.
- **Deep links:** On load, `pathSegments` from the URL are resolved to folder IDs via `openPathFromSegments()` (walking the tree by name) and the target folder is opened.
- **Upload:** A global `UploadManager` (Koin `@Single`) holds the queue. Files can be added via:
  - **Upload menu** in the Files top bar (Upload files / Upload folder / Upload large file(s)).
  - **Drag-and-drop** on the app (files are added to the upload queue; drop target folder is root when drop is handled at root level).
- **Banner:** Bottom-right `UploadBanner` shows progress, list of items, minimize/expand, and dismiss when all done. It is shown in `MainContent` and uses `UploadManager` state.
- **Chunked and parallel:** Files above `LARGE_FILE_THRESHOLD` (100 MB) use chunked upload (init → upload chunks → complete) and are processed one after another. Smaller files are uploaded in parallel (up to 3 at a time). Queue is processed in order: all large files first, then small files in parallel.
- **Folder upload:** `addFolderEntries()` creates the folder structure (get-or-create by path) then queues each file with the correct `parentId`. Files are processed with the same chunked/parallel rules.
- **Large file picker:** `pickLargeFilesForUpload()` (expect/actual) returns `List<UploadQueueEntry>` with `Chunked` entries wrapping `ChunkedFileSource` (platform adapters for `LargeSelectedFile`).
- **Upload destination:** When a folder is open in Files, new uploads (menu or drop on the app) go into that folder via `UploadManager.setUploadDestination(viewModel.currentFolderId)`; destination is cleared when leaving Files.
- **In-list drag-and-drop:** In Files mode (when not in selection mode), the user can drag a file or folder and drop it onto another folder (move into that folder) or onto the breadcrumb row (move to parent). `FilesViewModel.moveItemToFolder(itemId, destinationId)` calls `batchMoveUseCase`. Grid items use `detectDragGestures` and `onGloballyPositioned` to track bounds; drop targets (folders + "parent" zone) are highlighted during drag. A **drag preview** card follows the pointer while dragging. **Optimistic update:** the moved item is removed from the list immediately, then the list is refreshed from the server after the move API call.
- **Upload to current folder (fix):** Backend `POST /storage/upload` now reads `parentId` from the multipart form (FormItem) as well as from query parameters, so uploads from the frontend (which send parentId in the form body) correctly target the open folder.
- **Context menu:** Long-press on a file or folder opens a context menu (Google Drive–style) with Open, Rename, Move to, Copy to, Download, Add to starred, Move to trash (or Restore / Delete permanently in Trash). Right-click support on desktop can be added later via Compose Desktop `ContextMenuArea`.
- **Unit tests:** `FilesUploadAndMoveTest` (commonTest) covers upload destination logic, move optimistic-update filtering, and parent-destination resolution from breadcrumbs. Backend upload route reads `parentId` from multipart FormItem.
