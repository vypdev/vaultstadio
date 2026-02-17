/**
 * Revoke Session Use Case
 */

package com.vaultstadio.app.domain.usecase.auth

import com.vaultstadio.app.data.network.ApiResult
import org.koin.core.annotation.Factory

/**
 * Use case for revoking a session (logging out a device).
 *
 * Note: Backend endpoint not yet implemented. Returns error.
 */
interface RevokeSessionUseCase {
    suspend operator fun invoke(sessionId: String): ApiResult<Unit>
}

@Factory(binds = [RevokeSessionUseCase::class])
class RevokeSessionUseCaseImpl : RevokeSessionUseCase {

    override suspend operator fun invoke(sessionId: String): ApiResult<Unit> {
        // Backend endpoint not yet implemented
        return ApiResult.Error(
            code = "NOT_IMPLEMENTED",
            message = "Session management not yet available",
        )
    }
}
