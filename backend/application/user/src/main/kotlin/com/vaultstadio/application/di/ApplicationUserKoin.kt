package com.vaultstadio.application.di

import com.vaultstadio.application.usecase.user.ChangePasswordUseCase
import com.vaultstadio.application.usecase.user.ChangePasswordUseCaseImpl
import com.vaultstadio.application.usecase.user.GetQuotaUseCase
import com.vaultstadio.application.usecase.user.GetQuotaUseCaseImpl
import com.vaultstadio.application.usecase.user.GetUserInfoUseCase
import com.vaultstadio.application.usecase.user.GetUserInfoUseCaseImpl
import com.vaultstadio.application.usecase.user.LogoutAllUseCase
import com.vaultstadio.application.usecase.user.LogoutAllUseCaseImpl
import com.vaultstadio.application.usecase.user.UpdateUserUseCase
import com.vaultstadio.application.usecase.user.UpdateUserUseCaseImpl
import org.koin.dsl.module

fun applicationUserModule() = module {
    single<GetQuotaUseCase> { GetQuotaUseCaseImpl(get()) }
    single<UpdateUserUseCase> { UpdateUserUseCaseImpl(get()) }
    single<ChangePasswordUseCase> { ChangePasswordUseCaseImpl(get()) }
    single<LogoutAllUseCase> { LogoutAllUseCaseImpl(get()) }
    single<GetUserInfoUseCase> { GetUserInfoUseCaseImpl(get()) }
}
