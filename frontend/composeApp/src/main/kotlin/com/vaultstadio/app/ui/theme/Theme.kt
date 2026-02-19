/**
 * VaultStadio Material 3 Theme
 *
 * Enhanced theme with light/dark/auto mode support.
 */

package com.vaultstadio.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.vaultstadio.app.core.resources.ThemeMode
import com.vaultstadio.app.platform.PlatformStorage
import com.vaultstadio.app.platform.StorageKeys

/**
 * Theme state holder with persistence.
 */
object ThemeSettings {
    private var _themeMode by mutableStateOf(ThemeMode.SYSTEM)

    var themeMode: ThemeMode
        get() = _themeMode
        set(value) {
            _themeMode = value
            // Persist to storage
            try {
                PlatformStorage.setString(
                    StorageKeys.THEME_MODE,
                    value.name,
                )
            } catch (_: Exception) {
                // Ignore storage errors
            }
        }

    /**
     * Convenience property for dark mode toggle.
     */
    var isDarkMode: Boolean
        get() = _themeMode == ThemeMode.DARK
        set(value) {
            themeMode = if (value) ThemeMode.DARK else ThemeMode.LIGHT
        }

    /**
     * Load saved theme from storage.
     */
    fun loadSavedTheme() {
        try {
            val saved = PlatformStorage.getString(
                StorageKeys.THEME_MODE,
            )
            if (saved != null) {
                _themeMode = ThemeMode.valueOf(saved)
            }
        } catch (_: Exception) {
            // Use default
        }
    }
}

// Material 3 Color Tokens - Light Theme
private val md_theme_light_primary = Color(0xFF4355B9)
private val md_theme_light_onPrimary = Color(0xFFFFFFFF)
private val md_theme_light_primaryContainer = Color(0xFFDFE0FF)
private val md_theme_light_onPrimaryContainer = Color(0xFF000964)
private val md_theme_light_secondary = Color(0xFF5B5D72)
private val md_theme_light_onSecondary = Color(0xFFFFFFFF)
private val md_theme_light_secondaryContainer = Color(0xFFE0E1F9)
private val md_theme_light_onSecondaryContainer = Color(0xFF181A2C)
private val md_theme_light_tertiary = Color(0xFF77536D)
private val md_theme_light_onTertiary = Color(0xFFFFFFFF)
private val md_theme_light_tertiaryContainer = Color(0xFFFFD7F1)
private val md_theme_light_onTertiaryContainer = Color(0xFF2D1228)
private val md_theme_light_error = Color(0xFFBA1A1A)
private val md_theme_light_errorContainer = Color(0xFFFFDAD6)
private val md_theme_light_onError = Color(0xFFFFFFFF)
private val md_theme_light_onErrorContainer = Color(0xFF410002)
private val md_theme_light_background = Color(0xFFFEFBFF)
private val md_theme_light_onBackground = Color(0xFF1B1B1F)
private val md_theme_light_surface = Color(0xFFFEFBFF)
private val md_theme_light_onSurface = Color(0xFF1B1B1F)
private val md_theme_light_surfaceVariant = Color(0xFFE3E1EC)
private val md_theme_light_onSurfaceVariant = Color(0xFF46464F)
private val md_theme_light_outline = Color(0xFF767680)
private val md_theme_light_inverseOnSurface = Color(0xFFF3EFF4)
private val md_theme_light_inverseSurface = Color(0xFF303034)
private val md_theme_light_inversePrimary = Color(0xFFBCC3FF)
private val md_theme_light_surfaceTint = Color(0xFF4355B9)
private val md_theme_light_outlineVariant = Color(0xFFC7C5D0)
private val md_theme_light_scrim = Color(0xFF000000)

// Material 3 Color Tokens - Dark Theme
private val md_theme_dark_primary = Color(0xFFBCC3FF)
private val md_theme_dark_onPrimary = Color(0xFF0F2089)
private val md_theme_dark_primaryContainer = Color(0xFF2A3CA0)
private val md_theme_dark_onPrimaryContainer = Color(0xFFDFE0FF)
private val md_theme_dark_secondary = Color(0xFFC4C5DD)
private val md_theme_dark_onSecondary = Color(0xFF2D2F42)
private val md_theme_dark_secondaryContainer = Color(0xFF434559)
private val md_theme_dark_onSecondaryContainer = Color(0xFFE0E1F9)
private val md_theme_dark_tertiary = Color(0xFFE5BAD8)
private val md_theme_dark_onTertiary = Color(0xFF44263E)
private val md_theme_dark_tertiaryContainer = Color(0xFF5D3C55)
private val md_theme_dark_onTertiaryContainer = Color(0xFFFFD7F1)
private val md_theme_dark_error = Color(0xFFFFB4AB)
private val md_theme_dark_errorContainer = Color(0xFF93000A)
private val md_theme_dark_onError = Color(0xFF690005)
private val md_theme_dark_onErrorContainer = Color(0xFFFFDAD6)
private val md_theme_dark_background = Color(0xFF1B1B1F)
private val md_theme_dark_onBackground = Color(0xFFE5E1E6)
private val md_theme_dark_surface = Color(0xFF1B1B1F)
private val md_theme_dark_onSurface = Color(0xFFE5E1E6)
private val md_theme_dark_surfaceVariant = Color(0xFF46464F)
private val md_theme_dark_onSurfaceVariant = Color(0xFFC7C5D0)
private val md_theme_dark_outline = Color(0xFF90909A)
private val md_theme_dark_inverseOnSurface = Color(0xFF1B1B1F)
private val md_theme_dark_inverseSurface = Color(0xFFE5E1E6)
private val md_theme_dark_inversePrimary = Color(0xFF4355B9)
private val md_theme_dark_surfaceTint = Color(0xFFBCC3FF)
private val md_theme_dark_outlineVariant = Color(0xFF46464F)
private val md_theme_dark_scrim = Color(0xFF000000)

