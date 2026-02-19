/**
 * VaultStadio Collaboration OT Tests
 *
 * Unit tests for transformCollaborationOperation and transformCollaborationAgainst (internal).
 */

package com.vaultstadio.core.domain.service

import com.vaultstadio.core.domain.model.CollaborationOperation
import com.vaultstadio.core.domain.model.DocumentState
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class CollaborationOTTest {

    private fun instant(epochMillis: Long): Instant = Instant.fromEpochMilliseconds(epochMillis)

    @Nested
    inner class TransformCollaborationOperationTests {

        @Test
        fun `when operation baseVersion at least state version returns operation unchanged`() {
            val op = CollaborationOperation.Insert(
                userId = "u1",
                timestamp = instant(100),
                baseVersion = 2,
                position = 0,
                text = "hi",
            )
            val state = DocumentState(
                itemId = "item1",
                version = 1,
                content = "",
                operations = emptyList(),
                lastModified = instant(50),
            )
            val result = transformCollaborationOperation(op, state)
            assertEquals(op, result)
        }

        @Test
        fun `when operation baseVersion less than state version transforms against concurrent ops`() {
            val op = CollaborationOperation.Insert(
                userId = "u1",
                timestamp = instant(50),
                baseVersion = 1,
                position = 0,
                text = "A",
            )
            val concurrent = CollaborationOperation.Insert(
                userId = "u2",
                timestamp = instant(60),
                baseVersion = 1,
                position = 0,
                text = "B",
            )
            val state = DocumentState(
                itemId = "item1",
                version = 2,
                content = "BA",
                operations = listOf(concurrent),
                lastModified = instant(60),
            )
            val result = transformCollaborationOperation(op, state)
            // op at 0, concurrent inserted "B" at 0 -> op should move to position 1
            assertEquals(1, (result as CollaborationOperation.Insert).position)
            assertEquals("A", result.text)
        }
    }

    @Nested
    inner class TransformCollaborationAgainstTests {

        @Test
        fun `Insert vs Insert when concurrent before position shifts position`() {
            val op = CollaborationOperation.Insert(
                userId = "u1",
                timestamp = instant(100),
                baseVersion = 1,
                position = 5,
                text = "X",
            )
            val concurrent = CollaborationOperation.Insert(
                userId = "u2",
                timestamp = instant(110),
                baseVersion = 1,
                position = 2,
                text = "ab",
            )
            val result = transformCollaborationAgainst(op, concurrent)
            assertEquals(7, (result as CollaborationOperation.Insert).position) // 5 + 2
            assertEquals("X", result.text)
        }

        @Test
        fun `Insert vs Insert when concurrent after position leaves op unchanged`() {
            val op = CollaborationOperation.Insert(
                userId = "u1",
                timestamp = instant(100),
                baseVersion = 1,
                position = 2,
                text = "X",
            )
            val concurrent = CollaborationOperation.Insert(
                userId = "u2",
                timestamp = instant(110),
                baseVersion = 1,
                position = 5,
                text = "ab",
            )
            val result = transformCollaborationAgainst(op, concurrent)
            assertEquals(2, (result as CollaborationOperation.Insert).position)
        }

        @Test
        fun `Insert vs Retain leaves op unchanged`() {
            val op = CollaborationOperation.Insert(
                userId = "u1",
                timestamp = instant(100),
                baseVersion = 1,
                position = 3,
                text = "Y",
            )
            val concurrent = CollaborationOperation.Retain(
                userId = "u2",
                timestamp = instant(110),
                baseVersion = 1,
                count = 10,
            )
            val result = transformCollaborationAgainst(op, concurrent)
            assertEquals(op, result)
        }

        @Test
        fun `Retain vs anything leaves op unchanged`() {
            val op = CollaborationOperation.Retain(
                userId = "u1",
                timestamp = instant(100),
                baseVersion = 1,
                count = 5,
            )
            val concurrent = CollaborationOperation.Insert(
                userId = "u2",
                timestamp = instant(110),
                baseVersion = 1,
                position = 0,
                text = "Z",
            )
            val result = transformCollaborationAgainst(op, concurrent)
            assertEquals(op, result)
        }

        @Test
        fun `Delete vs Insert when insert before delete position adjusts position`() {
            val op = CollaborationOperation.Delete(
                userId = "u1",
                timestamp = instant(100),
                baseVersion = 1,
                position = 5,
                length = 2,
            )
            val concurrent = CollaborationOperation.Insert(
                userId = "u2",
                timestamp = instant(110),
                baseVersion = 1,
                position = 3,
                text = "!!",
            )
            val result = transformCollaborationAgainst(op, concurrent)
            assertEquals(7, (result as CollaborationOperation.Delete).position) // 5 + 2
            assertEquals(2, result.length)
        }
    }
}
