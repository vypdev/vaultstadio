package com.vaultstadio.app.platform

import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import java.awt.EventQueue
import java.awt.FileDialog
import java.awt.Frame
import java.nio.file.Path
import kotlin.coroutines.resumeWithException

/** Owns the foreign event-queue boundary; file publication remains in the saver. */
internal interface DesktopDownloadDialog : AutoCloseable {
    fun select(name: String): Path?
}

internal suspend fun chooseDownloadTarget(
    name: String,
    createDialog: () -> DesktopDownloadDialog = ::AwtSaveDialog,
): Path? = suspendCancellableCoroutine { continuation ->
    EventQueue.invokeLater { openDownloadDialog(name, createDialog, continuation) }
}

// An exception escaping an AWT callback cannot reach its suspended caller without this boundary.
@Suppress("TooGenericExceptionCaught")
private fun openDownloadDialog(
    name: String,
    createDialog: () -> DesktopDownloadDialog,
    continuation: CancellableContinuation<Path?>,
) {
    if (!continuation.isActive) return
    try {
        val selected = createDialog().use { dialog ->
            continuation.invokeOnCancellation { EventQueue.invokeLater { dialog.close() } }
            if (continuation.isActive) dialog.select(name) else null
        }
        if (continuation.isActive) continuation.resumeWith(Result.success(selected))
    } catch (error: Exception) {
        if (continuation.isActive) continuation.resumeWithException(error)
    }
}

private class AwtSaveDialog : DesktopDownloadDialog {
    private val dialog = FileDialog(null as Frame?, "Save File", FileDialog.SAVE)
    override fun select(name: String): Path? {
        dialog.file = name
        dialog.isVisible = true
        val directory = dialog.directory ?: return null
        val file = dialog.file ?: return null
        return Path.of(directory, file)
    }
    override fun close() {
        dialog.dispose()
    }
}
