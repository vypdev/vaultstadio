/**
 * VaultStadio Thumbnail Cache
 *
 * Simple in-memory cache for generated thumbnails.
 * For production, consider using Redis or filesystem caching.
 */

package com.vaultstadio.api.service

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

private val logger = KotlinLogging.logger {}

/**
 * Cache key for thumbnails.
 */
data class ThumbnailCacheKey(
    val itemId: String,
    val size: String,
    val version: Long = 0,
)

/**
 * Cached thumbnail entry.
 */
data class CachedThumbnail(
    val data: ByteArray,
    val contentType: String,
    val createdAt: Instant,
    val itemVersion: Long,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as CachedThumbnail
        return data.contentEquals(other.data) && contentType == other.contentType
    }

    override fun hashCode(): Int {
        var result = data.contentHashCode()
        result = 31 * result + contentType.hashCode()
        return result
    }
}

/**
 * Interface for thumbnail caching.
 */
interface ThumbnailCache {

    /**
     * Gets a cached thumbnail if available and valid.
     */
    fun get(key: ThumbnailCacheKey): CachedThumbnail?

    /**
     * Puts a thumbnail in the cache.
     */
    fun put(key: ThumbnailCacheKey, thumbnail: CachedThumbnail)

    /**
     * Invalidates cache entries for an item.
     */
    fun invalidate(itemId: String)

    /**
     * Clears expired entries from the cache.
     */
    fun cleanup(maxAge: Duration = 24.hours): Int

    /**
     * Gets cache statistics.
     */
    fun getStats(): ThumbnailCacheStats
}

/**
 * Cache statistics.
 */
data class ThumbnailCacheStats(
    val size: Int,
    val hits: Long,
    val misses: Long,
    val hitRate: Double,
)

/**
 * In-memory implementation of ThumbnailCache.
 * Uses LRU-style eviction based on access time.
 */
class InMemoryThumbnailCache(
    private val maxEntries: Int = 1000,
    private val maxSizeBytes: Long = 100 * 1024 * 1024, // 100MB default
    private val defaultTtl: Duration = 24.hours,
) : ThumbnailCache {

    private val cache = ConcurrentHashMap<ThumbnailCacheKey, CachedThumbnail>()
    private val accessTimes = ConcurrentHashMap<ThumbnailCacheKey, Instant>()

    private var hits = 0L
    private var misses = 0L
    private var currentSizeBytes = 0L

    override fun get(key: ThumbnailCacheKey): CachedThumbnail? {
        val entry = cache[key]

        if (entry == null) {
            misses++
            return null
        }

        // Check if expired
        val now = Clock.System.now()
        val createdAt = entry.createdAt
        if ((now - createdAt) > defaultTtl) {
            remove(key)
            misses++
            return null
        }

        // Update access time
        accessTimes[key] = now
        hits++

        return entry
    }

    override fun put(key: ThumbnailCacheKey, thumbnail: CachedThumbnail) {
        // Evict if necessary
        evictIfNeeded(thumbnail.data.size.toLong())

        // Remove old entry if exists
        cache[key]?.let { old ->
            currentSizeBytes -= old.data.size
        }

        cache[key] = thumbnail
        accessTimes[key] = Clock.System.now()
        currentSizeBytes += thumbnail.data.size

        logger.debug { "Cached thumbnail for ${key.itemId} (size: ${thumbnail.data.size} bytes)" }
    }

    override fun invalidate(itemId: String) {
        val keysToRemove = cache.keys.filter { it.itemId == itemId }
        keysToRemove.forEach { remove(it) }

        if (keysToRemove.isNotEmpty()) {
            logger.debug { "Invalidated ${keysToRemove.size} cached thumbnails for item $itemId" }
        }
    }

    override fun cleanup(maxAge: Duration): Int {
        val now = Clock.System.now()
        val cutoff = now - maxAge

        val expiredKeys = cache.entries
            .filter { it.value.createdAt < cutoff }
            .map { it.key }

        expiredKeys.forEach { remove(it) }

        if (expiredKeys.isNotEmpty()) {
            logger.info { "Cleaned up ${expiredKeys.size} expired thumbnail cache entries" }
        }

        return expiredKeys.size
    }

    override fun getStats(): ThumbnailCacheStats {
        val total = hits + misses
        val hitRate = if (total > 0) hits.toDouble() / total else 0.0

        return ThumbnailCacheStats(
            size = cache.size,
            hits = hits,
            misses = misses,
            hitRate = hitRate,
        )
    }

    private fun remove(key: ThumbnailCacheKey) {
        cache.remove(key)?.let { old ->
            currentSizeBytes -= old.data.size
        }
        accessTimes.remove(key)
    }

    private fun evictIfNeeded(newEntrySize: Long) {
        // Evict by count
        while (cache.size >= maxEntries) {
            evictOldest()
        }

        // Evict by size
        while (currentSizeBytes + newEntrySize > maxSizeBytes && cache.isNotEmpty()) {
            evictOldest()
        }
    }

    private fun evictOldest() {
        val oldest = accessTimes.entries.minByOrNull { it.value }
        oldest?.let { remove(it.key) }
    }
}
