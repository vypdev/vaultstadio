/**
 * VaultStadio Desktop Download Implementation
 *
 * Uses AWT for file save dialogs.
 */

package com.vaultstadio.app.platform

import com.vaultstadio.app.domain.storage.transfer.isSafeDownloadName
import java.awt.Desktop
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.net.URI

/**
 * Trigger a file download/save dialog.
 */
actual fun downloadFile(fileName: String, data: ByteArray, mimeType: String) {
    require(isSafeDownloadName(fileName))
    val dialog = FileDialog(null as Frame?, "Save File", FileDialog.SAVE)
    try {
        dialog.file = fileName
        dialog.isVisible = true
        val directory = dialog.directory
        val file = dialog.file
        if (directory != null && file != null) writeDownloadAtomically(File(directory, file).toPath(), data)
    } finally {
        dialog.dispose()
    }
}

/**
 * Open a URL in the default browser for download.
 */
actual fun openDownloadUrl(url: String) {
    if (Desktop.isDesktopSupported()) {
        Desktop.getDesktop().browse(URI(url))
    }
}
