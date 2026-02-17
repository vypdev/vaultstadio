package com.vaultstadio.app.feature.plugins

import com.arkivanov.decompose.ComponentContext

/**
 * Component for plugin management.
 *
 * With @KoinViewModel, the ViewModel is obtained directly in the Composable
 * using koinViewModel().
 */
interface PluginsComponent {
    fun onBack()
}

/**
 * Default implementation of PluginsComponent.
 *
 * Simplified: ViewModel is now injected directly in PluginsContent via koinViewModel.
 */
class DefaultPluginsComponent(
    componentContext: ComponentContext,
    private val onNavigateBack: () -> Unit,
) : PluginsComponent, ComponentContext by componentContext {

    override fun onBack() = onNavigateBack()
}
