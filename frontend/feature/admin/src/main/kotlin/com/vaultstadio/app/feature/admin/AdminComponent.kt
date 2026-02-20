package com.vaultstadio.app.feature.admin

import com.arkivanov.decompose.ComponentContext

interface AdminComponent {
    fun onBack()
}

class DefaultAdminComponent(
    componentContext: ComponentContext,
    private val onNavigateBack: () -> Unit,
) : AdminComponent, ComponentContext by componentContext {

    override fun onBack() = onNavigateBack()
}
