package com.vaultstadio.app.feature.settings

import com.arkivanov.decompose.ComponentContext

interface SettingsComponent {
    fun onBack()
    fun logout()
    fun navigateToProfile()
    fun navigateToChangePassword()
    fun navigateToLicenses()
}

class DefaultSettingsComponent(
    componentContext: ComponentContext,
    private val onNavigateBack: () -> Unit,
    private val onLogoutAction: () -> Unit,
    private val onNavigateToProfile: () -> Unit = {},
    private val onNavigateToChangePassword: () -> Unit = {},
    private val onNavigateToLicenses: () -> Unit = {},
) : SettingsComponent, ComponentContext by componentContext {
    override fun onBack() = onNavigateBack()
    override fun logout() = onLogoutAction()
    override fun navigateToProfile() = onNavigateToProfile()
    override fun navigateToChangePassword() = onNavigateToChangePassword()
    override fun navigateToLicenses() = onNavigateToLicenses()
}
