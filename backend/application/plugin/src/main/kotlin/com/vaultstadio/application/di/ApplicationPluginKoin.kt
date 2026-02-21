package com.vaultstadio.application.di

import com.vaultstadio.application.usecase.plugin.DisablePluginUseCase
import com.vaultstadio.application.usecase.plugin.DisablePluginUseCaseImpl
import com.vaultstadio.application.usecase.plugin.EnablePluginUseCase
import com.vaultstadio.application.usecase.plugin.EnablePluginUseCaseImpl
import com.vaultstadio.application.usecase.plugin.GetPluginEndpointsUseCase
import com.vaultstadio.application.usecase.plugin.GetPluginEndpointsUseCaseImpl
import com.vaultstadio.application.usecase.plugin.GetPluginStateUseCase
import com.vaultstadio.application.usecase.plugin.GetPluginStateUseCaseImpl
import com.vaultstadio.application.usecase.plugin.GetPluginUseCase
import com.vaultstadio.application.usecase.plugin.GetPluginUseCaseImpl
import com.vaultstadio.application.usecase.plugin.HandlePluginEndpointUseCase
import com.vaultstadio.application.usecase.plugin.HandlePluginEndpointUseCaseImpl
import com.vaultstadio.application.usecase.plugin.ListPluginsUseCase
import com.vaultstadio.application.usecase.plugin.ListPluginsUseCaseImpl
import org.koin.dsl.module

fun applicationPluginModule() = module {
    single<ListPluginsUseCase> { ListPluginsUseCaseImpl(get()) }
    single<GetPluginUseCase> { GetPluginUseCaseImpl(get()) }
    single<GetPluginStateUseCase> { GetPluginStateUseCaseImpl(get()) }
    single<EnablePluginUseCase> { EnablePluginUseCaseImpl(get()) }
    single<DisablePluginUseCase> { DisablePluginUseCaseImpl(get()) }
    single<GetPluginEndpointsUseCase> { GetPluginEndpointsUseCaseImpl(get()) }
    single<HandlePluginEndpointUseCase> { HandlePluginEndpointUseCaseImpl(get()) }
}
