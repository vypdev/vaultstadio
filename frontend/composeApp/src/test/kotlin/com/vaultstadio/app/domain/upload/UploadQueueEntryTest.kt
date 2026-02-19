/**
 * Unit tests for upload domain types: UploadQueueEntry, FolderUploadEntry.
 */

package com.vaultstadio.app.domain.upload

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UploadQueueEntryWithDataTest {

    @Test
    fun withData_holdsNameSizeMimeTypeAndData() {
        val entry = UploadQueueEntry.WithData(
            name = "doc.pdf",
            size = 1024L,
            mimeType = "application/pdf",
            data = byteArrayOf(1, 2, 3),
        )
        assertEquals("doc.pdf", entry.name)
        assertEquals(1024L, entry.size)
        assertEquals("application/pdf", entry.mimeType)
        assertEquals(3, entry.data.size)
    }

    @Test
    fun withData_equalityByContent() {
        val a = UploadQueueEntry.WithData("a", 1L, "text/plain", byteArrayOf(1))
        val b = UploadQueueEntry.WithData("a", 1L, "text/plain", byteArrayOf(1))
        val c = UploadQueueEntry.WithData("a", 1L, "text/plain", byteArrayOf(2))
        assertTrue(a == b)
        assertFalse(a == c)
    }
}

class UploadQueueEntryChunkedTest {

    @Test
    fun chunked_holdsNameSizeMimeTypeAndSource() {
        val source = object : ChunkedFileSource {
            override val name: String = "large.bin"
            override val size: Long = 1000L
            override val mimeType: String = "application/octet-stream"
            override suspend fun readChunk(start: Long, end: Long): ByteArray = ByteArray(0)
        }
        val entry = UploadQueueEntry.Chunked(
            name = "large.bin",
            size = 1000L,
            mimeType = "application/octet-stream",
            source = source,
        )
        assertEquals("large.bin", entry.name)
        assertEquals(1000L, entry.size)
        assertEquals(source, entry.source)
    }
}

class FolderUploadEntryTest {

    @Test
    fun folderUploadEntry_holdsRelativePathAndData() {
        val entry = FolderUploadEntry(
            name = "file.txt",
            relativePath = "docs/file.txt",
            size = 100L,
            mimeType = "text/plain",
            data = byteArrayOf(1, 2, 3),
        )
        assertEquals("file.txt", entry.name)
        assertEquals("docs/file.txt", entry.relativePath)
        assertEquals(100L, entry.size)
        assertEquals(3, entry.data.size)
    }

    @Test
    fun folderUploadEntry_equalityByContent() {
        val a = FolderUploadEntry("a", "p/a", 1L, "text/plain", byteArrayOf(1))
        val b = FolderUploadEntry("a", "p/a", 1L, "text/plain", byteArrayOf(1))
        val c = FolderUploadEntry("a", "p/b", 1L, "text/plain", byteArrayOf(1))
        assertTrue(a == b)
        assertFalse(a == c)
    }
}

class ChunkedFileSourceTest {

    @Test
    fun chunkedFileSource_readChunkReturnsRequestedRange() = runTest {
        val source = object : ChunkedFileSource {
            override val name: String = "test"
            override val size: Long = 10L
            override val mimeType: String = "application/octet-stream"
            override suspend fun readChunk(start: Long, end: Long): ByteArray {
                return ByteArray((end - start).toInt()) { (start + it).toInt().toByte() }
            }
        }
        val chunk = source.readChunk(2, 5)
        assertEquals(3, chunk.size)
        assertEquals(2, chunk[0].toInt())
        assertEquals(3, chunk[1].toInt())
        assertEquals(4, chunk[2].toInt())
    }
}
