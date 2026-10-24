/**
 * ViewModel for Settings screen.
 */

package com.vaultstadio.app.feature.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaultstadio.app.core.resources.Language
import com.vaultstadio.app.core.resources.ThemeMode
import com.vaultstadio.app.domain.auth.model.User
import com.vaultstadio.app.domain.auth.usecase.GetCurrentUserUseCase
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val component: SettingsComponent,
) : ViewModel() {

    var currentUser by mutableStateOf<User?>(null)
        private set

    var themeMode by mutableStateOf(component.initialThemeMode)
        private set

    var currentLanguage by mutableStateOf(component.initialLanguage)
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
        val newDark = themeMode != ThemeMode.DARK
        themeMode = if (newDark) ThemeMode.DARK else ThemeMode.LIGHT
        component.onThemeModeChange(themeMode)
    }

    fun updateThemeMode(mode: ThemeMode) {
        themeMode = mode
        component.onThemeModeChange(mode)
    }

    fun setLanguage(language: Language) {
        currentLanguage = language
        component.onLanguageChange(language)
    }

    fun clearCache() {
        viewModelScope.launch {
            isClearingCache = true
            delay(500)
            isClearingCache = false
            cacheCleared = true
        }
    }

    fun resetCacheCleared() {
        cacheCleared = false
    }
}
