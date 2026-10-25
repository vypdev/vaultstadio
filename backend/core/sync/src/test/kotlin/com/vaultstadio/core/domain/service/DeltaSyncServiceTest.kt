/**
 * Tests for DeltaSyncService
 */

package com.vaultstadio.core.domain.service

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DeltaSyncServiceTest {

    private lateinit var deltaSyncService: DeltaSyncService

    @BeforeEach
    fun setup() {
        deltaSyncService = DeltaSyncService(defaultBlockSize = 16) // Small block size for testing
    }

    @Nested
    inner class RollingChecksumTests {

        @Test
        fun `rolling checksum calculates consistent values`() {
            val rolling = RollingChecksum(16)
            val data = "Hello, World!!!".toByteArray()

            val checksum1 = rolling.calculate(data)
            val checksum2 = rolling.calculate(data)

            assertEquals(checksum1, checksum2, "Same data should produce same checksum")
        }

        @Test
        fun `different data produces different checksums`() {
            val rolling = RollingChecksum(16)

            val checksum1 = rolling.calculate("Hello, World!!!".toByteArray())
            val checksum2 = rolling.calculate("Goodbye, World!".toByteArray())

            assertTrue(checksum1 != checksum2, "Different data should produce different checksums")
        }
    }

    @Nested
    inner class SignatureGenerationTests {

        @Test
        fun `generates signature for file`() {
            val fileData = "This is test file content for signature generation."
            val inputStream = fileData.toByteArray().inputStream()

            val signature = deltaSyncService.generateSignature(
                fileId = "test-file-1",
                inputStream = inputStream,
                fileSize = fileData.length.toLong(),
            )

            assertEquals("test-file-1", signature.fileId)
            assertEquals(fileData.length.toLong(), signature.fileSize)
            assertTrue(signature.blocks.isNotEmpty(), "Should have at least one block")
        }

        @Test
        fun `each block has weak and strong checksums`() {
            val fileData = "A".repeat(100) // 100 bytes
            val inputStream = fileData.toByteArray().inputStream()

            val signature = deltaSyncService.generateSignature(
                fileId = "test-file-2",
                inputStream = inputStream,
                fileSize = fileData.length.toLong(),
            )

            for (block in signature.blocks) {
                assertTrue(block.weakChecksum != 0L, "Weak checksum should not be zero")
                assertTrue(block.strongChecksum.isNotEmpty(), "Strong checksum should not be empty")
                assertEquals(32, block.strongChecksum.length, "Strong checksum should be MD5 hex (32 chars)")
            }
        }
    }

    @Nested
    inner class DeltaCalculationTests {

        @Test
        fun `identical files produce empty delta`() {
            val originalData = "This is the original content."
            val inputStream = originalData.toByteArray().inputStream()

            val signature = deltaSyncService.generateSignature(
                fileId = "test-file",
                inputStream = inputStream,
                fileSize = originalData.length.toLong(),
            )

            val delta = deltaSyncService.calculateDelta(signature, originalData.toByteArray())

            // All instructions should be CopyBlock
            val copyCount = delta.instructions.count { it is DeltaInstruction.CopyBlock }
            val insertCount = delta.instructions.count { it is DeltaInstruction.InsertData }

            assertEquals(0, insertCount, "Identical files should have no InsertData instructions")
            assertTrue(copyCount > 0, "Should have CopyBlock instructions for matching blocks")
        }

        @Test
        fun `completely different files produce full insert delta`() {
            val originalData = "AAAAAAAAAAAAAAAA" // 16 A's
            val newData = "BBBBBBBBBBBBBBBB" // 16 B's

            val inputStream = originalData.toByteArray().inputStream()
            val signature = deltaSyncService.generateSignature(
                fileId = "test-file",
                inputStream = inputStream,
                fileSize = originalData.length.toLong(),
            )

            val delta = deltaSyncService.calculateDelta(signature, newData.toByteArray())

            val insertCount = delta.instructions.count { it is DeltaInstruction.InsertData }
            assertTrue(insertCount > 0, "Different files should have InsertData instructions")
        }

        @Test
        fun `delta size is correct`() {
            val originalData = "Hello, World!!!"
            val newData = "Hello, Universe!"

            val inputStream = originalData.toByteArray().inputStream()
            val signature = deltaSyncService.generateSignature(
                fileId = "test-file",
                inputStream = inputStream,
                fileSize = originalData.length.toLong(),
            )

            val delta = deltaSyncService.calculateDelta(signature, newData.toByteArray())

            assertEquals(newData.length.toLong(), delta.targetFileSize)
        }
    }

    @Nested
    inner class DeltaApplicationTests {

        @Test
        fun `applying delta reconstructs new file`() {
            val originalData = "This is the original content for testing."
            val newData = "This is the modified content for testing!"

            val inputStream = originalData.toByteArray().inputStream()
            val signature = deltaSyncService.generateSignature(
                fileId = "test-file",
                inputStream = inputStream,
                fileSize = originalData.length.toLong(),
            )

            val delta = deltaSyncService.calculateDelta(signature, newData.toByteArray())
            val reconstructed = deltaSyncService.applyDelta(originalData.toByteArray(), delta)

            assertEquals(newData, String(reconstructed), "Reconstructed file should match new data")
        }

        @Test
        fun `round trip with identical data`() {
            val data = "Round trip test data that stays the same."

            val inputStream = data.toByteArray().inputStream()
            val signature = deltaSyncService.generateSignature(
                fileId = "test-file",
                inputStream = inputStream,
                fileSize = data.length.toLong(),
            )

            val delta = deltaSyncService.calculateDelta(signature, data.toByteArray())
            val reconstructed = deltaSyncService.applyDelta(data.toByteArray(), delta)

            assertEquals(data, String(reconstructed))
        }
    }

    @Nested
    inner class EfficiencyTests {

        @Test
        fun `efficiency is 0 for identical files`() {
            val data = "A".repeat(64)

            val inputStream = data.toByteArray().inputStream()
            val signature = deltaSyncService.generateSignature(
                fileId = "test-file",
                inputStream = inputStream,
                fileSize = data.length.toLong(),
            )

            val delta = deltaSyncService.calculateDelta(signature, data.toByteArray())
            val efficiency = deltaSyncService.calculateDeltaEfficiency(delta)

            assertEquals(0.0, efficiency, 0.01, "Identical files should have 0% new data")
        }

        @Test
        fun `efficiency is 1 for completely different files`() {
            val originalData = "A".repeat(64)
            val newData = "B".repeat(64)

            val inputStream = originalData.toByteArray().inputStream()
            val signature = deltaSyncService.generateSignature(
                fileId = "test-file",
                inputStream = inputStream,
                fileSize = originalData.length.toLong(),
            )

            val delta = deltaSyncService.calculateDelta(signature, newData.toByteArray())
            val efficiency = deltaSyncService.calculateDeltaEfficiency(delta)

            assertEquals(1.0, efficiency, 0.01, "Completely different files should have 100% new data")
        }
    }
}
