/**
 * VaultStadio Settings Screen
 *
 * Provides user preferences and application settings configuration.
 */

package com.vaultstadio.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vaultstadio.app.i18n.Language
import com.vaultstadio.app.i18n.LocalStrings
import com.vaultstadio.app.ui.screens.settings.AccountInfoItem
import com.vaultstadio.app.ui.screens.settings.LanguageSelectionDialog
import com.vaultstadio.app.ui.screens.settings.LogoutConfirmDialog
import com.vaultstadio.app.ui.screens.settings.SettingsCard
import com.vaultstadio.app.ui.screens.settings.SettingsClickableItem
import com.vaultstadio.app.ui.screens.settings.SettingsSectionHeader
import com.vaultstadio.app.ui.screens.settings.SettingsToggleItem
import com.vaultstadio.app.ui.screens.settings.ThemeSelectionDialog
import com.vaultstadio.app.ui.screens.settings.VersionInfoDialog
import com.vaultstadio.app.ui.theme.ThemeMode

/**
 * Settings screen with categorized preferences.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    userName: String,
    userEmail: String,
    themeMode: ThemeMode,
    currentLanguage: Language,
    isClearingCache: Boolean,
    onThemeModeChange: (ThemeMode) -> Unit,
    onLanguageChange: (Language) -> Unit,
    onClearCache: () -> Unit,
    onNavigateBack: () -> Unit,
    onLogout: () -> Unit,
    onNavigateToProfile: () -> Unit = {},
    onNavigateToChangePassword: () -> Unit = {},
    onShowLicenses: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showVersionDialog by remember { mutableStateOf(false) }
    var autoSync by remember { mutableStateOf(true) }
    var notifications by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.settingsTitle) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = modifier.fillMaxSize().padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item { SettingsSectionHeader(strings.settingsAccount) }
            item {
                SettingsCard {
                    AccountInfoItem(
                        name = userName.ifBlank { strings.settingsAccount },
                        email = userEmail,
                        onClick = onNavigateToProfile,
                    )
                }
            }

            item { SettingsSectionHeader(strings.settingsAppearance) }
            item {
                SettingsCard {
                    Column {
                        SettingsClickableItem(
                            icon = Icons.Default.Palette,
                            title = strings.settingsTheme,
                            subtitle = when (themeMode) {
                                ThemeMode.LIGHT -> strings.settingsThemeLight
                                ThemeMode.DARK -> strings.settingsThemeDark
                                ThemeMode.SYSTEM -> strings.settingsThemeSystem
                            },
                            onClick = { showThemeDialog = true },
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        SettingsClickableItem(
                            icon = Icons.Default.Language,
                            title = strings.settingsLanguage,
                            subtitle = currentLanguage.displayName,
                            onClick = { showLanguageDialog = true },
                        )
                    }
                }
            }

            item { SettingsSectionHeader(strings.settingsStorage) }
            item {
                SettingsCard {
                    Column {
                        SettingsToggleItem(
                            icon = Icons.Default.Sync,
                            title = strings.settingsAutoSync,
                            subtitle = strings.settingsAutoSyncDesc,
                            checked = autoSync,
                            onCheckedChange = { autoSync = it },
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        SettingsClickableItem(
                            icon = Icons.Default.CleaningServices,
                            title = if (isClearingCache) {
                                "${strings.settingsClearCache}..."
                            } else {
                                strings.settingsClearCache
                            },
                            subtitle = strings.settingsClearCacheDesc,
                            onClick = { if (!isClearingCache) onClearCache() },
                        )
                    }
                }
            }

            item { SettingsSectionHeader(strings.settingsNotifications) }
            item {
                SettingsCard {
                    SettingsToggleItem(
                        icon = Icons.Default.Notifications,
                        title = strings.settingsPushNotifications,
                        subtitle = strings.settingsPushNotificationsDesc,
                        checked = notifications,
                        onCheckedChange = { notifications = it },
                    )
                }
            }

            item { SettingsSectionHeader(strings.settingsSecurity) }
            item {
                SettingsCard {
                    SettingsClickableItem(
                        icon = Icons.Default.Lock,
                        title = strings.settingsChangePassword,
                        subtitle = strings.settingsChangePasswordDesc,
                        onClick = onNavigateToChangePassword,
                    )
                }
            }

            item { SettingsSectionHeader(strings.settingsAbout) }
            item {
                SettingsCard {
                    Column {
                        SettingsClickableItem(
                            icon = Icons.Default.Info,
                            title = strings.settingsVersion,
                            subtitle = "2.0.0",
                            onClick = { showVersionDialog = true },
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        SettingsClickableItem(
                            icon = Icons.Default.Description,
                            title = strings.settingsLicenses,
                            subtitle = strings.settingsLicensesDesc,
                            onClick = onShowLicenses,
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }
            item {
                SettingsCard(containerColor = MaterialTheme.colorScheme.errorContainer) {
                    SettingsClickableItem(
                        icon = Icons.AutoMirrored.Filled.ExitToApp,
                        title = strings.actionLogout,
                        subtitle = strings.settingsSignOutDesc,
                        onClick = { showLogoutDialog = true },
                        textColor = MaterialTheme.colorScheme.error,
                    )
                }
            }
            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }

    if (showThemeDialog) {
        ThemeSelectionDialog(
            currentTheme = themeMode,
            onThemeSelected = {
                onThemeModeChange(it)
                showThemeDialog = false
            },
            onDismiss = { showThemeDialog = false },
        )
    }
    if (showLanguageDialog) {
        LanguageSelectionDialog(
            currentLanguage = currentLanguage,
            onLanguageSelected = {
                onLanguageChange(it)
                showLanguageDialog = false
            },
            onDismiss = { showLanguageDialog = false },
        )
    }
    if (showVersionDialog) {
        VersionInfoDialog(onDismiss = { showVersionDialog = false })
    }
    if (showLogoutDialog) {
        LogoutConfirmDialog(
            onConfirm = {
                showLogoutDialog = false
                onLogout()
            },
            onDismiss = { showLogoutDialog = false },
        )
    }
}
