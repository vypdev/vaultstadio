package com.vaultstadio.app.feature.upload

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.domain.storage.StorageRepository
import com.vaultstadio.app.domain.upload.UploadQueueEntry
import com.vaultstadio.app.platform.DEFAULT_CHUNK_SIZE
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

/** Executes a single owned entry. Queue state and UI are not part of transport orchestration. */
internal class UploadEntryProcessor(
    private val repository: StorageRepository,
    private val dispatcher: CoroutineDispatcher,
) {
    suspend fun upload(
        entry: UploadQueueEntry,
        parent: String?,
        progress: (Float) -> Unit,
    ): Result<Unit> = withContext(dispatcher) {
        when (entry) {
            is UploadQueueEntry.WithData -> {
                if (entry.size < 0 || entry.data.size.toLong() != entry.size) {
                    invalidContent()
                } else {
                    repository.uploadFile(entry.name, entry.data, entry.mimeType, parent, progress).map { Unit }
                }
            }
            is UploadQueueEntry.Chunked -> chunked(entry, parent, progress)
        }
    }

    private suspend fun chunked(
        entry: UploadQueueEntry.Chunked,
        parent: String?,
        progress: (Float) -> Unit,
    ): Result<Unit> {
        if (entry.size < 0 || entry.source.size != entry.size) return invalidContent()
        if (entry.size ==
            0L
        ) {
            return repository.uploadFile(entry.name, byteArrayOf(), entry.mimeType, parent, progress).map { Unit }
        }
        val initialized = repository.initChunkedUpload(
            entry.name,
            entry.size,
            entry.mimeType,
            parent,
            DEFAULT_CHUNK_SIZE,
        )
        if (initialized !is Result.Success) return initialized.map { Unit }
        val session = initialized.data
        var committed = false
        try {
            if (session.chunkSize <= 0) return invalidContent()
            val chunks = entry.size / session.chunkSize + if (entry.size % session.chunkSize > 0) 1 else 0
            if (chunks > Int.MAX_VALUE || chunks.toInt() != session.totalChunks) return invalidContent()
            for (index in 0 until session.totalChunks) {
                currentCoroutineContext().ensureActive()
                val start = index.toLong() * session.chunkSize
                val end = minOf(start + session.chunkSize, entry.size)
                val bytes = entry.source.readChunk(start, end)
                if (bytes.size.toLong() != end - start) return invalidContent()
                val uploaded = repository.uploadChunk(session.uploadId, index, bytes)
                if (uploaded !is Result.Success) return uploaded.map { Unit }
                progress((index + 1).toFloat() / session.totalChunks)
            }
            val completed = repository.completeChunkedUpload(session.uploadId)
            committed = completed is Result.Success
            return completed.map { Unit }
        } finally {
            if (!committed) {
                // Cancellation is bounded best effort; an unacknowledged remote session retains its server TTL.
                withContext(NonCancellable + dispatcher) {
                    try {
                        withTimeout(5000) { repository.cancelChunkedUpload(session.uploadId) }
                    } catch (
                        _: Exception,
                    ) { }
                }
            }
        }
    }

    private fun invalidContent() = Result.Error("UPLOAD_INVALID_CONTENT", "Select the file again")
}
