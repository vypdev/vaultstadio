package com.vaultstadio.app.platform

import com.vaultstadio.app.domain.upload.FolderFileMetadata
import com.vaultstadio.app.domain.upload.FolderFileSource
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path

internal data class DesktopFolderRecord(val path: Path, val metadata: FolderFileMetadata)

/** Constructed only after metadata traversal succeeds; MIME probing never precedes validation. */
internal class DesktopFolderSource(
    record: DesktopFolderRecord,
    private val readFile: (Path, Long) -> ByteArray,
) : FolderFileSource {
    private val path = record.path
    override val metadata = record.metadata.copy(mimeType = Files.probeContentType(path) ?: "application/octet-stream")
    override suspend fun readBytes(): ByteArray = readFile(path, metadata.size)
}

/** Validated size is at most32MiB; a growing source can produce only snapshot+1 bytes. */
internal fun readDesktopFolderBytes(path: Path, declaredSize: Long): ByteArray =
    Files.newInputStream(path, LinkOption.NOFOLLOW_LINKS).use { it.readNBytes((declaredSize + 1).toInt()) }
