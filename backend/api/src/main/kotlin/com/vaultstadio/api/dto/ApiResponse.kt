/**
 * VaultStadio API Response DTOs
 */

package com.vaultstadio.api.dto

import com.vaultstadio.core.domain.model.Activity
import com.vaultstadio.core.domain.model.ActivityType
import com.vaultstadio.core.domain.model.ItemType
import com.vaultstadio.core.domain.model.ShareLink
import com.vaultstadio.core.domain.model.StorageItem
import com.vaultstadio.core.domain.model.StorageQuota
import com.vaultstadio.core.domain.model.User
import com.vaultstadio.core.domain.model.UserInfo
import com.vaultstadio.core.domain.model.UserRole
import com.vaultstadio.core.domain.model.UserStatus
import com.vaultstadio.core.domain.model.Visibility
import com.vaultstadio.core.domain.repository.PagedResult
import kotlinx.datetime.Instant
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive

/**
 * Generic API response wrapper.
 */
@Serializable
data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val error: ApiError? = null,
)

/**
 * API error details.
 */
@Serializable
data class ApiError(
    val code: String,
    val message: String,
    val details: Map<String, String>? = null,
)

/**
 * Paginated response.
 */
@Serializable
data class PaginatedResponse<T>(
    val items: List<T>,
    val total: Long,
    val page: Int,
    val pageSize: Int,
    val totalPages: Int,
    val hasMore: Boolean,
)

fun <T> PagedResult<T>.toResponse(): PaginatedResponse<T> = PaginatedResponse(
    items = items,
    total = total,
    page = currentPage,
    pageSize = limit,
    totalPages = totalPages,
    hasMore = hasMore,
)

// Storage Item DTOs

@Serializable
data class StorageItemResponse(
    val id: String,
    val name: String,
    val path: String,
    val type: ItemType,
    val parentId: String?,
    val size: Long,
    val mimeType: String?,
    val visibility: Visibility,
    val isStarred: Boolean,
    val isTrashed: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
    val metadata: Map<String, String>? = null,
)

fun StorageItem.toResponse(metadata: Map<String, String>? = null) = StorageItemResponse(
    id = id,
    name = name,
    path = path,
    type = type,
    parentId = parentId,
    size = size,
    mimeType = mimeType,
    visibility = visibility,
    isStarred = isStarred,
    isTrashed = isTrashed,
    createdAt = createdAt,
    updatedAt = updatedAt,
    metadata = metadata,
)

@Serializable
data class CreateFolderRequest(
    val name: String,
    val parentId: String? = null,
)

@Serializable
data class RenameRequest(
    val name: String,
)

@Serializable
data class MoveRequest(
    val destinationId: String?,
    val newName: String? = null,
)

@Serializable
data class CopyRequest(
    val destinationId: String?,
    val newName: String? = null,
)

// User DTOs

@Serializable
data class UserResponse(
    val id: String,
    val email: String,
    val username: String,
    val role: UserRole,
    val status: UserStatus,
    val avatarUrl: String?,
    val createdAt: Instant,
)

fun UserInfo.toResponse() = UserResponse(
    id = id,
    email = email,
    username = username,
    role = role,
    status = status,
    avatarUrl = avatarUrl,
    createdAt = createdAt,
)

fun User.toResponse() = UserResponse(
    id = id,
    email = email,
    username = username,
    role = role,
    status = status,
    avatarUrl = avatarUrl,
    createdAt = createdAt,
)

/**
 * Admin user response with quota information.
 */
@Serializable
data class AdminUserResponse(
    val id: String,
    val email: String,
    val username: String,
    val role: UserRole,
    val status: UserStatus,
    val avatarUrl: String?,
    val quotaBytes: Long?,
    val usedBytes: Long,
    val createdAt: Instant,
    val lastLoginAt: Instant?,
)

fun User.toAdminResponse(usedBytes: Long = 0L) = AdminUserResponse(
    id = id,
    email = email,
    username = username,
    role = role,
    status = status,
    avatarUrl = avatarUrl,
    quotaBytes = quotaBytes,
    usedBytes = usedBytes,
    createdAt = createdAt,
    lastLoginAt = lastLoginAt,
)

@Serializable
data class RegisterRequest(
    val email: String,
    val username: String,
    val password: String,
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
)

@Serializable
data class LoginResponse(
    val user: UserResponse,
    val token: String,
    val refreshToken: String,
    val expiresAt: Instant,
)

