/**
 * Logout Use Case
 */

package com.vaultstadio.app.domain.auth.usecase

import com.vaultstadio.app.domain.result.Result

/**
 * Use case for user logout.
 */
interface LogoutUseCase {
    suspend operator fun invoke(): Result<Unit>
}
