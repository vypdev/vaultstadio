/**
 * VaultStadio WebDAV Routes Tests
 *
 * Unit tests for WebDAV protocol components.
 */

package com.vaultstadio.api.routes

import com.vaultstadio.api.routes.storage.WebDAVMethods
import com.vaultstadio.api.routes.storage.WebDAVProperties
import com.vaultstadio.core.domain.service.DistributedLock
import com.vaultstadio.core.domain.service.InMemoryLockManager
import com.vaultstadio.core.domain.service.LockManager
import io.ktor.http.HttpMethod
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WebDAVRoutesTest {

    private lateinit var lockManager: LockManager

    @BeforeEach
    fun setup() {
        // Create a fresh LockManager for each test
        lockManager = InMemoryLockManager()
    }

    // ========================================================================
    // WebDAV Methods Object Tests
    // ========================================================================

    @Test
    fun `WebDAVMethods should define PROPFIND method`() {
        assertEquals("PROPFIND", WebDAVMethods.PROPFIND.value)
        assertEquals(HttpMethod("PROPFIND"), WebDAVMethods.PROPFIND)
    }

    @Test
    fun `WebDAVMethods should define PROPPATCH method`() {
        assertEquals("PROPPATCH", WebDAVMethods.PROPPATCH.value)
        assertEquals(HttpMethod("PROPPATCH"), WebDAVMethods.PROPPATCH)
    }

    @Test
    fun `WebDAVMethods should define MKCOL method`() {
        assertEquals("MKCOL", WebDAVMethods.MKCOL.value)
        assertEquals(HttpMethod("MKCOL"), WebDAVMethods.MKCOL)
    }

    @Test
    fun `WebDAVMethods should define COPY method`() {
        assertEquals("COPY", WebDAVMethods.COPY.value)
        assertEquals(HttpMethod("COPY"), WebDAVMethods.COPY)
    }

    @Test
    fun `WebDAVMethods should define MOVE method`() {
        assertEquals("MOVE", WebDAVMethods.MOVE.value)
        assertEquals(HttpMethod("MOVE"), WebDAVMethods.MOVE)
    }

    @Test
    fun `WebDAVMethods should define LOCK method`() {
        assertEquals("LOCK", WebDAVMethods.LOCK.value)
        assertEquals(HttpMethod("LOCK"), WebDAVMethods.LOCK)
    }

    @Test
    fun `WebDAVMethods should define UNLOCK method`() {
        assertEquals("UNLOCK", WebDAVMethods.UNLOCK.value)
        assertEquals(HttpMethod("UNLOCK"), WebDAVMethods.UNLOCK)
    }

    // ========================================================================
    // WebDAV Properties Object Tests
    // ========================================================================

    @Test
    fun `WebDAVProperties DISPLAY_NAME should be correct`() {
        assertEquals("displayname", WebDAVProperties.DISPLAY_NAME)
    }

    @Test
    fun `WebDAVProperties RESOURCE_TYPE should be correct`() {
        assertEquals("resourcetype", WebDAVProperties.RESOURCE_TYPE)
    }

    @Test
    fun `WebDAVProperties CONTENT_TYPE should be correct`() {
        assertEquals("getcontenttype", WebDAVProperties.CONTENT_TYPE)
    }

    @Test
    fun `WebDAVProperties CONTENT_LENGTH should be correct`() {
        assertEquals("getcontentlength", WebDAVProperties.CONTENT_LENGTH)
    }

    @Test
    fun `WebDAVProperties CREATION_DATE should be correct`() {
        assertEquals("creationdate", WebDAVProperties.CREATION_DATE)
    }

    @Test
    fun `WebDAVProperties LAST_MODIFIED should be correct`() {
        assertEquals("getlastmodified", WebDAVProperties.LAST_MODIFIED)
    }

    @Test
    fun `WebDAVProperties ETAG should be correct`() {
        assertEquals("getetag", WebDAVProperties.ETAG)
    }

    @Test
    fun `WebDAVProperties LOCK_DISCOVERY should be correct`() {
        assertEquals("lockdiscovery", WebDAVProperties.LOCK_DISCOVERY)
    }

    @Test
    fun `WebDAVProperties SUPPORTED_LOCK should be correct`() {
        assertEquals("supportedlock", WebDAVProperties.SUPPORTED_LOCK)
    }

    // ========================================================================
    // LockManager Tests - Create Lock
    // ========================================================================

    @Test
    fun `LockManager lock should create exclusive lock`() = runBlocking {
        val resource = "/test/exclusive/lock"
        val owner = "user-123"

        val lock = lockManager.lock(resource, owner, "0", 3600L, true)

        try {
            assertNotNull(lock)
            assertEquals(resource, lock.resource)
            assertEquals(owner, lock.owner)
            assertEquals("0", lock.depth)
            assertEquals(3600L, lock.timeoutSeconds)
            assertTrue(lock.exclusive)
            assertTrue(lock.token.isNotEmpty())
            assertTrue(lock.createdAt > 0)
        } finally {
            lock?.let { lockManager.unlock(resource, it.token) }
        }
    }

    @Test
    fun `LockManager lock should create shared lock`() = runBlocking {
        val resource = "/test/shared/lock"

        val lock = lockManager.lock(resource, "user-1", "0", 3600L, false)

        try {
            assertNotNull(lock)
            assertFalse(lock.exclusive)
        } finally {
            lock?.let { lockManager.unlock(resource, it.token) }
        }
    }

    @Test
    fun `LockManager lock should support infinity depth`() = runBlocking {
        val resource = "/test/infinity/lock"

        val lock = lockManager.lock(resource, "user-1", "infinity", 7200L, true)

        try {
            assertNotNull(lock)
            assertEquals("infinity", lock.depth)
            assertEquals(7200L, lock.timeoutSeconds)
        } finally {
            lock?.let { lockManager.unlock(resource, it.token) }
        }
    }

    @Test
    fun `LockManager lock should generate unique tokens`() = runBlocking {
        val resource1 = "/test/unique/path1"
        val resource2 = "/test/unique/path2"

        val lock1 = lockManager.lock(resource1, "user-1", "0", 3600L, true)
        val lock2 = lockManager.lock(resource2, "user-1", "0", 3600L, true)

        try {
            assertNotNull(lock1)
            assertNotNull(lock2)
            assertTrue(lock1.token != lock2.token)
        } finally {
            lock1?.let { lockManager.unlock(resource1, it.token) }
            lock2?.let { lockManager.unlock(resource2, it.token) }
        }
    }

    // ========================================================================
    // LockManager Tests - isLocked
    // ========================================================================

    @Test
    fun `LockManager isLocked should return true for locked resource`() = runBlocking {
        val resource = "/test/islocked/true"
        val lock = lockManager.lock(resource, "user-1", "0", 3600L, true)

        try {
            assertNotNull(lock)
            assertTrue(lockManager.isLocked(resource))
        } finally {
            lock?.let { lockManager.unlock(resource, it.token) }
        }
    }

    @Test
    fun `LockManager isLocked should return false for unlocked resource`() = runBlocking {
        val resource = "/test/islocked/false"
        assertFalse(lockManager.isLocked(resource))
    }

    // ========================================================================
    // LockManager Tests - getLock
    // ========================================================================

    @Test
    fun `LockManager getLock should return lock for locked resource`() = runBlocking {
        val resource = "/test/getlock/exists"
        val lock = lockManager.lock(resource, "user-1", "0", 3600L, true)

        try {
            assertNotNull(lock)
            val retrieved = lockManager.getLock(resource)
            assertNotNull(retrieved)
            assertEquals(lock.token, retrieved.token)
            assertEquals(lock.owner, retrieved.owner)
        } finally {
            lock?.let { lockManager.unlock(resource, it.token) }
        }
    }

    @Test
    fun `LockManager getLock should return null for unlocked resource`() = runBlocking {
        val resource = "/test/getlock/notexists"
        assertNull(lockManager.getLock(resource))
    }

    // ========================================================================
    // LockManager Tests - Unlock
    // ========================================================================

    @Test
    fun `LockManager unlock should remove lock with correct token`() = runBlocking {
        val resource = "/test/unlock/correct"
        val lock = lockManager.lock(resource, "user-1", "0", 3600L, true)

        assertNotNull(lock)
        assertTrue(lockManager.isLocked(resource))

        val unlocked = lockManager.unlock(resource, lock.token)

        assertTrue(unlocked)
        assertFalse(lockManager.isLocked(resource))
        assertNull(lockManager.getLock(resource))
    }

    @Test
    fun `LockManager unlock should fail with incorrect token`() = runBlocking {
        val resource = "/test/unlock/wrongtoken"
        val lock = lockManager.lock(resource, "user-1", "0", 3600L, true)

        try {
            assertNotNull(lock)
            val unlocked = lockManager.unlock(resource, "wrong-token")

            assertFalse(unlocked)
            assertTrue(lockManager.isLocked(resource)) // Still locked
        } finally {
            lock?.let { lockManager.unlock(resource, it.token) }
        }
    }

    @Test
    fun `LockManager unlock should fail for non-existent lock`() = runBlocking {
        val resource = "/test/unlock/nonexistent"

        val unlocked = lockManager.unlock(resource, "any-token")

        assertFalse(unlocked)
    }

    // ========================================================================
    // DistributedLock Data Class Tests
    // ========================================================================

    @Test
    fun `DistributedLock should have all properties accessible`() {
        val lock = DistributedLock(
            token = "lock-abc123",
            resource = "/test/path",
            owner = "user-456",
            depth = "infinity",
            timeoutSeconds = 7200L,
            exclusive = true,
            createdAt = System.currentTimeMillis(),
        )

        assertEquals("lock-abc123", lock.token)
        assertEquals("/test/path", lock.resource)
        assertEquals("user-456", lock.owner)
        assertEquals("infinity", lock.depth)
        assertEquals(7200L, lock.timeoutSeconds)
        assertTrue(lock.exclusive)
        assertTrue(lock.createdAt > 0)
    }

    @Test
    fun `DistributedLock data class should support copy`() {
        val original = DistributedLock(
            token = "token1",
            resource = "/path1",
            owner = "user1",
            depth = "0",
            timeoutSeconds = 3600L,
            exclusive = true,
            createdAt = 1000L,
        )

        val copied = original.copy(exclusive = false)

        assertEquals(original.token, copied.token)
        assertEquals(original.resource, copied.resource)
        assertTrue(original.exclusive)
        assertFalse(copied.exclusive)
    }

    @Test
    fun `DistributedLock isExpired should return true for expired lock`() {
        val expiredLock = DistributedLock(
            token = "token1",
            resource = "/path1",
            owner = "user1",
            timeoutSeconds = 1,
            createdAt = System.currentTimeMillis() - 2000, // 2 seconds ago
        )

        assertTrue(expiredLock.isExpired())
    }

    @Test
    fun `DistributedLock isExpired should return false for valid lock`() {
        val validLock = DistributedLock(
            token = "token1",
            resource = "/path1",
            owner = "user1",
            timeoutSeconds = 3600,
            createdAt = System.currentTimeMillis(),
        )

        assertFalse(validLock.isExpired())
    }

    // ========================================================================
    // Lock Replacement Tests
    // ========================================================================

    @Test
    fun `LockManager should not allow lock on already locked resource`() = runBlocking {
        val resource = "/test/conflict/lock"

        val lock1 = lockManager.lock(resource, "user-1", "0", 3600L, true)
        val lock2 = lockManager.lock(resource, "user-2", "infinity", 7200L, false)

        try {
            assertNotNull(lock1)
            // Second lock should fail (null) since resource is already locked
            assertNull(lock2)

            // First lock should still be valid
            val current = lockManager.getLock(resource)
            assertNotNull(current)
            assertEquals("user-1", current.owner)
        } finally {
            lock1?.let { lockManager.unlock(resource, it.token) }
        }
    }
}
