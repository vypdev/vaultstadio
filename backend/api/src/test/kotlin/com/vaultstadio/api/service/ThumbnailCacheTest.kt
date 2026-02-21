/**
 * VaultStadio Thumbnail Cache Tests
 *
 * Unit tests for InMemoryThumbnailCache, ThumbnailCacheKey, CachedThumbnail, and ThumbnailCacheStats.
 */

package com.vaultstadio.api.service

import kotlinx.datetime.Clock
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds

class ThumbnailCacheTest {

    private lateinit var cache: ThumbnailCache

    @BeforeEach
    fun setup() {
        cache = InMemoryThumbnailCache(
            maxEntries = 100,
            maxSizeBytes = 1024 * 1024,
            defaultTtl = 24.hours,
        )
    }

    private fun key(itemId: String = "item-1", size: String = "256", version: Long = 0) =
        ThumbnailCacheKey(itemId = itemId, size = size, version = version)

    private fun thumbnail(data: ByteArray = byteArrayOf(1, 2, 3), contentType: String = "image/jpeg") =
        CachedThumbnail(
            data = data,
            contentType = contentType,
            createdAt = Clock.System.now(),
            itemVersion = 0,
        )

    @Nested
    inner class GetAndPutTests {

        @Test
        fun `get returns null on miss`() {
            assertNull(cache.get(key()))
        }

        @Test
        fun `put then get returns cached thumbnail`() {
            val k = key()
            val t = thumbnail()
            cache.put(k, t)
            val retrieved = cache.get(k)
            assertEquals(t.data.toList(), retrieved!!.data.toList())
            assertEquals(t.contentType, retrieved.contentType)
        }

        @Test
        fun `get increments hits and stats`() {
            val k = key()
            cache.put(k, thumbnail())
            cache.get(k)
            cache.get(k)
            val stats = cache.getStats()
            assertEquals(2, stats.hits)
            assertEquals(1, stats.size)
        }

        @Test
        fun `get on miss increments misses`() {
            cache.get(key())
            cache.get(key("other"))
            val stats = cache.getStats()
            assertEquals(2, stats.misses)
        }

        @Test
        fun `get returns null and increments miss when entry expired by TTL`() {
            val shortTtlCache = InMemoryThumbnailCache(
                maxEntries = 10,
                maxSizeBytes = 1024,
                defaultTtl = 1.milliseconds,
            )
            val k = key()
            shortTtlCache.put(k, thumbnail())
            Thread.sleep(2)
            val retrieved = shortTtlCache.get(k)
            assertNull(retrieved)
            val stats = shortTtlCache.getStats()
            assertEquals(1, stats.misses)
        }

        @Test
        fun `put overwriting same key replaces entry and updates size`() {
            val k = key()
            cache.put(k, thumbnail(byteArrayOf(1, 2, 3, 4, 5)))
            cache.put(k, thumbnail(byteArrayOf(1, 2)))
            val stats = cache.getStats()
            assertEquals(1, stats.size)
            assertEquals(byteArrayOf(1, 2).toList(), cache.get(k)!!.data.toList())
        }
    }

    @Nested
    inner class EvictionTests {

        @Test
        fun `eviction by maxEntries removes oldest by access time`() {
            val smallCache = InMemoryThumbnailCache(
                maxEntries = 2,
                maxSizeBytes = 1024 * 1024,
                defaultTtl = 24.hours,
            )
            smallCache.put(key("i1"), thumbnail())
            smallCache.put(key("i2"), thumbnail())
            smallCache.get(key("i1"))
            smallCache.put(key("i3"), thumbnail())
            assertNull(smallCache.get(key("i2")))
            assertNotNull(smallCache.get(key("i1")))
            assertNotNull(smallCache.get(key("i3")))
            assertEquals(2, smallCache.getStats().size)
        }

        @Test
        fun `eviction by maxSizeBytes removes oldest when over limit`() {
            val smallSizeCache = InMemoryThumbnailCache(
                maxEntries = 100,
                maxSizeBytes = 20,
                defaultTtl = 24.hours,
            )
            smallSizeCache.put(key("i1"), thumbnail(ByteArray(10)))
            smallSizeCache.put(key("i2"), thumbnail(ByteArray(10)))
            smallSizeCache.get(key("i1"))
            smallSizeCache.put(key("i3"), thumbnail(ByteArray(10)))
            assertNull(smallSizeCache.get(key("i2")))
            assertNotNull(smallSizeCache.get(key("i1")))
            assertNotNull(smallSizeCache.get(key("i3")))
        }
    }

