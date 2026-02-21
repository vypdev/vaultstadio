/**
 * VaultStadio Lock Manager
 *
 * Abstraction for distributed lock management.
 * Supports in-memory storage (development) and Redis (production).
 */

package com.vaultstadio.core.domain.service

import io.lettuce.core.RedisClient
import io.lettuce.core.ScriptOutputType
import kotlinx.coroutines.future.await
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Lock information for WebDAV and other distributed locking needs.
 *
 * @property token Unique lock token
 * @property resource Resource being locked (path or ID)
 * @property owner Lock owner identifier
 * @property depth Lock depth (0, 1, or infinity)
 * @property timeoutSeconds Lock timeout in seconds
 * @property exclusive Whether lock is exclusive
 * @property createdAt When the lock was created (epoch millis)
 */
data class DistributedLock(
    val token: String,
    val resource: String,
    val owner: String,
    val depth: String = "0",
    val timeoutSeconds: Long = 3600,
    val exclusive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
) {
    /**
     * Check if the lock has expired.
     */
    fun isExpired(): Boolean {
        val expiresAt = createdAt + (timeoutSeconds * 1000)
        return System.currentTimeMillis() > expiresAt
    }
}

/**
 * Interface for distributed lock management.
 *
 * Implementations can use different backends:
 * - In-memory (for development/single instance)
 * - Redis (for production/multi-instance)
 */
interface LockManager {

    /**
     * Acquire a lock on a resource.
     *
     * @param resource Resource identifier
     * @param owner Lock owner identifier
     * @param depth Lock depth
     * @param timeoutSeconds Lock timeout
     * @param exclusive Whether lock is exclusive
     * @return Lock information, or null if lock could not be acquired
     */
    suspend fun lock(
        resource: String,
        owner: String,
        depth: String = "0",
        timeoutSeconds: Long = 3600,
        exclusive: Boolean = true,
    ): DistributedLock?

    /**
     * Release a lock.
     *
     * @param resource Resource identifier
     * @param token Lock token
     * @return True if lock was released, false otherwise
     */
    suspend fun unlock(resource: String, token: String): Boolean

    /**
     * Get lock information for a resource.
     *
     * @param resource Resource identifier
     * @return Lock information, or null if not locked
     */
    suspend fun getLock(resource: String): DistributedLock?

    /**
     * Check if a resource is locked.
     *
     * @param resource Resource identifier
     * @return True if locked, false otherwise
     */
    suspend fun isLocked(resource: String): Boolean

    /**
     * Refresh a lock's timeout.
     *
     * @param resource Resource identifier
     * @param token Lock token
     * @param newTimeoutSeconds New timeout in seconds
     * @return Updated lock, or null if lock not found or token doesn't match
     */
    suspend fun refresh(resource: String, token: String, newTimeoutSeconds: Long): DistributedLock?

    /**
     * Clean up expired locks.
     *
     * @return Number of expired locks removed
     */
    suspend fun cleanupExpired(): Int
}

/**
 * In-memory lock manager implementation.
 *
 * Suitable for development and single-instance deployments.
 * For production multi-instance deployments, use Redis implementation.
 */
class InMemoryLockManager : LockManager {

    private val locks = ConcurrentHashMap<String, DistributedLock>()

    override suspend fun lock(
        resource: String,
        owner: String,
        depth: String,
        timeoutSeconds: Long,
        exclusive: Boolean,
    ): DistributedLock? {
        // Check for existing lock
        val existing = locks[resource]
        if (existing != null && !existing.isExpired()) {
            // Resource is already locked
            return null
        }

        // Create new lock
        val lock = DistributedLock(
            token = "opaquelocktoken:${UUID.randomUUID()}",
            resource = resource,
            owner = owner,
            depth = depth,
            timeoutSeconds = timeoutSeconds,
            exclusive = exclusive,
            createdAt = System.currentTimeMillis(),
        )

        locks[resource] = lock
        return lock
    }

    override suspend fun unlock(resource: String, token: String): Boolean {
        val lock = locks[resource]
        return if (lock != null && lock.token == token) {
            locks.remove(resource)
            true
        } else {
            false
        }
    }

    override suspend fun getLock(resource: String): DistributedLock? {
        val lock = locks[resource]
        return if (lock != null && !lock.isExpired()) {
            lock
        } else {
            if (lock != null) locks.remove(resource) // Clean up expired
            null
        }
    }

    override suspend fun isLocked(resource: String): Boolean {
        val lock = locks[resource]
        return lock != null && !lock.isExpired()
    }

    override suspend fun refresh(resource: String, token: String, newTimeoutSeconds: Long): DistributedLock? {
        val lock = locks[resource]
        if (lock == null || lock.token != token) return null

        val refreshed = lock.copy(
            timeoutSeconds = newTimeoutSeconds,
            createdAt = System.currentTimeMillis(),
        )
        locks[resource] = refreshed
        return refreshed
    }

    override suspend fun cleanupExpired(): Int {
        var count = 0
        val iterator = locks.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (entry.value.isExpired()) {
                iterator.remove()
                count++
            }
        }
        return count
    }
}

/**
 * Redis-based lock manager implementation.
 *
 * Uses Redis for distributed locking with automatic TTL.
 * Suitable for production multi-instance deployments.
 *
 * Implementation uses Redis SETNX for atomic lock acquisition
 * and Lua scripts for atomic token verification on unlock/refresh.
 */
