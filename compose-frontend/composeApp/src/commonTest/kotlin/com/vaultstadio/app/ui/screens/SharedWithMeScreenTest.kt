/**
 * Unit tests for SharedWithMe screen logic: grouping by owner, empty state, item properties.
 */

package com.vaultstadio.app.ui.screens

import com.vaultstadio.app.domain.model.ItemType
import com.vaultstadio.app.domain.model.StorageItem
import com.vaultstadio.app.domain.model.Visibility
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SharedWithMeScreenTest {

    private val testInstant = Instant.fromEpochMilliseconds(0L)

    private fun storageItem(id: String, name: String, type: ItemType = ItemType.FILE) =
        StorageItem(
            id = id,
            name = name,
            path = "/$name",
            type = type,
            parentId = null,
            size = 1024L,
            mimeType = "text/plain",
            visibility = Visibility.PRIVATE,
            isStarred = false,
            isTrashed = false,
            createdAt = testInstant,
            updatedAt = testInstant,
        )

    private fun sharedItem(item: StorageItem, sharedBy: String, sharedByEmail: String) =
        SharedWithMeItem(
            item = item,
            sharedBy = sharedBy,
            sharedByEmail = sharedByEmail,
            sharedAt = testInstant,
            permissions = emptyList(),
        )

    @Test
    fun sharedWithMeItem_holdsItemAndSharingInfo() {
        val item = storageItem("1", "file.txt")
        val shared = sharedItem(item, "user1", "user1@example.com")
        assertEquals(item, shared.item)
        assertEquals("user1", shared.sharedBy)
        assertEquals("user1@example.com", shared.sharedByEmail)
        assertEquals(emptyList(), shared.permissions)
    }

    @Test
    fun sharedWithMe_groupByOwner() {
        val items = listOf(
            sharedItem(storageItem("1", "file1.txt"), "user1@example.com", "user1@example.com"),
            sharedItem(storageItem("2", "file2.txt"), "user2@example.com", "user2@example.com"),
            sharedItem(storageItem("3", "file3.txt"), "user1@example.com", "user1@example.com"),
        )
        val grouped = items.groupBy { it.sharedBy }
        assertEquals(2, grouped.size)
        assertEquals(2, grouped["user1@example.com"]?.size)
        assertEquals(1, grouped["user2@example.com"]?.size)
    }

    @Test
    fun sharedWithMe_emptyList() {
        val items = emptyList<SharedWithMeItem>()
        assertTrue(items.isEmpty())
        val grouped = items.groupBy { it.sharedBy }
        assertTrue(grouped.isEmpty())
    }

    @Test
    fun sharedWithMe_filterByFileType() {
        val items = listOf(
            sharedItem(storageItem("1", "a.pdf", ItemType.FILE), "u@x.com", "u@x.com"),
            sharedItem(storageItem("2", "folder", ItemType.FOLDER), "u@x.com", "u@x.com"),
        )
        val files = items.filter { it.item.isFile }
        val folders = items.filter { it.item.isFolder }
        assertEquals(1, files.size)
        assertEquals(1, folders.size)
    }
}
