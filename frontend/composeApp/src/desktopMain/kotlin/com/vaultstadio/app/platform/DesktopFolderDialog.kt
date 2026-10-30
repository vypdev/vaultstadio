package com.vaultstadio.app.platform

import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import java.awt.EventQueue
import java.io.File
import javax.swing.JFileChooser
import javax.swing.SwingUtilities
import kotlin.coroutines.resumeWithException

internal interface DesktopFolderDialog : AutoCloseable {
    fun select(): File?
}

/** Owns only the native dialog; approved filesystem work belongs to DesktopFolderFiles. */
internal suspend fun chooseDesktopFolder(
    createDialog: () -> DesktopFolderDialog = ::SwingFolderDialog,
): File? = suspendCancellableCoroutine { continuation ->
    EventQueue.invokeLater { openFolderDialog(createDialog, continuation) }
}

@Suppress("TooGenericExceptionCaught")
private fun openFolderDialog(
    createDialog: () -> DesktopFolderDialog,
    continuation: CancellableContinuation<File?>,
) {
    if (!continuation.isActive) return
    try {
        val selected = createDialog().use { dialog ->
            continuation.invokeOnCancellation { EventQueue.invokeLater { closeCanceledDialog(dialog) } }
            if (continuation.isActive) dialog.select() else null
        }
        if (continuation.isActive) continuation.resumeWith(Result.success(selected))
    } catch (failure: Exception) {
        if (continuation.isActive) continuation.resumeWithException(failure)
    }
}

@Suppress("TooGenericExceptionCaught")
private fun closeCanceledDialog(dialog: DesktopFolderDialog) {
    try {
        dialog.close()
    } catch (_: Exception) {
        // The caller is already canceled; a disposal failure cannot escape the EDT or publish a result.
    }
}

private class SwingFolderDialog : DesktopFolderDialog {
    private val chooser = JFileChooser().apply {
        fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
        dialogTitle = "Select Folder to Upload"
    }

    override fun select(): File? =
        if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) chooser.selectedFile else null

    override fun close() {
        SwingUtilities.getWindowAncestor(chooser)?.dispose()
    }
}
