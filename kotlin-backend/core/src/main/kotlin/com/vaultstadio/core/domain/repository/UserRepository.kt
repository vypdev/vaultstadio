/**
 * VaultStadio User Repository
 *
 * Interface for user persistence operations.
 */

package com.vaultstadio.core.domain.repository

import arrow.core.Either
import com.vaultstadio.core.domain.model.ApiKey
import com.vaultstadio.core.domain.model.User
import com.vaultstadio.core.domain.model.UserRole
import com.vaultstadio.core.domain.model.UserSession
import com.vaultstadio.core.domain.model.UserStatus
import com.vaultstadio.core.exception.StorageException

/**
 * Query parameters for listing users.
 */
data class UserQuery(
    val role: UserRole? = null,
    val status: UserStatus? = null,
    val searchQuery: String? = null,
    val sortField: String = "createdAt",
    val sortOrder: SortOrder = SortOrder.DESC,
    val offset: Int = 0,
    val limit: Int = 50,
)

/**
 * Repository interface for users.
 */
interface UserRepository {

    /**
     * Creates a new user.
     *
     * @param user The user to create
     * @return Either an error or the created user
     */
    suspend fun create(user: User): Either<StorageException, User>

    /**
     * Finds a user by ID.
     *
     * @param id User ID
     * @return Either an error or the user (null if not found)
     */
    suspend fun findById(id: String): Either<StorageException, User?>

    /**
     * Finds a user by email.
     *
     * @param email User email
     * @return Either an error or the user (null if not found)
     */
    suspend fun findByEmail(email: String): Either<StorageException, User?>

    /**
     * Finds a user by username.
     *
     * @param username Username
     * @return Either an error or the user (null if not found)
     */
    suspend fun findByUsername(username: String): Either<StorageException, User?>

    /**
     * Updates a user.
     *
     * @param user The user to update
     * @return Either an error or the updated user
     */
    suspend fun update(user: User): Either<StorageException, User>

    /**
     * Deletes a user permanently.
     *
     * @param id User ID
     * @return Either an error or Unit on success
     */
    suspend fun delete(id: String): Either<StorageException, Unit>

    /**
     * Queries users with pagination.
     *
     * @param query Query parameters
     * @return Either an error or paginated results
     */
    suspend fun query(query: UserQuery): Either<StorageException, PagedResult<User>>

    /**
     * Checks if an email is already registered.
     *
     * @param email Email to check
     * @return Either an error or boolean
     */
    suspend fun existsByEmail(email: String): Either<StorageException, Boolean>

    /**
     * Checks if a username is already taken.
     *
     * @param username Username to check
     * @return Either an error or boolean
     */
    suspend fun existsByUsername(username: String): Either<StorageException, Boolean>

    /**
     * Counts total users.
     *
     * @return Either an error or the count
     */
    suspend fun countAll(): Either<StorageException, Long>

    /**
     * Updates the last login timestamp.
     *
     * @param userId User ID
     * @return Either an error or Unit on success
     */
    suspend fun updateLastLogin(userId: String): Either<StorageException, Unit>
}

/**
 * Repository interface for API keys.
 */
interface ApiKeyRepository {

    suspend fun create(apiKey: ApiKey): Either<StorageException, ApiKey>

    suspend fun findById(id: String): Either<StorageException, ApiKey?>

    suspend fun findByKeyHash(keyHash: String): Either<StorageException, ApiKey?>

    suspend fun findByUserId(userId: String): Either<StorageException, List<ApiKey>>

    suspend fun update(apiKey: ApiKey): Either<StorageException, ApiKey>

    suspend fun delete(id: String): Either<StorageException, Unit>

    suspend fun deleteByUserId(userId: String): Either<StorageException, Unit>
}

/**
 * Repository interface for user sessions.
 */
interface SessionRepository {

    suspend fun create(session: UserSession): Either<StorageException, UserSession>

    suspend fun findById(id: String): Either<StorageException, UserSession?>

    suspend fun findByTokenHash(tokenHash: String): Either<StorageException, UserSession?>

    suspend fun findByRefreshTokenHash(refreshTokenHash: String): Either<StorageException, UserSession?>

    suspend fun findByUserId(userId: String): Either<StorageException, List<UserSession>>

    suspend fun update(session: UserSession): Either<StorageException, UserSession>

    suspend fun delete(id: String): Either<StorageException, Unit>

    suspend fun deleteByUserId(userId: String): Either<StorageException, Unit>

    suspend fun deleteExpired(): Either<StorageException, Int>

    suspend fun updateLastActivity(id: String): Either<StorageException, Unit>
}
