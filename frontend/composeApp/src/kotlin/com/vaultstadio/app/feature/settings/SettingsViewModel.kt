package com.vaultstadio.app.feature.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaultstadio.app.domain.auth.model.User
import com.vaultstadio.app.domain.auth.usecase.GetCurrentUserUseCase
import com.vaultstadio.app.i18n.Language
import com.vaultstadio.app.i18n.Strings
import com.vaultstadio.app.ui.theme.ThemeMode
import com.vaultstadio.app.ui.theme.ThemeSettings
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
/**
 * ViewModel for Settings screen.
 */
class SettingsViewModel(
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
) : ViewModel() {

    var currentUser by mutableStateOf<User?>(null)
        private set

    var isDarkMode by mutableStateOf(ThemeSettings.isDarkMode)
        private set

    var themeMode by mutableStateOf(ThemeSettings.themeMode)
        private set

    var currentLanguage by mutableStateOf(Strings.currentLanguage)
        private set

    var isClearingCache by mutableStateOf(false)
        private set

    var cacheCleared by mutableStateOf(false)
        private set

    init {
        observeCurrentUser()
    }

    private fun observeCurrentUser() {
        viewModelScope.launch {
            getCurrentUserUseCase.currentUserFlow.collectLatest { user ->
                currentUser = user
            }
        }
    }

    fun toggleDarkMode() {
        isDarkMode = !isDarkMode
        ThemeSettings.isDarkMode = isDarkMode
    }

    fun updateThemeMode(mode: ThemeMode) {
        themeMode = mode
        ThemeSettings.themeMode = mode
    }

    fun setLanguage(language: Language) {
        currentLanguage = language
        Strings.currentLanguage = language
    }

    fun clearCache() {
        viewModelScope.launch {
            isClearingCache = true
            // Simulating cache clear operation
            // In a real implementation, this would clear local storage, image cache, etc.
            kotlinx.coroutines.delay(500)
            isClearingCache = false
            cacheCleared = true
        }
    }

    fun resetCacheCleared() {
        cacheCleared = false
    }
}
