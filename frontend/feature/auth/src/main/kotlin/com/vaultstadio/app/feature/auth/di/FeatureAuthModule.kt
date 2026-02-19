/**
 * Koin module for feature:auth.
 * Declares AuthViewModel (LoginUseCase and RegisterUseCase are resolved by Koin at app level).
 * Entry points must load authModule before this module.
 */

package com.vaultstadio.app.feature.auth.di

import com.vaultstadio.app.feature.auth.AuthSuccessCallback
import com.vaultstadio.app.feature.auth.AuthViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val featureAuthModule = module {
    viewModel { (callback: AuthSuccessCallback) ->
        AuthViewModel(get(), get(), callback)
    }
}
