/**
 * Koin module for admin (API, service, repository, use cases).
 * ComposeApp loads it via modules(... + adminModule).
 */

package com.vaultstadio.app.data.admin.di

import com.vaultstadio.app.data.admin.api.AdminApi
import com.vaultstadio.app.data.admin.repository.AdminRepositoryImpl
import com.vaultstadio.app.data.admin.service.AdminService
import com.vaultstadio.app.data.admin.usecase.GetAdminUsersUseCaseImpl
import com.vaultstadio.app.data.admin.usecase.UpdateUserQuotaUseCaseImpl
import com.vaultstadio.app.data.admin.usecase.UpdateUserRoleUseCaseImpl
import com.vaultstadio.app.data.admin.usecase.UpdateUserStatusUseCaseImpl
import com.vaultstadio.app.domain.admin.AdminRepository
import com.vaultstadio.app.domain.admin.usecase.GetAdminUsersUseCase
import com.vaultstadio.app.domain.admin.usecase.UpdateUserQuotaUseCase
import com.vaultstadio.app.domain.admin.usecase.UpdateUserRoleUseCase
import com.vaultstadio.app.domain.admin.usecase.UpdateUserStatusUseCase
import io.ktor.client.HttpClient
import org.koin.dsl.module

val adminModule = module {
    single { AdminApi(get<HttpClient>()) }
    single { AdminService(get()) }
    single<AdminRepository> { AdminRepositoryImpl(get()) }

    factory<GetAdminUsersUseCase> { GetAdminUsersUseCaseImpl(get()) }
    factory<UpdateUserQuotaUseCase> { UpdateUserQuotaUseCaseImpl(get()) }
    factory<UpdateUserRoleUseCase> { UpdateUserRoleUseCaseImpl(get()) }
    factory<UpdateUserStatusUseCase> { UpdateUserStatusUseCaseImpl(get()) }
}
