package com.vaultstadio.app.feature.upload

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.domain.storage.StorageRepository
import com.vaultstadio.app.domain.upload.FolderUploadEntry
import com.vaultstadio.app.domain.upload.UploadFailure
import com.vaultstadio.app.domain.upload.UploadQueueEntry
import com.vaultstadio.app.platform.LARGE_FILE_THRESHOLD
import com.vaultstadio.app.ui.components.dialogs.UploadItem
import com.vaultstadio.app.ui.components.dialogs.UploadStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlin.time.Clock

/** Session-owned queue. Terminal facts and retry snapshots are independent of transport execution. */
class UploadManager(
    private val storageRepository: StorageRepository,
    workDispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val processor = UploadEntryProcessor(storageRepository, workDispatcher)
    private val tasks = UploadTaskOwner()
    private val queue = mutableListOf<UploadWork>()
    private val retained = mutableMapOf<String, UploadWork>()
    private val attempts = MutableStateFlow<Map<String, Long>>(emptyMap())
    private val semaphore = Semaphore(3)
    private var processing = false
    private var closed = false
    private var counter = 0
    private val mutableItems = MutableStateFlow<List<UploadItem>>(emptyList())
    val items = mutableItems.asStateFlow()
    private val minimized = MutableStateFlow(false)
    val isMinimized = minimized.asStateFlow()
    private val destination = MutableStateFlow<String?>(null)
    val uploadDestinationFolderId = destination.asStateFlow()
    private val completed = MutableSharedFlow<Unit>(replay = 0, extraBufferCapacity = 1)

    /** Emits successful completed uploads only. Failure/cancellation cannot announce completion. */
    val uploadCompleted = completed.asSharedFlow()

    fun setUploadDestination(folderId: String?) {
        destination.value = folderId
    }
    fun getCurrentDestinationFolderId(): String? = destination.value
    fun setMinimized(value: Boolean) {
        minimized.value = value
    }

    fun addEntries(entries: List<UploadQueueEntry>, parentId: String?) {
        if (closed) return
        entries.forEach { enqueue(it, parentId, it.name) }
        startProcessing()
    }

    private fun enqueue(entry: UploadQueueEntry, parent: String?, path: String) {
        val id = "upload-${++counter}-${Clock.System.now().toEpochMilliseconds()}"
        val work = UploadWork(id, entry, parent, 0)
        retained[id] = work
        attempts.update { it + (id to 0L) }
        queue += work
        mutableItems.update { it + UploadItem(id, entry.name, path, entry.size) }
    }

    fun addFolderEntries(entries: List<FolderUploadEntry>, parentId: String?) {
        if (closed || entries.isEmpty()) return
        scope.launch {
            val folders = mutableMapOf<String, String>()
            for (entry in entries.sortedBy { it.relativePath.count { c -> c == '/' || c == '\\' } }) {
                val segments = entry.relativePath.split("/", "\\").filter { it.isNotBlank() }
                var parent = parentId
                var failed = false
                for (segment in segments.dropLast(1)) {
                    val key = "$parent/$segment"
                    val cached = folders[key]
                    if (cached != null) {
                        parent = cached
                        continue
                    }
                    when (val result = storageRepository.createFolder(segment, parent)) {
                        is Result.Success -> {
                            parent = result.data.id
                            folders[key] = parent
                        }
                        else -> {
                            failed = true
                            break
                        }
                    }
                }
                if (failed) {
                    val id = "upload-${++counter}-${Clock.System.now().toEpochMilliseconds()}"
                    mutableItems.update {
                        it + UploadItem(
                            id,
                            entry.name,
                            entry.relativePath,
                            entry.size,
                            status = UploadStatus.FAILED,
                            failure = UploadFailure.FOLDER_UNAVAILABLE,
                        )
                    }
                } else {
                    enqueue(
                        UploadQueueEntry.WithData(entry.name, entry.size, entry.mimeType, entry.data),
                        parent,
                        entry.relativePath,
                    )
                }
            }
            startProcessing()
        }
    }

    fun cancelUpload(id: String) {
        if (mutableItems.value.none {
                it.id == id && it.status in listOf(UploadStatus.PENDING, UploadStatus.UPLOADING)
            }
        ) {
            return
        }
        attempts.update { it + (id to ((it[id] ?: 0) + 1)) }
        queue.removeAll { it.id == id }
        val active = tasks.isActive(id)
        mutableItems.update { list ->
            list.map {
                if (it.id == id) {
                    it.copy(
                        status = UploadStatus.CANCELED,
                        failure = UploadFailure.CANCELED,
                        cleanupInProgress = active,
                    )
                } else {
                    it
                }
            }
        }
        tasks.cancel(id)
    }

    fun canRetry(id: String): Boolean {
        val item = mutableItems.value.find { it.id == id } ?: return false
        val entry = retained[id]?.entry as? UploadQueueEntry.WithData ?: return false
        return item.status in listOf(UploadStatus.FAILED, UploadStatus.CANCELED) &&
            !item.cleanupInProgress &&
            !tasks.isActive(id) &&
            entry.size >= 0 &&
            entry.data.size.toLong() == entry.size
    }

    fun retryUpload(id: String): Boolean {
        if (closed || !canRetry(id)) return false
        val previous = retained.getValue(id)
        val attempt = (attempts.value[id] ?: 0) + 1
        attempts.update { it + (id to attempt) }
        val retry = previous.copy(attempt = attempt)
        retained[id] = retry
        queue += retry
        mutableItems.update { list ->
            list.map {
                if (it.id == id) {
                    it.copy(
                        status = UploadStatus.PENDING,
                        progress = 0f,
                        failure = null,
                        cleanupInProgress = false,
                    )
                } else {
                    it
                }
            }
        }
        startProcessing()
        return true
    }

    fun dismissCompleted() {
        val terminal = mutableItems.value.filter {
            it.status in
                listOf(UploadStatus.COMPLETED, UploadStatus.FAILED, UploadStatus.CANCELED)
        }
        val ids = terminal.filterNot { it.cleanupInProgress }.map { it.id }.toSet()
        ids.forEach(retained::remove)
        attempts.update { it - ids }
        mutableItems.update { list -> list.filterNot { it.id in ids } }
    }

    private fun startProcessing() {
        if (closed || processing || queue.isEmpty()) return
        processing = true
        scope.launch {
            try {
                while (queue.isNotEmpty()) {
                    val snapshot = queue.toList()
                    queue.clear()
                    val (large, small) = snapshot.partition { it.entry.size > LARGE_FILE_THRESHOLD }
                    for (work in large) uploadOne(work)
                    coroutineScope {
                        small.map { work -> async { semaphore.withPermit { uploadOne(work) } } }.awaitAll()
                    }
                }
            } finally {
                processing = false
                if (!closed && queue.isNotEmpty()) startProcessing()
            }
        }
    }

    private suspend fun uploadOne(work: UploadWork) {
        if (attempts.value[work.id] != work.attempt) return
        update(work) { it.copy(status = UploadStatus.UPLOADING, failure = null) }
        try {
            val result = try {
                tasks.run(work.id) {
                    processor.upload(work.entry, work.parentId) { progress ->
                        if (progress.isFinite()) {
                            update(work) { item ->
                                item.copy(progress = progress.coerceIn(0f, 1f))
                            }
                        }
                    }
                }
            } catch (
                error: CancellationException,
            ) {
                throw error
            } catch (_: Exception) {
                Result.Error("UPLOAD_FAILED", "Upload did not complete")
            }
            if (attempts.value[work.id] != work.attempt) return
            when (result) {
                is Result.Success -> {
                    update(work) { it.copy(status = UploadStatus.COMPLETED, progress = 1f, failure = null) }
                    retained.remove(work.id)
                    attempts.update { it - work.id }
                    completed.emit(Unit)
                }
                else -> {
                    val failure = if (result == null) {
                        UploadFailure.CANCELED
                    } else {
                        if (result is Result.Error &&
                            result.code == "UPLOAD_INVALID_CONTENT"
                        ) {
                            UploadFailure.INVALID_CONTENT
                        } else {
                            UploadFailure.TRANSFER_FAILED
                        }
                    }
                    update(work) {
                        it.copy(
                            status = if (result ==
                                null
                            ) {
                                UploadStatus.CANCELED
                            } else {
                                UploadStatus.FAILED
                            },
                            failure = failure,
                        )
                    }
                }
            }
        } finally {
            mutableItems.update { list ->
                list.map {
                    if (it.id == work.id &&
                        it.cleanupInProgress
                    ) {
                        it.copy(cleanupInProgress = false)
                    } else {
                        it
                    }
                }
            }
        }
    }

    private fun update(work: UploadWork, change: (UploadItem) -> UploadItem) {
        if (attempts.value[work.id] != work.attempt) return
        mutableItems.update { list ->
            list.map {
                if (it.id == work.id &&
                    attempts.value[work.id] == work.attempt
                ) {
                    change(it)
                } else {
                    it
                }
            }
        }
    }

    fun clear() {
        if (closed) return
        closed = true
        scope.cancel()
        queue.clear()
        retained.clear()
        attempts.value = emptyMap()
    }
}
