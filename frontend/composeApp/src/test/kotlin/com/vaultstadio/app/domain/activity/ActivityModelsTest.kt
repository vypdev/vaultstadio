/**
 * Unit tests for activity domain models: Activity.
 */

package com.vaultstadio.app.domain.activity

import com.vaultstadio.app.domain.activity.model.Activity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.datetime.Instant

class ActivityTest {

    private val testInstant = Instant.fromEpochMilliseconds(0L)

    @Test
    fun activity_construction() {
        val activity = Activity(
            id = "act-1",
            type = "FILE_UPLOADED",
            userId = "user-1",
            itemId = "item-1",
            itemPath = "/path/file.txt",
            details = "{\"size\":1024}",
            createdAt = testInstant,
        )
        assertEquals("act-1", activity.id)
        assertEquals("FILE_UPLOADED", activity.type)
        assertEquals("user-1", activity.userId)
        assertEquals("item-1", activity.itemId)
        assertEquals("/path/file.txt", activity.itemPath)
        assertEquals("{\"size\":1024}", activity.details)
        assertEquals(testInstant, activity.createdAt)
    }

    @Test
    fun activity_withNullOptionals() {
        val activity = Activity(
            id = "a2",
            type = "USER_LOGOUT",
            userId = null,
            itemId = null,
            itemPath = null,
            details = null,
            createdAt = testInstant,
        )
        assertNull(activity.userId)
        assertNull(activity.itemId)
        assertNull(activity.itemPath)
        assertNull(activity.details)
    }

    @Test
    fun activity_withEmptyDetails() {
        val activity = Activity(
            id = "a3",
            type = "FOLDER_CREATED",
            userId = "u1",
            itemId = "folder-1",
            itemPath = "/docs",
            details = "",
            createdAt = testInstant,
        )
        assertEquals("", activity.details)
        assertEquals("FOLDER_CREATED", activity.type)
    }
}
