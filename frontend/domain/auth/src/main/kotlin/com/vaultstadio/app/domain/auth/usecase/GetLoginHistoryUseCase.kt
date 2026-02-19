/**
 * Get Login History Use Case
 */

package com.vaultstadio.app.domain.auth.usecase

import com.vaultstadio.app.domain.auth.model.LoginEvent
import com.vaultstadio.app.domain.result.Result

/**
 * Use case for getting login history.
 */
interface GetLoginHistoryUseCase {
    suspend operator fun invoke(): Result<List<LoginEvent>>
}
