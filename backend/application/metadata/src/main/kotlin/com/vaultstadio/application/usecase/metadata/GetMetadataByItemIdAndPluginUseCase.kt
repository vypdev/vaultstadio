/**
 * Get Metadata By Item Id And Plugin Use Case
 *
 * Returns metadata for an item from a specific plugin (after access is verified elsewhere).
 */

package com.vaultstadio.application.usecase.metadata

import arrow.core.Either
import com.vaultstadio.core.domain.model.StorageItemMetadata
import com.vaultstadio.core.domain.repository.MetadataRepository
import com.vaultstadio.domain.common.exception.StorageException

interface GetMetadataByItemIdAndPluginUseCase {
    suspend operator fun invoke(
        itemId: String,
        pluginId: String,
    ): Either<StorageException, List<StorageItemMetadata>>
}

class GetMetadataByItemIdAndPluginUseCaseImpl(
    private val metadataRepository: MetadataRepository,
) : GetMetadataByItemIdAndPluginUseCase {

    override suspend fun invoke(
        itemId: String,
        pluginId: String,
    ): Either<StorageException, List<StorageItemMetadata>> =
        metadataRepository.findByItemIdAndPluginId(itemId, pluginId)
}
