/**
 * VaultStadio Batch Routes Tests
 *
 * Unit tests for batch operations API endpoints.
 */

package com.vaultstadio.api.routes

import com.vaultstadio.api.routes.storage.BatchCopyRequest
import com.vaultstadio.api.routes.storage.BatchDeleteRequest
import com.vaultstadio.api.routes.storage.BatchError
import com.vaultstadio.api.routes.storage.BatchMoveRequest
import com.vaultstadio.api.routes.storage.BatchResult
import com.vaultstadio.api.routes.storage.BatchStarRequest
import com.vaultstadio.api.routes.storage.DownloadZipRequest
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class BatchRoutesTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Nested
    inner class BatchDeleteTests {

        @Test
        fun `batch delete request body should contain itemIds array`() {
            // Verify BatchDeleteRequest structure
            val request = BatchDeleteRequest(
                itemIds = listOf("item-1", "item-2"),
                permanent = false,
            )
            assertEquals(2, request.itemIds.size)
            assertEquals(false, request.permanent)
        }

        @Test
        fun `batch delete permanent flag should default to false`() {
            val request = BatchDeleteRequest(itemIds = listOf("item-1"))
            assertEquals(false, request.permanent)
        }

        @Test
        fun `batch delete should serialize correctly`() {
            val request = BatchDeleteRequest(
                itemIds = listOf("item-1", "item-2", "item-3"),
                permanent = true,
            )
            val serialized = json.encodeToString(BatchDeleteRequest.serializer(), request)
            assertTrue(serialized.contains("item-1"))
            assertTrue(serialized.contains("permanent"))
        }
    }

    @Nested
    inner class BatchMoveTests {

        @Test
        fun `batch move request should allow null destination for root`() {
            val request = BatchMoveRequest(
                itemIds = listOf("item-1"),
                destinationId = null,
            )
            assertEquals(null, request.destinationId)
        }

        @Test
        fun `batch move should serialize correctly`() {
            val request = BatchMoveRequest(
                itemIds = listOf("item-1", "item-2"),
                destinationId = "folder-123",
            )
            val serialized = json.encodeToString(BatchMoveRequest.serializer(), request)
            assertTrue(serialized.contains("folder-123"))
        }
    }

    @Nested
    inner class BatchCopyTests {

        @Test
        fun `batch copy request structure is correct`() {
            val request = BatchCopyRequest(
                itemIds = listOf("item-1", "item-2"),
                destinationId = "folder-123",
            )
            assertEquals(2, request.itemIds.size)
            assertEquals("folder-123", request.destinationId)
        }

        @Test
        fun `batch copy should serialize correctly`() {
            val request = BatchCopyRequest(
                itemIds = listOf("item-1"),
                destinationId = "folder-456",
            )
            val serialized = json.encodeToString(BatchCopyRequest.serializer(), request)
            assertTrue(serialized.contains("folder-456"))
        }
    }

    @Nested
    inner class BatchStarTests {

        @Test
        fun `batch star request supports starred true and false`() {
            val starRequest = BatchStarRequest(itemIds = listOf("item-1"), starred = true)
            val unstarRequest = BatchStarRequest(itemIds = listOf("item-1"), starred = false)

            assertTrue(starRequest.starred)
            assertTrue(!unstarRequest.starred)
        }

        @Test
        fun `batch star uses starred parameter correctly`() {
            // This tests the fix for the starred parameter issue
            val request = BatchStarRequest(itemIds = listOf("item-1", "item-2"), starred = true)
            assertEquals(true, request.starred)
            assertEquals(2, request.itemIds.size)
        }

        @Test
        fun `batch unstar uses starred parameter correctly`() {
            val request = BatchStarRequest(itemIds = listOf("item-1"), starred = false)
            assertEquals(false, request.starred)
        }
    }

    @Nested
    inner class DownloadZipTests {

        @Test
        fun `download zip request structure is correct`() {
            val request = DownloadZipRequest(itemIds = listOf("item-1", "item-2", "item-3"))
            assertEquals(3, request.itemIds.size)
        }

        @Test
        fun `download zip should serialize correctly`() {
            val request = DownloadZipRequest(itemIds = listOf("a", "b"))
            val serialized = json.encodeToString(DownloadZipRequest.serializer(), request)
            assertTrue(serialized.contains("itemIds"))
        }
    }

    @Nested
    inner class EmptyTrashTests {

        @Test
        fun `empty trash response uses BatchResult structure`() {
            val result = BatchResult(successful = 3, failed = 0, errors = emptyList())
            assertEquals(3, result.successful)
            assertEquals(0, result.failed)
            assertTrue(result.errors.isEmpty())
        }

        @Test
        fun `empty trash with partial failures tracks failed count`() {
            val result = BatchResult(
                successful = 2,
                failed = 1,
                errors = listOf(BatchError("item-3", "Delete failed")),
            )
            assertEquals(2, result.successful)
            assertEquals(1, result.failed)
            assertEquals("item-3", result.errors.first().itemId)
        }
    }

    @Nested
    inner class BatchResultTests {

        @Test
        fun `batch result should track successful and failed counts`() {
            val result = BatchResult(
                successful = 5,
                failed = 2,
                errors = listOf(
                    BatchError("item-1", "Item not found"),
                    BatchError("item-2", "Permission denied"),
                ),
            )

            assertEquals(5, result.successful)
            assertEquals(2, result.failed)
            assertEquals(2, result.errors.size)
            assertEquals("Item not found", result.errors[0].error)
        }

        @Test
        fun `batch result with no errors should have empty list`() {
            val result = BatchResult(successful = 3, failed = 0)
            assertEquals(0, result.errors.size)
        }

        @Test
        fun `batch error contains item id and error message`() {
            val error = BatchError(itemId = "test-item", error = "Test error message")
            assertEquals("test-item", error.itemId)
            assertEquals("Test error message", error.error)
        }
    }
}
