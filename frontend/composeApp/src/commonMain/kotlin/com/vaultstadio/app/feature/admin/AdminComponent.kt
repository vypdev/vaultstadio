package com.vaultstadio.app.feature.admin

import com.arkivanov.decompose.ComponentContext

/**
 * Component for admin user management.
 *
 * With @KoinViewModel, the ViewModel is obtained directly in the Composable
 * using koinViewModel().
 */
interface AdminComponent {
    fun onBack()
}

/**
 * Default implementation of AdminComponent.
 *
 * Simplified: ViewModel is now injected directly in AdminContent via koinViewModel.
 */
class DefaultAdminComponent(
    componentContext: ComponentContext,
    private val onNavigateBack: () -> Unit,
) : AdminComponent, ComponentContext by componentContext {

    override fun onBack() = onNavigateBack()
}
