/**
 * VaultStadio Plugin Context
 *
 * Provides plugins with access to core VaultStadio APIs.
 */

package com.vaultstadio.plugins.context

import arrow.core.Either
import com.vaultstadio.core.domain.event.EventBus
import com.vaultstadio.core.domain.model.StorageItemMetadata
import com.vaultstadio.domain.auth.model.UserInfo
import com.vaultstadio.domain.common.exception.StorageException
import com.vaultstadio.domain.common.pagination.PagedResult
import com.vaultstadio.domain.storage.model.StorageItem
import kotlinx.coroutines.CoroutineScope
import java.io.InputStream
import java.nio.file.Path

/**
 * Context provided to plugins with access to VaultStadio APIs.
 *
 * The context provides a sandboxed view of the core system,
 * limiting access based on the plugin's declared permissions.
 */
interface PluginContext {

    /**
     * The plugin's unique identifier.
     */
    val pluginId: String

    /**
     * Coroutine scope for the plugin's async operations.
     * This scope is cancelled when the plugin is stopped.
     */
    val scope: CoroutineScope

    /**
     * Event bus for subscribing to and publishing events.
     */
    val eventBus: EventBus

    /**
     * Storage API for file operations.
     */
    val storage: StorageApi

    /**
     * Metadata API for reading and writing custom metadata.
     */
    val metadata: MetadataApi

    /**
     * User API for accessing user information.
     */
    val users: UserApi

    /**
     * Logger for the plugin.
     */
    val logger: PluginLogger

    /**
     * Configuration store for plugin settings.
     */
    val config: ConfigStore

    /**
     * Temporary directory for plugin use.
     */
    val tempDirectory: Path

    /**
     * Data directory for persistent plugin data.
     */
    val dataDirectory: Path

    /**
     * HTTP client for making external requests (if permission granted).
     */
    val httpClient: HttpClientApi?

    /**
     * AI API for AI-powered operations (if permission granted).
     * Returns null if AI_ACCESS permission is not granted or no AI provider is configured.
     */
    val ai: AIApi?

    /**
     * Registers a custom API endpoint.
     *
     * @param method HTTP method
     * @param path Endpoint path (prefixed with /plugins/{pluginId}/)
     * @param handler Request handler
     */
    fun registerEndpoint(
        method: String,
        path: String,
        handler: suspend (EndpointRequest) -> EndpointResponse,
    )

    /**
     * Unregisters a custom API endpoint.
     *
     * @param method HTTP method
     * @param path Endpoint path
     */
    fun unregisterEndpoint(method: String, path: String)

    /**
     * Schedules a background task.
     *
     * @param name Task name for identification
     * @param cronExpression Cron expression for scheduling (or null for one-time)
     * @param task The task to execute
     * @return Task ID
     */
    suspend fun scheduleTask(
        name: String,
        cronExpression: String?,
        task: suspend () -> Unit,
    ): String

    /**
     * Cancels a scheduled task.
     *
     * @param taskId Task ID returned from scheduleTask
     */
    suspend fun cancelTask(taskId: String)
}

/**
 * Storage API available to plugins.
 */
interface StorageApi {

    /**
     * Gets a storage item by ID.
     */
    suspend fun getItem(itemId: String): Either<StorageException, StorageItem?>

    /**
     * Gets a storage item by path.
     */
    suspend fun getItemByPath(path: String, ownerId: String): Either<StorageException, StorageItem?>

    /**
     * Lists items in a folder.
     */
    suspend fun listFolder(
        folderId: String?,
        ownerId: String,
        limit: Int = 100,
        offset: Int = 0,
    ): Either<StorageException, PagedResult<StorageItem>>

    /**
     * Reads file content.
     * Requires READ_FILES permission.
     */
    suspend fun readFile(itemId: String): Either<StorageException, InputStream>

    /**
     * Retrieves file content by storage key.
     * Requires READ_FILES permission.
     */
    suspend fun retrieve(storageKey: String): Either<StorageException, InputStream>

