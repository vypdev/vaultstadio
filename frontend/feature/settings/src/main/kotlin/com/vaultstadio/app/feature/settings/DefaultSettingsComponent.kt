/**
 * Default implementation of SettingsComponent.
 */

package com.vaultstadio.app.feature.settings

import com.arkivanov.decompose.ComponentContext
import com.vaultstadio.app.core.resources.Language
import com.vaultstadio.app.core.resources.ThemeMode

class DefaultSettingsComponent(
    componentContext: ComponentContext,
    private val onNavigateBack: () -> Unit,
    private val onLogoutAction: () -> Unit,
    private val onNavigateToProfile: () -> Unit,
    private val onNavigateToChangePassword: () -> Unit,
    private val onNavigateToLicenses: () -> Unit,
    override val initialThemeMode: ThemeMode,
    private val onThemeModeChangeCallback: (ThemeMode) -> Unit,
    override val initialLanguage: Language,
    private val onLanguageChangeCallback: (Language) -> Unit,
) : SettingsComponent, ComponentContext by componentContext {

    override fun onBack() = onNavigateBack()
    override fun logout() = onLogoutAction()
    override fun navigateToProfile() = onNavigateToProfile()
    override fun navigateToChangePassword() = onNavigateToChangePassword()
    override fun navigateToLicenses() = onNavigateToLicenses()
    override fun onThemeModeChange(mode: ThemeMode) = onThemeModeChangeCallback(mode)
    override fun onLanguageChange(language: Language) = onLanguageChangeCallback(language)
}
