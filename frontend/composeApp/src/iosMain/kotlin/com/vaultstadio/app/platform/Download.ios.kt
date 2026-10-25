/**
 * VaultStadio iOS Download Implementation
 *
 * Stub implementation. Full implementation would use UIActivityViewController
 * or share sheet for saving files.
 */

package com.vaultstadio.app.platform

/**
 * Trigger a file download. Stub: no-op on iOS until native implementation.
 */
actual fun downloadFile(fileName: String, data: ByteArray, mimeType: String) {
    // TODO: Use UIActivityViewController or share sheet to save file
}

/**
 * Open a URL for download. Stub: no-op on iOS until native implementation.
 */
actual fun openDownloadUrl(url: String) {
    // TODO: Use UIApplication.openURL or SFSafariViewController
}
