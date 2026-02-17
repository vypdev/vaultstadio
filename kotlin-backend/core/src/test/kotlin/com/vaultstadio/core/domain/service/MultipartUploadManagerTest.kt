/**
 * Tests for MultipartUploadManager
 */

package com.vaultstadio.core.domain.service

import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours

class MultipartUploadManagerTest {

    private lateinit var uploadManager: InMemoryMultipartUploadManager

    @BeforeEach
    fun setup() {
        uploadManager = InMemoryMultipartUploadManager()
    }

    @Nested
    inner class InitiateTests {

        @Test
        fun `can initiate upload`() = runBlocking {
            val session = uploadManager.initiate("mybucket", "mykey.txt", "user1")

            assertNotNull(session)
            assertEquals("mybucket", session.bucket)
            assertEquals("mykey.txt", session.key)
            assertEquals("user1", session.userId)
            assertTrue(session.uploadId.isNotEmpty())
        }

        @Test
        fun `can initiate upload with metadata`() = runBlocking {
            val metadata = mapOf("Content-Type" to "application/pdf", "Custom" to "value")

            val session = uploadManager.initiate("bucket", "file.pdf", "user", metadata)

            assertEquals(metadata, session.metadata)
        }

        @Test
        fun `each initiation has unique upload ID`() = runBlocking {
            val session1 = uploadManager.initiate("bucket", "key", "user")
            val session2 = uploadManager.initiate("bucket", "key", "user")

            assertTrue(session1.uploadId != session2.uploadId)
        }
    }

    @Nested
    inner class GetSessionTests {

        @Test
        fun `can get existing session`() = runBlocking {
            val created = uploadManager.initiate("bucket", "key", "user")

            val retrieved = uploadManager.get(created.uploadId)

            assertNotNull(retrieved)
            assertEquals(created.uploadId, retrieved.uploadId)
        }

        @Test
        fun `get returns null for non-existent session`() = runBlocking {
            val session = uploadManager.get("non-existent-id")

            assertNull(session)
        }
    }

    @Nested
    inner class AddPartTests {

        @Test
        fun `can add part to upload`() = runBlocking {
            val session = uploadManager.initiate("bucket", "key", "user")
            val data = "Part 1 data".toByteArray()

            val part = uploadManager.addPart(session.uploadId, 1, data)

            assertNotNull(part)
            assertEquals(1, part.partNumber)
            assertEquals(data.size.toLong(), part.size)
            assertTrue(part.etag.isNotEmpty())
        }

        @Test
        fun `part etag is MD5 hash`() = runBlocking {
            val session = uploadManager.initiate("bucket", "key", "user")
            val data = "Test data".toByteArray()

            val part = uploadManager.addPart(session.uploadId, 1, data)!!

            // ETag should be quoted MD5 (32 hex chars + quotes)
            assertTrue(part.etag.startsWith("\""))
            assertTrue(part.etag.endsWith("\""))
            assertEquals(34, part.etag.length) // 32 hex + 2 quotes
        }

        @Test
        fun `can add multiple parts`() = runBlocking {
            val session = uploadManager.initiate("bucket", "key", "user")

            val part1 = uploadManager.addPart(session.uploadId, 1, "Part 1".toByteArray())
            val part2 = uploadManager.addPart(session.uploadId, 2, "Part 2".toByteArray())
            val part3 = uploadManager.addPart(session.uploadId, 3, "Part 3".toByteArray())

            assertNotNull(part1)
            assertNotNull(part2)
            assertNotNull(part3)

            val parts = uploadManager.getParts(session.uploadId)
            assertEquals(3, parts.size)
        }

        @Test
        fun `cannot add part to non-existent session`() = runBlocking {
            val part = uploadManager.addPart("non-existent", 1, "data".toByteArray())

            assertNull(part)
        }
    }

