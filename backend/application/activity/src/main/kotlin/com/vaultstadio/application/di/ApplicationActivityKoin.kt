package com.vaultstadio.application.di

import com.vaultstadio.application.usecase.activity.GetRecentActivityByItemUseCase
import com.vaultstadio.application.usecase.activity.GetRecentActivityByItemUseCaseImpl
import com.vaultstadio.application.usecase.activity.GetRecentActivityByUserUseCase
import com.vaultstadio.application.usecase.activity.GetRecentActivityByUserUseCaseImpl
import org.koin.dsl.module

fun applicationActivityModule() = module {
    single<GetRecentActivityByUserUseCase> { GetRecentActivityByUserUseCaseImpl(get()) }
    single<GetRecentActivityByItemUseCase> { GetRecentActivityByItemUseCaseImpl(get()) }
}
