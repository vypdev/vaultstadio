/**
 * Unit tests for share domain models: ShareLink.
 */

package com.vaultstadio.app.domain.share

import com.vaultstadio.app.domain.share.model.ShareLink
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.datetime.Instant

class ShareLinkTest {

    private val testInstant = Instant.fromEpochMilliseconds(0L)

    @Test
    fun shareLink_construction() {
        val link = ShareLink(
            id = "share-1",
            itemId = "item-1",
            token = "abc123",
            url = "https://api.example.com/share/abc123",
            expiresAt = testInstant,
            hasPassword = false,
            maxDownloads = 10,
            downloadCount = 0,
            isActive = true,
            createdAt = testInstant,
            createdBy = "user-1",
            sharedWithUsers = emptyList(),
        )
        assertEquals("share-1", link.id)
        assertEquals("item-1", link.itemId)
        assertEquals("abc123", link.token)
        assertEquals(10, link.maxDownloads)
        assertFalse(link.hasPassword)
        assertTrue(link.isActive)
        assertEquals("user-1", link.createdBy)
    }

    @Test
    fun shareLink_defaults() {
        val link = ShareLink(
            id = "s2",
            itemId = "i2",
            token = "t",
            url = "https://api.test/share/t",
            expiresAt = null,
            hasPassword = true,
            maxDownloads = null,
            downloadCount = 1,
            isActive = false,
            createdAt = testInstant,
            createdBy = "",
        )
        assertEquals("", link.createdBy)
        assertTrue(link.sharedWithUsers.isEmpty())
    }

    @Test
    fun shareLink_withSharedWithUsers() {
        val link = ShareLink(
            id = "s3",
            itemId = "i3",
            token = "tok",
            url = "https://api.test/share/tok",
            expiresAt = null,
            hasPassword = false,
            maxDownloads = 5,
            downloadCount = 0,
            isActive = true,
            createdAt = testInstant,
            createdBy = "owner",
            sharedWithUsers = listOf("user-a", "user-b"),
        )
        assertEquals(2, link.sharedWithUsers.size)
        assertEquals("user-a", link.sharedWithUsers[0])
        assertEquals("user-b", link.sharedWithUsers[1])
    }
}
