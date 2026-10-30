package com.vaultstadio.app.platform

import com.vaultstadio.app.domain.storage.transfer.DownloadPayload
import kotlinx.browser.window

/** Compatibility callback for existing profile export; uses the same binary/resource-safe saver. */
actual fun downloadFile(fileName: String, data: ByteArray, mimeType: String) {
    BrowserDownloadSaver().handoff(DownloadPayload(fileName, data, mimeType))
}

actual fun openDownloadUrl(url: String) {
    window.open(url, "_blank")
}
