/**
 * VaultStadio Settings Dialogs
 *
 * Dialog components for the Settings screen.
 */

package com.vaultstadio.app.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vaultstadio.app.core.resources.Language
import com.vaultstadio.app.core.resources.LocalStrings
import com.vaultstadio.app.ui.theme.ThemeMode
import com.vaultstadio.app.ui.theme.VaultStadioPreview
import org.jetbrains.compose.ui.tooling.preview.Preview

private val SampleThemeMode = ThemeMode.LIGHT
private val SampleLanguage = Language.ENGLISH

/**
 * Theme selection dialog.
 */
@Composable
fun ThemeSelectionDialog(
    currentTheme: ThemeMode,
    onThemeSelected: (ThemeMode) -> Unit,
    onDismiss: () -> Unit,
) {
    val strings = LocalStrings.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.settingsTheme) },
        text = {
            Column {
                ThemeMode.entries.forEach { theme ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onThemeSelected(theme) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = currentTheme == theme,
                            onClick = { onThemeSelected(theme) },
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = when (theme) {
                                ThemeMode.LIGHT -> strings.settingsThemeLight
                                ThemeMode.DARK -> strings.settingsThemeDark
                                ThemeMode.SYSTEM -> strings.settingsThemeSystem
                            },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(strings.actionClose) }
        },
    )
}

/**
 * Language selection dialog.
 */
@Composable
fun LanguageSelectionDialog(
    currentLanguage: Language,
    onLanguageSelected: (Language) -> Unit,
    onDismiss: () -> Unit,
) {
    val strings = LocalStrings.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.settingsLanguage) },
        text = {
            Column {
                Language.entries.forEach { language ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onLanguageSelected(language) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = currentLanguage == language,
                            onClick = { onLanguageSelected(language) },
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = language.displayName)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(strings.actionClose) }
        },
    )
}

/**
 * Version info dialog.
 */
@Composable
fun VersionInfoDialog(
    onDismiss: () -> Unit,
) {
    val strings = LocalStrings.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.settingsVersion) },
        text = {
            Column {
                Text("VaultStadio v2.0.0")
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "A self-hosted storage platform with plugin architecture.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Built with Kotlin Multiplatform and Compose",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(strings.actionClose) }
        },
    )
}

/**
 * Logout confirmation dialog.
 */
@Composable
fun LogoutConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val strings = LocalStrings.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.actionLogout) },
        text = { Text(strings.settingsLogoutConfirm) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(strings.actionLogout, color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.actionCancel) }
        },
    )
}

// region Previews

@Preview
@Composable
internal fun ThemeSelectionDialogPreview() {
    VaultStadioPreview {
        ThemeSelectionDialog(
            currentTheme = SampleThemeMode,
            onThemeSelected = {},
            onDismiss = {},
        )
    }
}

@Preview
@Composable
internal fun LanguageSelectionDialogPreview() {
    VaultStadioPreview {
        LanguageSelectionDialog(
            currentLanguage = SampleLanguage,
            onLanguageSelected = {},
            onDismiss = {},
        )
    }
}

@Preview
@Composable
internal fun VersionInfoDialogPreview() {
    VaultStadioPreview {
        VersionInfoDialog(onDismiss = {})
    }
}

@Preview
@Composable
internal fun LogoutConfirmDialogPreview() {
    VaultStadioPreview {
        LogoutConfirmDialog(
            onConfirm = {},
            onDismiss = {},
        )
    }
}

// endregion
