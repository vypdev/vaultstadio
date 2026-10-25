/**
 * Unit tests for storage domain models: BatchResult, BatchError, ChunkedUploadInit, ChunkedUploadStatus,
 * FolderUploadFile, FolderUploadResult, FolderUploadError, Breadcrumb, PaginatedResponse.
 */

package com.vaultstadio.app.domain.storage

import com.vaultstadio.app.domain.storage.model.BatchError
import com.vaultstadio.app.domain.storage.model.BatchResult
import com.vaultstadio.app.domain.storage.model.ChunkedUploadInit
import com.vaultstadio.app.domain.storage.model.ChunkedUploadStatus
import com.vaultstadio.app.domain.storage.model.Breadcrumb
import com.vaultstadio.app.domain.storage.model.FolderUploadError
import com.vaultstadio.app.domain.storage.model.FolderUploadFile
import com.vaultstadio.app.domain.storage.model.FolderUploadResult
import com.vaultstadio.app.domain.storage.model.PaginatedResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BatchResultTest {

    @Test
    fun batchResult_constructionAndDefaults() {
        val r = BatchResult(successful = 2, failed = 0)
        assertEquals(2, r.successful)
        assertEquals(0, r.failed)
        assertEquals(emptyList<BatchError>(), r.errors)
    }

    @Test
    fun batchResult_withErrors() {
        val errors = listOf(
            BatchError(itemId = "id1", error = "Not found"),
            BatchError(itemId = "id2", error = "Permission denied"),
        )
        val r = BatchResult(successful = 1, failed = 2, errors = errors)
        assertEquals(1, r.successful)
        assertEquals(2, r.failed)
        assertEquals(2, r.errors.size)
        assertEquals("id1", r.errors[0].itemId)
        assertEquals("Not found", r.errors[0].error)
        assertEquals("id2", r.errors[1].itemId)
        assertEquals("Permission denied", r.errors[1].error)
    }
}

class BatchErrorTest {

    @Test
    fun batchError_construction() {
        val e = BatchError(itemId = "item-1", error = "Access denied")
        assertEquals("item-1", e.itemId)
        assertEquals("Access denied", e.error)
    }
}

class ChunkedUploadInitTest {

    @Test
    fun chunkedUploadInit_construction() {
        val init = ChunkedUploadInit(
            uploadId = "up-123",
            chunkSize = 1024L,
            totalChunks = 10,
        )
        assertEquals("up-123", init.uploadId)
        assertEquals(1024L, init.chunkSize)
        assertEquals(10, init.totalChunks)
    }
}

class ChunkedUploadStatusTest {

    @Test
    fun chunkedUploadStatus_construction() {
        val status = ChunkedUploadStatus(
            uploadId = "up-456",
            fileName = "file.bin",
            totalSize = 2048L,
            uploadedBytes = 1024L,
            progress = 0.5f,
            receivedChunks = listOf(0, 1),
            missingChunks = listOf(2, 3),
            isComplete = false,
        )
        assertEquals("up-456", status.uploadId)
        assertEquals("file.bin", status.fileName)
        assertEquals(2048L, status.totalSize)
        assertEquals(1024L, status.uploadedBytes)
        assertEquals(0.5f, status.progress)
        assertEquals(listOf(0, 1), status.receivedChunks)
        assertEquals(listOf(2, 3), status.missingChunks)
        assertFalse(status.isComplete)
    }

    @Test
    fun chunkedUploadStatus_isCompleteTrue() {
        val status = ChunkedUploadStatus(
            uploadId = "up-7",
            fileName = "done.bin",
            totalSize = 100L,
            uploadedBytes = 100L,
            progress = 1f,
            receivedChunks = listOf(0),
            missingChunks = emptyList(),
            isComplete = true,
        )
        assertTrue(status.isComplete)
        assertEquals(1f, status.progress)
    }
}

class FolderUploadFileTest {