// Semantic Colors
val Success = Color(0xFF22C55E)
val SuccessDark = Color(0xFF4ADE80)
val Warning = Color(0xFFF59E0B)
val WarningDark = Color(0xFFFBBF24)
val FolderColor = Color(0xFFFCD34D)
val FileColor = Color(0xFF60A5FA)

private val LightColorScheme = lightColorScheme(
    primary = md_theme_light_primary,
    onPrimary = md_theme_light_onPrimary,
    primaryContainer = md_theme_light_primaryContainer,
    onPrimaryContainer = md_theme_light_onPrimaryContainer,
    secondary = md_theme_light_secondary,
    onSecondary = md_theme_light_onSecondary,
    secondaryContainer = md_theme_light_secondaryContainer,
    onSecondaryContainer = md_theme_light_onSecondaryContainer,
    tertiary = md_theme_light_tertiary,
    onTertiary = md_theme_light_onTertiary,
    tertiaryContainer = md_theme_light_tertiaryContainer,
    onTertiaryContainer = md_theme_light_onTertiaryContainer,
    error = md_theme_light_error,
    errorContainer = md_theme_light_errorContainer,
    onError = md_theme_light_onError,
    onErrorContainer = md_theme_light_onErrorContainer,
    background = md_theme_light_background,
    onBackground = md_theme_light_onBackground,
    surface = md_theme_light_surface,
    onSurface = md_theme_light_onSurface,
    surfaceVariant = md_theme_light_surfaceVariant,
    onSurfaceVariant = md_theme_light_onSurfaceVariant,
    outline = md_theme_light_outline,
    inverseOnSurface = md_theme_light_inverseOnSurface,
    inverseSurface = md_theme_light_inverseSurface,
    inversePrimary = md_theme_light_inversePrimary,
    surfaceTint = md_theme_light_surfaceTint,
    outlineVariant = md_theme_light_outlineVariant,
    scrim = md_theme_light_scrim,
)

private val DarkColorScheme = darkColorScheme(
    primary = md_theme_dark_primary,
    onPrimary = md_theme_dark_onPrimary,
    primaryContainer = md_theme_dark_primaryContainer,
    onPrimaryContainer = md_theme_dark_onPrimaryContainer,
    secondary = md_theme_dark_secondary,
    onSecondary = md_theme_dark_onSecondary,
    secondaryContainer = md_theme_dark_secondaryContainer,
    onSecondaryContainer = md_theme_dark_onSecondaryContainer,
    tertiary = md_theme_dark_tertiary,
    onTertiary = md_theme_dark_onTertiary,
    tertiaryContainer = md_theme_dark_tertiaryContainer,
    onTertiaryContainer = md_theme_dark_onTertiaryContainer,
    error = md_theme_dark_error,
    errorContainer = md_theme_dark_errorContainer,
    onError = md_theme_dark_onError,
    onErrorContainer = md_theme_dark_onErrorContainer,
    background = md_theme_dark_background,
    onBackground = md_theme_dark_onBackground,
    surface = md_theme_dark_surface,
    onSurface = md_theme_dark_onSurface,
    surfaceVariant = md_theme_dark_surfaceVariant,
    onSurfaceVariant = md_theme_dark_onSurfaceVariant,
    outline = md_theme_dark_outline,
    inverseOnSurface = md_theme_dark_inverseOnSurface,
    inverseSurface = md_theme_dark_inverseSurface,
    inversePrimary = md_theme_dark_inversePrimary,
    surfaceTint = md_theme_dark_surfaceTint,
    outlineVariant = md_theme_dark_outlineVariant,
    scrim = md_theme_dark_scrim,
)

/**
 * Custom Typography with Material 3 specs.
 */
private val AppTypography = Typography()

/**
 * VaultStadio Material 3 Theme.
 */
@Composable
fun VaultStadioTheme(
    themeMode: ThemeMode = ThemeSettings.themeMode,
    content: @Composable () -> Unit,
) {
    val systemDarkTheme = isSystemInDarkTheme()

    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> systemDarkTheme
    }

    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content,
    )
}

/**
 * Composable local for accessing theme mode.
 */
val LocalDarkTheme = staticCompositionLocalOf { false }

/**
 * Preview wrapper that applies VaultStadio theme.
 * Use this in @Preview functions to see components styled correctly.
 */
@Composable
fun VaultStadioPreview(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    VaultStadioTheme(
        themeMode = if (darkTheme) ThemeMode.DARK else ThemeMode.LIGHT,
        content = content,
    )
}
