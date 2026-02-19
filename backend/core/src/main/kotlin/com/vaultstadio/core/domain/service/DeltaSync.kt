/**
 * VaultStadio Delta Sync
 *
 * Implements rsync-like delta synchronization using rolling checksums.
 * Enables efficient file synchronization by transferring only changed blocks.
 */

package com.vaultstadio.core.domain.service

import kotlinx.serialization.Serializable
import java.io.InputStream
import java.security.MessageDigest

/**
 * Block signature containing weak and strong checksums.
 *
 * @property blockIndex Block index in the file
 * @property offset Byte offset in the file
 * @property size Block size in bytes
 * @property weakChecksum Adler-32 style rolling checksum
 * @property strongChecksum MD5 hash for definitive matching
 */
@Serializable
data class BlockSignature(
    val blockIndex: Int,
    val offset: Long,
    val size: Int,
    val weakChecksum: Long,
    val strongChecksum: String,
)

/**
 * File signature containing all block signatures.
 *
 * @property fileId File identifier
 * @property fileSize Total file size
 * @property blockSize Block size used for chunking
 * @property blocks List of block signatures
 */
@Serializable
data class FileSignature(
    val fileId: String,
    val fileSize: Long,
    val blockSize: Int,
    val blocks: List<BlockSignature>,
)

/**
 * Delta instruction types.
 */
@Serializable
sealed class DeltaInstruction {
    /**
     * Copy a block from the original file.
     */
    @Serializable
    data class CopyBlock(
        val blockIndex: Int,
        val offset: Long,
        val size: Int,
    ) : DeltaInstruction()

    /**
     * Insert new data.
     */
    @Serializable
    data class InsertData(
        val offset: Long,
        val data: ByteArray,
    ) : DeltaInstruction() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is InsertData) return false
            return offset == other.offset && data.contentEquals(other.data)
        }

        override fun hashCode(): Int {
            return 31 * offset.hashCode() + data.contentHashCode()
        }
    }
}

/**
 * Delta between two file versions.
 *
 * @property sourceFileId Source file identifier
 * @property targetFileSize Target file size after applying delta
 * @property instructions List of delta instructions
 */
@Serializable
data class FileDelta(
    val sourceFileId: String,
    val targetFileSize: Long,
    val instructions: List<DeltaInstruction>,
)

/**
 * Rolling checksum calculator using Adler-32 style algorithm.
 *
 * The checksum can be efficiently updated when sliding a window.
 */
class RollingChecksum(private val blockSize: Int) {
    private var a: Long = 0 // Sum of bytes
    private var b: Long = 0 // Weighted sum
    private var count = 0

    companion object {
        const val MOD = 65536L // 2^16
    }

    /**
     * Calculate checksum from a byte array.
     */
    fun calculate(data: ByteArray, offset: Int = 0, length: Int = data.size): Long {
        reset()
        for (i in offset until minOf(offset + length, data.size)) {
            val byte = data[i].toInt() and 0xFF
            a = (a + byte) % MOD
            b = (b + a) % MOD
            count++
        }
        return (b shl 16) or a
    }

    /**
     * Update the rolling checksum by removing one byte and adding another.
     * This is the key optimization for the rsync algorithm.
     */
    fun roll(oldByte: Byte, newByte: Byte): Long {
        val oldVal = oldByte.toInt() and 0xFF
        val newVal = newByte.toInt() and 0xFF

        a = (a - oldVal + newVal + MOD) % MOD
        b = (b - (count.toLong() * oldVal) % MOD + a + MOD) % MOD

        return (b shl 16) or a
    }

    /**
     * Get the current checksum value.
     */
    fun value(): Long = (b shl 16) or a

    /**
     * Reset the checksum.
     */
    fun reset() {
        a = 0
        b = 0
        count = 0
    }
}

/**
 * Delta sync service implementing rsync-like algorithm.
 */
