/**
 * Koin module for auth (API, service, repository, use cases).
 * Uses classic DSL to avoid Koin compiler codegen issues on Kotlin/WASM.
 * ComposeApp loads it via modules(runtimeModules(url) + authModule) in Main.kt.
 */

package com.vaultstadio.app.data.auth.di

import com.vaultstadio.app.data.auth.api.AuthApi
import com.vaultstadio.app.data.auth.repository.AuthRepositoryImpl
import com.vaultstadio.app.data.auth.service.AuthService
import com.vaultstadio.app.data.auth.usecase.ChangePasswordUseCaseImpl
import com.vaultstadio.app.data.auth.usecase.GetActiveSessionsUseCaseImpl
import com.vaultstadio.app.data.auth.usecase.GetCurrentUserUseCaseImpl
import com.vaultstadio.app.data.auth.usecase.GetLoginHistoryUseCaseImpl
import com.vaultstadio.app.data.auth.usecase.GetQuotaUseCaseImpl
import com.vaultstadio.app.data.auth.usecase.GetSecuritySettingsUseCaseImpl
import com.vaultstadio.app.data.auth.usecase.LoginUseCaseImpl
import com.vaultstadio.app.data.auth.usecase.LogoutUseCaseImpl
import com.vaultstadio.app.data.auth.usecase.RegisterUseCaseImpl
import com.vaultstadio.app.data.auth.usecase.RevokeSessionUseCaseImpl
import com.vaultstadio.app.data.auth.usecase.UpdateProfileUseCaseImpl
import com.vaultstadio.app.domain.auth.AuthRepository
import com.vaultstadio.app.domain.auth.usecase.ChangePasswordUseCase
import com.vaultstadio.app.domain.auth.usecase.GetActiveSessionsUseCase
import com.vaultstadio.app.domain.auth.usecase.GetCurrentUserUseCase
import com.vaultstadio.app.domain.auth.usecase.GetLoginHistoryUseCase
import com.vaultstadio.app.domain.auth.usecase.GetQuotaUseCase
import com.vaultstadio.app.domain.auth.usecase.GetSecuritySettingsUseCase
import com.vaultstadio.app.domain.auth.usecase.LoginUseCase
import com.vaultstadio.app.domain.auth.usecase.LogoutUseCase
import com.vaultstadio.app.domain.auth.usecase.RegisterUseCase
import com.vaultstadio.app.domain.auth.usecase.RevokeSessionUseCase
import com.vaultstadio.app.domain.auth.usecase.UpdateProfileUseCase
import io.ktor.client.HttpClient
import org.koin.dsl.module

val authModule = module {
    single { AuthApi(get<HttpClient>()) }
    single { AuthService(get()) }
    single<AuthRepository> { AuthRepositoryImpl(get(), get()) }

    factory<LoginUseCase> { LoginUseCaseImpl(get()) }
    factory<RegisterUseCase> { RegisterUseCaseImpl(get()) }
    factory<LogoutUseCase> { LogoutUseCaseImpl(get()) }
    factory<GetCurrentUserUseCase> { GetCurrentUserUseCaseImpl(get()) }
    factory<GetQuotaUseCase> { GetQuotaUseCaseImpl(get()) }
    factory<UpdateProfileUseCase> { UpdateProfileUseCaseImpl(get()) }
    factory<ChangePasswordUseCase> { ChangePasswordUseCaseImpl(get()) }
    factory<GetActiveSessionsUseCase> { GetActiveSessionsUseCaseImpl() }
    factory<RevokeSessionUseCase> { RevokeSessionUseCaseImpl() }
    factory<GetSecuritySettingsUseCase> { GetSecuritySettingsUseCaseImpl() }
    factory<GetLoginHistoryUseCase> { GetLoginHistoryUseCaseImpl() }
}