    @Nested
    inner class InvalidateTests {

        @Test
        fun `invalidate removes entries for itemId`() {
            cache.put(key(itemId = "i1"), thumbnail())
            cache.put(key(itemId = "i1", size = "128"), thumbnail(byteArrayOf(4, 5)))
            cache.put(key(itemId = "i2"), thumbnail())
            cache.invalidate("i1")
            assertNull(cache.get(key(itemId = "i1")))
            assertNull(cache.get(key(itemId = "i1", size = "128")))
            assertNotNull(cache.get(key(itemId = "i2")))
        }
    }

    @Nested
    inner class CleanupTests {

        @Test
        fun `cleanup returns zero when no entries expired`() {
            cache.put(key(), thumbnail())
            val count = cache.cleanup(maxAge = 1.milliseconds)
            assertEquals(0, count)
        }

        @Test
        fun `cleanup with large maxAge leaves entries`() {
            cache.put(key(), thumbnail())
            val count = cache.cleanup(maxAge = 24.hours)
            assertEquals(0, count)
            assertNotNull(cache.get(key()))
        }

        @Test
        fun `cleanup removes expired entries`() {
            cache.put(key("i1"), thumbnail())
            cache.put(key("i2"), thumbnail())
            Thread.sleep(2)
            val count = cache.cleanup(maxAge = 1.milliseconds)
            assertEquals(2, count)
            assertNull(cache.get(key("i1")))
            assertNull(cache.get(key("i2")))
        }

        @Test
        fun `cleanup with default maxAge leaves recent entries`() {
            cache.put(key(), thumbnail())
            val count = cache.cleanup()
            assertEquals(0, count)
            assertNotNull(cache.get(key()))
        }
    }

    @Nested
    inner class ThumbnailCacheKeyTests {

        @Test
        fun `key holds itemId size version`() {
            val k = ThumbnailCacheKey(itemId = "x", size = "64", version = 2)
            assertEquals("x", k.itemId)
            assertEquals("64", k.size)
            assertEquals(2L, k.version)
        }
    }

    @Nested
    inner class ThumbnailCacheStatsTests {

        @Test
        fun `hitRate calculated from hits and misses`() {
            cache.put(key(), thumbnail())
            cache.get(key())
            cache.get(key("other"))
            val stats = cache.getStats()
            assertEquals(1, stats.hits)
            assertEquals(1, stats.misses)
            assertEquals(0.5, stats.hitRate, 0.01)
        }

        @Test
        fun `hitRate is zero when no requests yet`() {
            val stats = cache.getStats()
            assertEquals(0, stats.size)
            assertEquals(0, stats.hits)
            assertEquals(0, stats.misses)
            assertEquals(0.0, stats.hitRate, 0.0)
        }
    }

    @Nested
    inner class CachedThumbnailTests {

        @Test
        fun `CachedThumbnail equals by data and contentType`() {
            val now = Clock.System.now()
            val a = CachedThumbnail(byteArrayOf(1, 2), "image/png", now, 0)
            val b = CachedThumbnail(byteArrayOf(1, 2), "image/png", now, 1)
            val c = CachedThumbnail(byteArrayOf(1, 2, 3), "image/png", now, 0)
            assertEquals(a, b)
            assertNotEquals(a, c)
            assertEquals(a.hashCode(), b.hashCode())
        }
    }
}
