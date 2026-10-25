/**
 * Use case for removing a sync device.
 */

package com.vaultstadio.app.domain.sync.usecase

import com.vaultstadio.app.domain.result.Result

interface RemoveDeviceUseCase {
    suspend operator fun invoke(deviceId: String): Result<Unit>
}
