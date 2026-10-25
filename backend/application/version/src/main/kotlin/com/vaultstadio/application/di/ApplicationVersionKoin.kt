package com.vaultstadio.application.di

import com.vaultstadio.application.usecase.version.ApplyRetentionPolicyUseCase
import com.vaultstadio.application.usecase.version.ApplyRetentionPolicyUseCaseImpl
import com.vaultstadio.application.usecase.version.CompareVersionsUseCase
import com.vaultstadio.application.usecase.version.CompareVersionsUseCaseImpl
import com.vaultstadio.application.usecase.version.DeleteVersionUseCase
import com.vaultstadio.application.usecase.version.DeleteVersionUseCaseImpl
import com.vaultstadio.application.usecase.version.GetVersionHistoryUseCase
import com.vaultstadio.application.usecase.version.GetVersionHistoryUseCaseImpl
import com.vaultstadio.application.usecase.version.GetVersionUseCase
import com.vaultstadio.application.usecase.version.GetVersionUseCaseImpl
import com.vaultstadio.application.usecase.version.RestoreVersionUseCase
import com.vaultstadio.application.usecase.version.RestoreVersionUseCaseImpl
import org.koin.dsl.module

fun applicationVersionModule() = module {
    single<GetVersionHistoryUseCase> { GetVersionHistoryUseCaseImpl(get()) }
    single<GetVersionUseCase> { GetVersionUseCaseImpl(get()) }
    single<RestoreVersionUseCase> { RestoreVersionUseCaseImpl(get()) }
    single<CompareVersionsUseCase> { CompareVersionsUseCaseImpl(get()) }
    single<DeleteVersionUseCase> { DeleteVersionUseCaseImpl(get()) }
    single<ApplyRetentionPolicyUseCase> { ApplyRetentionPolicyUseCaseImpl(get()) }
}
