/**
 * Revoke Session Use Case
 */

package com.vaultstadio.app.domain.auth.usecase

import com.vaultstadio.app.domain.result.Result

/**
 * Use case for revoking a session (logging out a device).
 */
interface RevokeSessionUseCase {
    suspend operator fun invoke(sessionId: String): Result<Unit>
}
