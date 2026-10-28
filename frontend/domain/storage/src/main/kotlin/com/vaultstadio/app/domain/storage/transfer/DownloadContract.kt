package com.vaultstadio.app.domain.storage.transfer

object DownloadLimits {
    const val MAX_BYTES: Int = 32 * 1024 * 1024
    const val MAX_ARCHIVE_ITEMS: Int = 256
}

/** A filename is a single supported segment, never a local path or a URL. */
fun isSafeDownloadName(name: String): Boolean =
    name.isNotBlank() &&
        name.length <= 255 &&
        name != "." &&
        name != ".." &&
        name.none { it == '/' || it == '\\' || it.code < 32 || it.code == 127 }

class DownloadPayload(val name: String, val bytes: ByteArray, val mimeType: String)

enum class SaveOutcome { SAVED, HANDED_TO_BROWSER, CANCELED, UNSUPPORTED }

/** Implementations own chooser/anchor, transient handles and cleanup. No transport or credentials. */
interface DownloadSaver {
    suspend fun save(payload: DownloadPayload): SaveOutcome
    fun close() = Unit
}
