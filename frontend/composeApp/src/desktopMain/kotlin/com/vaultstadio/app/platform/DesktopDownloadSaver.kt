package com.vaultstadio.app.platform

import com.vaultstadio.app.domain.storage.transfer.DownloadLimits
import com.vaultstadio.app.domain.storage.transfer.DownloadPayload
import com.vaultstadio.app.domain.storage.transfer.DownloadSaver
import com.vaultstadio.app.domain.storage.transfer.SaveOutcome
import com.vaultstadio.app.domain.storage.transfer.isSafeDownloadName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption.ATOMIC_MOVE
import java.nio.file.StandardCopyOption.REPLACE_EXISTING

actual fun createDownloadSaver(): DownloadSaver = DesktopDownloadSaver()

internal class DesktopDownloadSaver(
    private val choose: suspend (String) -> Path? = ::chooseDownloadTarget,
) : DownloadSaver {
    private var closed = false
    override suspend fun save(payload: DownloadPayload): SaveOutcome {
        check(!closed)
        require(isSafeDownloadName(payload.name) && payload.bytes.size <= DownloadLimits.MAX_BYTES)
        currentCoroutineContext().ensureActive()
        val target = choose(payload.name) ?: return SaveOutcome.CANCELED
        currentCoroutineContext().ensureActive()
        check(!closed)
        val context = currentCoroutineContext()
        withContext(Dispatchers.IO) { writeDownloadAtomically(target, payload.bytes) { context.ensureActive() } }
        return SaveOutcome.SAVED
    }
    override fun close() {
        closed = true
    }
}

/** Same-parent staging preserves an existing destination until the complete write is published. */
internal fun writeDownloadAtomically(target: Path, bytes: ByteArray, checkActive: () -> Unit = {}) {
    val absolute = target.toAbsolutePath()
    val temporary = Files.createTempFile(absolute.parent, ".vaultstadio-download-", ".part")
    try {
        Files.newOutputStream(temporary).use { stream ->
            var offset = 0
            while (offset < bytes.size) {
                checkActive()
                val count = minOf(8192, bytes.size - offset)
                stream.write(bytes, offset, count)
                offset += count
            }
        }
        checkActive()
        Files.move(temporary, absolute, ATOMIC_MOVE, REPLACE_EXISTING)
    } finally {
        Files.deleteIfExists(temporary)
    }
}
