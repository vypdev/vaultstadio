/**
 * Get Security Settings Use Case
 */

package com.vaultstadio.app.domain.auth.usecase

import com.vaultstadio.app.domain.auth.model.SecuritySettings
import com.vaultstadio.app.domain.result.Result

/**
 * Use case for getting user security settings (2FA, etc.).
 */
interface GetSecuritySettingsUseCase {
    suspend operator fun invoke(): Result<SecuritySettings>
}
