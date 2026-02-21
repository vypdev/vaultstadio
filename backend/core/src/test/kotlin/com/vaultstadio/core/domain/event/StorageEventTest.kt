/**
 * Tests for StorageEvent sealed classes (FileEvent, FolderEvent, ShareEvent, UserEvent, SystemEvent).
 * Verifies instantiation and property access to improve domain.event package coverage.
 */

package com.vaultstadio.core.domain.event

import com.vaultstadio.domain.auth.model.UserInfo
import com.vaultstadio.domain.auth.model.UserRole
import com.vaultstadio.domain.auth.model.UserStatus
import com.vaultstadio.domain.share.model.ShareLink
import com.vaultstadio.domain.storage.model.ItemType
import com.vaultstadio.domain.storage.model.StorageItem
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class StorageEventTest {

    private val now: Instant = Clock.System.now()
    private val testItem: StorageItem = StorageItem(
        id = "item-1",
        name = "test.txt",
        path = "/test.txt",
        type = ItemType.FILE,
        ownerId = "user-1",
        size = 100,
        mimeType = "text/plain",
        createdAt = now,
        updatedAt = now,
    )
    private val testFolder: StorageItem = StorageItem(
        id = "folder-1",
        name = "docs",
        path = "/docs",
        type = ItemType.FOLDER,
        ownerId = "user-1",
        size = 0,
        mimeType = null,
        createdAt = now,
        updatedAt = now,
    )

    @Test
    fun `FileEvent Uploaded has expected properties`() {
        val e = FileEvent.Uploaded(userId = "u1", item = testItem, contentStream = "file:///tmp/x")
        assertEquals("u1", e.userId)
        assertEquals(testItem, e.item)
        assertEquals("file:///tmp/x", e.contentStream)
        assertNotNull(e.id)
        assertNotNull(e.timestamp)
    }

    @Test
    fun `FileEvent Downloaded with share`() {
        val e = FileEvent.Downloaded(userId = "u1", item = testItem, accessedViaShare = true, shareId = "share-1")
        assertTrue(e.accessedViaShare)
        assertEquals("share-1", e.shareId)
    }

    @Test
    fun `FileEvent Deleting permanent`() {
        val e = FileEvent.Deleting(userId = "u1", item = testItem, permanent = true)
        assertTrue(e.permanent)
    }

    @Test
    fun `FileEvent Deleted`() {
        val e = FileEvent.Deleted(userId = "u1", item = testItem, permanent = false)
        assertEquals(testItem, e.item)
    }

    @Test
    fun `FileEvent Moved has previousPath and previousParentId`() {
        val e = FileEvent.Moved(
            userId = "u1",
            item = testItem,
            previousPath = "/old.txt",
            previousParentId = "parent-1",
        )
        assertEquals("/old.txt", e.previousPath)
        assertEquals("parent-1", e.previousParentId)
    }

    @Test
    fun `FileEvent Renamed has previousName`() {
        val e = FileEvent.Renamed(userId = "u1", item = testItem, previousName = "old.txt")
        assertEquals("old.txt", e.previousName)
    }

    @Test
    fun `FileEvent Copied has sourceItem`() {
        val source = testItem.copy(id = "source-1")
        val e = FileEvent.Copied(userId = "u1", item = testItem, sourceItem = source)
        assertEquals(source, e.sourceItem)
    }

    @Test
    fun `FileEvent Restored`() {
        val e = FileEvent.Restored(userId = "u1", item = testItem)
        assertEquals(testItem, e.item)
    }

    @Test
    fun `FileEvent StarredChanged`() {
        val e = FileEvent.StarredChanged(userId = "u1", item = testItem, isStarred = true)
        assertTrue(e.isStarred)
    }

    @Test
    fun `FolderEvent Created`() {
        val e = FolderEvent.Created(userId = "u1", folder = testFolder)
        assertEquals(testFolder, e.folder)
    }

    @Test
    fun `FolderEvent Deleted with itemCount`() {
        val e = FolderEvent.Deleted(userId = "u1", folder = testFolder, itemCount = 5)
        assertEquals(5, e.itemCount)
    }

    @Test
    fun `FolderEvent Moved`() {
        val e = FolderEvent.Moved(userId = "u1", folder = testFolder, previousPath = "/old/docs")
        assertEquals("/old/docs", e.previousPath)
    }

    @Test
    fun `ShareEvent Created`() {
        val share = ShareLink(itemId = "i1", createdBy = "u1", token = "t1", createdAt = now)
        val e = ShareEvent.Created(userId = "u1", share = share, item = testItem)
        assertEquals(share, e.share)
        assertEquals(testItem, e.item)
    }

    @Test
    fun `ShareEvent Accessed`() {
        val share = ShareLink(itemId = "i1", createdBy = "u1", token = "t1", createdAt = now)
        val e = ShareEvent.Accessed(
            userId = "u1",
            share = share,
            item = testItem,
            ipAddress = "127.0.0.1",
            userAgent = "Test",
        )
        assertEquals("127.0.0.1", e.ipAddress)
        assertEquals("Test", e.userAgent)
    }

    @Test
    fun `ShareEvent Deleted`() {
        val e = ShareEvent.Deleted(userId = "u1", shareId = "share-1", itemId = "item-1")
        assertEquals("share-1", e.shareId)
        assertEquals("item-1", e.itemId)
    }

    @Test
    fun `UserEvent LoggedIn`() {
        val e = UserEvent.LoggedIn(userId = "u1", ipAddress = "1.2.3.4", userAgent = "Mozilla")
        assertEquals("1.2.3.4", e.ipAddress)
        assertEquals("Mozilla", e.userAgent)
    }

    @Test
    fun `UserEvent LoggedOut`() {
        val e = UserEvent.LoggedOut(userId = "u1")
        assertEquals("u1", e.userId)
    }

    @Test
    fun `UserEvent Created`() {
        val user = UserInfo(
            id = "u1",
            email = "a@b.com",
            username = "alice",
            role = UserRole.USER,
            status = UserStatus.ACTIVE,
            quotaBytes = 1_000_000,
            avatarUrl = null,
            createdAt = now,
        )
        val e = UserEvent.Created(userId = "u1", user = user)
        assertEquals(user, e.user)
    }

    @Test
    fun `UserEvent QuotaChanged`() {
        val e = UserEvent.QuotaChanged(userId = "u1", previousQuota = 1000, newQuota = 2000)
        assertEquals(1000, e.previousQuota)
        assertEquals(2000, e.newQuota)
    }

    @Test
    fun `SystemEvent PluginInstalled`() {
        val e = SystemEvent.PluginInstalled(userId = "u1", pluginId = "p1", pluginVersion = "1.0")
        assertEquals("p1", e.pluginId)
        assertEquals("1.0", e.pluginVersion)
    }

    @Test
    fun `SystemEvent PluginUninstalled`() {
        val e = SystemEvent.PluginUninstalled(userId = "u1", pluginId = "p1")
        assertEquals("p1", e.pluginId)
    }

    @Test
    fun `SystemEvent MaintenanceRun`() {
        val e = SystemEvent.MaintenanceRun(userId = null, taskType = "cleanup", details = "removed 10 files")
        assertEquals("cleanup", e.taskType)
        assertEquals("removed 10 files", e.details)
    }
}
