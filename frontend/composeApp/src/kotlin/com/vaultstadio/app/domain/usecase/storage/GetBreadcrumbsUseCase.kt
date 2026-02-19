/**
 * Get Breadcrumbs Use Case
 */

package com.vaultstadio.app.domain.usecase.storage

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.data.repository.StorageRepository
import com.vaultstadio.app.domain.model.Breadcrumb
import org.koin.core.annotation.Factory

/**
 * Use case for getting breadcrumbs for navigation.
 */
interface GetBreadcrumbsUseCase {
    suspend operator fun invoke(itemId: String): Result<List<Breadcrumb>>
}

@Factory(binds = [GetBreadcrumbsUseCase::class])
class GetBreadcrumbsUseCaseImpl(
    private val storageRepository: StorageRepository,
) : GetBreadcrumbsUseCase {

    override suspend operator fun invoke(itemId: String): Result<List<Breadcrumb>> =
        storageRepository.getBreadcrumbs(itemId)
}
