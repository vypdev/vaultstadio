package com.vaultstadio.application.di

import com.vaultstadio.application.usecase.sync.DeactivateDeviceUseCase
import com.vaultstadio.application.usecase.sync.DeactivateDeviceUseCaseImpl
import com.vaultstadio.application.usecase.sync.GenerateFileSignatureUseCase
import com.vaultstadio.application.usecase.sync.GenerateFileSignatureUseCaseImpl
import com.vaultstadio.application.usecase.sync.GetPendingConflictsUseCase
import com.vaultstadio.application.usecase.sync.GetPendingConflictsUseCaseImpl
import com.vaultstadio.application.usecase.sync.ListDevicesUseCase
import com.vaultstadio.application.usecase.sync.ListDevicesUseCaseImpl
import com.vaultstadio.application.usecase.sync.RecordChangeUseCase
import com.vaultstadio.application.usecase.sync.RecordChangeUseCaseImpl
import com.vaultstadio.application.usecase.sync.RegisterDeviceUseCase
import com.vaultstadio.application.usecase.sync.RegisterDeviceUseCaseImpl
import com.vaultstadio.application.usecase.sync.RemoveDeviceUseCase
import com.vaultstadio.application.usecase.sync.RemoveDeviceUseCaseImpl
import com.vaultstadio.application.usecase.sync.ResolveConflictUseCase
import com.vaultstadio.application.usecase.sync.ResolveConflictUseCaseImpl
import com.vaultstadio.application.usecase.sync.SyncPullUseCase
import com.vaultstadio.application.usecase.sync.SyncPullUseCaseImpl
import org.koin.dsl.module

fun applicationSyncModule() = module {
    single<RegisterDeviceUseCase> { RegisterDeviceUseCaseImpl(get()) }
    single<ListDevicesUseCase> { ListDevicesUseCaseImpl(get()) }
    single<DeactivateDeviceUseCase> { DeactivateDeviceUseCaseImpl(get()) }
    single<RemoveDeviceUseCase> { RemoveDeviceUseCaseImpl(get()) }
    single<SyncPullUseCase> { SyncPullUseCaseImpl(get()) }
    single<RecordChangeUseCase> { RecordChangeUseCaseImpl(get()) }
    single<GetPendingConflictsUseCase> { GetPendingConflictsUseCaseImpl(get()) }
    single<ResolveConflictUseCase> { ResolveConflictUseCaseImpl(get()) }
    single<GenerateFileSignatureUseCase> { GenerateFileSignatureUseCaseImpl(get()) }
}
