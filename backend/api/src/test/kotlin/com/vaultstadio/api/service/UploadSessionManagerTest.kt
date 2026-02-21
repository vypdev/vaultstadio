/**
 * VaultStadio Upload Session Manager Tests
 *
 * Unit tests for InMemoryUploadSessionManager and UploadSession.
 */

package com.vaultstadio.api.service

import com.vaultstadio.core.domain.service.UploadSession
import com.vaultstadio.core.domain.service.UploadSessionManager
import kotlinx.datetime.Clock
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.milliseconds

class UploadSessionManagerTest {

    private lateinit var manager: UploadSessionManager

    @BeforeEach
    fun setup() {
        manager = InMemoryUploadSessionManager()
    }

    private fun createSession(
        id: String = "upload-1",
        userId: String = "user-1",
        totalChunks: Int = 5,
        chunkSize: Long = 1024,
        tempDir: String = java.io.File.createTempFile("upload_test_", "").parent + "/session-$id",
    ): UploadSession {
        val now = Clock.System.now()
        return UploadSession(
            id = id,
            userId = userId,
            fileName = "file.bin",
            totalSize = totalChunks * chunkSize,
            mimeType = "application/octet-stream",
            parentId = null,
            chunkSize = chunkSize,
            totalChunks = totalChunks,
            receivedChunks = mutableSetOf(),
            createdAt = now,
            lastActivityAt = now,
            tempDir = tempDir,
        )
    }

    @Nested
    inner class CreateAndGetTests {

        @Test
        fun `createSession stores session and getSession returns it`() {
            val session = createSession()
            manager.createSession(session)
            val retrieved = manager.getSession(session.id)
            assertNotNull(retrieved)
            assertEquals(session.id, retrieved!!.id)
            assertEquals(session.userId, retrieved.userId)
            assertEquals(session.fileName, retrieved.fileName)
        }

        @Test
        fun `getSessionForUser returns session only for matching user`() {
            val session = createSession(id = "up-1", userId = "alice")
            manager.createSession(session)
            assertNotNull(manager.getSessionForUser("up-1", "alice"))
            assertNull(manager.getSessionForUser("up-1", "bob"))
        }

        @Test
        fun `getSession returns null for missing id`() {
            assertNull(manager.getSession("nonexistent"))
        }
    }

    @Nested
    inner class UpdateAndChunksTests {

        @Test
        fun `updateSession overwrites session`() {
            val session = createSession()
            manager.createSession(session)
            session.receivedChunks.add(0)
            session.receivedChunks.add(1)
            manager.updateSession(session)
            val retrieved = manager.getSession(session.id)
            assertEquals(2, retrieved!!.receivedChunks.size)
        }

        @Test
        fun `markChunkReceived adds chunk and returns true`() {
            val session = createSession()
            manager.createSession(session)
            assertTrue(manager.markChunkReceived(session.id, 0))
            assertTrue(manager.markChunkReceived(session.id, 1))
            val retrieved = manager.getSession(session.id)
            assertTrue(retrieved!!.receivedChunks.contains(0))
            assertTrue(retrieved.receivedChunks.contains(1))
        }

        @Test
        fun `markChunkReceived returns false for unknown session`() {
            assertFalse(manager.markChunkReceived("nonexistent", 0))
        }
    }

    @Nested
    inner class RemoveAndCleanupTests {

        @Test
        fun `removeSession removes and returns session`() {
            val session = createSession()
            manager.createSession(session)
            val removed = manager.removeSession(session.id)
            assertNotNull(removed)
            assertEquals(session.id, removed!!.id)
            assertNull(manager.getSession(session.id))
        }

        @Test
        fun `removeSession returns null for unknown id`() {
            assertNull(manager.removeSession("nonexistent"))
        }

        @Test
        fun `cleanupExpiredSessions does not remove recent sessions`() {
            val session = createSession(id = "recent-session")
            manager.createSession(session)
            assertEquals(1, manager.getActiveSessionCount())
            val cleaned = manager.cleanupExpiredSessions(maxAge = 1.milliseconds)
            assertEquals(0, cleaned)
            assertNotNull(manager.getSession("recent-session"))
        }
    }

    @Nested
    inner class ListAndCountTests {

        @Test
        fun `getSessionsForUser returns only that user sessions`() {
            manager.createSession(createSession(id = "a1", userId = "u1"))
            manager.createSession(createSession(id = "a2", userId = "u1"))
            manager.createSession(createSession(id = "b1", userId = "u2"))
            val list = manager.getSessionsForUser("u1")
            assertEquals(2, list.size)
            assertTrue(list.all { it.userId == "u1" })
        }

        @Test
        fun `getActiveSessionCount returns number of sessions`() {
            assertEquals(0, manager.getActiveSessionCount())
            manager.createSession(createSession(id = "1"))
            assertEquals(1, manager.getActiveSessionCount())
            manager.createSession(createSession(id = "2"))
            assertEquals(2, manager.getActiveSessionCount())
            manager.removeSession("1")
            assertEquals(1, manager.getActiveSessionCount())
        }
    }

    @Nested
    inner class UploadSessionPropertiesTests {

        @Test
        fun `isComplete true when all chunks received`() {
            val session = createSession(totalChunks = 3)
            session.receivedChunks.addAll(listOf(0, 1, 2))
            assertTrue(session.isComplete)
        }

        @Test
        fun `isComplete false when not all chunks received`() {
            val session = createSession(totalChunks = 3)
            session.receivedChunks.add(0)
            assertFalse(session.isComplete)
        }

        @Test
        fun `uploadedBytes and progress`() {
            val session = createSession(totalChunks = 4, chunkSize = 100)
            session.receivedChunks.addAll(listOf(0, 1))
            assertEquals(200L, session.uploadedBytes)
            assertEquals(0.5f, session.progress)
        }
    }
}
