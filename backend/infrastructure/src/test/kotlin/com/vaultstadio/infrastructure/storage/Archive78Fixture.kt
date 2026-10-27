package com.vaultstadio.infrastructure.storage

import com.vaultstadio.domain.storage.model.ArchiveEntry
import com.vaultstadio.domain.storage.model.ItemType
import com.vaultstadio.domain.storage.model.StorageItem
import kotlinx.datetime.Instant
import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream

internal fun archive78Entry(name: String = "bytes.bin", size: Long = 3, folder: Boolean = false) = ArchiveEntry(
    name,
    StorageItem(
        id = name,
        name = name.substringAfterLast('/'),
        path = "/$name",
        ownerId = "owner78",
        type = if (folder) ItemType.FOLDER else ItemType.FILE,
        size = size,
        createdAt = Instant.fromEpochMilliseconds(0),
        updatedAt = Instant.fromEpochMilliseconds(0),
    ),
)
internal class Archive78Input(bytes: ByteArray) : ByteArrayInputStream(bytes) {
    var closed = false
    override fun close() {
        closed = true
        super.close()
    }
}
internal fun archive78Unzip(
    bytes: ByteArray,
): Map<String, ByteArray> = ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
    val entries = linkedMapOf<String, ByteArray>()
    while (true) {
        val entry = zip.nextEntry ?: break
        check(entry.name !in entries)
        entries[entry.name] = zip.readBytes()
        zip.closeEntry()
    }
    entries
}
