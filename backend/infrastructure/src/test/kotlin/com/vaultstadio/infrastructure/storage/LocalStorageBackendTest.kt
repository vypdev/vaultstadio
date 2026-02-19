/**
 * VaultStadio Local Storage Backend Tests
 */

package com.vaultstadio.infrastructure.storage

import arrow.core.Either
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.ByteArrayInputStream
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LocalStorageBackendTest {

    @TempDir
    lateinit var tempDir: Path

    private lateinit var backend: LocalStorageBackend

    @BeforeEach
    fun setup() {
        backend = LocalStorageBackend(tempDir)
    }

    @Nested
    inner class StoreTests {

        @Test
        fun `store should save file and return storage key`() = runTest {
            val content = "Hello, World!".toByteArray()
            val inputStream = ByteArrayInputStream(content)

            val result = backend.store(inputStream, content.size.toLong(), "text/plain")

            assertTrue(result.isRight())
            val storageKey = (result as Either.Right).value
            assertNotNull(storageKey)
            assertTrue(storageKey.isNotEmpty())
        }

        @Test
        fun `store should handle binary data`() = runTest {
            val content = ByteArray(1024) { it.toByte() }
            val inputStream = ByteArrayInputStream(content)

            val result = backend.store(inputStream, content.size.toLong(), "application/octet-stream")

            assertTrue(result.isRight())
        }

        @Test
        fun `store should handle empty file`() = runTest {
            val content = ByteArray(0)
            val inputStream = ByteArrayInputStream(content)

            val result = backend.store(inputStream, 0, "text/plain")

            assertTrue(result.isRight())
        }

        @Test
        fun `store should generate unique keys`() = runTest {
            val keys = mutableSetOf<String>()

            repeat(10) {
                val content = "File $it".toByteArray()
                val inputStream = ByteArrayInputStream(content)

                val result = backend.store(inputStream, content.size.toLong(), "text/plain")
                assertTrue(result.isRight())

                val key = (result as Either.Right).value
                assertFalse(keys.contains(key), "Duplicate key generated: $key")
                keys.add(key)
            }
        }
    }

    @Nested
    inner class RetrieveTests {

        @Test
        fun `retrieve should return stored content`() = runTest {
            val content = "Hello, World!".toByteArray()
            val storeResult = backend.store(
                ByteArrayInputStream(content),
                content.size.toLong(),
                "text/plain",
            )
            val storageKey = (storeResult as Either.Right).value

            val result = backend.retrieve(storageKey)

            assertTrue(result.isRight())
            val retrievedContent = (result as Either.Right).value.readBytes()
            assertEquals(content.size, retrievedContent.size)
            assertTrue(content.contentEquals(retrievedContent))
        }

        @Test
        fun `retrieve should return error for non-existent key`() = runTest {
            val result = backend.retrieve("nonexistent-key-12345678901234567890")

            assertTrue(result.isLeft())
        }
    }

    @Nested
    inner class DeleteTests {

        @Test
        fun `delete should remove stored file`() = runTest {
            val content = "To be deleted".toByteArray()
            val storeResult = backend.store(
                ByteArrayInputStream(content),
                content.size.toLong(),
                "text/plain",
            )
            val storageKey = (storeResult as Either.Right).value

            val deleteResult = backend.delete(storageKey)
            assertTrue(deleteResult.isRight())

            // Verify file is gone
            val retrieveResult = backend.retrieve(storageKey)
            assertTrue(retrieveResult.isLeft())
        }

        @Test
        fun `delete should handle non-existent key`() = runTest {
            val result = backend.delete("nonexistent-key-12345678901234567890")

            // Should succeed or return appropriate error
            // Depends on implementation
        }
    }

    @Nested
    inner class ExistsTests {

        @Test
        fun `exists should return true for stored file`() = runTest {
            val content = "Test content".toByteArray()
            val storeResult = backend.store(
                ByteArrayInputStream(content),
                content.size.toLong(),
                "text/plain",
            )
            val storageKey = (storeResult as Either.Right).value

            val result = backend.exists(storageKey)

            assertTrue(result.isRight())
            assertTrue((result as Either.Right).value)
        }

        @Test
        fun `exists should return false for non-existent key`() = runTest {
            val result = backend.exists("nonexistent-key-12345678901234567890")

            assertTrue(result.isRight())
            assertFalse((result as Either.Right).value)
        }
    }

    @Nested
    inner class SizeTests {

        @Test
        fun `getSize should return correct file size`() = runTest {
            val content = "Hello, World!".toByteArray()
            val storeResult = backend.store(
                ByteArrayInputStream(content),
                content.size.toLong(),
                "text/plain",
            )
            val storageKey = (storeResult as arrow.core.Either.Right).value

            val result = backend.getSize(storageKey)

            assertTrue(result.isRight())
            assertEquals(content.size.toLong(), (result as arrow.core.Either.Right).value)
        }

        @Test
        fun `getSize should return error for non-existent key`() = runTest {
            val result = backend.getSize("nonexistent-key-12345678901234567890")

            assertTrue(result.isLeft())
        }
    }

    @Nested
    inner class DirectoryStructureTests {

        @Test
        fun `should create hierarchical directory structure`() = runTest {
            val content = "Test".toByteArray()
            backend.store(ByteArrayInputStream(content), content.size.toLong(), "text/plain")

            // Verify subdirectories were created
            val subdirs = Files.list(tempDir).toList()
            assertTrue(subdirs.isNotEmpty())
        }
    }

    @Nested
    inner class CopyTests {

        @Test
        fun `copy should duplicate file with new key`() = runTest {
            val content = "Original content".toByteArray()
            val storeResult = backend.store(
                ByteArrayInputStream(content),
                content.size.toLong(),
                "text/plain",
            )
            val originalKey = (storeResult as Either.Right).value

            val copyResult = backend.copy(originalKey)

            assertTrue(copyResult.isRight())
            val newKey = (copyResult as Either.Right).value

            // Both files should exist and have same content
            assertTrue((backend.exists(originalKey) as Either.Right).value)
            assertTrue((backend.exists(newKey) as Either.Right).value)

            val originalContent = (backend.retrieve(originalKey) as Either.Right).value.readBytes()
            val copiedContent = (backend.retrieve(newKey) as Either.Right).value.readBytes()
            assertTrue(originalContent.contentEquals(copiedContent))
        }

        @Test
        fun `copy should return error for non-existent source key`() = runTest {
            val result = backend.copy("nonexistent-key-12345678901234567890")
            assertTrue(result.isLeft())
        }
    }

    @Nested
    inner class GetPresignedUrlTests {

        @Test
        fun `getPresignedUrl returns Right null for local storage`() = runTest {
            val result = backend.getPresignedUrl("any-key", 3600L)
            assertTrue(result.isRight())
            assertNull((result as Either.Right).value)
        }
    }

    @Nested
    inner class IsAvailableTests {

        @Test
        fun `isAvailable returns true when base path exists and is writable`() = runTest {
            val result = backend.isAvailable()
            assertTrue(result.isRight())
            assertTrue((result as Either.Right).value)
        }
    }

    @Nested
    inner class CalculateChecksumTests {

        @Test
        fun `calculateChecksum returns SHA-256 hash for stored file`() = runTest {
            val content = "Hello, World!".toByteArray()
            val storeResult = backend.store(
                ByteArrayInputStream(content),
                content.size.toLong(),
                "text/plain",
            )
            val storageKey = (storeResult as Either.Right).value
            val checksum = backend.calculateChecksum(storageKey)
            assertNotNull(checksum)
            assertEquals(64, checksum.length)
            assertTrue(checksum.all { it in '0'..'9' || it in 'a'..'f' })
        }

        @Test
        fun `calculateChecksum returns null for non-existent key`() {
            val checksum = backend.calculateChecksum("nonexistent-key-12345678901234567890")
            assertNull(checksum)
        }
    }
}
