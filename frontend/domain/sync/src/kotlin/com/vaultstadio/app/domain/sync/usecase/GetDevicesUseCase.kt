/**
 * Use case for getting registered sync devices.
 */

package com.vaultstadio.app.domain.sync.usecase

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.domain.sync.model.SyncDevice

interface GetDevicesUseCase {
    suspend operator fun invoke(activeOnly: Boolean = true): Result<List<SyncDevice>>
}
