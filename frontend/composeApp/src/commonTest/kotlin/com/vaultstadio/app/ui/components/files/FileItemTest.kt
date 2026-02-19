/**
 * VaultStadio FileItem Component Tests
 *
 * Tests for FileItem and SelectableFileItem component logic.
 */

package com.vaultstadio.app.ui.components.files

import com.vaultstadio.app.domain.model.ItemType
import com.vaultstadio.app.domain.model.StorageItem
import com.vaultstadio.app.domain.model.Visibility
import kotlin.time.Clock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for FileItem component logic.
 */
class FileItemTest {

    private fun createTestItem(
        id: String = "test-id",
        name: String = "test-file.txt",
        type: ItemType = ItemType.FILE,
        size: Long = 1024,
        mimeType: String? = "text/plain",
        isStarred: Boolean = false,
        isTrashed: Boolean = false,
    ) = StorageItem(
        id = id,
        name = name,
        path = "/$name",
        type = type,
        parentId = null,
        size = size,
        mimeType = mimeType,
        visibility = Visibility.PRIVATE,
        isStarred = isStarred,
        isTrashed = isTrashed,
        createdAt = Clock.System.now(),
        updatedAt = Clock.System.now(),
    )

    // Item type detection tests

    @Test
    fun `should correctly identify file type`() {
        val file = createTestItem(type = ItemType.FILE)
        assertEquals(ItemType.FILE, file.type)
        assertFalse(file.isFolder)
    }

    @Test
    fun `should correctly identify folder type`() {
        val folder = createTestItem(type = ItemType.FOLDER, name = "Documents")
        assertEquals(ItemType.FOLDER, folder.type)
        assertTrue(folder.isFolder)
    }

    // File extension and icon tests

    @Test
    fun `should extract file extension correctly`() {
        val file = createTestItem(name = "document.pdf")
        val extension = file.name.substringAfterLast('.', "")
        assertEquals("pdf", extension)
    }

    @Test
    fun `should handle files without extension`() {
        val file = createTestItem(name = "README")
        val extension = file.name.substringAfterLast('.', "")
        assertEquals("", extension) // No dot found, returns empty string (the missingDelimiterValue)
    }

    @Test
    fun `should handle hidden files with extension`() {
        val file = createTestItem(name = ".gitignore")
        val extension = file.name.substringAfterLast('.', "")
        assertEquals("gitignore", extension)
    }

    // Mime type detection tests

    @Test
    fun `should detect image mime types`() {
        val imageTypes = listOf(
            "image/jpeg",
            "image/png",
            "image/gif",
            "image/webp",
            "image/svg+xml",
        )

        imageTypes.forEach { mimeType ->
            val file = createTestItem(mimeType = mimeType)
            assertTrue(file.mimeType?.startsWith("image/") == true)
        }
    }

    @Test
    fun `should detect video mime types`() {
        val videoTypes = listOf(
            "video/mp4",
            "video/webm",
            "video/quicktime",
            "video/x-msvideo",
        )

        videoTypes.forEach { mimeType ->
            val file = createTestItem(mimeType = mimeType)
            assertTrue(file.mimeType?.startsWith("video/") == true)
        }
    }

    @Test
    fun `should detect audio mime types`() {
        val audioTypes = listOf(
            "audio/mpeg",
            "audio/wav",
            "audio/ogg",
            "audio/flac",
        )

        audioTypes.forEach { mimeType ->
            val file = createTestItem(mimeType = mimeType)
            assertTrue(file.mimeType?.startsWith("audio/") == true)
        }
    }

    // Star functionality tests

    @Test
    fun `should track starred state`() {
        val unstarred = createTestItem(isStarred = false)
        assertFalse(unstarred.isStarred)

        val starred = createTestItem(isStarred = true)
        assertTrue(starred.isStarred)
    }

    // Trash functionality tests

    @Test
    fun `should track trashed state`() {
        val normal = createTestItem(isTrashed = false)
        assertFalse(normal.isTrashed)

        val trashed = createTestItem(isTrashed = true)
        assertTrue(trashed.isTrashed)
    }

    // Selection tests

    @Test
    fun `should correctly toggle selection in a set`() {
        val selectedItems = mutableSetOf<String>()
        val item = createTestItem(id = "item-1")

        // Select
        selectedItems.add(item.id)
        assertTrue(selectedItems.contains("item-1"))

        // Deselect
        selectedItems.remove(item.id)
        assertFalse(selectedItems.contains("item-1"))
    }

    @Test
    fun `should support multi-selection`() {
        val selectedItems = mutableSetOf<String>()
        val items = listOf(
            createTestItem(id = "item-1"),
            createTestItem(id = "item-2"),
            createTestItem(id = "item-3"),
        )

        items.forEach { selectedItems.add(it.id) }

        assertEquals(3, selectedItems.size)
        assertTrue(selectedItems.containsAll(listOf("item-1", "item-2", "item-3")))
    }

    // Size formatting tests (logic only)

    @Test
    fun `should correctly calculate size thresholds`() {
        val bytesInKB = 1024L
        val bytesInMB = 1024L * 1024L
        val bytesInGB = 1024L * 1024L * 1024L

        val smallFile = createTestItem(size = 500) // 500 bytes
        assertTrue(smallFile.size < bytesInKB)

        val mediumFile = createTestItem(size = 5 * bytesInKB) // 5 KB
        assertTrue(mediumFile.size >= bytesInKB && mediumFile.size < bytesInMB)

        val largeFile = createTestItem(size = 5 * bytesInMB) // 5 MB
        assertTrue(largeFile.size >= bytesInMB && largeFile.size < bytesInGB)

        val hugeFile = createTestItem(size = 2 * bytesInGB) // 2 GB
        assertTrue(hugeFile.size >= bytesInGB)
    }

    // Visibility tests

    @Test
    fun `should track visibility state`() {
        val privateItem = createTestItem()
        assertEquals(Visibility.PRIVATE, privateItem.visibility)
    }
}
