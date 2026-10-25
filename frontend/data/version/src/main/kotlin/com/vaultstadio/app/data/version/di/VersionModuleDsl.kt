/**
 * Koin module for version (API, service, repository, use cases).
 * ComposeApp loads it via modules(... + versionModule).
 */

package com.vaultstadio.app.data.version.di

import com.vaultstadio.app.data.network.ApiClientConfig
import com.vaultstadio.app.data.network.TokenStorage
import com.vaultstadio.app.data.version.api.VersionApi
import com.vaultstadio.app.data.version.repository.VersionRepositoryImpl
import com.vaultstadio.app.data.version.service.VersionService
import com.vaultstadio.app.data.version.usecase.CleanupVersionsUseCaseImpl
import com.vaultstadio.app.data.version.usecase.CompareVersionsUseCaseImpl
import com.vaultstadio.app.data.version.usecase.DeleteVersionUseCaseImpl
import com.vaultstadio.app.data.version.usecase.GetVersionHistoryUseCaseImpl
import com.vaultstadio.app.data.version.usecase.GetVersionUseCaseImpl
import com.vaultstadio.app.data.version.usecase.RestoreVersionUseCaseImpl
import com.vaultstadio.app.domain.version.VersionRepository
import com.vaultstadio.app.domain.version.usecase.CleanupVersionsUseCase
import com.vaultstadio.app.domain.version.usecase.CompareVersionsUseCase
import com.vaultstadio.app.domain.version.usecase.DeleteVersionUseCase
import com.vaultstadio.app.domain.version.usecase.GetVersionHistoryUseCase
import com.vaultstadio.app.domain.version.usecase.GetVersionUseCase
import com.vaultstadio.app.domain.version.usecase.RestoreVersionUseCase
import io.ktor.client.HttpClient
import org.koin.dsl.module

val versionModule = module {
    single { VersionApi(get<HttpClient>()) }
    single { VersionService(get()) }
    single<VersionRepository> {
        VersionRepositoryImpl(get(), get<ApiClientConfig>(), get<TokenStorage>())
    }

    factory<GetVersionHistoryUseCase> { GetVersionHistoryUseCaseImpl(get()) }
    factory<GetVersionUseCase> { GetVersionUseCaseImpl(get()) }
    factory<RestoreVersionUseCase> { RestoreVersionUseCaseImpl(get()) }
    factory<CompareVersionsUseCase> { CompareVersionsUseCaseImpl(get()) }
    factory<DeleteVersionUseCase> { DeleteVersionUseCaseImpl(get()) }
    factory<CleanupVersionsUseCase> { CleanupVersionsUseCaseImpl(get()) }
}