    @Test
    fun folderUploadFile_construction() {
        val data = byteArrayOf(1, 2, 3)
        val file = FolderUploadFile(
            name = "a.txt",
            relativePath = "dir/a.txt",
            mimeType = "text/plain",
            data = data,
        )
        assertEquals("a.txt", file.name)
        assertEquals("dir/a.txt", file.relativePath)
        assertEquals("text/plain", file.mimeType)
        assertEquals(3, file.data.size)
    }

    @Test
    fun folderUploadFile_equalsUsesOnlyNameAndRelativePath() {
        val a = FolderUploadFile("f", "p/f", "text/plain", byteArrayOf(1))
        val b = FolderUploadFile("f", "p/f", "application/octet-stream", byteArrayOf(2))
        assertTrue(a == b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun folderUploadFile_equalsFalseWhenNameOrPathDiffers() {
        val a = FolderUploadFile("f", "p/f", "text/plain", byteArrayOf(1))
        assertFalse(a == FolderUploadFile("g", "p/f", "text/plain", byteArrayOf(1)))
        assertFalse(a == FolderUploadFile("f", "p/g", "text/plain", byteArrayOf(1)))
    }
}

class FolderUploadResultTest {

    @Test
    fun folderUploadResult_constructionAndDefaults() {
        val r = FolderUploadResult(uploadedFiles = 3, createdFolders = 2)
        assertEquals(3, r.uploadedFiles)
        assertEquals(2, r.createdFolders)
        assertTrue(r.errors.isEmpty())
    }

    @Test
    fun folderUploadResult_withErrors() {
        val errors = listOf(
            FolderUploadError(path = "a/b", error = "Conflict"),
        )
        val r = FolderUploadResult(uploadedFiles = 1, createdFolders = 1, errors = errors)
        assertEquals(1, r.errors.size)
        assertEquals("a/b", r.errors[0].path)
        assertEquals("Conflict", r.errors[0].error)
    }
}

class FolderUploadErrorTest {

    @Test
    fun folderUploadError_construction() {
        val e = FolderUploadError(path = "docs/file.pdf", error = "Too large")
        assertEquals("docs/file.pdf", e.path)
        assertEquals("Too large", e.error)
    }
}

class BreadcrumbTest {

    @Test
    fun breadcrumb_construction() {
        val b = Breadcrumb(id = "folder-1", name = "Documents", path = "/Documents")
        assertEquals("folder-1", b.id)
        assertEquals("Documents", b.name)
        assertEquals("/Documents", b.path)
    }

    @Test
    fun breadcrumb_withNullId() {
        val b = Breadcrumb(id = null, name = "Root", path = "/")
        assertEquals(null, b.id)
        assertEquals("Root", b.name)
    }
}

class PaginatedResponseTest {

    @Test
    fun paginatedResponse_construction() {
        val items = listOf("a", "b")
        val r = PaginatedResponse(
            items = items,
            total = 10L,
            page = 1,
            pageSize = 5,
            totalPages = 2,
            hasMore = true,
        )
        assertEquals(2, r.items.size)
        assertEquals(10L, r.total)
        assertEquals(1, r.page)
        assertEquals(5, r.pageSize)
        assertEquals(2, r.totalPages)
        assertTrue(r.hasMore)
    }

    @Test
    fun paginatedResponse_hasMoreFalseOnLastPage() {
        val r = PaginatedResponse(
            items = listOf(1, 2),
            total = 2L,
            page = 1,
            pageSize = 5,
            totalPages = 1,
            hasMore = false,
        )
        assertFalse(r.hasMore)
        assertEquals(1, r.totalPages)
    }

    @Test
    fun paginatedResponse_emptyItems() {
        val r = PaginatedResponse(
            items = emptyList<String>(),
            total = 0L,
            page = 1,
            pageSize = 10,
            totalPages = 0,
            hasMore = false,
        )
        assertTrue(r.items.isEmpty())
        assertEquals(0L, r.total)
        assertEquals(0, r.totalPages)
        assertFalse(r.hasMore)
    }
}
