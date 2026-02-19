/**
 * Global upload queue manager: processes file and folder uploads with chunked support
 * for large files and parallel uploads for small files. Designed for Google Drive–style UX.
 */

package com.vaultstadio.app.feature.upload

import com.vaultstadio.app.data.network.ApiResult
import com.vaultstadio.app.data.repository.StorageRepository
import com.vaultstadio.app.domain.upload.FolderUploadEntry
import com.vaultstadio.app.domain.upload.UploadQueueEntry
import com.vaultstadio.app.platform.DEFAULT_CHUNK_SIZE
import com.vaultstadio.app.platform.LARGE_FILE_THRESHOLD
import com.vaultstadio.app.ui.components.dialogs.UploadItem
import com.vaultstadio.app.ui.components.dialogs.UploadStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import org.koin.core.annotation.Single

/** Max number of small-file uploads to run in parallel. */
private const val SMALL_FILE_PARALLELISM = 3

@Single
class UploadManager(
    private val storageRepository: StorageRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _items = MutableStateFlow<List<UploadItem>>(emptyList())
    val items: StateFlow<List<UploadItem>> = _items.asStateFlow()

    private val _isMinimized = MutableStateFlow(false)
    val isMinimized: StateFlow<Boolean> = _isMinimized.asStateFlow()

    /** Current folder to use as upload/drop destination when user is viewing a folder. */
    private val _uploadDestinationFolderId = MutableStateFlow<String?>(null)
    val uploadDestinationFolderId: StateFlow<String?> = _uploadDestinationFolderId.asStateFlow()

    /** Emits when any upload completes (success or fail). Collect to refresh the file list. */
    private val _uploadCompleted = MutableSharedFlow<Unit>(replay = 0, extraBufferCapacity = 1)
    val uploadCompleted: SharedFlow<Unit> = _uploadCompleted.asSharedFlow()

    /** Call when the Files screen is showing so drops and uploads go to this folder; null = root. */
    fun setUploadDestination(folderId: String?) {
        _uploadDestinationFolderId.value = folderId
    }

    fun getCurrentDestinationFolderId(): String? = _uploadDestinationFolderId.value

    /** Pending queue: (itemId, entry, parentId). Cleared as we process. */
    private val queue = mutableListOf<Triple<String, UploadQueueEntry, String?>>()

    private val smallFileSemaphore = Semaphore(SMALL_FILE_PARALLELISM)
    private var isProcessing = false
    private var uploadIdCounter = 0

    fun addEntries(entries: List<UploadQueueEntry>, parentId: String?) {
        val newItems = entries.map { entry ->
            val id = "upload-${++uploadIdCounter}-${Clock.System.now().toEpochMilliseconds()}"
            val name = entry.name
            val size = entry.size
            queue.add(Triple(id, entry, parentId))
            UploadItem(
                id = id,
                fileName = name,
                filePath = name,
                size = size,
                progress = 0f,
                status = UploadStatus.PENDING,
            )
        }
        _items.update { it + newItems }
        startProcessing()
    }

    /**
     * Adds folder files: creates folder structure first, then queues each file with the correct parentId.
     */
    fun addFolderEntries(entries: List<FolderUploadEntry>, parentId: String?) {
        if (entries.isEmpty()) return
        scope.launch {
            val folderIds = mutableMapOf<String, String>()
            val sorted = entries.sortedBy { it.relativePath.count { c -> c == '/' || c == '\\' } }
            for (entry in sorted) {
                val pathParts = entry.relativePath.split("/", "\\").filter { it.isNotBlank() }
                val fileName = pathParts.lastOrNull() ?: entry.name
                val folderPathParts = pathParts.dropLast(1)
                var currentParentId = parentId
                for (segment in folderPathParts) {
                    val pathKey = if (currentParentId == null) segment else "$currentParentId/$segment"
                    val existing = folderIds[pathKey]
                    if (existing != null) {
                        currentParentId = existing
                    } else {
                        when (val result = storageRepository.createFolder(segment, currentParentId)) {
                            is ApiResult.Success -> {
                                val folderId = result.data.id
                                folderIds[pathKey] = folderId
                                currentParentId = folderId
                            }
                            else -> break
                        }
                    }
                }
                val id = "upload-${++uploadIdCounter}-${Clock.System.now().toEpochMilliseconds()}"
                queue.add(
                    Triple(
                        id,
                        UploadQueueEntry.WithData(entry.name, entry.size, entry.mimeType, entry.data),
                        currentParentId,
                    ),
                )
                _items.update { list ->
                    list + UploadItem(
                        id = id,
                        fileName = fileName,
                        filePath = entry.relativePath,
                        size = entry.size,
                        progress = 0f,
                        status = UploadStatus.PENDING,
                    )
                }
            }
            startProcessing()
        }
    }

    fun setMinimized(minimized: Boolean) {
        _isMinimized.value = minimized
    }

    fun dismissCompleted() {
        _items.update { list ->
            list.filter { it.status != UploadStatus.COMPLETED && it.status != UploadStatus.FAILED }
        }
    }

    fun cancelUpload(itemId: String) {
        _items.update { list ->
            list.map { if (it.id == itemId) it.copy(status = UploadStatus.FAILED) else it }
        }
        queue.removeAll { it.first == itemId }
    }

    private fun startProcessing() {
        if (isProcessing || queue.isEmpty()) return
        isProcessing = true
        scope.launch {
            try {
                while (queue.isNotEmpty()) {
                    val snapshot = queue.toList()
                    queue.clear()
                    val (largeList, smallList) = snapshot.partition { (_, entry, _) ->
                        entry.size > LARGE_FILE_THRESHOLD
                    }
                    for ((id, entry, parentId) in largeList) {
                        uploadOne(id, entry, parentId)
                    }
                    coroutineScope {
                        smallList.map { (id, entry, parentId) ->
                            async {
                                smallFileSemaphore.withPermit { uploadOne(id, entry, parentId) }
                            }
                        }.awaitAll()
                    }
                }
            } finally {
                isProcessing = false
                if (queue.isNotEmpty()) startProcessing()
            }
        }
    }

    private suspend fun uploadOne(itemId: String, entry: UploadQueueEntry, parentId: String?) {
        setStatus(itemId, UploadStatus.UPLOADING)
        val success = when (entry) {
            is UploadQueueEntry.WithData -> uploadWithData(itemId, entry, parentId)
            is UploadQueueEntry.Chunked -> uploadChunked(itemId, entry, parentId)
        }
        setStatus(itemId, if (success) UploadStatus.COMPLETED else UploadStatus.FAILED)
        if (success) setProgress(itemId, 1f)
        scope.launch { _uploadCompleted.emit(Unit) }
    }

    private suspend fun uploadWithData(
        itemId: String,
        entry: UploadQueueEntry.WithData,
        parentId: String?,
    ): Boolean {
        if (entry.data.isEmpty()) return false
        return withContext(Dispatchers.Default) {
            when (
                storageRepository.uploadFile(
                    entry.name,
                    entry.data,
                    entry.mimeType,
                    parentId,
                ) { p -> setProgress(itemId, p) }
            ) {
                is ApiResult.Success -> true
                else -> false
            }
        }
    }

    private suspend fun uploadChunked(
        itemId: String,
        entry: UploadQueueEntry.Chunked,
        parentId: String?,
    ): Boolean {
        val chunkSize = DEFAULT_CHUNK_SIZE
        val totalChunks = ((entry.size + chunkSize - 1) / chunkSize).toInt()
        val initResult = storageRepository.initChunkedUpload(
            entry.name,
            entry.size,
            entry.mimeType,
            parentId,
            chunkSize,
        )
        if (initResult !is ApiResult.Success) return false
        val init = initResult.data
        val source = entry.source
        for (i in 0 until totalChunks) {
            val start = i * chunkSize
            val end = minOf(start + chunkSize, entry.size)
            val chunk = source.readChunk(start, end)
            when (storageRepository.uploadChunk(init.uploadId, i, chunk)) {
                is ApiResult.Success -> setProgress(itemId, (i + 1).toFloat() / totalChunks)
                else -> {
                    storageRepository.cancelChunkedUpload(init.uploadId)
                    return false
                }
            }
        }
        return when (storageRepository.completeChunkedUpload(init.uploadId)) {
            is ApiResult.Success -> true
            else -> false
        }
    }

    private fun setProgress(itemId: String, progress: Float) {
        _items.update { list ->
            list.map { if (it.id == itemId) it.copy(progress = progress) else it }
        }
    }

    private fun setStatus(itemId: String, status: UploadStatus) {
        _items.update { list ->
            list.map { if (it.id == itemId) it.copy(status = status) else it }
        }
    }

    fun clear() {
        scope.cancel()
    }
}
