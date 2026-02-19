/**
 * Get Item Metadata Use Case
 *
 * Verifies access via GetItemUseCase then returns metadata for the item.
 */

package com.vaultstadio.api.application.usecase.metadata

import arrow.core.Either
import arrow.core.flatMap
import com.vaultstadio.api.application.usecase.storage.GetItemUseCase
import com.vaultstadio.core.domain.model.StorageItem
import com.vaultstadio.core.domain.model.StorageItemMetadata
import com.vaultstadio.core.domain.repository.MetadataRepository
import com.vaultstadio.core.exception.StorageException

/**
 * Returns the storage item and its metadata after verifying user access.
 */
interface GetItemMetadataUseCase {
    suspend operator fun invoke(
        itemId: String,
        userId: String,
    ): Either<StorageException, Pair<StorageItem, List<StorageItemMetadata>>>
}

class GetItemMetadataUseCaseImpl(
    private val getItemUseCase: GetItemUseCase,
    private val metadataRepository: MetadataRepository,
) : GetItemMetadataUseCase {

    override suspend fun invoke(
        itemId: String,
        userId: String,
    ): Either<StorageException, Pair<StorageItem, List<StorageItemMetadata>>> =
        getItemUseCase(itemId, userId).flatMap { item ->
            metadataRepository.findByItemId(itemId).map { metadataList -> item to metadataList }
        }
}