    /**
     * Gets a pre-signed URL for direct file access.
     * Requires READ_FILES permission.
     */
    suspend fun getPresignedUrl(
        itemId: String,
        expirationSeconds: Long = 3600,
    ): Either<StorageException, String?>

    /**
     * Searches for items.
     */
    suspend fun search(
        query: String,
        ownerId: String? = null,
        mimeTypePattern: String? = null,
        limit: Int = 50,
    ): Either<StorageException, List<StorageItem>>

    /**
     * Gets items by MIME type pattern.
     */
    suspend fun getItemsByMimeType(
        pattern: String,
        ownerId: String? = null,
        limit: Int = 100,
    ): Either<StorageException, List<StorageItem>>
}

/**
 * Metadata API for plugins to attach custom metadata to items.
 */
interface MetadataApi {

    /**
     * Gets all metadata for an item created by this plugin.
     */
    suspend fun getMetadata(itemId: String): Either<StorageException, List<StorageItemMetadata>>

    /**
     * Gets a specific metadata value.
     */
    suspend fun getValue(itemId: String, key: String): Either<StorageException, String?>

    /**
     * Sets a metadata value.
     * Requires WRITE_METADATA permission.
     */
    suspend fun setValue(itemId: String, key: String, value: String): Either<StorageException, StorageItemMetadata>

    /**
     * Sets multiple metadata values at once.
     * Requires WRITE_METADATA permission.
     */
    suspend fun setValues(
        itemId: String,
        values: Map<String, String>,
    ): Either<StorageException, List<StorageItemMetadata>>

    /**
     * Saves a list of metadata entries.
     * Requires WRITE_METADATA permission.
     */
    suspend fun saveAll(metadata: List<StorageItemMetadata>): Either<StorageException, List<StorageItemMetadata>>

    /**
     * Deletes a metadata value.
     * Requires WRITE_METADATA permission.
     */
    suspend fun deleteValue(itemId: String, key: String): Either<StorageException, Unit>

    /**
     * Deletes all metadata for an item created by this plugin.
     * Requires WRITE_METADATA permission.
     */
    suspend fun deleteAllForItem(itemId: String): Either<StorageException, Int>

    /**
     * Searches items by metadata value.
     */
    suspend fun searchByValue(
        key: String,
        valuePattern: String,
        limit: Int = 100,
    ): Either<StorageException, List<String>>
}

/**
 * User API for accessing user information.
 */
interface UserApi {

    /**
     * Gets public user information.
     * Requires READ_USERS permission.
     */
    suspend fun getUserInfo(userId: String): Either<StorageException, UserInfo?>

    /**
     * Gets the current authenticated user (from request context).
     */
    suspend fun getCurrentUser(): UserInfo?
}

/**
 * Plugin logger interface.
 */
interface PluginLogger {
    fun trace(message: () -> String)
    fun debug(message: () -> String)
    fun info(message: () -> String)
    fun warn(message: () -> String)
    fun error(message: () -> String)
    fun error(throwable: Throwable, message: () -> String)
}

/**
 * Configuration store for plugin settings.
 */
interface ConfigStore {

    /**
     * Gets a configuration value.
     */
    fun <T> get(key: String, defaultValue: T): T

    /**
     * Gets a configuration value or null.
     */
    fun <T> getOrNull(key: String): T?

    /**
     * Sets a configuration value.
     */
    suspend fun <T> set(key: String, value: T)

    /**
     * Gets all configuration values.
     */
    fun getAll(): Map<String, Any?>

    /**
     * Gets a string value.
     */
    fun getString(key: String): String? = getOrNull(key)

    /**
     * Gets a boolean value.
     */
    fun getBoolean(key: String): Boolean? = getOrNull(key)

    /**
     * Gets an integer value.
     */
    fun getInt(key: String): Int? = getOrNull(key)

    /**
     * Gets a long value.
     */
    fun getLong(key: String): Long? = getOrNull(key)

    /**
     * Gets a double value.
     */
    fun getDouble(key: String): Double? = getOrNull(key)