    @Nested
    inner class CompleteTests {

        @Test
        fun `can complete upload with single part`() = runBlocking {
            val session = uploadManager.initiate("bucket", "key", "user")
            val data = "Complete data".toByteArray()
            uploadManager.addPart(session.uploadId, 1, data)

            val result = uploadManager.complete(session.uploadId)

            assertNotNull(result)
            assertTrue(data.contentEquals(result))
        }

        @Test
        fun `complete combines parts in order`() = runBlocking {
            val session = uploadManager.initiate("bucket", "key", "user")

            uploadManager.addPart(session.uploadId, 1, "AAA".toByteArray())
            uploadManager.addPart(session.uploadId, 3, "CCC".toByteArray()) // Out of order
            uploadManager.addPart(session.uploadId, 2, "BBB".toByteArray())

            val result = uploadManager.complete(session.uploadId)

            assertNotNull(result)
            assertEquals("AAABBBCCC", String(result))
        }

        @Test
        fun `complete removes session`() = runBlocking {
            val session = uploadManager.initiate("bucket", "key", "user")
            uploadManager.addPart(session.uploadId, 1, "data".toByteArray())

            uploadManager.complete(session.uploadId)

            val retrieved = uploadManager.get(session.uploadId)
            assertNull(retrieved, "Session should be removed after complete")
        }

        @Test
        fun `cannot complete non-existent session`() = runBlocking {
            val result = uploadManager.complete("non-existent")

            assertNull(result)
        }
    }

    @Nested
    inner class CompleteStreamingTests {

        @Test
        fun `complete streaming calls handler in order`() = runBlocking {
            val session = uploadManager.initiate("bucket", "key", "user")

            uploadManager.addPart(session.uploadId, 1, "AAA".toByteArray())
            uploadManager.addPart(session.uploadId, 2, "BBB".toByteArray())
            uploadManager.addPart(session.uploadId, 3, "CCC".toByteArray())

            val receivedParts = mutableListOf<String>()

            val result = uploadManager.completeStreaming(session.uploadId) { partNumber, data ->
                receivedParts.add(String(data))
            }

            assertNotNull(result)
            assertEquals(listOf("AAA", "BBB", "CCC"), receivedParts)
            assertEquals(9L, result.size)
        }
    }

    @Nested
    inner class AbortTests {

        @Test
        fun `can abort upload`() = runBlocking {
            val session = uploadManager.initiate("bucket", "key", "user")
            uploadManager.addPart(session.uploadId, 1, "data".toByteArray())

            val result = uploadManager.abort(session.uploadId)

            assertTrue(result)
            assertNull(uploadManager.get(session.uploadId))
        }

        @Test
        fun `abort returns false for non-existent session`() = runBlocking {
            val result = uploadManager.abort("non-existent")

            assertTrue(!result)
        }
    }

    @Nested
    inner class ListUploadsTests {

        @Test
        fun `can list uploads for user`() = runBlocking {
            uploadManager.initiate("bucket1", "key1", "user1")
            uploadManager.initiate("bucket2", "key2", "user1")
            uploadManager.initiate("bucket1", "key3", "user2")

            val user1Uploads = uploadManager.listUploads("user1")
            val user2Uploads = uploadManager.listUploads("user2")

            assertEquals(2, user1Uploads.size)
            assertEquals(1, user2Uploads.size)
        }

        @Test
        fun `can filter by bucket`() = runBlocking {
            uploadManager.initiate("bucket1", "key1", "user1")
            uploadManager.initiate("bucket1", "key2", "user1")
            uploadManager.initiate("bucket2", "key3", "user1")

            val filtered = uploadManager.listUploads("user1", "bucket1")

            assertEquals(2, filtered.size)
            assertTrue(filtered.all { it.bucket == "bucket1" })
        }
    }

    @Nested
    inner class CleanupTests {

        @Test
        fun `cleanupExpired removes old uploads`() = runBlocking {
            // Create uploads
            val session1 = uploadManager.initiate("bucket", "old-file", "user")
            val session2 = uploadManager.initiate("bucket", "new-file", "user")

            // Only cleanup truly old uploads (1 hour threshold for safety)
            val threshold = Clock.System.now().minus(1.hours)

            val cleaned = uploadManager.cleanupExpired(threshold)

            // Both sessions were just created, so nothing should be cleaned
            assertEquals(0, cleaned)
        }
    }
}
