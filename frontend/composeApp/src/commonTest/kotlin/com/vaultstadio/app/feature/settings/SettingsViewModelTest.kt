/**
 * Unit tests for SettingsViewModel: theme, language, and cache-cleared state.
 * init observes currentUserFlow; theme/language methods update global ThemeSettings/Strings.
 */

package com.vaultstadio.app.feature.settings

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.domain.model.User
import com.vaultstadio.app.domain.model.UserRole
import com.vaultstadio.app.domain.auth.usecase.GetCurrentUserUseCase
import com.vaultstadio.app.feature.ViewModelTestBase
import com.vaultstadio.app.i18n.Language
import com.vaultstadio.app.ui.theme.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

private val testInstant = Instant.fromEpochMilliseconds(0L)

private class FakeGetCurrentUserUseCase(
    initialUser: User? = null,
) : GetCurrentUserUseCase {
    private val user = initialUser
    private val _flow = MutableStateFlow(user)
    override val currentUserFlow: StateFlow<User?> = _flow.asStateFlow()

    private val defaultUser = User("u1", "a@b.com", "user", UserRole.USER, null, testInstant)

    override suspend fun invoke(): Result<User> =
        Result.success(user ?: defaultUser)

    override suspend fun refresh() {}

    override fun isLoggedIn(): Boolean = user != null
}

class SettingsViewModelTest {

    private fun createViewModel(): SettingsViewModel =
        SettingsViewModel(getCurrentUserUseCase = FakeGetCurrentUserUseCase())

    @Test
    fun toggleDarkMode_flipsIsDarkMode() = ViewModelTestBase.withMainDispatcher {
        val vm = createViewModel()
        val before = vm.isDarkMode
        vm.toggleDarkMode()
        assertEquals(!before, vm.isDarkMode)
        vm.toggleDarkMode()
        assertEquals(before, vm.isDarkMode)
    }

    @Test
    fun updateThemeMode_updatesThemeMode() = ViewModelTestBase.withMainDispatcher {
        val vm = createViewModel()
        vm.updateThemeMode(ThemeMode.DARK)
        assertEquals(ThemeMode.DARK, vm.themeMode)
        vm.updateThemeMode(ThemeMode.LIGHT)
        assertEquals(ThemeMode.LIGHT, vm.themeMode)
        vm.updateThemeMode(ThemeMode.SYSTEM)
        assertEquals(ThemeMode.SYSTEM, vm.themeMode)
    }

    @Test
    fun setLanguage_updatesCurrentLanguage() = ViewModelTestBase.withMainDispatcher {
        val vm = createViewModel()
        vm.setLanguage(Language.SPANISH)
        assertEquals(Language.SPANISH, vm.currentLanguage)
        vm.setLanguage(Language.ENGLISH)
        assertEquals(Language.ENGLISH, vm.currentLanguage)
    }

    @Test
    fun resetCacheCleared_resetsFlag() = ViewModelTestBase.withMainDispatcher {
        val vm = createViewModel()
        assertFalse(vm.cacheCleared)
        vm.resetCacheCleared()
        assertFalse(vm.cacheCleared)
    }
}
