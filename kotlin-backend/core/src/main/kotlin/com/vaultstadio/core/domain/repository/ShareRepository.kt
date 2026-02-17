/**
 * VaultStadio Share Repository
 *
 * Interface for share link and activity persistence.
 */

package com.vaultstadio.core.domain.repository

import arrow.core.Either
import com.vaultstadio.core.domain.model.Activity
import com.vaultstadio.core.domain.model.ActivityType
import com.vaultstadio.core.domain.model.ShareLink
import com.vaultstadio.core.domain.model.StorageStatistics
import com.vaultstadio.core.exception.StorageException
import kotlinx.datetime.Instant

/**
 * Repository interface for share links.
 */
interface ShareRepository {

    /**
     * Creates a new share link.
     *
     * @param share The share link to create
     * @return Either an error or the created share
     */
    suspend fun create(share: ShareLink): Either<StorageException, ShareLink>

    /**
     * Finds a share link by ID.
     *
     * @param id Share ID
     * @return Either an error or the share (null if not found)
     */
    suspend fun findById(id: String): Either<StorageException, ShareLink?>

    /**
     * Finds a share link by token.
     *
     * @param token Share token
     * @return Either an error or the share (null if not found)
     */
    suspend fun findByToken(token: String): Either<StorageException, ShareLink?>

    /**
     * Finds all share links for a storage item.
     *
     * @param itemId Storage item ID
     * @return Either an error or list of shares
     */
    suspend fun findByItemId(itemId: String): Either<StorageException, List<ShareLink>>

    /**
     * Finds all share links created by a user.
     *
     * @param userId User ID
     * @param activeOnly Only return active shares
     * @return Either an error or list of shares
     */
    suspend fun findByCreatedBy(
        userId: String,
        activeOnly: Boolean = true,
    ): Either<StorageException, List<ShareLink>>

    /**
     * Finds all share links shared with a specific user.
     *
     * @param userId User ID to find shares for
     * @param activeOnly Only return active shares
     * @return Either an error or list of shares
     */
    suspend fun findSharedWithUser(
        userId: String,
        activeOnly: Boolean = true,
    ): Either<StorageException, List<ShareLink>>

    /**
     * Updates a share link.
     *
     * @param share The share to update
     * @return Either an error or the updated share
     */
    suspend fun update(share: ShareLink): Either<StorageException, ShareLink>

    /**
     * Increments the download count for a share.
     *
     * @param id Share ID
     * @return Either an error or the new download count
     */
    suspend fun incrementDownloadCount(id: String): Either<StorageException, Int>

    /**
     * Deletes a share link.
     *
     * @param id Share ID
     * @return Either an error or Unit on success
     */
    suspend fun delete(id: String): Either<StorageException, Unit>

    /**
     * Deletes all share links for a storage item.
     *
     * @param itemId Storage item ID
     * @return Either an error or number of deleted shares
     */
    suspend fun deleteByItemId(itemId: String): Either<StorageException, Int>

    /**
     * Deactivates expired shares.
     *
     * @param before Deactivate shares expired before this time
     * @return Either an error or number of deactivated shares
     */
    suspend fun deactivateExpired(before: Instant): Either<StorageException, Int>
}

/**
 * Query parameters for activity logs.
 */
data class ActivityQuery(
    val userId: String? = null,
    val itemId: String? = null,
    val types: List<ActivityType>? = null,
    val startTime: Instant? = null,
    val endTime: Instant? = null,
    val sortOrder: SortOrder = SortOrder.DESC,
    val offset: Int = 0,
    val limit: Int = 100,
)

/**
 * Repository interface for activity logs.
 */
interface ActivityRepository {

    /**
     * Creates a new activity log entry.
     *
     * @param activity The activity to log
     * @return Either an error or the created activity
     */
    suspend fun create(activity: Activity): Either<StorageException, Activity>

    /**
     * Queries activity logs.
     *
     * @param query Query parameters
     * @return Either an error or paginated results
     */
    suspend fun query(query: ActivityQuery): Either<StorageException, PagedResult<Activity>>

    /**
     * Gets recent activities for a user.
     *
     * @param userId User ID
     * @param limit Maximum number of activities
     * @return Either an error or list of activities
     */
    suspend fun getRecentByUser(userId: String, limit: Int = 20): Either<StorageException, List<Activity>>

    /**
     * Gets recent activities for an item.
     *
     * @param itemId Storage item ID
     * @param limit Maximum number of activities
     * @return Either an error or list of activities
     */
    suspend fun getRecentByItem(itemId: String, limit: Int = 20): Either<StorageException, List<Activity>>

    /**
     * Deletes activities older than a specified time.
     *
     * @param before Delete activities before this time
     * @return Either an error or number of deleted activities
     */
    suspend fun deleteOlderThan(before: Instant): Either<StorageException, Int>

    /**
     * Gets storage statistics.
     *
     * @return Either an error or storage statistics
     */
    suspend fun getStatistics(): Either<StorageException, StorageStatistics>
}
