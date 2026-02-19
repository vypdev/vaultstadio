/**
 * Get Breadcrumbs Use Case
 *
 * Application use case for getting ancestor path (breadcrumbs) for an item.
 */

package com.vaultstadio.api.application.usecase.storage

import arrow.core.Either
import com.vaultstadio.core.domain.model.StorageItem
import com.vaultstadio.core.domain.service.StorageService
import com.vaultstadio.core.exception.StorageException

/**
 * Use case for getting breadcrumb path for an item.
 */
interface GetBreadcrumbsUseCase {

    suspend operator fun invoke(itemId: String, userId: String): Either<StorageException, List<StorageItem>>
}

/**
 * Default implementation delegating to [StorageService].
 */
class GetBreadcrumbsUseCaseImpl(
    private val storageService: StorageService,
) : GetBreadcrumbsUseCase {

    override suspend fun invoke(
        itemId: String,
        userId: String,
    ): Either<StorageException, List<StorageItem>> =
        storageService.getBreadcrumbs(itemId, userId)
}
