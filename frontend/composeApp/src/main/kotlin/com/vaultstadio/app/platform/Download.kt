/**
 * VaultStadio Platform Download
 *
 * Expect/actual declarations for platform-specific file download.
 */

package com.vaultstadio.app.platform

/**
 * Trigger a file download in the browser or file save dialog.
 */
expect fun downloadFile(fileName: String, data: ByteArray, mimeType: String)

/**
 * Open a URL in a new tab/window for download.
 */
expect fun openDownloadUrl(url: String)
