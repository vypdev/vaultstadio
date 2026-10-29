package com.vaultstadio.app.domain.share

/** Acknowledged output boundary; cancellation cannot roll back an already-started native write. */
fun interface ShareLinkClipboard {
    suspend fun write(link: String): ClipboardWriteOutcome
}

enum class ClipboardWriteOutcome { COPIED, UNAVAILABLE, DENIED, FAILED }
