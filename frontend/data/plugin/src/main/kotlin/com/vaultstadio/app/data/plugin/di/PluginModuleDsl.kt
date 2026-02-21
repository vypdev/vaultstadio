/**
 * Koin module for plugin (API, service, repository, use cases).
 * ComposeApp loads it via modules(... + pluginModule).
 */

package com.vaultstadio.app.data.plugin.di

import com.vaultstadio.app.data.plugin.api.PluginApi
import com.vaultstadio.app.data.plugin.repository.PluginRepositoryImpl
import com.vaultstadio.app.data.plugin.service.PluginService
import com.vaultstadio.app.data.plugin.usecase.DisablePluginUseCaseImpl
import com.vaultstadio.app.data.plugin.usecase.EnablePluginUseCaseImpl
import com.vaultstadio.app.data.plugin.usecase.GetPluginsUseCaseImpl
import com.vaultstadio.app.domain.plugin.PluginRepository
import com.vaultstadio.app.domain.plugin.usecase.DisablePluginUseCase
import com.vaultstadio.app.domain.plugin.usecase.EnablePluginUseCase
import com.vaultstadio.app.domain.plugin.usecase.GetPluginsUseCase
import io.ktor.client.HttpClient
import org.koin.dsl.module

val pluginModule = module {
    single { PluginApi(get<HttpClient>()) }
    single { PluginService(get()) }
    single<PluginRepository> { PluginRepositoryImpl(get()) }

    factory<GetPluginsUseCase> { GetPluginsUseCaseImpl(get()) }
    factory<EnablePluginUseCase> { EnablePluginUseCaseImpl(get()) }
    factory<DisablePluginUseCase> { DisablePluginUseCaseImpl(get()) }
}
