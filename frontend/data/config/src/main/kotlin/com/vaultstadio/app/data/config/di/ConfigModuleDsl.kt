/**
 * Koin module for config (repository, use cases).
 * Uses classic DSL. ComposeApp loads it via modules(... + configModule).
 */

package com.vaultstadio.app.data.config.di

import com.vaultstadio.app.data.config.repository.ConfigRepositoryImpl
import com.vaultstadio.app.data.config.usecase.GetCollaborationUrlUseCaseImpl
import com.vaultstadio.app.data.config.usecase.GetShareUrlUseCaseImpl
import com.vaultstadio.app.data.config.usecase.GetStorageUrlsUseCaseImpl
import com.vaultstadio.app.data.config.usecase.GetVersionUrlsUseCaseImpl
import com.vaultstadio.app.domain.config.ConfigRepository
import com.vaultstadio.app.domain.config.usecase.GetCollaborationUrlUseCase
import com.vaultstadio.app.domain.config.usecase.GetShareUrlUseCase
import com.vaultstadio.app.domain.config.usecase.GetStorageUrlsUseCase
import com.vaultstadio.app.domain.config.usecase.GetVersionUrlsUseCase
import org.koin.dsl.module

val configModule = module {
    single<ConfigRepository> { ConfigRepositoryImpl(get()) }
    factory<GetStorageUrlsUseCase> { GetStorageUrlsUseCaseImpl(get()) }
    factory<GetShareUrlUseCase> { GetShareUrlUseCaseImpl(get()) }
    factory<GetCollaborationUrlUseCase> { GetCollaborationUrlUseCaseImpl(get()) }
    factory<GetVersionUrlsUseCase> { GetVersionUrlsUseCaseImpl(get()) }
}
