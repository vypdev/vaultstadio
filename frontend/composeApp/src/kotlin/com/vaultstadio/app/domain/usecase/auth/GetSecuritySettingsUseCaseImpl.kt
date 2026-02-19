package com.vaultstadio.app.domain.usecase.auth

import com.vaultstadio.app.domain.auth.model.SecuritySettings
import com.vaultstadio.app.domain.auth.usecase.GetSecuritySettingsUseCase
import com.vaultstadio.app.domain.result.Result

class GetSecuritySettingsUseCaseImpl : GetSecuritySettingsUseCase {

    override suspend operator fun invoke(): Result<SecuritySettings> =
        Result.success(
            SecuritySettings(
                twoFactorEnabled = false,
                twoFactorMethod = null,
            ),
        )
}
