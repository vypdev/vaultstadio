package com.vaultstadio.application.di

import com.vaultstadio.application.usecase.auth.LoginUseCase
import com.vaultstadio.application.usecase.auth.LoginUseCaseImpl
import com.vaultstadio.application.usecase.auth.LogoutUseCase
import com.vaultstadio.application.usecase.auth.LogoutUseCaseImpl
import com.vaultstadio.application.usecase.auth.RefreshSessionUseCase
import com.vaultstadio.application.usecase.auth.RefreshSessionUseCaseImpl
import com.vaultstadio.application.usecase.auth.RegisterUseCase
import com.vaultstadio.application.usecase.auth.RegisterUseCaseImpl
import org.koin.dsl.module

fun applicationAuthModule() = module {
    single<RegisterUseCase> { RegisterUseCaseImpl(get()) }
    single<LoginUseCase> { LoginUseCaseImpl(get()) }
    single<RefreshSessionUseCase> { RefreshSessionUseCaseImpl(get()) }
    single<LogoutUseCase> { LogoutUseCaseImpl(get()) }
}
