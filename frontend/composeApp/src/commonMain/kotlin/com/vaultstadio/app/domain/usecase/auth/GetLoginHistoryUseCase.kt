/**
 * Get Login History Use Case
 */

package com.vaultstadio.app.domain.usecase.auth

import com.vaultstadio.app.data.network.ApiResult
import com.vaultstadio.app.domain.model.LoginEvent
import org.koin.core.annotation.Factory

/**
 * Use case for getting login history.
 *
 * Note: Backend endpoint not yet implemented. Returns empty list.
 */
interface GetLoginHistoryUseCase {
    suspend operator fun invoke(): ApiResult<List<LoginEvent>>
}

@Factory(binds = [GetLoginHistoryUseCase::class])
class GetLoginHistoryUseCaseImpl : GetLoginHistoryUseCase {

    override suspend operator fun invoke(): ApiResult<List<LoginEvent>> {
        // Backend endpoint not yet implemented
        // Return empty list - UI will show "No login history available"
        return ApiResult.Success(emptyList())
    }
}
