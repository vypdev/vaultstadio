/**
 * Global string provider and composition.
 * Use [Strings.resources] or [LocalStrings] for current locale.
 * App is responsible for persisting language (call [loadSavedLanguage] with saved code at startup).
 */

package com.vaultstadio.app.core.resources

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf

object Strings {
    private var _currentLanguage by mutableStateOf(Language.ENGLISH)

    var currentLanguage: Language
        get() = _currentLanguage
        set(value) {
            _currentLanguage = value
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
     * Load language from a saved code (e.g. from platform storage).
     * Call at app startup with the persisted value; pass null to keep default.
     */
    fun loadSavedLanguage(savedCode: String?) {
        if (savedCode != null) {
            _currentLanguage = Language.entries.find { it.code == savedCode } ?: Language.ENGLISH
        }
    }
}

@Composable
fun strings(): StringResources = Strings.resources

val LocalStrings = staticCompositionLocalOf<StringResources> { EnglishStrings }
