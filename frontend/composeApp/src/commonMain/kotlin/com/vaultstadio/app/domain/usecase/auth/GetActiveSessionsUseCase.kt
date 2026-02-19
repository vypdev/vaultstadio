/**
 * Get Active Sessions Use Case
 */

package com.vaultstadio.app.domain.usecase.auth

import com.vaultstadio.app.data.network.ApiResult
import com.vaultstadio.app.domain.model.ActiveSession
import org.koin.core.annotation.Factory

/**
 * Use case for getting active sessions (connected devices).
 *
 * Note: Backend endpoint not yet implemented. Returns current session only.
 */
interface GetActiveSessionsUseCase {
    suspend operator fun invoke(): ApiResult<List<ActiveSession>>
}

@Factory(binds = [GetActiveSessionsUseCase::class])
class GetActiveSessionsUseCaseImpl : GetActiveSessionsUseCase {

    override suspend operator fun invoke(): ApiResult<List<ActiveSession>> {
        // Backend endpoint not yet implemented
        // Return empty list - UI will show "No other sessions"
        return ApiResult.Success(emptyList())
    }
}