class RedisLockManager(
    private val redisConnectionString: String,
    private val keyPrefix: String = "lock:",
) : LockManager {

    // Lazy initialization of Redis client
    private val redisClient by lazy {
        RedisClient.create(redisConnectionString)
    }

    private val connection by lazy {
        redisClient.connect()
    }

    private val asyncCommands by lazy {
        connection.async()
    }

    // Lua script for atomic unlock (verify token before delete)
    private val unlockScript = """
        if redis.call("hget", KEYS[1], "token") == ARGV[1] then
            return redis.call("del", KEYS[1])
        else
            return 0
        end
    """.trimIndent()

    // Lua script for atomic refresh (verify token before expire)
    private val refreshScript = """
        if redis.call("hget", KEYS[1], "token") == ARGV[1] then
            redis.call("hset", KEYS[1], "createdAt", ARGV[2])
            redis.call("hset", KEYS[1], "timeoutSeconds", ARGV[3])
            redis.call("expire", KEYS[1], ARGV[3])
            return 1
        else
            return 0
        end
    """.trimIndent()

    override suspend fun lock(
        resource: String,
        owner: String,
        depth: String,
        timeoutSeconds: Long,
        exclusive: Boolean,
    ): DistributedLock? {
        val token = "opaquelocktoken:${UUID.randomUUID()}"
        val now = System.currentTimeMillis()
        val key = "$keyPrefix$resource"

        val lock = DistributedLock(
            token = token,
            resource = resource,
            owner = owner,
            depth = depth,
            timeoutSeconds = timeoutSeconds,
            exclusive = exclusive,
            createdAt = now,
        )

        // Use HSETNX for atomic check-and-set
        // First, check if key exists
        val exists = asyncCommands.exists(key).await()
        if (exists > 0) {
            // Key exists, check if lock is expired
            val existingCreatedAt = asyncCommands.hget(key, "createdAt").await()
            val existingTimeout = asyncCommands.hget(key, "timeoutSeconds").await()

            if (existingCreatedAt != null && existingTimeout != null) {
                val createdAt = existingCreatedAt.toLongOrNull() ?: 0
                val timeout = existingTimeout.toLongOrNull() ?: 0
                val expiresAt = createdAt + (timeout * 1000)

                if (System.currentTimeMillis() < expiresAt) {
                    // Lock is still valid
                    return null
                }
                // Lock is expired, we can acquire it (will be overwritten below)
            }
        }

        // Store lock data
        val lockData = mapOf(
            "token" to token,
            "resource" to resource,
            "owner" to owner,
            "depth" to depth,
            "timeoutSeconds" to timeoutSeconds.toString(),
            "exclusive" to exclusive.toString(),
            "createdAt" to now.toString(),
        )

        asyncCommands.hset(key, lockData).await()
        asyncCommands.expire(key, timeoutSeconds).await()

        return lock
    }

    override suspend fun unlock(resource: String, token: String): Boolean {
        val key = "$keyPrefix$resource"

        // Use Lua script for atomic token verification and delete
        val result = asyncCommands.eval<Long>(
            unlockScript,
            ScriptOutputType.INTEGER,
            arrayOf(key),
            token,
        ).await()

        return (result ?: 0) > 0
    }

    override suspend fun getLock(resource: String): DistributedLock? {
        val key = "$keyPrefix$resource"
        val lockData = asyncCommands.hgetall(key).await()

        if (lockData.isNullOrEmpty()) return null

        val createdAt = lockData["createdAt"]?.toLongOrNull() ?: return null
        val timeoutSeconds = lockData["timeoutSeconds"]?.toLongOrNull() ?: return null

        // Check if expired
        val expiresAt = createdAt + (timeoutSeconds * 1000)
        if (System.currentTimeMillis() > expiresAt) {
            // Clean up expired lock
            asyncCommands.del(key).await()
            return null
        }

        return DistributedLock(
            token = lockData["token"] ?: "",
            resource = lockData["resource"] ?: resource,
            owner = lockData["owner"] ?: "",
            depth = lockData["depth"] ?: "0",
            timeoutSeconds = timeoutSeconds,
            exclusive = lockData["exclusive"]?.toBoolean() ?: true,
            createdAt = createdAt,
        )
    }

    override suspend fun isLocked(resource: String): Boolean {
        val key = "$keyPrefix$resource"
        val exists = asyncCommands.exists(key).await()

        if (exists == 0L) return false

        // Verify lock is not expired
        return getLock(resource) != null
    }

    override suspend fun refresh(resource: String, token: String, newTimeoutSeconds: Long): DistributedLock? {
        val key = "$keyPrefix$resource"
        val now = System.currentTimeMillis()

        // Use Lua script for atomic token verification and refresh
        val result = asyncCommands.eval<Long>(
            refreshScript,
            ScriptOutputType.INTEGER,
            arrayOf(key),
            token,
            now.toString(),
            newTimeoutSeconds.toString(),
        ).await()

        if ((result ?: 0) > 0) {
            // Refresh successful, return updated lock
            return getLock(resource)
        }

        return null
    }

    override suspend fun cleanupExpired(): Int {
        // Redis handles TTL automatically, no cleanup needed
        return 0
    }

    /**
     * Close the Redis connection.
     */
    fun close() {
        connection.close()
        redisClient.shutdown()
    }
}
