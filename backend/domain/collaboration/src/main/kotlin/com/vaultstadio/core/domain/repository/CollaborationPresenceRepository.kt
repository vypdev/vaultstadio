package com.vaultstadio.core.domain.repository

import arrow.core.Either
import com.vaultstadio.core.domain.model.UserPresence
import com.vaultstadio.domain.common.exception.StorageException
import kotlinx.coroutines.flow.Flow

interface CollaborationPresenceRepository {
    /**
     * Update user presence.
     *
     * @param presence Updated presence
     * @return The updated presence
     */
    suspend fun updatePresence(presence: UserPresence): Either<StorageException, UserPresence>

    /**
     * Get presence for a user.
     *
     * @param userId User ID
     * @return Current presence or null
     */
    suspend fun getPresence(userId: String): Either<StorageException, UserPresence?>

    /**
     * Get presence for multiple users.
     *
     * @param userIds List of user IDs
     * @return Map of user ID to presence
     */
    suspend fun getPresenceBulk(userIds: List<String>): Either<StorageException, Map<String, UserPresence>>

    /**
     * Set user as offline.
     *
     * @param userId User ID
     * @return Unit on success
     */
    suspend fun setOffline(userId: String): Either<StorageException, Unit>

    /**
     * Get online users in a folder.
     *
     * @param folderId Folder ID (null for root)
     * @return List of online users
     */
    suspend fun getOnlineUsersInFolder(folderId: String?): Either<StorageException, List<UserPresence>>

    /**
     * Stream presence updates.
     *
     * @param userIds User IDs to watch
     * @return Flow of presence updates
     */
    fun streamPresence(userIds: List<String>): Flow<UserPresence>
}
