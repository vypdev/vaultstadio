/**
 * Koin module for metadata (API, service, repository, use cases).
 * ComposeApp loads it via modules(... + metadataModule).
 */

package com.vaultstadio.app.data.metadata.di

import com.vaultstadio.app.data.metadata.api.MetadataApi
import com.vaultstadio.app.data.metadata.repository.MetadataRepositoryImpl
import com.vaultstadio.app.data.metadata.service.MetadataService
import com.vaultstadio.app.data.metadata.usecase.AdvancedSearchUseCaseImpl
import com.vaultstadio.app.data.metadata.usecase.GetDocumentMetadataUseCaseImpl
import com.vaultstadio.app.data.metadata.usecase.GetFileMetadataUseCaseImpl
import com.vaultstadio.app.data.metadata.usecase.GetImageMetadataUseCaseImpl
import com.vaultstadio.app.data.metadata.usecase.GetSearchSuggestionsUseCaseImpl
import com.vaultstadio.app.data.metadata.usecase.GetVideoMetadataUseCaseImpl
import com.vaultstadio.app.data.metadata.usecase.SearchByMetadataUseCaseImpl
import com.vaultstadio.app.domain.metadata.MetadataRepository
import com.vaultstadio.app.domain.metadata.usecase.AdvancedSearchUseCase
import com.vaultstadio.app.domain.metadata.usecase.GetDocumentMetadataUseCase
import com.vaultstadio.app.domain.metadata.usecase.GetFileMetadataUseCase
import com.vaultstadio.app.domain.metadata.usecase.GetImageMetadataUseCase
import com.vaultstadio.app.domain.metadata.usecase.GetSearchSuggestionsUseCase
import com.vaultstadio.app.domain.metadata.usecase.GetVideoMetadataUseCase
import com.vaultstadio.app.domain.metadata.usecase.SearchByMetadataUseCase
import io.ktor.client.HttpClient
import org.koin.dsl.module

val metadataModule = module {
    single { MetadataApi(get<HttpClient>()) }
    single { MetadataService(get()) }
    single<MetadataRepository> { MetadataRepositoryImpl(get()) }

    factory<GetFileMetadataUseCase> { GetFileMetadataUseCaseImpl(get()) }
    factory<GetImageMetadataUseCase> { GetImageMetadataUseCaseImpl(get()) }
    factory<GetVideoMetadataUseCase> { GetVideoMetadataUseCaseImpl(get()) }
    factory<GetDocumentMetadataUseCase> { GetDocumentMetadataUseCaseImpl(get()) }
    factory<AdvancedSearchUseCase> { AdvancedSearchUseCaseImpl(get()) }
    factory<SearchByMetadataUseCase> { SearchByMetadataUseCaseImpl(get()) }
    factory<GetSearchSuggestionsUseCase> { GetSearchSuggestionsUseCaseImpl(get()) }
}