    /**
     * Gets a list of strings.
     */
    fun getStringList(key: String): List<String>? = getOrNull(key)
}

/**
 * HTTP client API for external requests.
 */
interface HttpClientApi {

    /**
     * Performs an HTTP request.
     */
    suspend fun request(
        method: String,
        url: String,
        headers: Map<String, String> = emptyMap(),
        body: String? = null,
    ): HttpResponse
}

/**
 * HTTP response.
 */
data class HttpResponse(
    val statusCode: Int,
    val headers: Map<String, List<String>>,
    val body: String?,
)

/**
 * Endpoint request.
 */
data class EndpointRequest(
    val method: String,
    val path: String,
    val headers: Map<String, List<String>>,
    val queryParams: Map<String, List<String>>,
    val body: String?,
    val userId: String?,
)

/**
 * Endpoint response.
 */
data class EndpointResponse(
    val statusCode: Int,
    val headers: Map<String, String> = emptyMap(),
    val body: String? = null,
    val contentType: String = "application/json",
) {
    companion object {
        fun ok(body: String? = null) = EndpointResponse(200, body = body)
        fun created(body: String? = null) = EndpointResponse(201, body = body)
        fun noContent() = EndpointResponse(204)
        fun badRequest(message: String) = EndpointResponse(400, body = """{"error": "$message"}""")
        fun notFound(message: String = "Not found") = EndpointResponse(404, body = """{"error": "$message"}""")
        fun error(message: String) = EndpointResponse(500, body = """{"error": "$message"}""")
    }
}

/**
 * AI API for plugins to use AI capabilities.
 */
interface AIApi {

    /**
     * Checks if an AI provider is configured and available.
     */
    suspend fun isAvailable(): Boolean

    /**
     * Analyzes an image and returns a description.
     *
     * @param imageData Base64-encoded image data
     * @param mimeType MIME type of the image
     * @param prompt Optional custom prompt
     * @return Description of the image
     */
    suspend fun describeImage(
        imageData: ByteArray,
        mimeType: String,
        prompt: String? = null,
    ): AIResult<String>

    /**
     * Tags an image with relevant keywords.
     *
     * @param imageData Raw image bytes
     * @param mimeType MIME type of the image
     * @return Comma-separated list of tags
     */
    suspend fun tagImage(
        imageData: ByteArray,
        mimeType: String,
    ): AIResult<String>

    /**
     * Classifies content into provided categories.
     *
     * @param text Text to classify
     * @param categories List of category names
     * @return Best matching category
     */
    suspend fun classify(
        text: String,
        categories: List<String>,
    ): AIResult<String>

    /**
     * Analyzes an image for specific content.
     *
     * @param imageData Raw image bytes
     * @param mimeType MIME type of the image
     * @param prompt Analysis prompt
     * @return Analysis result
     */
    suspend fun analyzeImage(
        imageData: ByteArray,
        mimeType: String,
        prompt: String,
    ): AIResult<String>

    /**
     * Chat completion with the AI model.
     *
     * @param messages List of messages (role -> content)
     * @param model Optional model name
     * @return AI response
     */
    suspend fun chat(
        messages: List<Pair<String, String>>,
        model: String? = null,
    ): AIResult<String>
}

/**
 * Result of an AI operation.
 */
sealed class AIResult<out T> {
    data class Success<T>(val data: T) : AIResult<T>()
    data class Error(val message: String, val code: String? = null) : AIResult<Nothing>()

    fun getOrNull(): T? = when (this) {
        is Success -> data
        is Error -> null
    }

    fun isSuccess(): Boolean = this is Success

    inline fun <R> map(transform: (T) -> R): AIResult<R> = when (this) {
        is Success -> Success(transform(data))
        is Error -> this
    }

    inline fun <R> fold(
        onSuccess: (T) -> R,
        onError: (Error) -> R,
    ): R = when (this) {
        is Success -> onSuccess(data)
        is Error -> onError(this)
    }
}
