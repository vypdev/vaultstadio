/**
 * VaultStadio Exception Hierarchy
 *
 * Structured exceptions for error handling throughout the application.
 */

package com.vaultstadio.domain.common.exception

/**
 * Base exception for all storage-related errors.
 */
sealed class StorageException(
    override val message: String,
    override val cause: Throwable? = null,
) : Exception(message, cause) {

    /**
     * Returns an error code for API responses.
     */
    abstract val errorCode: String

    /**
     * HTTP status code to return.
     */
    abstract val httpStatus: Int
}

/**
 * Item not found errors.
 */
class ItemNotFoundException(
    val itemId: String? = null,
    val path: String? = null,
    message: String = "Item not found${itemId?.let { ": $it" } ?: path?.let { " at path: $it" } ?: ""}",
) : StorageException(message) {
    override val errorCode: String = "ITEM_NOT_FOUND"
    override val httpStatus: Int = 404
}

/**
 * User not found errors.
 */
class UserNotFoundException(
    val userId: String? = null,
    val email: String? = null,
    message: String = "User not found",
) : StorageException(message) {
    override val errorCode: String = "USER_NOT_FOUND"
    override val httpStatus: Int = 404
}

/**
 * Item already exists errors.
 */
class ItemAlreadyExistsException(
    val path: String,
    message: String = "Item already exists at path: $path",
) : StorageException(message) {
    override val errorCode: String = "ITEM_ALREADY_EXISTS"
    override val httpStatus: Int = 409
}

/**
 * Validation errors.
 */
class ValidationException(
    val field: String? = null,
    val details: Map<String, String> = emptyMap(),
    message: String = "Validation failed${field?.let { " for field: $it" } ?: ""}",
) : StorageException(message) {
    override val errorCode: String = "VALIDATION_ERROR"
    override val httpStatus: Int = 400
}

/**
 * Authentication errors.
 */
class AuthenticationException(
    message: String = "Authentication required",
) : StorageException(message) {
    override val errorCode: String = "AUTHENTICATION_REQUIRED"
    override val httpStatus: Int = 401
}

/**
 * Authorization/Permission errors.
 */
class AuthorizationException(
    val requiredPermission: String? = null,
    message: String = "Access denied${requiredPermission?.let { ": requires $it" } ?: ""}",
) : StorageException(message) {
    override val errorCode: String = "ACCESS_DENIED"
    override val httpStatus: Int = 403
}

/**
 * Quota exceeded errors.
 */
class QuotaExceededException(
    val usedBytes: Long,
    val quotaBytes: Long,
    val requiredBytes: Long,
    message: String = "Storage quota exceeded. Used: $usedBytes, Quota: $quotaBytes, Required: $requiredBytes",
) : StorageException(message) {
    override val errorCode: String = "QUOTA_EXCEEDED"
    override val httpStatus: Int = 507
}

/**
 * File size limit errors.
 */
class FileSizeLimitException(
    val fileSize: Long,
    val maxSize: Long,
    message: String = "File size ($fileSize bytes) exceeds maximum allowed size ($maxSize bytes)",
) : StorageException(message) {
    override val errorCode: String = "FILE_TOO_LARGE"
    override val httpStatus: Int = 413
}

/**
 * Invalid operation errors.
 */
class InvalidOperationException(
    val operation: String,
    message: String = "Invalid operation: $operation",
) : StorageException(message) {
    override val errorCode: String = "INVALID_OPERATION"
    override val httpStatus: Int = 400
}

/**
 * Storage backend errors (file system, S3, etc.).
 */
class StorageBackendException(
    val backend: String,
    message: String = "Storage backend error: $backend",
    cause: Throwable? = null,
) : StorageException(message, cause) {
    override val errorCode: String = "STORAGE_BACKEND_ERROR"
    override val httpStatus: Int = 500
}

/**
 * Database errors.
 */
class DatabaseException(
    message: String = "Database error",
    cause: Throwable? = null,
) : StorageException(message, cause) {
    override val errorCode: String = "DATABASE_ERROR"
    override val httpStatus: Int = 500
}

/**
 * Concurrent modification errors (optimistic locking).
 */
class ConcurrentModificationException(
    val itemId: String,
    val expectedVersion: Long,
    val actualVersion: Long,
    message: String = "Item $itemId was modified by another request",
) : StorageException(message) {
    override val errorCode: String = "CONCURRENT_MODIFICATION"
    override val httpStatus: Int = 409
}

/**
 * Share errors.
 */
sealed class ShareException(
    message: String,
    cause: Throwable? = null,
) : StorageException(message, cause)

class ShareNotFoundException(
    val shareId: String? = null,
    val token: String? = null,
    message: String = "Share not found",
) : ShareException(message) {
    override val errorCode: String = "SHARE_NOT_FOUND"
    override val httpStatus: Int = 404
}

class ShareExpiredException(
    val shareId: String,
    message: String = "Share has expired",
) : ShareException(message) {
    override val errorCode: String = "SHARE_EXPIRED"
    override val httpStatus: Int = 410
}

class ShareDownloadLimitException(
    val shareId: String,
    val downloadLimit: Int,
    message: String = "Share download limit ($downloadLimit) reached",
) : ShareException(message) {
    override val errorCode: String = "SHARE_DOWNLOAD_LIMIT"
    override val httpStatus: Int = 429
}

class SharePasswordRequiredException(
    val shareId: String,
    message: String = "Password required to access this share",
) : ShareException(message) {
    override val errorCode: String = "SHARE_PASSWORD_REQUIRED"
    override val httpStatus: Int = 401
}

class SharePasswordInvalidException(
    val shareId: String,
    message: String = "Invalid password for share",
) : ShareException(message) {
    override val errorCode: String = "SHARE_PASSWORD_INVALID"
    override val httpStatus: Int = 403
}

/**
 * Plugin errors.
 */
sealed class PluginException(
    message: String,
    cause: Throwable? = null,
) : StorageException(message, cause)

class PluginNotFoundException(
    val pluginId: String,
    message: String = "Plugin not found: $pluginId",
) : PluginException(message) {
    override val errorCode: String = "PLUGIN_NOT_FOUND"
    override val httpStatus: Int = 404
}

class PluginLoadException(
    val pluginId: String,
    message: String = "Failed to load plugin: $pluginId",
    cause: Throwable? = null,
) : PluginException(message, cause) {
    override val errorCode: String = "PLUGIN_LOAD_ERROR"
    override val httpStatus: Int = 500
}

class PluginExecutionException(
    val pluginId: String,
    val operation: String,
    message: String = "Plugin $pluginId failed during $operation",
    cause: Throwable? = null,
) : PluginException(message, cause) {
    override val errorCode: String = "PLUGIN_EXECUTION_ERROR"
    override val httpStatus: Int = 500
}

class PluginVersionException(
    val pluginId: String,
    val requiredVersion: String,
    val actualVersion: String,
    message: String = "Plugin $pluginId version mismatch. Required: $requiredVersion, Actual: $actualVersion",
) : PluginException(message) {
    override val errorCode: String = "PLUGIN_VERSION_MISMATCH"
    override val httpStatus: Int = 409
}
