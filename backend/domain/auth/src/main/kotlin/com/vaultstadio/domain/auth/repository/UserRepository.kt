/**
 * VaultStadio User and Session Repository Ports
 */

package com.vaultstadio.domain.auth.repository

import arrow.core.Either
import com.vaultstadio.domain.auth.model.User
import com.vaultstadio.domain.auth.model.UserRole
import com.vaultstadio.domain.auth.model.UserStatus
import com.vaultstadio.domain.common.exception.StorageException
import com.vaultstadio.domain.common.pagination.PagedResult
import com.vaultstadio.domain.common.pagination.SortOrder

data class UserQuery(
    val role: UserRole? = null,
    val status: UserStatus? = null,
    val searchQuery: String? = null,
    val sortField: String = "createdAt",
    val sortOrder: SortOrder = SortOrder.DESC,
    val offset: Int = 0,
    val limit: Int = 50,
)

interface UserRepository {
    suspend fun create(user: User): Either<StorageException, User>
    suspend fun findById(id: String): Either<StorageException, User?>
    suspend fun findByEmail(email: String): Either<StorageException, User?>
    suspend fun findByUsername(username: String): Either<StorageException, User?>
    suspend fun update(user: User): Either<StorageException, User>
    suspend fun delete(id: String): Either<StorageException, Unit>
    suspend fun query(query: UserQuery): Either<StorageException, PagedResult<User>>
    suspend fun existsByEmail(email: String): Either<StorageException, Boolean>
    suspend fun existsByUsername(username: String): Either<StorageException, Boolean>
    suspend fun countAll(): Either<StorageException, Long>
    suspend fun updateLastLogin(userId: String): Either<StorageException, Unit>
}
