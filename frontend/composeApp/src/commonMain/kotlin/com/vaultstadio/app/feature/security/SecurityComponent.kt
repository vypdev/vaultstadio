package com.vaultstadio.app.feature.security

import com.arkivanov.decompose.ComponentContext

/**
 * Component for security settings.
 */
interface SecurityComponent {
    fun onBack()
}

/**
 * Default implementation of SecurityComponent.
 */
class DefaultSecurityComponent(
    componentContext: ComponentContext,
    private val onNavigateBack: () -> Unit,
) : SecurityComponent, ComponentContext by componentContext {

    override fun onBack() = onNavigateBack()
}
