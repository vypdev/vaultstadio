/**
 * VaultStadio Multipart Upload Manager
 *
 * Abstraction for managing multipart uploads across distributed systems.
 * Supports in-memory storage (development) and Redis (production).
 */

package com.vaultstadio.core.domain.service

import io.lettuce.core.RedisClient
import kotlinx.coroutines.future.await
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Multipart upload session data.
 *
 * @property uploadId Unique upload identifier
 * @property bucket Bucket/container name
 * @property key Object key/path
 * @property userId User who initiated the upload
 * @property initiatedAt When the upload was initiated
 * @property metadata Optional metadata for the upload
 */
data class MultipartUploadSession(
    val uploadId: String,
    val bucket: String,
    val key: String,
    val userId: String,
    val initiatedAt: Instant,
    val metadata: Map<String, String> = emptyMap(),
)

/**
 * Individual part of a multipart upload.
 *
 * @property partNumber Part number (1-based)
 * @property etag ETag of the part (usually MD5 hash)
 * @property size Size of the part in bytes
 */
data class UploadPart(
    val partNumber: Int,
    val etag: String,
    val size: Long,
)

/**
 * Complete multipart upload result.
 *
 * @property uploadId Upload ID
 * @property bucket Bucket name
 * @property key Object key
 * @property etag Final ETag of combined object
 * @property size Total size in bytes
 */
data class CompleteUploadResult(
    val uploadId: String,
    val bucket: String,
    val key: String,
    val etag: String,
    val size: Long,
)

/**
 * Interface for multipart upload management.
 *
 * Implementations can use different backends:
 * - In-memory (for development/single instance)
 * - Redis (for production/multi-instance)
 * - Database (for persistence across restarts)
 */
interface MultipartUploadManagerInterface {

    /**
     * Initiate a new multipart upload.
     *
     * @param bucket Bucket/container name
     * @param key Object key/path
     * @param userId User initiating the upload
     * @param metadata Optional metadata
     * @return Upload session
     */
    suspend fun initiate(
        bucket: String,
        key: String,
        userId: String,
        metadata: Map<String, String> = emptyMap(),
    ): MultipartUploadSession

    /**
     * Get an existing upload session.
     *
     * @param uploadId Upload identifier
     * @return Upload session, or null if not found
     */
    suspend fun get(uploadId: String): MultipartUploadSession?

    /**
     * Add a part to an upload.
     *
     * @param uploadId Upload identifier
     * @param partNumber Part number (1-based)
     * @param data Part data
     * @return Part info, or null if upload not found
     */
    suspend fun addPart(uploadId: String, partNumber: Int, data: ByteArray): UploadPart?

    /**
     * Get all parts for an upload.
     *
     * @param uploadId Upload identifier
     * @return List of parts, or empty if upload not found
     */
    suspend fun getParts(uploadId: String): List<UploadPart>

    /**
     * Complete a multipart upload and combine parts.
     *
     * @param uploadId Upload identifier
     * @return Combined data as ByteArray, or null if upload not found
     */
    suspend fun complete(uploadId: String): ByteArray?

    /**
     * Complete a multipart upload and return result without combining in memory.
     * Useful for large files where data should be streamed to storage.
     *
     * @param uploadId Upload identifier
     * @param partHandler Handler that receives each part in order
     * @return Upload result, or null if upload not found
     */
    suspend fun completeStreaming(
        uploadId: String,
        partHandler: suspend (partNumber: Int, data: ByteArray) -> Unit,
    ): CompleteUploadResult?

    /**
     * Abort a multipart upload and clean up.
     *
     * @param uploadId Upload identifier
     * @return True if upload was aborted, false if not found
     */
    suspend fun abort(uploadId: String): Boolean

    /**
     * List all active uploads for a user.
     *
     * @param userId User identifier
     * @param bucket Optional bucket filter
     * @return List of upload sessions
     */
    suspend fun listUploads(userId: String, bucket: String? = null): List<MultipartUploadSession>

    /**
     * Clean up expired uploads.
     *
     * @param olderThan Clean up uploads older than this instant
     * @return Number of uploads cleaned up
     */
    suspend fun cleanupExpired(olderThan: Instant): Int
}

