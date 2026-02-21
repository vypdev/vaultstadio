/**
 * VaultStadio Storage Item Repository Port
 */

package com.vaultstadio.domain.storage.repository

import arrow.core.Either
import com.vaultstadio.domain.common.exception.StorageException
import com.vaultstadio.domain.common.pagination.PagedResult
import com.vaultstadio.domain.common.pagination.SortOrder
import com.vaultstadio.domain.storage.model.ItemType
import com.vaultstadio.domain.storage.model.StorageItem
import com.vaultstadio.domain.storage.model.StorageQuota
import kotlinx.coroutines.flow.Flow

/**
 * Sorting options for storage items.
 */
enum class SortField {
    NAME,
    SIZE,
    CREATED_AT,
    UPDATED_AT,
    TYPE,
}

/**
 * Query parameters for listing storage items.
 */
data class StorageItemQuery(
    val parentId: String? = null,
    val filterByParent: Boolean = false,
    val ownerId: String? = null,
    val type: ItemType? = null,
    val mimeType: String? = null,
    val isTrashed: Boolean? = null,
    val isStarred: Boolean? = null,
    val searchQuery: String? = null,
    val sortField: SortField = SortField.NAME,
    val sortOrder: SortOrder = SortOrder.ASC,
    val offset: Int = 0,
    val limit: Int = 100,
)

/**
 * Repository interface for storage items.
 */
@Suppress("ComplexInterface")
interface StorageItemRepository {

    suspend fun create(item: StorageItem): Either<StorageException, StorageItem>

    suspend fun findById(id: String): Either<StorageException, StorageItem?>

    suspend fun findByPath(path: String, ownerId: String): Either<StorageException, StorageItem?>

    suspend fun update(item: StorageItem): Either<StorageException, StorageItem>

    suspend fun delete(id: String): Either<StorageException, Unit>

    suspend fun query(query: StorageItemQuery): Either<StorageException, PagedResult<StorageItem>>

    suspend fun listChildren(parentId: String?, ownerId: String): Either<StorageException, List<StorageItem>>

    suspend fun getAncestors(itemId: String): Either<StorageException, List<StorageItem>>

    suspend fun move(itemId: String, newParentId: String?, newName: String): Either<StorageException, StorageItem>

    suspend fun existsByPath(path: String, ownerId: String): Either<StorageException, Boolean>

    suspend fun getQuota(userId: String): Either<StorageException, StorageQuota>

    fun stream(query: StorageItemQuery): Flow<StorageItem>

    suspend fun count(query: StorageItemQuery): Either<StorageException, Long>

    suspend fun calculateTotalSize(ownerId: String): Either<StorageException, Long>
}
