/**
 * Entry for the global upload queue: either in-memory data or a chunked source for large files.
 */

package com.vaultstadio.app.domain.upload

/**
 * A single file to upload. Either [WithData] (small, in memory) or [Chunked] (large, read in chunks).
 */
sealed class UploadQueueEntry {
    abstract val name: String
    abstract val size: Long
    abstract val mimeType: String

    data class WithData(
        override val name: String,
        override val size: Long,
        override val mimeType: String,
        val data: ByteArray,
    ) : UploadQueueEntry() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false
            other as WithData
            return name == other.name && size == other.size && data.contentEquals(other.data)
        }

        override fun hashCode(): Int {
            var result = name.hashCode()
            result = 31 * result + size.hashCode()
            result = 31 * result + data.contentHashCode()
            return result
        }
    }

    data class Chunked(
        override val name: String,
        override val size: Long,
        override val mimeType: String,
        val source: ChunkedFileSource,
    ) : UploadQueueEntry()
}
