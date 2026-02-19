/**
 * Koin module for feature:settings.
 */

package com.vaultstadio.app.feature.settings.di

import com.vaultstadio.app.feature.settings.SettingsComponent
import com.vaultstadio.app.feature.settings.SettingsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val featureSettingsModule = module {
    viewModel { (component: SettingsComponent) ->
        SettingsViewModel(get(), component)
    }
}
