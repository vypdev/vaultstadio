/**
 * VaultStadio Share Service
 *
 * Service for managing file sharing and share links.
 */

package com.vaultstadio.core.domain.service

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.vaultstadio.core.domain.event.EventBus
import com.vaultstadio.core.domain.event.ShareEvent
import com.vaultstadio.domain.common.exception.AuthorizationException
import com.vaultstadio.domain.common.exception.ItemNotFoundException
import com.vaultstadio.domain.common.exception.StorageException
import com.vaultstadio.domain.share.model.ShareLink
import com.vaultstadio.domain.share.repository.ShareRepository
import com.vaultstadio.domain.storage.model.StorageItem
import com.vaultstadio.domain.storage.repository.StorageItemRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.datetime.Clock
import java.security.SecureRandom
import java.util.Base64
import java.util.UUID
import kotlin.time.Duration.Companion.days

private val logger = KotlinLogging.logger {}

/**
 * Input for creating a share link.
 */
data class CreateShareInput(
    val itemId: String,
    val userId: String,
    val expirationDays: Int? = 7,
    val password: String? = null,
    val maxDownloads: Int? = null,
)

/**
 * Input for accessing a share.
 */
data class AccessShareInput(
    val token: String,
    val password: String? = null,
    val ipAddress: String? = null,
    val userAgent: String? = null,
)

/**
 * Service for managing share links.
 */
class ShareService(
    private val shareRepository: ShareRepository,
    private val storageItemRepository: StorageItemRepository,
    private val passwordHasher: PasswordHasher,
    private val eventBus: EventBus,
) {

    private val secureRandom = SecureRandom()

    /**
     * Creates a new share link for an item.
     */
    suspend fun createShare(input: CreateShareInput): Either<StorageException, ShareLink> {
        logger.info { "Creating share for item ${input.itemId}" }

        // Verify item exists and user owns it
        val item = when (val result = storageItemRepository.findById(input.itemId)) {
            is Either.Left -> return result
            is Either.Right -> {
                val found = result.value
                    ?: return ItemNotFoundException("Item not found").left()
                found
            }
        }

        if (item.ownerId != input.userId) {
            return AuthorizationException("Only the owner can share this item").left()
        }

        val now = Clock.System.now()
        val token = generateShareToken()
        val hashedPassword = input.password?.let { passwordHasher.hash(it) }
        val expiresAt = input.expirationDays?.let { now + it.days }

        val share = ShareLink(
            id = UUID.randomUUID().toString(),
            itemId = input.itemId,
            token = token,
            createdBy = input.userId,
            expiresAt = expiresAt,
            password = hashedPassword,
            maxDownloads = input.maxDownloads,
            downloadCount = 0,
            isActive = true,
            createdAt = now,
        )

        return shareRepository.create(share).also { result ->
            if (result.isRight()) {
                val created = (result as Either.Right).value
                eventBus.publish(ShareEvent.Created(userId = input.userId, share = created, item = item))
            }
        }
    }

    /**
     * Accesses a shared item by token.
     */
    suspend fun accessShare(input: AccessShareInput): Either<StorageException, Pair<ShareLink, StorageItem>> {
        // Find share by token
        val share = when (val result = shareRepository.findByToken(input.token)) {
            is Either.Left -> return result
            is Either.Right -> {
                val found = result.value
                    ?: return ItemNotFoundException("Share not found").left()
                found
            }
        }

        // Check if share is active
        if (!share.isActive) {
            return AuthorizationException("Share is no longer active").left()
        }

        // Check expiration
        val expiresAt = share.expiresAt
        if (expiresAt != null && expiresAt < Clock.System.now()) {
            return AuthorizationException("Share has expired").left()
        }

        // Check download limit
        val maxDownloads = share.maxDownloads
        if (maxDownloads != null && share.downloadCount >= maxDownloads) {
            return AuthorizationException("Download limit reached").left()
        }

        // Check password
        val sharePassword = share.password
        if (sharePassword != null) {
            if (input.password == null) {
                return AuthorizationException("Password required").left()
            }
            if (!passwordHasher.verify(input.password, sharePassword)) {
                return AuthorizationException("Invalid password").left()
            }
        }

        // Get the item
        val item = when (val result = storageItemRepository.findById(share.itemId)) {
            is Either.Left -> return result
            is Either.Right -> {
                val found = result.value
                    ?: return ItemNotFoundException("Shared item not found").left()
                found
            }
        }

        // Increment download count
        shareRepository.incrementDownloadCount(share.id)

        return Pair(share, item).right()
    }

    /**
     * Gets shares created by a user.
     */
    suspend fun getSharesByUser(userId: String, activeOnly: Boolean = true): Either<StorageException, List<ShareLink>> {
        return shareRepository.findByCreatedBy(userId, activeOnly)
    }

    /**
     * Gets shares that have been shared with a specific user.
     */
    suspend fun getSharesSharedWithUser(
        userId: String,
        activeOnly: Boolean = true,
    ): Either<StorageException, List<ShareLink>> {
        return shareRepository.findSharedWithUser(userId, activeOnly)
    }

    /**
     * Gets shares for an item.
     */
    suspend fun getSharesByItem(itemId: String, userId: String): Either<StorageException, List<ShareLink>> {
        // Verify user owns the item
        val item = when (val result = storageItemRepository.findById(itemId)) {
            is Either.Left -> return result
            is Either.Right -> {
                val found = result.value
                    ?: return ItemNotFoundException("Item not found").left()
                found
            }
        }

        if (item.ownerId != userId) {
            return AuthorizationException("Access denied").left()
        }

        return shareRepository.findByItemId(itemId)
    }

    /**
     * Gets a share by ID.
     */
    suspend fun getShare(shareId: String): Either<StorageException, ShareLink> {
        return when (val result = shareRepository.findById(shareId)) {
            is Either.Left -> result
            is Either.Right -> {
                val share = result.value
                if (share == null) {
                    ItemNotFoundException("Share not found").left()
                } else {
                    share.right()
                }
            }
        }
    }

    /**
     * Deletes a share.
     */
    suspend fun deleteShare(shareId: String, userId: String): Either<StorageException, Unit> {
        val share = when (val result = getShare(shareId)) {
            is Either.Left -> return result
            is Either.Right -> result.value
        }

        if (share.createdBy != userId) {
            return AuthorizationException("Only the creator can delete this share").left()
        }

        return shareRepository.delete(shareId).also { result ->
            if (result.isRight()) {
                eventBus.publish(ShareEvent.Deleted(userId = userId, shareId = share.id, itemId = share.itemId))
            }
        }
    }

    /**
     * Deactivates a share.
     */
    suspend fun deactivateShare(shareId: String, userId: String): Either<StorageException, ShareLink> {
        val share = when (val result = getShare(shareId)) {
            is Either.Left -> return result
            is Either.Right -> result.value
        }

        if (share.createdBy != userId) {
            return AuthorizationException("Only the creator can deactivate this share").left()
        }

        val updatedShare = share.copy(isActive = false)
        return shareRepository.update(updatedShare)
    }

    private fun generateShareToken(): String {
        val bytes = ByteArray(24)
        secureRandom.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }
}
