package com.vaultstadio.app.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.vaultstadio.app.ui.screens.SettingsScreen
import org.koin.compose.viewmodel.koinViewModel

/**
 * Settings feature content - delegates to SettingsScreen.
 */
@Composable
fun SettingsContent(
    component: SettingsComponent,
    modifier: Modifier = Modifier,
) {
    val viewModel: SettingsViewModel = koinViewModel()

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
