/**
 * VaultStadio S3 Storage Backend
 *
 * S3-compatible storage backend for cloud deployments.
 * Supports AWS S3, MinIO, and other S3-compatible services.
 */

package com.vaultstadio.infrastructure.storage

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.CopyObjectRequest
import aws.sdk.kotlin.services.s3.model.DeleteObjectRequest
import aws.sdk.kotlin.services.s3.model.GetObjectRequest
import aws.sdk.kotlin.services.s3.model.HeadBucketRequest
import aws.sdk.kotlin.services.s3.model.HeadObjectRequest
import aws.sdk.kotlin.services.s3.model.PutObjectRequest
import aws.sdk.kotlin.services.s3.presigners.presignGetObject
import aws.smithy.kotlin.runtime.content.ByteStream
import aws.smithy.kotlin.runtime.content.toByteArray
import com.vaultstadio.core.domain.service.StorageBackend
import com.vaultstadio.core.exception.StorageBackendException
import com.vaultstadio.core.exception.StorageException
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

private val logger = KotlinLogging.logger {}

/**
 * S3 storage backend configuration.
 */
data class S3StorageConfig(
    val bucket: String,
    val region: String = "us-east-1",
    val endpoint: String? = null,
    val accessKeyId: String? = null,
    val secretAccessKey: String? = null,
    val usePathStyle: Boolean = false,
    val prefix: String = "",
)

/**
 * S3-compatible storage backend.
 *
 * Files are stored in the configured bucket with an optional prefix.
 * Uses a hierarchical key structure similar to LocalStorageBackend:
 *
 * bucket/
 *   └── prefix/
 *       ├── ab/
 *       │   └── cd/
 *       │       └── abcdef123456...
 *       └── 12/
 *           └── 34/
 *               └── 1234567890...
 */
