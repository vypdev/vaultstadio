/**
 * Settings feature content.
 */

package com.vaultstadio.app.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun SettingsContent(
    component: SettingsComponent,
    modifier: Modifier = Modifier,
) {
    val viewModel: SettingsViewModel = koinViewModel {
        parametersOf(component)
    }

    LaunchedEffect(viewModel.cacheCleared) {
        if (viewModel.cacheCleared) {
            viewModel.resetCacheCleared()
        }
    }

    SettingsScreen(
        userName = viewModel.currentUser?.username ?: "",
        userEmail = viewModel.currentUser?.email ?: "",
        themeMode = viewModel.themeMode,
        currentLanguage = viewModel.currentLanguage,
        isClearingCache = viewModel.isClearingCache,
        onThemeModeChange = viewModel::updateThemeMode,
        onLanguageChange = viewModel::setLanguage,
        onClearCache = viewModel::clearCache,
        onNavigateBack = component::onBack,
        onLogout = component::logout,
        onNavigateToProfile = component::navigateToProfile,
        onNavigateToChangePassword = component::navigateToChangePassword,
        onShowLicenses = component::navigateToLicenses,
        modifier = modifier,
    )
}
