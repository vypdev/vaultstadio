package com.vaultstadio.app.feature.plugins.di

import com.vaultstadio.app.feature.plugins.PluginsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val featurePluginsModule = module {
    viewModel { PluginsViewModel(get(), get(), get()) }
}
