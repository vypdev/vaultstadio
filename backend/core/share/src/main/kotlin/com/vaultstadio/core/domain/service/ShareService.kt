/**
 * VaultStadio Share Service
 *
 * Service for managing file sharing and share links.
 */

package com.vaultstadio.core.domain.service

import arrow.core.Either
import arrow.core.left
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.right
import com.vaultstadio.core.domain.event.EventBus
import com.vaultstadio.core.domain.event.ShareEvent
import com.vaultstadio.domain.common.exception.AuthorizationException
import com.vaultstadio.domain.common.exception.ItemNotFoundException
import com.vaultstadio.domain.common.exception.StorageException
import com.vaultstadio.domain.common.exception.ValidationException
import com.vaultstadio.domain.share.access.PublicSharePolicy
import com.vaultstadio.domain.share.model.ShareLink
import com.vaultstadio.domain.share.repository.ShareRepository
import com.vaultstadio.domain.storage.repository.StorageItemRepository
import kotlinx.datetime.Clock
import java.security.SecureRandom
import java.util.Base64
import java.util.UUID
import kotlin.time.Duration.Companion.days

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
    suspend fun createShare(input: CreateShareInput): Either<StorageException, ShareLink> = either {
        ensure(PublicSharePolicy.validCreation(input.expirationDays, input.maxDownloads, input.password)) {
            ValidationException(message = "Invalid share expiry, download limit or password length")
        }
        val item = storageItemRepository.findById(input.itemId).bind()
            ?: raise(ItemNotFoundException(message = "Item not found"))
        ensure(item.ownerId == input.userId) { AuthorizationException(message = "Only the owner can share this item") }
        val now = Clock.System.now()
        val share = ShareLink(
            id = UUID.randomUUID().toString(),
            itemId = item.id,
            token = generateShareToken(),
            createdBy = input.userId,
            createdAt = now,
            expiresAt = input.expirationDays?.let { now + it.days },
            maxDownloads = input.maxDownloads,
        )
        ensure(PublicSharePolicy.itemFailure(share, item) == null) {
            ValidationException(message = "Only an active stored file can be shared")
        }
        val created = shareRepository.create(
            share.copy(
                password = input.password?.let {
                    passwordHasher.hash(it)
                },
            ),
        ).bind()
        eventBus.publish(ShareEvent.Created(userId = input.userId, share = created, item = item))
        created
    }

    /**
     * Gets shares created by a user.
     */
    suspend fun getSharesByUser(userId: String, activeOnly: Boolean = true): Either<StorageException, List<ShareLink>> =
        shareRepository.findByCreatedBy(userId, activeOnly)

    /**
     * Gets shares that have been shared with a specific user.
     */
    suspend fun getSharesSharedWithUser(
        userId: String,
        activeOnly: Boolean = true,
    ): Either<StorageException, List<ShareLink>> = shareRepository.findSharedWithUser(userId, activeOnly)

    /**
     * Gets shares for an item.
     */
    suspend fun getSharesByItem(itemId: String, userId: String): Either<StorageException, List<ShareLink>> {
        // Verify user owns the item
        val item = when (val result = storageItemRepository.findById(itemId)) {
            is Either.Left -> {
                return result
            }
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
            is Either.Left -> {
                result
            }
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
            is Either.Left -> {
                return result
            }
            is Either.Right -> {
                result.value
            }
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
            is Either.Left -> {
                return result
            }
            is Either.Right -> {
                result.value
            }
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
