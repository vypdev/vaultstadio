package com.vaultstadio.app.feature.security

import com.arkivanov.decompose.ComponentContext

interface SecurityComponent {
    fun onBack()
}

class DefaultSecurityComponent(
    componentContext: ComponentContext,
    private val onNavigateBack: () -> Unit,
) : SecurityComponent, ComponentContext by componentContext {
    override fun onBack() = onNavigateBack()
}
