/**
 * Component for authentication flow.
 *
 * ViewModel is obtained in the Composable using koinViewModel { parametersOf(onSuccess) }.
 */

package com.vaultstadio.app.feature.auth

import com.arkivanov.decompose.ComponentContext

interface AuthComponent {
    val onAuthSuccess: () -> Unit
}

/**
 * Default implementation of AuthComponent.
 */
class DefaultAuthComponent(
    componentContext: ComponentContext,
    override val onAuthSuccess: () -> Unit,
) : AuthComponent, ComponentContext by componentContext
