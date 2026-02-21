package com.vaultstadio.application.di

import com.vaultstadio.application.usecase.admin.DeleteUserUseCase
import com.vaultstadio.application.usecase.admin.DeleteUserUseCaseImpl
import com.vaultstadio.application.usecase.admin.GetAdminStatisticsUseCase
import com.vaultstadio.application.usecase.admin.GetAdminStatisticsUseCaseImpl
import com.vaultstadio.application.usecase.admin.ListUsersUseCase
import com.vaultstadio.application.usecase.admin.ListUsersUseCaseImpl
import com.vaultstadio.application.usecase.admin.UpdateQuotaUseCase
import com.vaultstadio.application.usecase.admin.UpdateQuotaUseCaseImpl
import org.koin.dsl.module

fun applicationAdminModule() = module {
    single<ListUsersUseCase> { ListUsersUseCaseImpl(get()) }
    single<GetAdminStatisticsUseCase> { GetAdminStatisticsUseCaseImpl(get()) }
    single<UpdateQuotaUseCase> { UpdateQuotaUseCaseImpl(get()) }
    single<DeleteUserUseCase> { DeleteUserUseCaseImpl(get()) }
}
