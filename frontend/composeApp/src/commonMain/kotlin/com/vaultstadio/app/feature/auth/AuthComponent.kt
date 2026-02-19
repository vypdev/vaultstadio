package com.vaultstadio.app.feature.auth

import com.arkivanov.decompose.ComponentContext

/**
 * Component for authentication flow.
 *
 * With @KoinViewModel, the ViewModel is obtained directly in the Composable
 * using koinViewModel { parametersOf(onSuccess) }.
 */
interface AuthComponent {
    val onAuthSuccess: () -> Unit
}

/**
 * Default implementation of AuthComponent.
 *
 * Simplified: ViewModel is now injected directly in AuthContent via koinViewModel.
 */
class DefaultAuthComponent(
    componentContext: ComponentContext,
    override val onAuthSuccess: () -> Unit,
) : AuthComponent, ComponentContext by componentContext