/**
 * In-memory multipart upload manager implementation.
 *
 * Stores parts in memory. Suitable for development and single-instance deployments.
 * For production multi-instance deployments, use Redis or database implementation.
 */
class InMemoryMultipartUploadManager : MultipartUploadManagerInterface {

    private data class InMemorySession(
        val session: MultipartUploadSession,
        val parts: MutableMap<Int, Pair<UploadPart, ByteArray>> = mutableMapOf(),
    )

    private val uploads = ConcurrentHashMap<String, InMemorySession>()

    override suspend fun initiate(
        bucket: String,
        key: String,
        userId: String,
        metadata: Map<String, String>,
    ): MultipartUploadSession {
        val session = MultipartUploadSession(
            uploadId = UUID.randomUUID().toString(),
            bucket = bucket,
            key = key,
            userId = userId,
            initiatedAt = Clock.System.now(),
            metadata = metadata,
        )
        uploads[session.uploadId] = InMemorySession(session)
        return session
    }

    override suspend fun get(uploadId: String): MultipartUploadSession? {
        return uploads[uploadId]?.session
    }

    override suspend fun addPart(uploadId: String, partNumber: Int, data: ByteArray): UploadPart? {
        val inMemory = uploads[uploadId] ?: return null
        val etag = "\"${calculateMD5(data)}\""
        val part = UploadPart(partNumber, etag, data.size.toLong())
        inMemory.parts[partNumber] = part to data
        return part
    }

    override suspend fun getParts(uploadId: String): List<UploadPart> {
        val inMemory = uploads[uploadId] ?: return emptyList()
        return inMemory.parts.values.map { it.first }.sortedBy { it.partNumber }
    }

    override suspend fun complete(uploadId: String): ByteArray? {
        val inMemory = uploads[uploadId] ?: return null
        val sortedParts = inMemory.parts.entries.sortedBy { it.key }
        val combined = sortedParts.fold(ByteArray(0)) { acc, entry ->
            acc + entry.value.second
        }
        uploads.remove(uploadId)
        return combined
    }

    override suspend fun completeStreaming(
        uploadId: String,
        partHandler: suspend (partNumber: Int, data: ByteArray) -> Unit,
    ): CompleteUploadResult? {
        val inMemory = uploads[uploadId] ?: return null
        val sortedParts = inMemory.parts.entries.sortedBy { it.key }

        var totalSize = 0L
        for ((partNumber, partData) in sortedParts) {
            partHandler(partNumber, partData.second)
            totalSize += partData.second.size
        }

        val combinedEtag = "\"${calculateCombinedEtag(sortedParts.map { it.value.first.etag })}\""

        uploads.remove(uploadId)

        return CompleteUploadResult(
            uploadId = uploadId,
            bucket = inMemory.session.bucket,
            key = inMemory.session.key,
            etag = combinedEtag,
            size = totalSize,
        )
    }

    override suspend fun abort(uploadId: String): Boolean {
        return uploads.remove(uploadId) != null
    }

    override suspend fun listUploads(userId: String, bucket: String?): List<MultipartUploadSession> {
        return uploads.values
            .filter { it.session.userId == userId }
            .filter { bucket == null || it.session.bucket == bucket }
            .map { it.session }
    }

    override suspend fun cleanupExpired(olderThan: Instant): Int {
        var count = 0
        val iterator = uploads.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (entry.value.session.initiatedAt < olderThan) {
                iterator.remove()
                count++
            }
        }
        return count
    }

    private fun calculateMD5(data: ByteArray): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(data)
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun calculateCombinedEtag(etags: List<String>): String {
        // S3-style combined ETag: MD5 of concatenated binary MD5s + "-" + part count
        val cleanedEtags = etags.map { it.trim('"') }
        val combined = cleanedEtags.joinToString("")
        return "${calculateMD5(combined.toByteArray())}-${etags.size}"
    }
}

/**
 * Redis-based multipart upload manager implementation.
 *
 * Uses Redis for distributed storage with automatic TTL.
 * Suitable for production multi-instance deployments.
 *
 * Data structure in Redis:
 * - Session: HASH at key "{keyPrefix}session:{uploadId}" with fields: bucket, key, userId, initiatedAt, metadata
 * - Parts: HASH at key "{keyPrefix}parts:{uploadId}" with fields: "part:{partNumber}" -> serialized part data
 * - User index: SET at key "{keyPrefix}user:{userId}" with upload IDs
 */
