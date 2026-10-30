package com.vaultstadio.app.feature.files.transfer

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.domain.storage.transfer.DownloadLimits
import com.vaultstadio.app.domain.storage.transfer.DownloadPayload
import com.vaultstadio.app.domain.storage.transfer.DownloadSaver
import com.vaultstadio.app.domain.storage.transfer.SaveOutcome
import com.vaultstadio.app.domain.storage.transfer.isSafeDownloadName
import com.vaultstadio.app.domain.storage.usecase.DownloadArchiveUseCase
import com.vaultstadio.app.domain.storage.usecase.DownloadFileUseCase
import com.vaultstadio.app.domain.storage.usecase.GetItemUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Screen-owned operation. Domain ports fetch content; the composition-supplied port saves it. */
class FilesDownloadController(
    private val scope: CoroutineScope,
    private val getItem: GetItemUseCase,
    private val downloadFile: DownloadFileUseCase,
    private val downloadArchive: DownloadArchiveUseCase,
    private val saver: DownloadSaver,
) {
    private val mutableState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val state = mutableState.asStateFlow()
    private var current: Job? = null
    private var generation = 0L
    private var closed = false
    private var last: Request? = null

    fun item(id: String): Boolean = start(Request.Item(id))
    fun archive(ids: List<String>): Boolean = start(Request.Archive(ids.toList()))
    fun retry(): Boolean = last?.let { start(it) } ?: false
    fun dismiss() {
        if (mutableState.value !is DownloadState.Running) mutableState.value = DownloadState.Idle
    }

    fun cancel() {
        if (mutableState.value !is DownloadState.Running) return
        val savingMayHaveCompleted = (mutableState.value as? DownloadState.Running)?.saving == true
        generation++
        current?.cancel()
        current = null
        mutableState.value = DownloadState.Finished("", SaveOutcome.CANCELED, savingMayHaveCompleted)
    }

    fun close() {
        if (closed) return
        cancel()
        closed = true
        saver.close()
    }

    private fun start(request: Request): Boolean {
        if (closed || !scope.isActive || mutableState.value is DownloadState.Running) return false
        last = request
        val owner = ++generation
        mutableState.value = DownloadState.Running("")
        val job = scope.launch {
            var saving = false
            try {
                val payload = when (request) {
                    is Request.Item -> file(request.id, owner)
                    is Request.Archive -> archivePayload(request.ids)
                }
                currentCoroutineContext().ensureActive()
                if (generation != owner || closed) throw CancellationException("Download no longer owns the screen")
                saving = true
                publish(owner, DownloadState.Running(payload.name, saving = true))
                val outcome = saver.save(payload)
                currentCoroutineContext().ensureActive()
                if (outcome == SaveOutcome.UNSUPPORTED) throw DownloadRefused(DownloadProblem.UNSUPPORTED)
                publish(owner, DownloadState.Finished(payload.name, outcome))
            } catch (error: CancellationException) {
                publish(owner, DownloadState.Finished("", SaveOutcome.CANCELED, destinationMayHaveContent = saving))
                throw error
            } catch (error: DownloadRefused) {
                publish(owner, DownloadState.Failed(error.problem))
            } catch (error: Exception) {
                publish(
                    owner,
                    DownloadState.Failed(if (saving) DownloadProblem.SAVE_FAILED else DownloadProblem.UNAVAILABLE),
                )
            }
        }
        current = job
        job.invokeOnCompletion {
            if (generation == owner && mutableState.value is DownloadState.Running) {
                publish(owner, DownloadState.Finished("", SaveOutcome.CANCELED))
            }
        }
        return true
    }

    private suspend fun file(id: String, owner: Long): DownloadPayload {
        if (id.isBlank()) throw DownloadRefused(DownloadProblem.INVALID_SELECTION)
        val item = getItem(id).downloadValue()
        if (item.id != id) throw DownloadRefused(DownloadProblem.CONTENT_CHANGED)
        if (item.isFolder) return archivePayload(listOf(id))
        if (!isSafeDownloadName(item.name)) throw DownloadRefused(DownloadProblem.INVALID_NAME)
        if (item.size < 0 || item.size > DownloadLimits.MAX_BYTES) throw DownloadRefused(DownloadProblem.TOO_LARGE)
        publish(owner, DownloadState.Running(item.name))
        val bytes = downloadFile(id).downloadValue()
        if (bytes.size.toLong() != item.size) throw DownloadRefused(DownloadProblem.CONTENT_CHANGED)
        return payload(item.name, bytes, item.mimeType ?: "application/octet-stream")
    }

    private suspend fun archivePayload(ids: List<String>): DownloadPayload {
        if (ids.isEmpty() ||
            ids.size > DownloadLimits.MAX_ARCHIVE_ITEMS ||
            ids.any { it.isBlank() } ||
            ids.distinct().size != ids.size
        ) {
            throw DownloadRefused(DownloadProblem.INVALID_SELECTION)
        }
        return payload("VaultStadio.zip", downloadArchive(ids).downloadValue(), "application/zip")
    }

    private fun payload(name: String, bytes: ByteArray, mime: String): DownloadPayload {
        if (bytes.size > DownloadLimits.MAX_BYTES) throw DownloadRefused(DownloadProblem.TOO_LARGE)
        return DownloadPayload(name, bytes, mime)
    }

    private fun publish(owner: Long, state: DownloadState) {
        if (generation == owner && !closed) mutableState.value = state
    }

    private sealed interface Request {
        data class Item(val id: String) : Request
        data class Archive(val ids: List<String>) : Request
    }
}

private fun <T> Result<T>.downloadValue(): T = when (this) {
    is Result.Success -> data
    is Result.NetworkError -> throw DownloadRefused(DownloadProblem.NETWORK)
    is Result.Error -> throw DownloadRefused(
        when (code) {
            "HTTP_401", "UNAUTHORIZED", "AUTHENTICATION_REQUIRED" -> DownloadProblem.AUTHENTICATION
            "HTTP_403", "ACCESS_DENIED" -> DownloadProblem.ACCESS_DENIED
            "HTTP_404", "NOT_FOUND", "ITEM_NOT_FOUND" -> DownloadProblem.NOT_FOUND
            "HTTP_413", "FILE_TOO_LARGE" -> DownloadProblem.TOO_LARGE
            "HTTP_400", "VALIDATION_ERROR" -> DownloadProblem.INVALID_SELECTION
            "HTTP_409", "DOWNLOAD_INCOMPLETE", "CONCURRENT_MODIFICATION" -> DownloadProblem.CONTENT_CHANGED
            else -> DownloadProblem.UNAVAILABLE
        },
    )
}
