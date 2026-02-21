/**
 * Unit tests for StorageItem computed properties (extension, isRoot, parentPath).
 * StorageItem lives in domain:storage; core tests it for compatibility.
 */

package com.vaultstadio.core.domain.model

import com.vaultstadio.domain.storage.model.ItemType
import com.vaultstadio.domain.storage.model.StorageItem
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

private val testInstant = Instant.fromEpochMilliseconds(0L)

private fun fileItem(
    name: String = "doc.pdf",
    path: String = "/documents/doc.pdf",
    parentId: String? = "parent-1",
) = StorageItem(
    name = name,
    path = path,
    type = ItemType.FILE,
    parentId = parentId,
    ownerId = "user-1",
    createdAt = testInstant,
    updatedAt = testInstant,
)

private fun folderItem(
    name: String = "documents",
    path: String = "/documents",
    parentId: String? = null,
) = StorageItem(
    name = name,
    path = path,
    type = ItemType.FOLDER,
    parentId = parentId,
    ownerId = "user-1",
    createdAt = testInstant,
    updatedAt = testInstant,
)

class StorageItemTest {

    @Test
    fun extension_returnsExtensionForFile() {
        assertEquals("pdf", fileItem(name = "doc.pdf").extension)
        assertEquals("txt", fileItem(name = "readme.txt").extension)
    }

    @Test
    fun extension_returnsNullForFileWithNoExtension() {
        assertNull(fileItem(name = "README").extension)
    }

    @Test
    fun extension_returnsNullForFolder() {
        assertNull(folderItem(name = "documents").extension)
    }

    @Test
    fun isRoot_trueWhenParentIdNull() {
        assertTrue(folderItem(path = "/", parentId = null).isRoot)
    }

    @Test
    fun isRoot_falseWhenParentIdSet() {
        assertFalse(folderItem(parentId = "parent-1").isRoot)
        assertFalse(fileItem(parentId = "parent-1").isRoot)
    }

    @Test
    fun parentPath_returnsPathBeforeLastSlash() {
        assertEquals("/documents", fileItem(path = "/documents/doc.pdf").parentPath)
        assertEquals("", fileItem(path = "doc.pdf").parentPath)
        assertEquals("/a/b", fileItem(path = "/a/b/c.pdf").parentPath)
    }
}
