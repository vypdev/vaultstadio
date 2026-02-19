/**
 * Koin module for auth (API, service, repository, use cases).
 * Kept in composeApp until :data:auth build cycle is resolved.
 */

package com.vaultstadio.app.di

import com.vaultstadio.app.data.api.AuthApi
import com.vaultstadio.app.data.network.TokenStorage
import com.vaultstadio.app.data.repository.AuthRepositoryImpl
import com.vaultstadio.app.data.service.AuthService
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
import com.vaultstadio.app.domain.usecase.auth.ChangePasswordUseCaseImpl
import com.vaultstadio.app.domain.usecase.auth.GetActiveSessionsUseCaseImpl
import com.vaultstadio.app.domain.usecase.auth.GetCurrentUserUseCaseImpl
import com.vaultstadio.app.domain.usecase.auth.GetLoginHistoryUseCaseImpl
import com.vaultstadio.app.domain.usecase.auth.GetQuotaUseCaseImpl
import com.vaultstadio.app.domain.usecase.auth.GetSecuritySettingsUseCaseImpl
import com.vaultstadio.app.domain.usecase.auth.LoginUseCaseImpl
import com.vaultstadio.app.domain.usecase.auth.LogoutUseCaseImpl
import com.vaultstadio.app.domain.usecase.auth.RegisterUseCaseImpl
import com.vaultstadio.app.domain.usecase.auth.RevokeSessionUseCaseImpl
import com.vaultstadio.app.domain.usecase.auth.UpdateProfileUseCaseImpl
import io.ktor.client.HttpClient
import org.koin.dsl.module

fun authModule() = module {

    single { AuthApi(get<HttpClient>()) }
    single { AuthService(get()) }
    single<AuthRepository> { AuthRepositoryImpl(get(), get<TokenStorage>()) }

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
