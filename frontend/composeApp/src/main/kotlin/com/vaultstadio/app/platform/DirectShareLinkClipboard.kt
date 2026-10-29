package com.vaultstadio.app.platform

import com.vaultstadio.app.domain.share.ClipboardWriteOutcome
import com.vaultstadio.app.domain.share.ShareLinkClipboard
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/** Native synchronous boundary; the callback must return only after the system accepted the write. */
internal class DirectShareLinkClipboard(private val writeText: (String) -> Unit) : ShareLinkClipboard {
    // Platform refusal must not become a copied confirmation or expose exception details.
    @Suppress("TooGenericExceptionCaught")
    override suspend fun write(link: String): ClipboardWriteOutcome = try {
        currentCoroutineContext().ensureActive()
        writeText(link)
        currentCoroutineContext().ensureActive()
        ClipboardWriteOutcome.COPIED
    } catch (error: CancellationException) {
        throw error
    } catch (_: Exception) {
        ClipboardWriteOutcome.FAILED
    }
}