class S3StorageBackend(
    private val config: S3StorageConfig,
    private val s3Client: S3Client,
) : StorageBackend {

    init {
        logger.info {
            "Initialized S3 storage backend: bucket=${config.bucket}, " +
                "region=${config.region}, endpoint=${config.endpoint ?: "default"}"
        }
    }

    override suspend fun store(
        inputStream: InputStream,
        size: Long,
        mimeType: String?,
    ): Either<StorageException, String> = withContext(Dispatchers.IO) {
        try {
            val storageKey = generateStorageKey()
            val objectKey = getObjectKey(storageKey)

            // Read input stream to byte array for S3 upload
            val bytes = inputStream.use { it.readBytes() }

            val request = PutObjectRequest {
                bucket = config.bucket
                key = objectKey
                contentType = mimeType
                contentLength = bytes.size.toLong()
                body = ByteStream.fromBytes(bytes)
            }

            s3Client.putObject(request)

            logger.debug { "Stored file in S3: $storageKey ($size bytes)" }

            storageKey.right()
        } catch (e: Exception) {
            logger.error(e) { "Failed to store file in S3" }
            StorageBackendException("s3", "Failed to store file: ${e.message}", e).left()
        }
    }

    override suspend fun retrieve(storageKey: String): Either<StorageException, InputStream> =
        withContext(Dispatchers.IO) {
            try {
                val objectKey = getObjectKey(storageKey)

                val request = GetObjectRequest {
                    bucket = config.bucket
                    key = objectKey
                }

                val response = s3Client.getObject(request) { resp ->
                    val bytes = resp.body?.toByteArray() ?: ByteArray(0)
                    ByteArrayInputStream(bytes)
                }

                response.right()
            } catch (e: aws.sdk.kotlin.services.s3.model.NoSuchKey) {
                StorageBackendException("s3", "File not found: $storageKey").left()
            } catch (e: Exception) {
                logger.error(e) { "Failed to retrieve file from S3: $storageKey" }
                StorageBackendException("s3", "Failed to retrieve file: ${e.message}", e).left()
            }
        }

    override suspend fun delete(storageKey: String): Either<StorageException, Unit> =
        withContext(Dispatchers.IO) {
            try {
                val objectKey = getObjectKey(storageKey)

                val request = DeleteObjectRequest {
                    bucket = config.bucket
                    key = objectKey
                }

                s3Client.deleteObject(request)

                logger.debug { "Deleted file from S3: $storageKey" }

                Unit.right()
            } catch (e: Exception) {
                logger.error(e) { "Failed to delete file from S3: $storageKey" }
                StorageBackendException("s3", "Failed to delete file: ${e.message}", e).left()
            }
        }

    override suspend fun copy(sourceKey: String): Either<StorageException, String> =
        withContext(Dispatchers.IO) {
            try {
                val sourceObjectKey = getObjectKey(sourceKey)
                val newKey = generateStorageKey()
                val destObjectKey = getObjectKey(newKey)

                val request = CopyObjectRequest {
                    bucket = config.bucket
                    copySource = "${config.bucket}/$sourceObjectKey"
                    key = destObjectKey
                }

                s3Client.copyObject(request)

                logger.debug { "Copied file in S3: $sourceKey -> $newKey" }

                newKey.right()
            } catch (e: aws.sdk.kotlin.services.s3.model.NoSuchKey) {
                StorageBackendException("s3", "Source file not found: $sourceKey").left()
            } catch (e: Exception) {
                logger.error(e) { "Failed to copy file in S3: $sourceKey" }
                StorageBackendException("s3", "Failed to copy file: ${e.message}", e).left()
            }
        }

    override suspend fun exists(storageKey: String): Either<StorageException, Boolean> =
        withContext(Dispatchers.IO) {
            try {
                val objectKey = getObjectKey(storageKey)

                val request = HeadObjectRequest {
                    bucket = config.bucket
                    key = objectKey
                }

                s3Client.headObject(request)
                true.right()
            } catch (e: aws.sdk.kotlin.services.s3.model.NotFound) {
                false.right()
            } catch (e: Exception) {
                StorageBackendException("s3", "Failed to check file existence: ${e.message}", e).left()
            }
        }

    override suspend fun getSize(storageKey: String): Either<StorageException, Long> =
        withContext(Dispatchers.IO) {
            try {
                val objectKey = getObjectKey(storageKey)

                val request = HeadObjectRequest {
                    bucket = config.bucket
                    key = objectKey
                }

                val response = s3Client.headObject(request)
                (response.contentLength ?: 0L).right()
            } catch (e: aws.sdk.kotlin.services.s3.model.NotFound) {
                StorageBackendException("s3", "File not found: $storageKey").left()
            } catch (e: Exception) {
                StorageBackendException("s3", "Failed to get file size: ${e.message}", e).left()
            }
        }

    override suspend fun getPresignedUrl(
        storageKey: String,
        expirationSeconds: Long,
    ): Either<StorageException, String?> = withContext(Dispatchers.IO) {
        try {
            val objectKey = getObjectKey(storageKey)

            val request = GetObjectRequest {
                bucket = config.bucket
                key = objectKey
            }

            val presignedRequest = s3Client.presignGetObject(request, expirationSeconds.seconds)
            presignedRequest.url.toString().right()
        } catch (e: Exception) {
            logger.error(e) { "Failed to generate presigned URL: $storageKey" }
            StorageBackendException("s3", "Failed to generate presigned URL: ${e.message}", e).left()
        }
    }

    override suspend fun isAvailable(): Either<StorageException, Boolean> =
        withContext(Dispatchers.IO) {
            try {
                val request = HeadBucketRequest {
                    bucket = config.bucket
                }

                s3Client.headBucket(request)
                true.right()
            } catch (e: aws.sdk.kotlin.services.s3.model.NotFound) {
                logger.warn { "S3 bucket not found: ${config.bucket}" }
                false.right()
            } catch (e: Exception) {
                logger.error(e) { "S3 availability check failed" }
                StorageBackendException("s3", "Storage check failed: ${e.message}", e).left()
            }
        }

    /**
     * Generates a unique storage key.
     */
    private fun generateStorageKey(): String {
        return UUID.randomUUID().toString().replace("-", "")
    }

    /**
     * Converts a storage key to an S3 object key.
     *
     * Uses the first 4 characters to create a 2-level directory structure,
     * similar to LocalStorageBackend.
     */
    private fun getObjectKey(storageKey: String): String {
        val dir1 = storageKey.take(2)
        val dir2 = storageKey.substring(2, 4)
        val prefix = if (config.prefix.isNotEmpty()) "${config.prefix}/" else ""
        return "$prefix$dir1/$dir2/$storageKey"
    }

    companion object {
        /**
         * Creates an S3 client with the given configuration.
         */
        suspend fun createClient(config: S3StorageConfig): S3Client {
            return S3Client {
                region = config.region
                config.endpoint?.let { endpointUrl = aws.smithy.kotlin.runtime.net.url.Url.parse(it) }
                forcePathStyle = config.usePathStyle

                if (config.accessKeyId != null && config.secretAccessKey != null) {
                    credentialsProvider = aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider {
                        accessKeyId = config.accessKeyId
                        secretAccessKey = config.secretAccessKey
                    }
                }
            }
        }
    }
}
