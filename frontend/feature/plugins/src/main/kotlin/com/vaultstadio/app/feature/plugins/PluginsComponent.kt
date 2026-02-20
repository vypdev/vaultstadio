package com.vaultstadio.app.feature.plugins

import com.arkivanov.decompose.ComponentContext

interface PluginsComponent {
    fun onBack()
}

class DefaultPluginsComponent(
    componentContext: ComponentContext,
    private val onNavigateBack: () -> Unit,
) : PluginsComponent, ComponentContext by componentContext {

    override fun onBack() = onNavigateBack()
}
