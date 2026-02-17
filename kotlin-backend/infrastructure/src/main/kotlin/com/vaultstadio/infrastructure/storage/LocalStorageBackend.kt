/**
 * VaultStadio Local Storage Backend
 *
 * File-based storage backend for local or NAS deployments.
 */

package com.vaultstadio.infrastructure.storage

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.vaultstadio.core.domain.service.StorageBackend
import com.vaultstadio.core.exception.StorageBackendException
import com.vaultstadio.core.exception.StorageException
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.UUID

private val logger = KotlinLogging.logger {}

/**
 * Local filesystem storage backend.
 *
 * Files are stored in a hierarchical directory structure based on
 * the first characters of their storage key to prevent too many
 * files in a single directory.
 *
 * Structure:
 * /base/path/
 *   ├── ab/
 *   │   ├── cd/
 *   │   │   └── abcdef123456...
 *   │   └── ef/
 *   │       └── abef789012...
 *   └── 12/
 *       └── 34/
 *           └── 1234567890...
 */
class LocalStorageBackend(
    private val basePath: Path,
) : StorageBackend {

    init {
        // Create base directory if it doesn't exist
        if (!Files.exists(basePath)) {
            Files.createDirectories(basePath)
            logger.info { "Created storage directory: $basePath" }
        }
    }

    override suspend fun store(
        inputStream: InputStream,
        size: Long,
        mimeType: String?,
    ): Either<StorageException, String> = withContext(Dispatchers.IO) {
        try {
            // Generate a unique storage key
            val storageKey = generateStorageKey()
            val filePath = getFilePath(storageKey)

            // Create parent directories
            Files.createDirectories(filePath.parent)

            // Copy file to storage
            inputStream.use { stream ->
                Files.copy(stream, filePath, StandardCopyOption.REPLACE_EXISTING)
            }

            logger.debug { "Stored file: $storageKey ($size bytes)" }

            storageKey.right()
        } catch (e: Exception) {
            logger.error(e) { "Failed to store file" }
            StorageBackendException("local", "Failed to store file", e).left()
        }
    }

    override suspend fun retrieve(storageKey: String): Either<StorageException, InputStream> =
        withContext(Dispatchers.IO) {
            try {
                val filePath = getFilePath(storageKey)

                if (!Files.exists(filePath)) {
                    return@withContext StorageBackendException(
                        "local",
                        "File not found: $storageKey",
                    ).left()
                }

                Files.newInputStream(filePath).right()
            } catch (e: Exception) {
                logger.error(e) { "Failed to retrieve file: $storageKey" }
                StorageBackendException("local", "Failed to retrieve file", e).left()
            }
        }

    override suspend fun delete(storageKey: String): Either<StorageException, Unit> =
        withContext(Dispatchers.IO) {
            try {
                val filePath = getFilePath(storageKey)

                if (Files.exists(filePath)) {
                    Files.delete(filePath)
                    logger.debug { "Deleted file: $storageKey" }

                    // Clean up empty parent directories
                    cleanupEmptyDirs(filePath.parent)
                }

                Unit.right()
            } catch (e: Exception) {
                logger.error(e) { "Failed to delete file: $storageKey" }
                StorageBackendException("local", "Failed to delete file", e).left()
            }
        }

    override suspend fun copy(sourceKey: String): Either<StorageException, String> =
        withContext(Dispatchers.IO) {
            try {
                val sourcePath = getFilePath(sourceKey)

                if (!Files.exists(sourcePath)) {
                    return@withContext StorageBackendException(
                        "local",
                        "Source file not found: $sourceKey",
                    ).left()
                }

                val newKey = generateStorageKey()
                val destPath = getFilePath(newKey)

                Files.createDirectories(destPath.parent)
                Files.copy(sourcePath, destPath, StandardCopyOption.REPLACE_EXISTING)

                logger.debug { "Copied file: $sourceKey -> $newKey" }

                newKey.right()
            } catch (e: Exception) {
                logger.error(e) { "Failed to copy file: $sourceKey" }
                StorageBackendException("local", "Failed to copy file", e).left()
            }
        }

    override suspend fun exists(storageKey: String): Either<StorageException, Boolean> =
        withContext(Dispatchers.IO) {
            try {
                Files.exists(getFilePath(storageKey)).right()
            } catch (e: Exception) {
                StorageBackendException("local", "Failed to check file existence", e).left()
            }
        }

    override suspend fun getSize(storageKey: String): Either<StorageException, Long> =
        withContext(Dispatchers.IO) {
            try {
                val filePath = getFilePath(storageKey)

                if (!Files.exists(filePath)) {
                    return@withContext StorageBackendException(
                        "local",
                        "File not found: $storageKey",
                    ).left()
                }

                Files.size(filePath).right()
            } catch (e: Exception) {
                StorageBackendException("local", "Failed to get file size", e).left()
            }
        }

    override suspend fun getPresignedUrl(
        storageKey: String,
        expirationSeconds: Long,
    ): Either<StorageException, String?> {
        // Local storage doesn't support pre-signed URLs
        // Return null to indicate the file should be served through the API
        return (null as String?).right()
    }

    override suspend fun isAvailable(): Either<StorageException, Boolean> =
        withContext(Dispatchers.IO) {
            try {
                // Check if base path exists and is writable
                val exists = Files.exists(basePath)
                val isDirectory = Files.isDirectory(basePath)
                val isWritable = Files.isWritable(basePath)

                if (!exists || !isDirectory) {
                    logger.warn { "Storage directory does not exist: $basePath" }
                    return@withContext false.right()
                }

                if (!isWritable) {
                    logger.warn { "Storage directory is not writable: $basePath" }
                    return@withContext false.right()
                }

                // Try to create and delete a test file to verify write access
                val testFile = basePath.resolve(".health_check_${System.currentTimeMillis()}")
                Files.createFile(testFile)
                Files.delete(testFile)

                true.right()
            } catch (e: Exception) {
                logger.error(e) { "Storage availability check failed" }
                StorageBackendException("local", "Storage check failed", e).left()
            }
        }

    /**
     * Generates a unique storage key.
     */
    private fun generateStorageKey(): String {
        return UUID.randomUUID().toString().replace("-", "")
    }

    /**
     * Converts a storage key to a file path.
     *
     * Uses the first 4 characters to create a 2-level directory structure.
     */
    private fun getFilePath(storageKey: String): Path {
        val dir1 = storageKey.take(2)
        val dir2 = storageKey.substring(2, 4)
        return basePath.resolve(dir1).resolve(dir2).resolve(storageKey)
    }

    /**
     * Cleans up empty parent directories.
     */
    private fun cleanupEmptyDirs(dir: Path) {
        try {
            var current = dir
            while (current != basePath && Files.isDirectory(current)) {
                val isEmpty = Files.list(current).use { it.count() == 0L }
                if (isEmpty) {
                    Files.delete(current)
                    current = current.parent
                } else {
                    break
                }
            }
        } catch (e: Exception) {
            // Ignore cleanup errors
            logger.warn { "Failed to cleanup empty directories: ${e.message}" }
        }
    }

    /**
     * Calculates SHA-256 checksum of a file.
     */
    fun calculateChecksum(storageKey: String): String? {
        return try {
            val filePath = getFilePath(storageKey)
            val digest = MessageDigest.getInstance("SHA-256")

            Files.newInputStream(filePath).use { stream ->
                val buffer = ByteArray(8192)
                var read: Int
                while (stream.read(buffer).also { read = it } != -1) {
                    digest.update(buffer, 0, read)
                }
            }

            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            logger.error(e) { "Failed to calculate checksum: $storageKey" }
            null
        }
    }
}
