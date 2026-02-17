package com.vaultstadio.app.feature.profile

import com.arkivanov.decompose.ComponentContext

/**
 * Component for user profile management.
 *
 * With @KoinViewModel, the ViewModel is obtained directly in the Composable
 * using koinViewModel().
 */
interface ProfileComponent {
    fun onBack()
    fun navigateToChangePassword()
    fun navigateToSecurity()
    fun exportData()
}

/**
 * Default implementation of ProfileComponent.
 *
 * Simplified: ViewModel is now injected directly in ProfileContent via koinViewModel.
 */
class DefaultProfileComponent(
    componentContext: ComponentContext,
    private val onNavigateBack: () -> Unit,
    private val onNavigateToChangePassword: () -> Unit = {},
    private val onNavigateToSecurity: () -> Unit = {},
    private val onExportData: () -> Unit = {},
) : ProfileComponent, ComponentContext by componentContext {

    override fun onBack() = onNavigateBack()
    override fun navigateToChangePassword() = onNavigateToChangePassword()
    override fun navigateToSecurity() = onNavigateToSecurity()
    override fun exportData() = onExportData()
}
