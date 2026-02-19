/**
 * Unit tests for SecurityViewModel: dialog state, error dismissal, and toggleTwoFactor.
 * Async loadSecurityData and revokeSession are covered by use case tests.
 */

package com.vaultstadio.app.feature.security

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.domain.auth.model.ActiveSession
import com.vaultstadio.app.domain.auth.model.LoginEvent
import com.vaultstadio.app.domain.auth.model.SecuritySettings
import com.vaultstadio.app.domain.auth.model.SessionDeviceType
import com.vaultstadio.app.domain.auth.usecase.GetActiveSessionsUseCase
import com.vaultstadio.app.domain.auth.usecase.GetLoginHistoryUseCase
import com.vaultstadio.app.domain.auth.usecase.GetSecuritySettingsUseCase
import com.vaultstadio.app.domain.auth.usecase.RevokeSessionUseCase
import com.vaultstadio.app.feature.ViewModelTestBase
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private val testInstant = Instant.fromEpochMilliseconds(0L)

private fun testSession(id: String = "s1") = ActiveSession(
    id = id,
    deviceName = "Chrome",
    deviceType = SessionDeviceType.WEB,
    lastActiveAt = testInstant,
    location = null,
    ipAddress = "127.0.0.1",
    isCurrent = false,
)

private class FakeGetActiveSessionsUseCase(
    var result: Result<List<ActiveSession>> = Result.success(emptyList()),
) : GetActiveSessionsUseCase {
    override suspend fun invoke(): Result<List<ActiveSession>> = result
}

private class FakeGetLoginHistoryUseCase(
    var result: Result<List<LoginEvent>> = Result.success(emptyList()),
) : GetLoginHistoryUseCase {
    override suspend fun invoke(): Result<List<LoginEvent>> = result
}

private class FakeGetSecuritySettingsUseCase(
    var result: Result<SecuritySettings> = Result.success(
        SecuritySettings(twoFactorEnabled = false, twoFactorMethod = null),
    ),
) : GetSecuritySettingsUseCase {
    override suspend fun invoke(): Result<SecuritySettings> = result
}

private class FakeRevokeSessionUseCase(
    var result: Result<Unit> = Result.success(Unit),
) : RevokeSessionUseCase {
    override suspend fun invoke(sessionId: String): Result<Unit> = result
}

class SecurityViewModelTest {

    private fun createViewModel(): SecurityViewModel = SecurityViewModel(
        getActiveSessionsUseCase = FakeGetActiveSessionsUseCase(),
        getLoginHistoryUseCase = FakeGetLoginHistoryUseCase(),
        getSecuritySettingsUseCase = FakeGetSecuritySettingsUseCase(),
        revokeSessionUseCase = FakeRevokeSessionUseCase(),
    )

    @Test
    fun showRevokeDialog_setsSession() = ViewModelTestBase.withMainDispatcher {
        val vm = createViewModel()
        assertNull(vm.showRevokeSessionDialog)
        val session = testSession("s99")
        vm.showRevokeDialog(session)
        assertEquals(session, vm.showRevokeSessionDialog)
    }

    @Test
    fun dismissRevokeDialog_clearsSession() = ViewModelTestBase.withMainDispatcher {
        val vm = createViewModel()
        vm.showRevokeDialog(testSession())
        vm.dismissRevokeDialog()
        assertNull(vm.showRevokeSessionDialog)
    }

    @Test
    fun dismissError_clearsErrorMessage() = ViewModelTestBase.withMainDispatcher {
        val vm = createViewModel()
        vm.toggleTwoFactor()
        assertTrue(vm.errorMessage != null)
        vm.dismissError()
        assertNull(vm.errorMessage)
    }

    @Test
    fun toggleTwoFactor_setsNotAvailableMessage() = ViewModelTestBase.withMainDispatcher {
        val vm = createViewModel()
        vm.toggleTwoFactor()
        assertEquals("Two-factor authentication setup is not yet available", vm.errorMessage)
    }
}
