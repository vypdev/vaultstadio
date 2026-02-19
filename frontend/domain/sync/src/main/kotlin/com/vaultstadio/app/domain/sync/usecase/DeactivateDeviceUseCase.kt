/**
 * Use case for deactivating a sync device.
 */

package com.vaultstadio.app.domain.sync.usecase

import com.vaultstadio.app.domain.result.Result

interface DeactivateDeviceUseCase {
    suspend operator fun invoke(deviceId: String): Result<Unit>
}
