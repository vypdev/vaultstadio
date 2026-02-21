package com.vaultstadio.application.di

import com.vaultstadio.application.usecase.share.AccessShareUseCase
import com.vaultstadio.application.usecase.share.AccessShareUseCaseImpl
import com.vaultstadio.application.usecase.share.CreateShareUseCase
import com.vaultstadio.application.usecase.share.CreateShareUseCaseImpl
import com.vaultstadio.application.usecase.share.DeleteShareUseCase
import com.vaultstadio.application.usecase.share.DeleteShareUseCaseImpl
import com.vaultstadio.application.usecase.share.GetShareUseCase
import com.vaultstadio.application.usecase.share.GetShareUseCaseImpl
import com.vaultstadio.application.usecase.share.GetSharesByItemUseCase
import com.vaultstadio.application.usecase.share.GetSharesByItemUseCaseImpl
import com.vaultstadio.application.usecase.share.GetSharesByUserUseCase
import com.vaultstadio.application.usecase.share.GetSharesByUserUseCaseImpl
import com.vaultstadio.application.usecase.share.GetSharesSharedWithUserUseCase
import com.vaultstadio.application.usecase.share.GetSharesSharedWithUserUseCaseImpl
import org.koin.dsl.module

fun applicationShareModule() = module {
    single<GetSharesByUserUseCase> { GetSharesByUserUseCaseImpl(get()) }
    single<GetSharesSharedWithUserUseCase> { GetSharesSharedWithUserUseCaseImpl(get()) }
    single<CreateShareUseCase> { CreateShareUseCaseImpl(get()) }
    single<AccessShareUseCase> { AccessShareUseCaseImpl(get()) }
    single<GetSharesByItemUseCase> { GetSharesByItemUseCaseImpl(get()) }
    single<GetShareUseCase> { GetShareUseCaseImpl(get()) }
    single<DeleteShareUseCase> { DeleteShareUseCaseImpl(get()) }
}
