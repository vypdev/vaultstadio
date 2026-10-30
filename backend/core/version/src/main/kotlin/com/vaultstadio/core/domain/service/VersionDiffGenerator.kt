package com.vaultstadio.core.domain.service

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import com.vaultstadio.core.domain.model.FileVersion
import com.vaultstadio.core.domain.model.VersionDiff
import com.vaultstadio.domain.common.exception.StorageBackendException
import com.vaultstadio.domain.common.exception.StorageException
import com.vaultstadio.domain.storage.model.StorageItem
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

/** Content comparison runs only after the facade has authorized and bound both operands. */
internal class VersionDiffGenerator(private val storageBackend: StorageBackend) {
    /**
     * Text MIME types that support diff operations.
     */
    private val textMimeTypes = setOf(
        "text/plain",
        "text/html",
        "text/css",
        "text/javascript",
        "text/xml",
        "text/csv",
        "text/markdown",
        "text/x-python",
        "text/x-java-source",
        "text/x-kotlin",
        "text/x-c",
        "text/x-c++",
        "text/x-swift",
        "text/x-go",
        "text/x-rust",
        "text/x-ruby",
        "text/x-php",
        "text/x-sh",
        "text/x-yaml",
        "text/x-toml",
        "application/json",
        "application/xml",
        "application/javascript",
        "application/x-yaml",
        "application/toml",
    )

    suspend fun generate(
        item: StorageItem,
        from: FileVersion,
        to: FileVersion,
    ): Either<StorageException, VersionDiff> {
        val sizeChange = to.size - from.size

        // Check if files are text-based (can be diffed)
        val isBinary = !isTextMimeType(item.mimeType)

        return if (isBinary) {
            // Binary files - just show size difference
            VersionDiff(
                fromVersion = from.versionNumber,
                toVersion = to.versionNumber,
                sizeChange = sizeChange,
                isBinary = true,
            ).right()
        } else {
            // Text files - generate line-by-line diff
            generateTextDiff(from, to, sizeChange)
        }
    }

    /**
     * Check if a MIME type is a text type.
     */
    private fun isTextMimeType(mimeType: String?): Boolean {
        if (mimeType == null) return false

        // Check exact match
        if (textMimeTypes.contains(mimeType)) return true

        // Check prefix match
        if (mimeType.startsWith("text/")) return true

        // Check common text subtypes
        if (mimeType.endsWith("+json")) return true
        if (mimeType.endsWith("+xml")) return true

        return false
    }

    /**
     * Generate a text diff between two versions.
     * Uses a simple line-by-line diff algorithm.
     */
    private suspend fun generateTextDiff(
        from: FileVersion,
        to: FileVersion,
        sizeChange: Long,
    ): Either<StorageException, VersionDiff> {
        return readLines(from.storageKey).flatMap { fromLines ->
            readLines(to.storageKey).map { toLines ->
                VersionDiff(
                    fromVersion = from.versionNumber,
                    toVersion = to.versionNumber,
                    sizeChange = sizeChange,
                    isBinary = false,
                    patches = VersionLineDiff.compare(fromLines, toLines),
                )
            }
        }
    }

    private suspend fun readLines(key: String): Either<StorageException, List<String>> =
        storageBackend.retrieve(key).flatMap { stream ->
            try {
                BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { it.readLines() }.right()
            } catch (error: IOException) {
                StorageBackendException("version-diff", "Cannot read version content", error).left()
            }
        }
}
