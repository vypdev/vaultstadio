@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package com.vaultstadio.app.platform

import com.vaultstadio.app.domain.share.ClipboardWriteOutcome
import com.vaultstadio.app.domain.share.ShareLinkClipboard
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.js.JsAny

actual fun createShareLinkClipboard(): ShareLinkClipboard = BrowserShareLinkClipboard()

internal class BrowserShareLinkClipboard(
    private val writer: () -> JsAny? = ::nativeClipboardWriter,
    private val fallback: (String) -> ClipboardWriteOutcome = ::browserClipboardFallback,
) : ShareLinkClipboard {
    override suspend fun write(link: String): ClipboardWriteOutcome = suspendCancellableCoroutine { continuation ->
        if (!continuation.isActive) return@suspendCancellableCoroutine
        val output = writer()
        if (output == null) {
            continuation.resume(fallback(link))
        } else {
            val handle = beginClipboardWrite(output, link, {
                if (continuation.isActive) continuation.resume(ClipboardWriteOutcome.COPIED)
            }, {
                if (continuation.isActive) continuation.resume(ClipboardWriteOutcome.DENIED)
            }, {
                if (continuation.isActive) continuation.resume(ClipboardWriteOutcome.FAILED)
            })
            continuation.invokeOnCancellation { cancelClipboardWrite(handle) }
        }
    }
}

private fun nativeClipboardWriter(): JsAny? = js(
    "typeof navigator.clipboard?.writeText === 'function' ? navigator.clipboard.writeText.bind(navigator.clipboard) : null",
)
private fun cancelClipboardWrite(handle: JsAny): Unit = js("handle.cancel()")
