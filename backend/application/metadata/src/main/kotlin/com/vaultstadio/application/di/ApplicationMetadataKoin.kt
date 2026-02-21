package com.vaultstadio.application.di

import com.vaultstadio.application.usecase.metadata.GetItemMetadataUseCase
import com.vaultstadio.application.usecase.metadata.GetItemMetadataUseCaseImpl
import com.vaultstadio.application.usecase.metadata.GetMetadataByItemIdAndPluginUseCase
import com.vaultstadio.application.usecase.metadata.GetMetadataByItemIdAndPluginUseCaseImpl
import org.koin.dsl.module

fun applicationMetadataModule() = module {
    single<GetItemMetadataUseCase> { GetItemMetadataUseCaseImpl(get(), get()) }
    single<GetMetadataByItemIdAndPluginUseCase> { GetMetadataByItemIdAndPluginUseCaseImpl(get()) }
}
