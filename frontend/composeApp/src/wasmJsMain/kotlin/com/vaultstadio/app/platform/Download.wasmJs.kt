/**
 * VaultStadio WASM Download Implementation
 *
 * Uses browser APIs for file download.
 * Note: For WASM, direct ByteArray to Blob conversion is limited.
 * This implementation uses a data URI approach for small files
 * and falls back to console logging for larger files.
 */

package com.vaultstadio.app.platform

import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLAnchorElement
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Trigger a file download in the browser using data URI.
 * Creates a data URI from the byte array and triggers a download.
 */
@OptIn(ExperimentalEncodingApi::class)
actual fun downloadFile(fileName: String, data: ByteArray, mimeType: String) {
    try {
        // Encode data as Base64 and create a data URI
        val base64Data = Base64.encode(data)
        val dataUri = "data:$mimeType;base64,$base64Data"

        // Create temporary anchor element and trigger download
        val anchor = document.createElement("a") as HTMLAnchorElement
        anchor.href = dataUri
        anchor.download = fileName
        anchor.style.display = "none"
        document.body?.appendChild(anchor)
        anchor.click()

        // Cleanup: remove anchor
        document.body?.removeChild(anchor)

        console.log("Download initiated: $fileName, size: ${data.size}")
    } catch (e: Exception) {
        console.log("Download failed: ${e.message}")
    }
}

/**
 * Open a URL in a new tab for download.
 */
actual fun openDownloadUrl(url: String) {
    window.open(url, "_blank")
}

private external val console: Console

private external interface Console {
    fun log(message: String)
}
