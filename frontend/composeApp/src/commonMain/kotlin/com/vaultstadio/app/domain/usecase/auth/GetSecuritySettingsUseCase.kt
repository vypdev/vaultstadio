/**
 * Get Security Settings Use Case
 */

package com.vaultstadio.app.domain.usecase.auth

import com.vaultstadio.app.data.network.ApiResult
import com.vaultstadio.app.domain.model.SecuritySettings
import org.koin.core.annotation.Factory

/**
 * Use case for getting user security settings (2FA, etc.).
 *
 * Note: Backend endpoint not yet implemented. Returns default settings.
 */
interface GetSecuritySettingsUseCase {
    suspend operator fun invoke(): ApiResult<SecuritySettings>
}

@Factory(binds = [GetSecuritySettingsUseCase::class])
class GetSecuritySettingsUseCaseImpl : GetSecuritySettingsUseCase {

    override suspend operator fun invoke(): ApiResult<SecuritySettings> {
        // Backend endpoint not yet implemented
        // Return default settings - 2FA not enabled
        return ApiResult.Success(
            SecuritySettings(
                twoFactorEnabled = false,
                twoFactorMethod = null,
            ),
        )
    }
}