class DeltaSyncService(
    private val defaultBlockSize: Int = 4096, // 4KB blocks
) {

    /**
     * Generate a file signature for delta synchronization.
     *
     * @param fileId File identifier
     * @param inputStream Input stream of the file
     * @param fileSize Total file size
     * @param blockSize Block size to use
     * @return File signature
     */
    fun generateSignature(
        fileId: String,
        inputStream: InputStream,
        fileSize: Long,
        blockSize: Int = defaultBlockSize,
    ): FileSignature {
        val blocks = mutableListOf<BlockSignature>()
        val buffer = ByteArray(blockSize)
        val rolling = RollingChecksum(blockSize)
        var offset = 0L
        var blockIndex = 0

        while (true) {
            val bytesRead = inputStream.read(buffer)
            if (bytesRead <= 0) break

            val blockData = if (bytesRead < blockSize) {
                buffer.copyOf(bytesRead)
            } else {
                buffer
            }

            val weakChecksum = rolling.calculate(blockData)
            val strongChecksum = calculateMD5(blockData)

            blocks.add(
                BlockSignature(
                    blockIndex = blockIndex,
                    offset = offset,
                    size = bytesRead,
                    weakChecksum = weakChecksum,
                    strongChecksum = strongChecksum,
                ),
            )

            offset += bytesRead
            blockIndex++
        }

        return FileSignature(
            fileId = fileId,
            fileSize = fileSize,
            blockSize = blockSize,
            blocks = blocks,
        )
    }

    /**
     * Calculate the delta between a new file and an existing signature.
     *
     * @param signature Signature of the existing file
     * @param newData New file data
     * @return Delta instructions
     */
    fun calculateDelta(
        signature: FileSignature,
        newData: ByteArray,
    ): FileDelta {
        val instructions = mutableListOf<DeltaInstruction>()

        if (newData.isEmpty()) {
            return FileDelta(signature.fileId, 0, emptyList())
        }

        // Build lookup table: weak checksum -> list of blocks
        val checksumTable = signature.blocks.groupBy { it.weakChecksum }

        val blockSize = signature.blockSize
        val rolling = RollingChecksum(blockSize)

        var i = 0
        var pendingData = mutableListOf<Byte>()

        while (i < newData.size) {
            val windowEnd = minOf(i + blockSize, newData.size)
            val windowSize = windowEnd - i

            // Calculate weak checksum
            val weakChecksum = rolling.calculate(newData, i, windowSize)

            // Look for matching block
            val candidates = checksumTable[weakChecksum] ?: emptyList()
            var matchedBlock: BlockSignature? = null

            for (candidate in candidates) {
                // Verify with strong checksum
                val windowData = newData.copyOfRange(i, windowEnd)
                val strongChecksum = calculateMD5(windowData)

                if (candidate.strongChecksum == strongChecksum && candidate.size == windowSize) {
                    matchedBlock = candidate
                    break
                }
            }

            if (matchedBlock != null) {
                // Flush pending data
                if (pendingData.isNotEmpty()) {
                    instructions.add(
                        DeltaInstruction.InsertData(
                            offset = (i - pendingData.size).toLong(),
                            data = pendingData.toByteArray(),
                        ),
                    )
                    pendingData = mutableListOf()
                }

                // Add copy instruction
                instructions.add(
                    DeltaInstruction.CopyBlock(
                        blockIndex = matchedBlock.blockIndex,
                        offset = matchedBlock.offset,
                        size = matchedBlock.size,
                    ),
                )

                i += matchedBlock.size
            } else {
                // No match - add byte to pending data
                pendingData.add(newData[i])
                i++
            }
        }

        // Flush remaining pending data
        if (pendingData.isNotEmpty()) {
            instructions.add(
                DeltaInstruction.InsertData(
                    offset = (newData.size - pendingData.size).toLong(),
                    data = pendingData.toByteArray(),
                ),
            )
        }

        return FileDelta(
            sourceFileId = signature.fileId,
            targetFileSize = newData.size.toLong(),
            instructions = instructions,
        )
    }

    /**
     * Apply a delta to reconstruct the new file.
     *
     * @param originalData Original file data
     * @param delta Delta instructions
     * @param blockSize Original block size
     * @return Reconstructed file data
     */
    fun applyDelta(
        originalData: ByteArray,
        delta: FileDelta,
        blockSize: Int = defaultBlockSize,
    ): ByteArray {
        val result = ByteArray(delta.targetFileSize.toInt())
        var writeOffset = 0

        for (instruction in delta.instructions) {
            when (instruction) {
                is DeltaInstruction.CopyBlock -> {
                    val start = instruction.offset.toInt()
                    val end = start + instruction.size
                    System.arraycopy(originalData, start, result, writeOffset, instruction.size)
                    writeOffset += instruction.size
                }
                is DeltaInstruction.InsertData -> {
                    System.arraycopy(instruction.data, 0, result, writeOffset, instruction.data.size)
                    writeOffset += instruction.data.size
                }
            }
        }

        return result
    }

    /**
     * Calculate delta efficiency (how much data needs to be transferred).
     *
     * @param delta The delta to analyze
     * @return Transfer ratio (0.0 = all copied, 1.0 = all new)
     */
    fun calculateDeltaEfficiency(delta: FileDelta): Double {
        var copiedBytes = 0L
        var insertedBytes = 0L

        for (instruction in delta.instructions) {
            when (instruction) {
                is DeltaInstruction.CopyBlock -> copiedBytes += instruction.size
                is DeltaInstruction.InsertData -> insertedBytes += instruction.data.size
            }
        }

        val totalBytes = copiedBytes + insertedBytes
        return if (totalBytes > 0) {
            insertedBytes.toDouble() / totalBytes
        } else {
            0.0
        }
    }

    private fun calculateMD5(data: ByteArray): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(data)
        return digest.joinToString("") { "%02x".format(it) }
    }
}