class RedisMultipartUploadManager(
    private val redisConnectionString: String,
    private val keyPrefix: String = "multipart:",
    private val defaultTtlSeconds: Long = 86400, // 24 hours
) : MultipartUploadManagerInterface {

    private val sessionKeyPrefix = "${keyPrefix}session:"
    private val partsKeyPrefix = "${keyPrefix}parts:"
    private val partsDataKeyPrefix = "${keyPrefix}data:"
    private val userIndexKeyPrefix = "${keyPrefix}user:"

    private val json = Json { ignoreUnknownKeys = true }

    // Lazy initialization of Redis client
    private val redisClient by lazy {
        RedisClient.create(redisConnectionString)
    }

    private val connection by lazy {
        redisClient.connect()
    }

    private val asyncCommands by lazy {
        connection.async()
    }

    override suspend fun initiate(
        bucket: String,
        key: String,
        userId: String,
        metadata: Map<String, String>,
    ): MultipartUploadSession {
        val uploadId = UUID.randomUUID().toString()
        val now = Clock.System.now()

        val session = MultipartUploadSession(
            uploadId = uploadId,
            bucket = bucket,
            key = key,
            userId = userId,
            initiatedAt = now,
            metadata = metadata,
        )

        // Store session in Redis
        val sessionKey = "$sessionKeyPrefix$uploadId"
        val metadataJson = json.encodeToString(
            MapSerializer(String.serializer(), String.serializer()),
            metadata,
        )
        val sessionData = mapOf(
            "bucket" to bucket,
            "key" to key,
            "userId" to userId,
            "initiatedAt" to now.toString(),
            "metadata" to metadataJson,
        )

        asyncCommands.hset(sessionKey, sessionData).await()
        asyncCommands.expire(sessionKey, defaultTtlSeconds).await()

        // Add to user index
        val userIndexKey = "$userIndexKeyPrefix$userId"
        asyncCommands.sadd(userIndexKey, uploadId).await()
        asyncCommands.expire(userIndexKey, defaultTtlSeconds).await()

        return session
    }

    override suspend fun get(uploadId: String): MultipartUploadSession? {
        val sessionKey = "$sessionKeyPrefix$uploadId"
        val sessionData = asyncCommands.hgetall(sessionKey).await()

        if (sessionData.isNullOrEmpty()) return null

        val metadata = try {
            json.decodeFromString(
                MapSerializer(String.serializer(), String.serializer()),
                sessionData["metadata"] ?: "{}",
            )
        } catch (e: Exception) {
            emptyMap()
        }

        return MultipartUploadSession(
            uploadId = uploadId,
            bucket = sessionData["bucket"] ?: "",
            key = sessionData["key"] ?: "",
            userId = sessionData["userId"] ?: "",
            initiatedAt = Instant.parse(sessionData["initiatedAt"] ?: Clock.System.now().toString()),
            metadata = metadata,
        )
    }

    override suspend fun addPart(uploadId: String, partNumber: Int, data: ByteArray): UploadPart? {
        // Verify session exists
        get(uploadId) ?: return null

        val etag = "\"${calculateMD5(data)}\""
        val part = UploadPart(partNumber, etag, data.size.toLong())

        // Store part metadata
        val partsKey = "$partsKeyPrefix$uploadId"
        val partInfo = json.encodeToString(SerializableUploadPart.serializer(), part.toSerializable())
        asyncCommands.hset(partsKey, "part:$partNumber", partInfo).await()
        asyncCommands.expire(partsKey, defaultTtlSeconds).await()

        // Store part data as base64 string (Redis strings are safer than raw bytes)
        val dataKey = "$partsDataKeyPrefix$uploadId:$partNumber"
        val base64Data = java.util.Base64.getEncoder().encodeToString(data)
        asyncCommands.set(dataKey, base64Data).await()
        asyncCommands.expire(dataKey, defaultTtlSeconds).await()

        return part
    }

    override suspend fun getParts(uploadId: String): List<UploadPart> {
        val partsKey = "$partsKeyPrefix$uploadId"
        val partsData = asyncCommands.hgetall(partsKey).await() ?: return emptyList()

        return partsData.entries
            .filter { (key, _) -> key.startsWith("part:") }
            .mapNotNull { (_, value) ->
                try {
                    json.decodeFromString(SerializableUploadPart.serializer(), value).toUploadPart()
                } catch (e: Exception) {
                    null
                }
            }
            .sortedBy { it.partNumber }
    }

    override suspend fun complete(uploadId: String): ByteArray? {
        val parts = getParts(uploadId)
        if (parts.isEmpty()) return null

        // Fetch and combine all parts
        val combined = mutableListOf<Byte>()
        for (part in parts.sortedBy { it.partNumber }) {
            val dataKey = "$partsDataKeyPrefix$uploadId:${part.partNumber}"
            val base64Data = asyncCommands.get(dataKey).await()
            if (base64Data != null) {
                val data = java.util.Base64.getDecoder().decode(base64Data)
                combined.addAll(data.toList())
            }
        }

        // Clean up
        cleanup(uploadId)

        return combined.toByteArray()
    }

    override suspend fun completeStreaming(
        uploadId: String,
        partHandler: suspend (partNumber: Int, data: ByteArray) -> Unit,
    ): CompleteUploadResult? {
        val session = get(uploadId) ?: return null
        val parts = getParts(uploadId)
        if (parts.isEmpty()) return null

        var totalSize = 0L
        val sortedParts = parts.sortedBy { it.partNumber }

        for (part in sortedParts) {
            val dataKey = "$partsDataKeyPrefix$uploadId:${part.partNumber}"
            val base64Data = asyncCommands.get(dataKey).await()
            if (base64Data != null) {
                val data = java.util.Base64.getDecoder().decode(base64Data)
                partHandler(part.partNumber, data)
                totalSize += data.size
            }
        }

        val combinedEtag = "\"${calculateCombinedEtag(sortedParts.map { it.etag })}\""

        // Clean up
        cleanup(uploadId)

        return CompleteUploadResult(
            uploadId = uploadId,
            bucket = session.bucket,
            key = session.key,
            etag = combinedEtag,
            size = totalSize,
        )
    }

    override suspend fun abort(uploadId: String): Boolean {
        get(uploadId) ?: return false
        cleanup(uploadId)
        return true
    }

    override suspend fun listUploads(userId: String, bucket: String?): List<MultipartUploadSession> {
        val userIndexKey = "$userIndexKeyPrefix$userId"
        val uploadIds = asyncCommands.smembers(userIndexKey).await() ?: return emptyList()

        return uploadIds.mapNotNull { uploadId ->
            get(uploadId)?.takeIf { bucket == null || it.bucket == bucket }
        }
    }

    override suspend fun cleanupExpired(olderThan: Instant): Int {
        // Redis handles TTL automatically
        return 0
    }

    private suspend fun cleanup(uploadId: String) {
        val session = get(uploadId)
        val parts = getParts(uploadId)

        // Delete part data
        for (part in parts) {
            val dataKey = "$partsDataKeyPrefix$uploadId:${part.partNumber}"
            asyncCommands.del(dataKey).await()
        }

        // Delete parts index
        asyncCommands.del("$partsKeyPrefix$uploadId").await()

        // Delete session
        asyncCommands.del("$sessionKeyPrefix$uploadId").await()

        // Remove from user index
        session?.let {
            asyncCommands.srem("$userIndexKeyPrefix${it.userId}", uploadId).await()
        }
    }

    private fun calculateMD5(data: ByteArray): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(data)
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun calculateCombinedEtag(etags: List<String>): String {
        val cleanedEtags = etags.map { it.trim('"') }
        val combined = cleanedEtags.joinToString("")
        return "${calculateMD5(combined.toByteArray())}-${etags.size}"
    }

    /**
     * Close the Redis connection.
     */
    fun close() {
        connection.close()
        redisClient.shutdown()
    }
}

/**
 * Serializable version of UploadPart for Redis storage.
 */
@Serializable
private data class SerializableUploadPart(
    val partNumber: Int,
    val etag: String,
    val size: Long,
) {
    fun toUploadPart(): UploadPart = UploadPart(partNumber, etag, size)
}

private fun UploadPart.toSerializable(): SerializableUploadPart =
    SerializableUploadPart(partNumber, etag, size)
