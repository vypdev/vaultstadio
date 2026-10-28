package com.vaultstadio.app.feature.upload

import com.vaultstadio.app.domain.upload.FolderFileMetadata
import com.vaultstadio.app.domain.upload.FolderSelectionException
import com.vaultstadio.app.domain.upload.FolderSelectionProblem
import com.vaultstadio.app.domain.upload.FolderUploadEntry
import com.vaultstadio.app.domain.upload.folderSelectionProblem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

internal sealed interface FolderPreparationState {
    data object Idle : FolderPreparationState
    data object Preparing : FolderPreparationState
    data object Canceled : FolderPreparationState
    data object Queued : FolderPreparationState
    data class Failed(val problem: FolderSelectionProblem) : FolderPreparationState
}

/** Screen-owned selection; only the current generation may enqueue into its original parent. */
internal class FolderUploadController(
    private val scope: CoroutineScope,
    private val available: () -> Boolean,
    private val pick: suspend () -> List<FolderUploadEntry>,
    private val enqueue: (List<FolderUploadEntry>, String?) -> Unit,
) {
    private val mutableState = MutableStateFlow<FolderPreparationState>(FolderPreparationState.Idle)
    val state = mutableState.asStateFlow()
    private var task: Job? = null
    private var generation = 0L
    private var closed = false
    private var originalParent: String? = null

    fun start(parent: String?): Boolean {
        if (closed || !scope.isActive || mutableState.value == FolderPreparationState.Preparing) return false
        originalParent = parent
        val revision = ++generation
        if (!available()) {
            mutableState.value = FolderPreparationState.Failed(FolderSelectionProblem.UNSUPPORTED)
            return false
        }
        mutableState.value = FolderPreparationState.Preparing
        task = scope.launch(start = CoroutineStart.LAZY) {
            try {
                val entries = pick()
                currentCoroutineContext().ensureActive()
                if (closed || revision != generation) return@launch
                folderSelectionProblem(
                    entries.map {
                        FolderFileMetadata(it.name, it.relativePath, it.size, it.mimeType)
                    },
                    requireRoot = false,
                )
                    ?.let { throw FolderSelectionException(it) }
                if (entries.any {
                        it.size != it.data.size.toLong()
                    }
                ) {
                    throw FolderSelectionException(FolderSelectionProblem.READ_FAILED)
                }
                if (entries.isEmpty()) {
                    mutableState.value = FolderPreparationState.Canceled
                } else {
                    enqueue(entries, parent)
                    mutableState.value = FolderPreparationState.Queued
                }
            } catch (canceled: CancellationException) {
                if (!closed && revision == generation) mutableState.value = FolderPreparationState.Canceled
                throw canceled
            } catch (failure: Exception) {
                if (!closed && revision == generation) {
                    mutableState.value = FolderPreparationState.Failed(
                        (failure as? FolderSelectionException)?.problem ?: FolderSelectionProblem.READ_FAILED,
                    )
                }
            } finally {
                if (!closed && revision == generation) {
                    task = null
                    if (mutableState.value == FolderPreparationState.Preparing) {
                        mutableState.value = FolderPreparationState.Failed(FolderSelectionProblem.READ_FAILED)
                    }
                }
            }
        }
        task?.start()
        return true
    }

    fun retry(): Boolean = if (mutableState.value is FolderPreparationState.Failed) start(originalParent) else false

    fun cancel() {
        if (mutableState.value != FolderPreparationState.Preparing) return
        ++generation
        task?.cancel()
        task = null
        mutableState.value = FolderPreparationState.Canceled
    }

    fun dismiss() {
        if (mutableState.value != FolderPreparationState.Preparing) mutableState.value = FolderPreparationState.Idle
    }

    fun close() {
        closed = true
        ++generation
        task?.cancel()
        task = null
        mutableState.value = FolderPreparationState.Idle
    }
}
