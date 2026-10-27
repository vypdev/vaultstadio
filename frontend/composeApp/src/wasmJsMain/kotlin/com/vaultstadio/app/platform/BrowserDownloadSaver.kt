package com.vaultstadio.app.platform

import com.vaultstadio.app.domain.storage.transfer.DownloadLimits
import com.vaultstadio.app.domain.storage.transfer.DownloadPayload
import com.vaultstadio.app.domain.storage.transfer.DownloadSaver
import com.vaultstadio.app.domain.storage.transfer.SaveOutcome
import com.vaultstadio.app.domain.storage.transfer.isSafeDownloadName
import kotlinx.browser.document
import kotlinx.browser.window
import org.khronos.webgl.Int8Array
import org.khronos.webgl.toInt8Array
import org.w3c.dom.HTMLAnchorElement
import org.w3c.files.Blob

actual fun createDownloadSaver(): DownloadSaver = BrowserDownloadSaver()

/** Owns only binary handoff resources. The browser decides the final destination and disk outcome. */
internal class BrowserDownloadSaver : DownloadSaver {
    private val pending = mutableMapOf<String, Int>()
    private var closed = false
    override suspend fun save(payload: DownloadPayload): SaveOutcome {
        require(payload.bytes.size <= DownloadLimits.MAX_BYTES)
        return handoff(payload)
    }

    fun handoff(payload: DownloadPayload): SaveOutcome {
        check(!closed)
        require(isSafeDownloadName(payload.name))
        val body = checkNotNull(document.body)
        val anchor = document.createElement("a") as HTMLAnchorElement
        val url = createDownloadObjectUrl(binaryDownloadBlob(payload.bytes.toInt8Array(), payload.mimeType))
        var handedOff = false
        try {
            anchor.href = url
            anchor.download = payload.name
            anchor.style.display = "none"
            body.appendChild(anchor)
            anchor.click()
            pending[url] = window.setTimeout({
                release(url)
                null
            }, 1000)
            handedOff = true
            return SaveOutcome.HANDED_TO_BROWSER
        } finally {
            anchor.remove()
            if (!handedOff) release(url)
        }
    }

    private fun release(url: String) {
        pending.remove(url)?.let(window::clearTimeout)
        revokeDownloadObjectUrl(url)
    }
    override fun close() {
        if (closed) return
        closed = true
        pending.keys.toList().forEach(::release)
    }
}

private fun binaryDownloadBlob(bytes: Int8Array, mime: String): Blob = js("new Blob([bytes], { type: mime })")
private fun createDownloadObjectUrl(blob: Blob): String = js("URL.createObjectURL(blob)")
private fun revokeDownloadObjectUrl(url: String): Unit = js("{ URL.revokeObjectURL(url); }")
