/**
 * Change Password Use Case
 */

package com.vaultstadio.app.domain.auth.usecase

import com.vaultstadio.app.domain.result.Result

/**
 * Use case for changing the current user's password.
 */
interface ChangePasswordUseCase {
    suspend operator fun invoke(currentPassword: String, newPassword: String): Result<Unit>
}
