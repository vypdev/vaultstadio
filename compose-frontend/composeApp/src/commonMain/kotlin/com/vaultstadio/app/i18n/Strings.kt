/**
 * VaultStadio Internationalization (i18n) – Global string provider and composition.
 *
 * Multi-language support: use [Strings.resources] or [LocalStrings] for current locale.
 */

package com.vaultstadio.app.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import com.vaultstadio.app.platform.PlatformStorage
import com.vaultstadio.app.platform.StorageKeys

/**
 * Global string provider with persistence.
 */
object Strings {
    private var _currentLanguage by mutableStateOf(Language.ENGLISH)

    var currentLanguage: Language
        get() = _currentLanguage
        set(value) {
            _currentLanguage = value
            try {
                PlatformStorage.setString(
                    StorageKeys.LANGUAGE,
                    value.code,
                )
            } catch (_: Exception) {
                // Ignore storage errors
            }
        }

    val resources: StringResources
        get() = when (_currentLanguage) {
            Language.ENGLISH -> EnglishStrings
            Language.SPANISH -> SpanishStrings
            Language.FRENCH -> FrenchStrings
            Language.GERMAN -> GermanStrings
            Language.PORTUGUESE -> PortugueseStrings
            Language.CHINESE -> ChineseStrings
            Language.JAPANESE -> JapaneseStrings
        }

    /**
     * Load saved language from storage.
     */
    fun loadSavedLanguage() {
        try {
            val saved = PlatformStorage.getString(
                StorageKeys.LANGUAGE,
            )
            if (saved != null) {
                _currentLanguage = Language.entries.find { it.code == saved } ?: Language.ENGLISH
            }
        } catch (_: Exception) {
            // Use default
        }
    }
}

/**
 * Composable to access current strings.
 */
@Composable
fun strings(): StringResources = Strings.resources

/**
 * Local composition for strings.
 */
val LocalStrings = staticCompositionLocalOf<StringResources> { EnglishStrings }
