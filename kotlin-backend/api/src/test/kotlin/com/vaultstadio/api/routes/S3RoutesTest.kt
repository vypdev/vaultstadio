/**
 * VaultStadio S3 Routes Tests
 *
 * Unit tests for S3-compatible API components.
 */

package com.vaultstadio.api.routes

import com.vaultstadio.api.routes.storage.S3ErrorCodes
import com.vaultstadio.api.routes.storage.buildS3Error
import com.vaultstadio.core.domain.service.InMemoryMultipartUploadManager
import com.vaultstadio.core.domain.service.MultipartUploadManagerInterface
import com.vaultstadio.core.domain.service.UploadPart
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class S3RoutesTest {

    private lateinit var uploadManager: MultipartUploadManagerInterface

    @BeforeEach
    fun setup() {
        uploadManager = InMemoryMultipartUploadManager()
    }

    // ========================================================================
    // S3 Error Response Tests
    // ========================================================================

    @Test
    fun `buildS3Error should create valid XML error response`() {
        val error = buildS3Error(
            code = S3ErrorCodes.NO_SUCH_KEY,
            message = "The specified key does not exist.",
            resource = "/bucket/key",
        )

        assertTrue(error.contains("<?xml version=\"1.0\""))
        assertTrue(error.contains("<Error>"))
        assertTrue(error.contains("<Code>NoSuchKey</Code>"))
        assertTrue(error.contains("<Message>The specified key does not exist.</Message>"))
        assertTrue(error.contains("<Resource>/bucket/key</Resource>"))
        assertTrue(error.contains("<RequestId>"))
        assertTrue(error.contains("</Error>"))
    }

    @Test
    fun `buildS3Error should work without resource`() {
        val error = buildS3Error(
            code = S3ErrorCodes.ACCESS_DENIED,
            message = "Access denied",
        )

        assertTrue(error.contains("<Code>AccessDenied</Code>"))
        assertTrue(error.contains("<Message>Access denied</Message>"))
        assertFalse(error.contains("<Resource>"))
    }

    @Test
    fun `buildS3Error should use custom requestId`() {
        val error = buildS3Error(
            code = S3ErrorCodes.INTERNAL_ERROR,
            message = "Internal error",
            requestId = "custom-request-id-123",
        )

        assertTrue(error.contains("<RequestId>custom-request-id-123</RequestId>"))
    }

    // ========================================================================
    // S3ErrorCodes Tests
    // ========================================================================

    @Test
    fun `S3ErrorCodes should have correct values`() {
        assertEquals("AccessDenied", S3ErrorCodes.ACCESS_DENIED)
        assertEquals("NoSuchBucket", S3ErrorCodes.NO_SUCH_BUCKET)
        assertEquals("NoSuchKey", S3ErrorCodes.NO_SUCH_KEY)
        assertEquals("BucketNotEmpty", S3ErrorCodes.BUCKET_NOT_EMPTY)
        assertEquals("BucketAlreadyExists", S3ErrorCodes.BUCKET_ALREADY_EXISTS)
        assertEquals("InvalidBucketName", S3ErrorCodes.INVALID_BUCKET_NAME)
        assertEquals("InvalidAccessKeyId", S3ErrorCodes.INVALID_ACCESS_KEY_ID)
        assertEquals("SignatureDoesNotMatch", S3ErrorCodes.SIGNATURE_DOES_NOT_MATCH)
        assertEquals("InternalError", S3ErrorCodes.INTERNAL_ERROR)
        assertEquals("NoSuchUpload", S3ErrorCodes.NO_SUCH_UPLOAD)
        assertEquals("InvalidPart", S3ErrorCodes.INVALID_PART)
        assertEquals("InvalidPartOrder", S3ErrorCodes.INVALID_PART_ORDER)
    }

    // ========================================================================
    // Multipart Upload Manager Tests
    // ========================================================================

    @Test
    fun `MultipartUploadManager should initiate and track uploads`() = runBlocking {
        val session = uploadManager.initiate("test-bucket", "test-key", "user-1")

        try {
            assertEquals("test-bucket", session.bucket)
            assertEquals("test-key", session.key)
            assertEquals("user-1", session.userId)
            assertTrue(session.uploadId.isNotEmpty())

            val retrieved = uploadManager.get(session.uploadId)
            assertNotNull(retrieved)
            assertEquals(session.uploadId, retrieved.uploadId)
        } finally {
            uploadManager.abort(session.uploadId)
        }
    }

    @Test
    fun `MultipartUploadManager addPart should store part`() = runBlocking {
        val session = uploadManager.initiate("bucket", "key", "user-1")
        val uploadId = session.uploadId

        try {
            val data = "test data".toByteArray()
            val part = uploadManager.addPart(uploadId, 1, data)

            assertNotNull(part)
            assertEquals(1, part.partNumber)
            assertTrue(part.etag.isNotEmpty())
            assertEquals(data.size.toLong(), part.size)
        } finally {
            uploadManager.abort(uploadId)
        }
    }

    @Test
    fun `MultipartUploadManager should track multiple parts`() = runBlocking {
        val session = uploadManager.initiate("bucket", "key", "user-1")
        val uploadId = session.uploadId

        try {
            uploadManager.addPart(uploadId, 1, "part1".toByteArray())
            uploadManager.addPart(uploadId, 2, "part2".toByteArray())

            val parts = uploadManager.getParts(uploadId)
            assertEquals(2, parts.size)
            assertEquals(1, parts[0].partNumber)
            assertEquals(2, parts[1].partNumber)
        } finally {
            uploadManager.abort(uploadId)
        }
    }

    @Test
    fun `MultipartUploadManager addPart should return null for non-existent upload`() = runBlocking {
        val result = uploadManager.addPart("non-existent-upload-id", 1, "data".toByteArray())
        assertNull(result)
    }

    @Test
    fun `MultipartUploadManager should complete and return combined data`() = runBlocking {
        val session = uploadManager.initiate("bucket", "key", "user-1")
        val uploadId = session.uploadId

        // Add parts
        val data1 = "AAAA".toByteArray()
        val data3 = "CCCC".toByteArray()
        val data2 = "BBBB".toByteArray()

        uploadManager.addPart(uploadId, 1, data1)
        uploadManager.addPart(uploadId, 3, data3)
        uploadManager.addPart(uploadId, 2, data2)

        // Complete and get combined data (parts are combined in order)
        val result = uploadManager.complete(uploadId)

        assertNotNull(result)
        // Parts should be combined in order: 1, 2, 3
        assertEquals("AAAABBBBCCCC", String(result))

        // Session should be removed after completion
        assertNull(uploadManager.get(uploadId))
    }

    @Test
    fun `MultipartUploadManager complete should return null for non-existent upload`() = runBlocking {
        val result = uploadManager.complete("non-existent-upload-id")
        assertNull(result)
    }

    @Test
    fun `MultipartUploadManager abort should remove session`() = runBlocking {
        val session = uploadManager.initiate("bucket", "key", "user-1")
        val uploadId = session.uploadId

        assertNotNull(uploadManager.get(uploadId))

        val aborted = uploadManager.abort(uploadId)

        assertTrue(aborted)
        assertNull(uploadManager.get(uploadId))
    }

    @Test
    fun `MultipartUploadManager abort non-existent should return false`() = runBlocking {
        val aborted = uploadManager.abort("non-existent-upload-id")
        assertFalse(aborted)
    }

    @Test
    fun `MultipartUploadManager get should return null for non-existent upload`() = runBlocking {
        val result = uploadManager.get("non-existent-upload-id")
        assertNull(result)
    }

    // ========================================================================
    // UploadPart Tests
    // ========================================================================

    @Test
    fun `UploadPart equals should compare partNumber and etag`() {
        val part1 = UploadPart(1, "\"abc123\"", 100L)
        val part2 = UploadPart(1, "\"abc123\"", 100L)
        val part3 = UploadPart(2, "\"abc123\"", 100L)
        val part4 = UploadPart(1, "\"def456\"", 100L)

        assertEquals(part1, part2)
        assertFalse(part1 == part3) // Different part number
        assertFalse(part1 == part4) // Different etag
    }

    @Test
    fun `UploadPart hashCode should be consistent`() {
        val part1 = UploadPart(1, "\"abc123\"", 100L)
        val part2 = UploadPart(1, "\"abc123\"", 100L)

        assertEquals(part1.hashCode(), part2.hashCode())
    }

    // ========================================================================
    // MultipartUploadSession Tests
    // ========================================================================

    @Test
    fun `MultipartUploadSession should store parts correctly`() = runBlocking {
        val session = uploadManager.initiate("bucket", "key", "user-1")
        val uploadId = session.uploadId

        try {
            // Initially no parts
            val initialParts = uploadManager.getParts(uploadId)
            assertTrue(initialParts.isEmpty())

            // Add a part
            uploadManager.addPart(uploadId, 1, "data".toByteArray())

            // Verify part was added
            val parts = uploadManager.getParts(uploadId)
            assertEquals(1, parts.size)
            assertEquals(1, parts[0].partNumber)
        } finally {
            uploadManager.abort(uploadId)
        }
    }
}
