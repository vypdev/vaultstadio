/**
 * Use case for setting user offline.
 */

package com.vaultstadio.app.domain.collaboration.usecase

import com.vaultstadio.app.domain.result.Result

interface SetOfflineUseCase {
    suspend operator fun invoke(): Result<Unit>
}
