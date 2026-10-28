package com.vaultstadio.app.feature.upload

import com.vaultstadio.app.domain.upload.UploadQueueEntry
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.supervisorScope

internal data class UploadWork(val id: String, val entry: UploadQueueEntry, val parentId: String?, val attempt: Long)

/** Main-owned registry; canceling one child does not cancel unrelated queue entries. */
internal class UploadTaskOwner {
    private val active = mutableMapOf<String, Job>()
    fun isActive(id: String): Boolean = active.containsKey(id)
    fun cancel(id: String) {
        active[id]?.cancel()
    }
    suspend fun <T : Any> run(id: String, operation: suspend () -> T): T? = supervisorScope {
        val child = async { operation() }
        active[id] = child
        try {
            child.await()
        } catch (error: CancellationException) {
            currentCoroutineContext().ensureActive()
            null
        } finally {
            if (active[id] === child) active.remove(id)
        }
    }
}