@Serializable
data class RefreshRequest(
    val refreshToken: String,
)

@Serializable
data class RefreshResponse(
    val user: UserResponse,
    val token: String,
    val refreshToken: String,
    val expiresAt: Instant,
)

@Serializable
data class UpdateProfileRequest(
    val username: String? = null,
    val avatarUrl: String? = null,
)

@Serializable
data class ChangePasswordRequest(
    val currentPassword: String,
    val newPassword: String,
)

// Share DTOs

@Serializable
data class ShareResponse(
    val id: String,
    val itemId: String,
    val token: String,
    val url: String,
    val expiresAt: Instant?,
    val hasPassword: Boolean,
    val maxDownloads: Int?,
    val downloadCount: Int,
    val isActive: Boolean,
    val createdAt: Instant,
    val createdBy: String,
    val sharedWithUsers: List<String> = emptyList(),
)

fun ShareLink.toResponse(baseUrl: String) = ShareResponse(
    id = id,
    itemId = itemId,
    token = token,
    url = "$baseUrl/share/$token",
    expiresAt = expiresAt,
    createdBy = createdBy,
    sharedWithUsers = sharedWithUsers,
    hasPassword = password != null,
    maxDownloads = maxDownloads,
    downloadCount = downloadCount,
    isActive = isActive,
    createdAt = createdAt,
)

@Serializable
data class CreateShareRequest(
    val itemId: String,
    val expirationDays: Int? = null,
    val password: String? = null,
    val maxDownloads: Int? = null,
)

@Serializable
data class AccessShareRequest(
    val password: String? = null,
)

// Quota DTOs

@Serializable
data class QuotaResponse(
    val usedBytes: Long,
    val quotaBytes: Long?,
    val usagePercentage: Double,
    val fileCount: Long,
    val folderCount: Long,
    val remainingBytes: Long?,
)

fun StorageQuota.toResponse() = QuotaResponse(
    usedBytes = usedBytes,
    quotaBytes = quotaBytes,
    usagePercentage = usagePercentage,
    fileCount = fileCount,
    folderCount = folderCount,
    remainingBytes = remainingBytes,
)

// Activity DTOs

@Serializable
data class ActivityResponse(
    val id: String,
    val type: ActivityType,
    val userId: String?,
    val itemId: String?,
    val itemPath: String?,
    val details: String?,
    val createdAt: Instant,
)

fun Activity.toResponse() = ActivityResponse(
    id = id,
    type = type,
    userId = userId,
    itemId = itemId,
    itemPath = itemPath,
    details = details,
    createdAt = createdAt,
)

// Search DTOs

@Serializable
data class SearchRequest(
    val query: String,
    val type: ItemType? = null,
    val mimeType: String? = null,
    val limit: Int = 50,
    val offset: Int = 0,
)

// Plugin DTOs

@Serializable
data class PluginInfoResponse(
    val id: String,
    val name: String,
    val version: String,
    val description: String,
    val author: String,
    val isEnabled: Boolean,
    val state: String,
)

@Serializable
data class PluginConfigResponse(
    val pluginId: String,
    val config: Map<
        String,
        @Serializable(with = AnySerializer::class)
        Any?,
        >,
)

// Serializer for Any type
object AnySerializer : KSerializer<Any?> {
    override val descriptor = kotlinx.serialization.descriptors.buildClassSerialDescriptor("Any")

    override fun serialize(encoder: Encoder, value: Any?) {
        val jsonEncoder = encoder as JsonEncoder
        val jsonElement = when (value) {
            null -> JsonNull
            is String -> JsonPrimitive(value)
            is Number -> JsonPrimitive(value)
            is Boolean -> JsonPrimitive(value)
            else -> JsonPrimitive(value.toString())
        }
        jsonEncoder.encodeJsonElement(jsonElement)
    }

    override fun deserialize(decoder: Decoder): Any? {
        val jsonDecoder = decoder as JsonDecoder
        val element = jsonDecoder.decodeJsonElement()
        return when (element) {
            is JsonNull -> null
            is JsonPrimitive -> {
                if (element.isString) {
                    element.content
                } else {
                    element.content.toIntOrNull()
                        ?: element.content.toLongOrNull()
                        ?: element.content.toDoubleOrNull()
                        ?: element.content.toBooleanStrictOrNull()
                        ?: element.content
                }
            }
            else -> element.toString()
        }
    }
}
