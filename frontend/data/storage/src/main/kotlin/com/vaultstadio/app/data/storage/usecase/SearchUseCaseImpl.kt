package com.vaultstadio.app.data.storage.usecase

import com.vaultstadio.app.domain.storage.StorageRepository
import com.vaultstadio.app.domain.storage.usecase.SearchUseCase
import org.koin.core.annotation.Factory

@Factory(binds = [SearchUseCase::class])
class SearchUseCaseImpl(
    private val storageRepository: StorageRepository,
) : SearchUseCase {

    override suspend fun invoke(query: String, limit: Int, offset: Int) =
        storageRepository.search(query, limit, offset)
}
