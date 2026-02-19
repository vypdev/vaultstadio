/**
 * Get Active Sessions Use Case
 */

package com.vaultstadio.app.domain.auth.usecase

import com.vaultstadio.app.domain.auth.model.ActiveSession
import com.vaultstadio.app.domain.result.Result

/**
 * Use case for getting active sessions (connected devices).
 */
interface GetActiveSessionsUseCase {
    suspend operator fun invoke(): Result<List<ActiveSession>>
}
