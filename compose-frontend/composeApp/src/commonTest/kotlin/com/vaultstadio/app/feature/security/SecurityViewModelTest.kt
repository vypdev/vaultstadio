/**
 * Unit tests for SecurityViewModel: dialog state, error dismissal, and toggleTwoFactor.
 * Async loadSecurityData and revokeSession are covered by use case tests.
 */

package com.vaultstadio.app.feature.security

import com.vaultstadio.app.data.network.ApiResult
import com.vaultstadio.app.domain.model.ActiveSession
import com.vaultstadio.app.domain.model.LoginEvent
import com.vaultstadio.app.domain.model.SecuritySettings
import com.vaultstadio.app.domain.model.SessionDeviceType
import com.vaultstadio.app.domain.model.TwoFactorMethod
import com.vaultstadio.app.domain.usecase.auth.GetActiveSessionsUseCase
import com.vaultstadio.app.domain.usecase.auth.GetLoginHistoryUseCase
import com.vaultstadio.app.domain.usecase.auth.GetSecuritySettingsUseCase
import com.vaultstadio.app.domain.usecase.auth.RevokeSessionUseCase
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
    var result: ApiResult<List<ActiveSession>> = ApiResult.success(emptyList()),
) : GetActiveSessionsUseCase {
    override suspend fun invoke(): ApiResult<List<ActiveSession>> = result
}

private class FakeGetLoginHistoryUseCase(
    var result: ApiResult<List<LoginEvent>> = ApiResult.success(emptyList()),
) : GetLoginHistoryUseCase {
    override suspend fun invoke(): ApiResult<List<LoginEvent>> = result
}

private class FakeGetSecuritySettingsUseCase(
    var result: ApiResult<SecuritySettings> = ApiResult.success(
        SecuritySettings(twoFactorEnabled = false, twoFactorMethod = null),
    ),
) : GetSecuritySettingsUseCase {
    override suspend fun invoke(): ApiResult<SecuritySettings> = result
}

private class FakeRevokeSessionUseCase(
    var result: ApiResult<Unit> = ApiResult.success(Unit),
) : RevokeSessionUseCase {
    override suspend fun invoke(sessionId: String): ApiResult<Unit> = result
}

class SecurityViewModelTest {

    private fun createViewModel(): SecurityViewModel = SecurityViewModel(
        getActiveSessionsUseCase = FakeGetActiveSessionsUseCase(),
        getLoginHistoryUseCase = FakeGetLoginHistoryUseCase(),
        getSecuritySettingsUseCase = FakeGetSecuritySettingsUseCase(),
        revokeSessionUseCase = FakeRevokeSessionUseCase(),
    )

    @Test
    fun showRevokeDialog_setsSession() {
        val vm = createViewModel()
        assertNull(vm.showRevokeSessionDialog)
        val session = testSession("s99")
        vm.showRevokeDialog(session)
        assertEquals(session, vm.showRevokeSessionDialog)
    }

    @Test
    fun dismissRevokeDialog_clearsSession() {
        val vm = createViewModel()
        vm.showRevokeDialog(testSession())
        vm.dismissRevokeDialog()
        assertNull(vm.showRevokeSessionDialog)
    }

    @Test
    fun dismissError_clearsErrorMessage() {
        val vm = createViewModel()
        vm.toggleTwoFactor()
        assertTrue(vm.errorMessage != null)
        vm.dismissError()
        assertNull(vm.errorMessage)
    }

    @Test
    fun toggleTwoFactor_setsNotAvailableMessage() {
        val vm = createViewModel()
        vm.toggleTwoFactor()
        assertEquals("Two-factor authentication setup is not yet available", vm.errorMessage)
    }
}
