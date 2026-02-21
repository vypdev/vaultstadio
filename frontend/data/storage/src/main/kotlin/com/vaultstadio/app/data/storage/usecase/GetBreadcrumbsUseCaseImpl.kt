package com.vaultstadio.app.data.storage.usecase

import com.vaultstadio.app.domain.storage.StorageRepository
import com.vaultstadio.app.domain.storage.usecase.GetBreadcrumbsUseCase
import org.koin.core.annotation.Factory

@Factory(binds = [GetBreadcrumbsUseCase::class])
class GetBreadcrumbsUseCaseImpl(
    private val storageRepository: StorageRepository,
) : GetBreadcrumbsUseCase {

    override suspend fun invoke(itemId: String) = storageRepository.getBreadcrumbs(itemId)
}
