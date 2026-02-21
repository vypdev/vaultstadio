package com.vaultstadio.application.di

import com.vaultstadio.application.usecase.health.GetDetailedHealthUseCase
import com.vaultstadio.application.usecase.health.GetDetailedHealthUseCaseImpl
import com.vaultstadio.application.usecase.health.GetReadinessUseCase
import com.vaultstadio.application.usecase.health.GetReadinessUseCaseImpl
import org.koin.dsl.module

fun applicationHealthModule() = module {
    single<GetReadinessUseCase> { GetReadinessUseCaseImpl(get(), get()) }
    single<GetDetailedHealthUseCase> { GetDetailedHealthUseCaseImpl(get()) }
}
