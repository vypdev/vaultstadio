/**
 * Koin module for sync (API, service, repository, use cases).
 * ComposeApp loads it via modules(... + syncModule).
 */

package com.vaultstadio.app.data.sync.di

import com.vaultstadio.app.data.sync.api.SyncApi
import com.vaultstadio.app.data.sync.repository.SyncRepositoryImpl
import com.vaultstadio.app.data.sync.service.SyncService
import com.vaultstadio.app.data.sync.usecase.DeactivateDeviceUseCaseImpl
import com.vaultstadio.app.data.sync.usecase.GetConflictsUseCaseImpl
import com.vaultstadio.app.data.sync.usecase.GetDevicesUseCaseImpl
import com.vaultstadio.app.data.sync.usecase.PullChangesUseCaseImpl
import com.vaultstadio.app.data.sync.usecase.RegisterDeviceUseCaseImpl
import com.vaultstadio.app.data.sync.usecase.RemoveDeviceUseCaseImpl
import com.vaultstadio.app.data.sync.usecase.ResolveConflictUseCaseImpl
import com.vaultstadio.app.domain.sync.SyncRepository
import com.vaultstadio.app.domain.sync.usecase.DeactivateDeviceUseCase
import com.vaultstadio.app.domain.sync.usecase.GetConflictsUseCase
import com.vaultstadio.app.domain.sync.usecase.GetDevicesUseCase
import com.vaultstadio.app.domain.sync.usecase.PullChangesUseCase
import com.vaultstadio.app.domain.sync.usecase.RegisterDeviceUseCase
import com.vaultstadio.app.domain.sync.usecase.RemoveDeviceUseCase
import com.vaultstadio.app.domain.sync.usecase.ResolveConflictUseCase
import io.ktor.client.HttpClient
import org.koin.dsl.module

val syncModule = module {
    single { SyncApi(get<HttpClient>()) }
    single { SyncService(get()) }
    single<SyncRepository> { SyncRepositoryImpl(get()) }

    factory<GetDevicesUseCase> { GetDevicesUseCaseImpl(get()) }
    factory<RegisterDeviceUseCase> { RegisterDeviceUseCaseImpl(get()) }
    factory<DeactivateDeviceUseCase> { DeactivateDeviceUseCaseImpl(get()) }
    factory<RemoveDeviceUseCase> { RemoveDeviceUseCaseImpl(get()) }
    factory<PullChangesUseCase> { PullChangesUseCaseImpl(get()) }
    factory<GetConflictsUseCase> { GetConflictsUseCaseImpl(get()) }
    factory<ResolveConflictUseCase> { ResolveConflictUseCaseImpl(get()) }
}
