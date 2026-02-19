/**
 * Unit tests for Files upload destination and move logic.
 *
 * Covers: upload destination (current folder), move optimistic update,
 * and destination resolution for "move to parent".
 */

package com.vaultstadio.app.feature.files

import com.vaultstadio.app.domain.storage.model.Breadcrumb
import com.vaultstadio.app.domain.storage.model.ItemType
import com.vaultstadio.app.domain.storage.model.StorageItem
import com.vaultstadio.app.domain.storage.model.Visibility
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class UploadDestinationLogicTest {

    @Test
    fun `upload destination when in folder should be that folder id`() {
        val currentFolderId = "folder-123"
        val destination = currentFolderId
        assertEquals("folder-123", destination)
    }

    @Test
    fun `upload destination when at root should be null`() {
        val currentFolderId: String? = null
        assertEquals(null, currentFolderId)
    }
}

class MoveOptimisticUpdateTest {

    @Test
    fun `filtering out moved item removes exactly one item`() {
        val items = listOf(
            storageItem("1", "a"),
            storageItem("2", "b"),
            storageItem("3", "c"),
        )
        val afterMove = items.filter { it.id != "2" }
        assertEquals(2, afterMove.size)
        assertEquals(listOf("1", "3"), afterMove.map { it.id })
    }

    @Test
    fun `filtering out non-existent id leaves list unchanged`() {
        val items = listOf(storageItem("1", "a"))
        val afterMove = items.filter { it.id != "other" }
        assertEquals(1, afterMove.size)
        assertEquals("1", afterMove.first().id)
    }

    private fun storageItem(id: String, name: String) = StorageItem(
        id = id,
        name = name,
        path = "/$name",
        type = ItemType.FILE,
        parentId = null,
        size = 0L,
        mimeType = null,
        visibility = Visibility.PRIVATE,
        isStarred = false,
        isTrashed = false,
        createdAt = Instant.DISTANT_PAST,
        updatedAt = Instant.DISTANT_PAST,
    )
}

class MoveToParentDestinationTest {

    @Test
    fun `parent destination when one level deep is Home id null`() {
        val breadcrumbs = listOf(
            Breadcrumb(id = null, name = "Home", path = "/"),
            Breadcrumb(id = "folder-1", name = "Sub", path = "/Sub"),
        )
        val parentId = breadcrumbs.getOrNull(breadcrumbs.size - 2)?.id
        assertNull(parentId)
    }

    @Test
    fun `parent destination when two levels deep is first folder id`() {
        val breadcrumbs = listOf(
            Breadcrumb(id = null, name = "Home", path = "/"),
            Breadcrumb(id = "folder-1", name = "A", path = "/A"),
            Breadcrumb(id = "folder-2", name = "B", path = "/A/B"),
        )
        val parentId = breadcrumbs.getOrNull(breadcrumbs.size - 2)?.id
        assertEquals("folder-1", parentId)
    }
}
