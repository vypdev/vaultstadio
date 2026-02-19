/**
 * VaultStadio Activity Screen Tests
 */

package com.vaultstadio.app.ui.screens

import com.vaultstadio.app.domain.model.Activity
import kotlin.time.Clock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ActivityScreenTest {

    @Test
    fun testActivityFilterEnumValues() {
        val filters = ActivityFilter.entries
        assertEquals(4, filters.size)
        assertTrue(filters.contains(ActivityFilter.ALL))
        assertTrue(filters.contains(ActivityFilter.FILES))
        assertTrue(filters.contains(ActivityFilter.SHARING))
        assertTrue(filters.contains(ActivityFilter.AUTH))
    }

    @Test
    fun testActivityModel() {
        val now = Clock.System.now()
        val activity = Activity(
            id = "a1",
            type = "upload",
            userId = "user1",
            itemId = "item1",
            itemPath = "/documents/file.txt",
            details = "Uploaded new file",
            createdAt = now,
        )

        assertEquals("upload", activity.type)
        assertEquals("/documents/file.txt", activity.itemPath)
        assertEquals("Uploaded new file", activity.details)
    }

    @Test
    fun testActivityWithoutDetails() {
        val now = Clock.System.now()
        val activity = Activity(
            id = "a2",
            type = "login",
            userId = "user1",
            itemId = null,
            itemPath = null,
            details = null,
            createdAt = now,
        )

        assertEquals("login", activity.type)
        assertEquals(null, activity.itemId)
        assertEquals(null, activity.itemPath)
    }

    @Test
    fun testActivityTypes() {
        val fileTypes =
            listOf("upload", "download", "create", "delete", "move", "rename", "copy", "restore", "trash", "star")
        val sharingTypes = listOf("share", "unshare", "share_access")
        val authTypes = listOf("login", "logout", "register", "password_change")

        assertTrue(fileTypes.isNotEmpty())
        assertTrue(sharingTypes.isNotEmpty())
        assertTrue(authTypes.isNotEmpty())
    }
}
