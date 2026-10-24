/**
 * Get Quota Use Case
 */

package com.vaultstadio.app.domain.auth.usecase

import com.vaultstadio.app.domain.auth.model.StorageQuota
import com.vaultstadio.app.domain.result.Result

/**
 * Use case for getting the current user's storage quota.
 */
interface GetQuotaUseCase {
    suspend operator fun invoke(): Result<StorageQuota>
}
