package com.vaultstadio.core.domain.service

import arrow.core.Either
import arrow.core.right
import com.vaultstadio.core.domain.model.FileVersion
import com.vaultstadio.core.domain.model.FileVersionHistory
import com.vaultstadio.core.domain.model.VersionRetentionPolicy
import com.vaultstadio.core.domain.repository.FileVersionRepository
import com.vaultstadio.domain.storage.model.ItemType
import com.vaultstadio.domain.storage.model.StorageItem
import com.vaultstadio.domain.storage.model.Visibility
import com.vaultstadio.domain.storage.repository.StorageItemRepository
import com.vaultstadio.domain.version.repository.VersionContentReferenceRepository
import com.vaultstadio.domain.version.repository.VersionItemScopeRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.datetime.Instant
import java.io.ByteArrayInputStream

/** Strict ports with observable rows/bytes; real SQL/byte adapters are covered by HTTP integration. */
internal class VersionServiceFixture(visibility: Visibility = Visibility.PRIVATE) {
    val versions = mockk<FileVersionRepository>()
    val items = mockk<StorageItemRepository>()
    val scopes = mockk<VersionItemScopeRepository>()
    val backend = mockk<StorageBackend>()
    val references = mockk<VersionContentReferenceRepository>()
    val service = FileVersionService(versions, items, backend, scopes, references)
    val time = Instant.parse("2026-10-25T12:00:00Z")
    var item = StorageItem(
        id = "item", name = "private-name.txt", path = "/private-name.txt",
        type = ItemType.FILE, ownerId = "owner", mimeType = "text/plain", visibility = visibility,
        createdAt = time, updatedAt = time, storageKey = "key2",
    )
    val rows = mutableListOf(
        FileVersion("v1", "item", 1, 4, "old-checksum", "key1", "previous-owner", time, isLatest = false),
        FileVersion("v2", "item", 2, 4, "new-checksum", "key2", "previous-owner", time, isLatest = true),
    )
    val bytes = mutableMapOf("key1" to "old\n".toByteArray(), "key2" to "new\n".toByteArray())
    val closedStreams = mutableListOf<String>()

    init {
        coEvery { references.hasOtherReferences(any(), any()) } answers {
            val key = firstArg<String>()
            val excluded = secondArg<Set<String>>()
            (item.storageKey == key || rows.any { it.id !in excluded && it.storageKey == key }).right()
        }
        coEvery { items.findById("item") } answers { item.right() }
        coEvery { scopes.findItemId(any()) } answers { rows.find { it.id == firstArg<String>() }?.itemId.right() }
        coEvery { versions.findById(any()) } answers { rows.find { it.id == firstArg<String>() }.right() }
        coEvery { versions.findByItemAndVersion(any(), any()) } answers {
            rows.find { it.itemId == firstArg<String>() && it.versionNumber == secondArg<Int>() }.right()
        }
        coEvery { versions.findLatest(any()) } answers { rows.find { it.isLatest }.right() }
        coEvery { versions.getNextVersionNumber(any()) } answers
            { ((rows.maxOfOrNull { it.versionNumber } ?: 0) + 1).right() }
        coEvery { versions.listVersions(any(), any(), any()) } answers {
            rows.sortedByDescending { it.versionNumber }.drop(thirdArg<Int>()).take(secondArg<Int>()).right()
        }
        coEvery { versions.getHistory(any()) } answers
            { FileVersionHistory(item, rows.toList(), rows.size, rows.sumOf { it.size }).right() }
        coEvery { versions.countVersions(any()) } answers { rows.size.right() }
        coEvery { versions.getTotalVersionSize(any()) } answers { rows.sumOf { it.size }.right() }
        coEvery { versions.create(any()) } answers {
            val row = firstArg<FileVersion>()
            rows.add(row)
            row.right()
        }
        coEvery { versions.update(any()) } answers {
            val row = firstArg<FileVersion>()
            rows[rows.indexOfFirst { it.id == row.id }] = row
            row.right()
        }
        coEvery { versions.delete(any()) } answers {
            rows.removeIf { it.id == firstArg<String>() }
            Unit.right()
        }
        coEvery { versions.deleteAllForItem(any()) } answers {
            val count = rows.size
            rows.clear()
            count.right()
        }
        coEvery { versions.applyRetentionPolicy(any(), any()) } answers {
            val removed = rows.filter { !it.isLatest }.map { it.id }
            rows.removeIf { it.id in removed }
            removed.right()
        }
        coEvery { backend.delete(any()) } answers {
            bytes.remove(firstArg<String>())
            Unit.right()
        }
        coEvery { backend.retrieve(any()) } answers {
            val key = firstArg<String>()
            object : ByteArrayInputStream(checkNotNull(bytes[key])) {
                override fun close() {
                    closedStreams.add(key)
                    super.close()
                }
            }.right()
        }
    }

    fun snapshot() = Triple(item, rows.toList(), bytes.mapValues { it.value.contentToString() })

    suspend fun invoke(operation: String, actor: String): Either<*, *> = when (operation) {
        "createVersion" -> service.createVersion(CreateVersionInput("item", 4, "new-checksum", "key2"), actor)
        "restoreVersion" -> service.restoreVersion(RestoreVersionInput("item", 1), actor)
        "getHistory" -> service.getHistory("item", actor)
        "getVersion" -> service.getVersion("item", 1, actor)
        "getLatestVersion" -> service.getLatestVersion("item", actor)
        "listVersions" -> service.listVersions("item", actor)
        "deleteVersion" -> service.deleteVersion("v1", actor)
        "applyRetentionPolicy" -> service.applyRetentionPolicy("item", actor, VersionRetentionPolicy(maxVersions = 1))
        "compareVersions" -> service.compareVersions("item", 1, 2, actor)
        "getVersionCount" -> service.getVersionCount("item", actor)
        "getTotalVersionSize" -> service.getTotalVersionSize("item", actor)
        "cleanupVersionsForItem" -> service.cleanupVersionsForItem("item", actor)
        else -> error("Unknown operation $operation")
    }

    companion object {
        val operations = listOf(
            "createVersion", "restoreVersion", "getHistory", "getVersion", "getLatestVersion",
            "listVersions", "deleteVersion", "applyRetentionPolicy", "compareVersions", "getVersionCount",
            "getTotalVersionSize", "cleanupVersionsForItem",
        )
    }
}
