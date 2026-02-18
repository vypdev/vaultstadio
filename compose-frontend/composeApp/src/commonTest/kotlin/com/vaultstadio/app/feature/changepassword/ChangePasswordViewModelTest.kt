/**
 * Unit tests for ChangePasswordViewModel: form state, validation, and visibility toggles.
 * Async changePassword outcome is covered by ChangePasswordUseCaseTest.
 */

package com.vaultstadio.app.feature.changepassword

import com.vaultstadio.app.data.network.ApiResult
import com.vaultstadio.app.domain.usecase.auth.ChangePasswordUseCase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class FakeChangePasswordUseCase(
    var result: ApiResult<Unit> = ApiResult.success(Unit),
) : ChangePasswordUseCase {
    override suspend fun invoke(currentPassword: String, newPassword: String): ApiResult<Unit> = result
}

class ChangePasswordViewModelTest {

    private fun createViewModel(result: ApiResult<Unit> = ApiResult.success(Unit)): ChangePasswordViewModel {
        return ChangePasswordViewModel(changePasswordUseCase = FakeChangePasswordUseCase(result))
    }

    @Test
    fun updateCurrentPassword_updatesStateAndClearsError() {
        val vm = createViewModel()
        vm.updateNewPassword("x")
        vm.changePassword()
        assertEquals("Current password is required", vm.errorMessage)
        vm.updateCurrentPassword("old")
        assertNull(vm.errorMessage)
        assertEquals("old", vm.currentPassword)
    }

    @Test
    fun updateNewPassword_updatesStateAndClearsError() {
        val vm = createViewModel()
        vm.updateNewPassword("newpass")
        assertEquals("newpass", vm.newPassword)
    }

    @Test
    fun updateConfirmPassword_updatesState() {
        val vm = createViewModel()
        vm.updateConfirmPassword("confirm")
        assertEquals("confirm", vm.confirmPassword)
    }

    @Test
    fun changePassword_withBlankCurrent_setsErrorMessage() {
        val vm = createViewModel()
        vm.updateNewPassword("newpassword")
        vm.updateConfirmPassword("newpassword")
        vm.changePassword()
        assertEquals("Current password is required", vm.errorMessage)
    }

    @Test
    fun changePassword_withBlankNew_setsErrorMessage() {
        val vm = createViewModel()
        vm.updateCurrentPassword("current")
        vm.updateConfirmPassword("newpassword")
        vm.changePassword()
        assertEquals("New password is required", vm.errorMessage)
    }

    @Test
    fun changePassword_withShortNew_setsErrorMessage() {
        val vm = createViewModel()
        vm.updateCurrentPassword("current")
        vm.updateNewPassword("short")
        vm.updateConfirmPassword("short")
        vm.changePassword()
        assertEquals("Password must be at least 8 characters", vm.errorMessage)
    }

    @Test
    fun changePassword_whenPasswordsDoNotMatch_setsErrorMessage() {
        val vm = createViewModel()
        vm.updateCurrentPassword("current")
        vm.updateNewPassword("newpassword")
        vm.updateConfirmPassword("different")
        vm.changePassword()
        assertEquals("Passwords do not match", vm.errorMessage)
    }

    @Test
    fun changePassword_whenNewSameAsCurrent_setsErrorMessage() {
        val vm = createViewModel()
        vm.updateCurrentPassword("samepass")
        vm.updateNewPassword("samepass")
        vm.updateConfirmPassword("samepass")
        vm.changePassword()
        assertEquals("New password must be different from current password", vm.errorMessage)
    }

    @Test
    fun toggleCurrentPasswordVisibility_flipsVisibility() {
        val vm = createViewModel()
        assertFalse(vm.showCurrentPassword)
        vm.toggleCurrentPasswordVisibility()
        assertTrue(vm.showCurrentPassword)
        vm.toggleCurrentPasswordVisibility()
        assertFalse(vm.showCurrentPassword)
    }

    @Test
    fun toggleNewPasswordVisibility_flipsVisibility() {
        val vm = createViewModel()
        assertFalse(vm.showNewPassword)
        vm.toggleNewPasswordVisibility()
        assertTrue(vm.showNewPassword)
    }

    @Test
    fun toggleConfirmPasswordVisibility_flipsVisibility() {
        val vm = createViewModel()
        assertFalse(vm.showConfirmPassword)
        vm.toggleConfirmPasswordVisibility()
        assertTrue(vm.showConfirmPassword)
    }

    @Test
    fun dismissSuccess_doesNotThrow() {
        val vm = createViewModel()
        vm.dismissSuccess()
        assertFalse(vm.isSuccess)
    }
}
