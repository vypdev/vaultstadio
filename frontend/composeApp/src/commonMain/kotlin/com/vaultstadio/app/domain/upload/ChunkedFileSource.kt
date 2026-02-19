/**
 * Source for reading a large file in chunks without loading it fully into memory.
 * Used for chunked upload of files that exceed [LARGE_FILE_THRESHOLD].
 */

package com.vaultstadio.app.domain.upload

/**
 * Provides chunked read access to a file (e.g. from platform file picker).
 */
interface ChunkedFileSource {
    val name: String
    val size: Long
    val mimeType: String

    /**
     * Reads the byte range [start, end) from the file.
     */
    suspend fun readChunk(start: Long, end: Long): ByteArray
}
