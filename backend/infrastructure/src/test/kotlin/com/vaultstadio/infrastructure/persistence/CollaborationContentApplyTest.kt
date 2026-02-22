/**
 * VaultStadio CollaborationContentApply Tests
 *
 * Unit tests for applyOperationToContent (insert, delete, retain; position clamping).
 */

package com.vaultstadio.infrastructure.persistence

import com.vaultstadio.core.domain.model.CollaborationOperation
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class CollaborationContentApplyTest {

    private fun instant(epochMillis: Long): Instant = Instant.fromEpochMilliseconds(epochMillis)

    @Test
    fun `applyOperationToContent Insert at start prepends text`() {
        val op = CollaborationOperation.Insert(
            userId = "u1",
            timestamp = instant(0),
            baseVersion = 1,
            position = 0,
            text = "Hello",
        )
        val result = applyOperationToContent("World", op)
        assertEquals("HelloWorld", result)
    }

    @Test
    fun `applyOperationToContent Insert in middle inserts text`() {
        val op = CollaborationOperation.Insert(
            userId = "u1",
            timestamp = instant(0),
            baseVersion = 1,
            position = 2,
            text = "XX",
        )
        val result = applyOperationToContent("abc", op)
        assertEquals("abXXc", result)
    }

    @Test
    fun `applyOperationToContent Insert at end appends text`() {
        val op = CollaborationOperation.Insert(
            userId = "u1",
            timestamp = instant(0),
            baseVersion = 1,
            position = 5,
            text = "!",
        )
        val result = applyOperationToContent("Hello", op)
        assertEquals("Hello!", result)
    }

    @Test
    fun `applyOperationToContent Insert position clamped when out of range`() {
        val op = CollaborationOperation.Insert(
            userId = "u1",
            timestamp = instant(0),
            baseVersion = 1,
            position = 100,
            text = "X",
        )
        val result = applyOperationToContent("ab", op)
        assertEquals("abX", result)
    }

    @Test
    fun `applyOperationToContent Delete removes range`() {
        val op = CollaborationOperation.Delete(
            userId = "u1",
            timestamp = instant(0),
            baseVersion = 1,
            position = 1,
            length = 3,
        )
        val result = applyOperationToContent("Hello", op)
        assertEquals("Ho", result)
    }

    @Test
    fun `applyOperationToContent Delete full content yields empty string`() {
        val op = CollaborationOperation.Delete(
            userId = "u1",
            timestamp = instant(0),
            baseVersion = 1,
            position = 0,
            length = 5,
        )
        val result = applyOperationToContent("Hello", op)
        assertEquals("", result)
    }

    @Test
    fun `applyOperationToContent Delete position and length clamped when out of range`() {
        val op = CollaborationOperation.Delete(
            userId = "u1",
            timestamp = instant(0),
            baseVersion = 1,
            position = 1,
            length = 100,
        )
        val result = applyOperationToContent("abc", op)
        assertEquals("a", result)
    }

    @Test
    fun `applyOperationToContent Retain leaves content unchanged`() {
        val op = CollaborationOperation.Retain(
            userId = "u1",
            timestamp = instant(0),
            baseVersion = 1,
            count = 5,
        )
        val content = "Hello"
        val result = applyOperationToContent(content, op)
        assertEquals(content, result)
    }

    @Test
    fun `applyOperationToContent Retain with empty content returns empty string`() {
        val op = CollaborationOperation.Retain(
            userId = "u1",
            timestamp = instant(0),
            baseVersion = 1,
            count = 0,
        )
        val result = applyOperationToContent("", op)
        assertEquals("", result)
    }

    @Test
    fun `applyOperationToContent Insert into empty string`() {
        val op = CollaborationOperation.Insert(
            userId = "u1",
            timestamp = instant(0),
            baseVersion = 1,
            position = 0,
            text = "x",
        )
        val result = applyOperationToContent("", op)
        assertEquals("x", result)
    }

    @Test
    fun `applyOperationToContent Delete with length zero leaves content unchanged`() {
        val op = CollaborationOperation.Delete(
            userId = "u1",
            timestamp = instant(0),
            baseVersion = 1,
            position = 1,
            length = 0,
        )
        val result = applyOperationToContent("abc", op)
        assertEquals("abc", result)
    }

    @Test
    fun `applyOperationToContent Insert with negative position clamped to start`() {
        val op = CollaborationOperation.Insert(
            userId = "u1",
            timestamp = instant(0),
            baseVersion = 1,
            position = -1,
            text = "X",
        )
        val result = applyOperationToContent("ab", op)
        assertEquals("Xab", result)
    }
}
