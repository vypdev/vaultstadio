/**
 * Component for Settings screen.
 * Theme and language are provided by the host so persistence can be handled there.
 */

package com.vaultstadio.app.feature.settings

import com.vaultstadio.app.core.resources.Language
import com.vaultstadio.app.core.resources.ThemeMode

interface SettingsComponent {
    fun onBack()
    fun logout()
    fun navigateToProfile()
    fun navigateToChangePassword()
    fun navigateToLicenses()
    val initialThemeMode: ThemeMode
    fun onThemeModeChange(mode: ThemeMode)
    val initialLanguage: Language
    fun onLanguageChange(language: Language)
}
