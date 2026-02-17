/**
 * Tests for LockManager
 */

package com.vaultstadio.core.domain.service

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LockManagerTest {

    private lateinit var lockManager: InMemoryLockManager

    @BeforeEach
    fun setup() {
        lockManager = InMemoryLockManager()
    }

    @Nested
    inner class LockAcquisitionTests {

        @Test
        fun `can acquire lock on unlocked resource`() = runBlocking {
            val lock = lockManager.lock("/path/to/file", "user1")

            assertNotNull(lock)
            assertEquals("/path/to/file", lock.resource)
            assertEquals("user1", lock.owner)
            assertTrue(lock.token.startsWith("opaquelocktoken:"))
        }

        @Test
        fun `cannot acquire lock on already locked resource`() = runBlocking {
            val lock1 = lockManager.lock("/path/to/file", "user1")
            val lock2 = lockManager.lock("/path/to/file", "user2")

            assertNotNull(lock1)
            assertNull(lock2, "Second lock should fail")
        }

        @Test
        fun `can acquire lock with custom timeout`() = runBlocking {
            val lock = lockManager.lock("/file", "user", timeoutSeconds = 7200)

            assertNotNull(lock)
            assertEquals(7200, lock.timeoutSeconds)
        }

        @Test
        fun `can acquire locks on different resources`() = runBlocking {
            val lock1 = lockManager.lock("/file1", "user1")
            val lock2 = lockManager.lock("/file2", "user2")

            assertNotNull(lock1)
            assertNotNull(lock2)
        }
    }

    @Nested
    inner class UnlockTests {

        @Test
        fun `can unlock with correct token`() = runBlocking {
            val lock = lockManager.lock("/file", "user")!!

            val result = lockManager.unlock("/file", lock.token)

            assertTrue(result)
            assertFalse(lockManager.isLocked("/file"))
        }

        @Test
        fun `cannot unlock with wrong token`() = runBlocking {
            lockManager.lock("/file", "user")

            val result = lockManager.unlock("/file", "wrong-token")

            assertFalse(result)
            assertTrue(lockManager.isLocked("/file"))
        }

        @Test
        fun `cannot unlock non-existent lock`() = runBlocking {
            val result = lockManager.unlock("/nonexistent", "token")

            assertFalse(result)
        }
    }

    @Nested
    inner class LockQueryTests {

        @Test
        fun `getLock returns lock for locked resource`() = runBlocking {
            val originalLock = lockManager.lock("/file", "user")!!

            val retrievedLock = lockManager.getLock("/file")

            assertNotNull(retrievedLock)
            assertEquals(originalLock.token, retrievedLock.token)
        }

        @Test
        fun `getLock returns null for unlocked resource`() = runBlocking {
            val lock = lockManager.getLock("/unlocked")

            assertNull(lock)
        }

        @Test
        fun `isLocked returns true for locked resource`() = runBlocking {
            lockManager.lock("/file", "user")

            assertTrue(lockManager.isLocked("/file"))
        }

        @Test
        fun `isLocked returns false for unlocked resource`() = runBlocking {
            assertFalse(lockManager.isLocked("/file"))
        }
    }

    @Nested
    inner class RefreshTests {

        @Test
        fun `can refresh lock with correct token`() = runBlocking {
            val originalLock = lockManager.lock("/file", "user", timeoutSeconds = 3600)!!

            val refreshed = lockManager.refresh("/file", originalLock.token, 7200)

            assertNotNull(refreshed)
            assertEquals(7200, refreshed.timeoutSeconds)
            assertEquals(originalLock.token, refreshed.token)
        }

        @Test
        fun `cannot refresh with wrong token`() = runBlocking {
            lockManager.lock("/file", "user")

            val refreshed = lockManager.refresh("/file", "wrong-token", 7200)

            assertNull(refreshed)
        }

        @Test
        fun `cannot refresh non-existent lock`() = runBlocking {
            val refreshed = lockManager.refresh("/nonexistent", "token", 7200)

            assertNull(refreshed)
        }
    }

    @Nested
    inner class ExpirationTests {

        @Test
        fun `isExpired returns false for fresh lock`() {
            val lock = DistributedLock(
                token = "token",
                resource = "/file",
                owner = "user",
                timeoutSeconds = 3600,
                createdAt = System.currentTimeMillis(),
            )

            assertFalse(lock.isExpired())
        }

        @Test
        fun `isExpired returns true for old lock`() {
            val lock = DistributedLock(
                token = "token",
                resource = "/file",
                owner = "user",
                timeoutSeconds = 1,
                createdAt = System.currentTimeMillis() - 2000, // 2 seconds ago
            )

            assertTrue(lock.isExpired())
        }

        @Test
        fun `can acquire lock on expired resource`() = runBlocking {
            // Create a lock that expires quickly
            val lock1 = lockManager.lock("/file", "user1", timeoutSeconds = 0)

            // Wait for expiration
            Thread.sleep(10)

            // Should be able to acquire new lock
            val lock2 = lockManager.lock("/file", "user2")

            assertNotNull(lock2, "Should acquire lock after expiration")
            assertEquals("user2", lock2.owner)
        }

        @Test
        fun `cleanupExpired removes expired locks`() = runBlocking {
            // Create expired lock by using 0 timeout
            lockManager.lock("/file1", "user1", timeoutSeconds = 0)
            lockManager.lock("/file2", "user2", timeoutSeconds = 3600) // Not expired

            Thread.sleep(10)

            val removed = lockManager.cleanupExpired()

            assertEquals(1, removed)
            assertFalse(lockManager.isLocked("/file1"))
            assertTrue(lockManager.isLocked("/file2"))
        }
    }
}
