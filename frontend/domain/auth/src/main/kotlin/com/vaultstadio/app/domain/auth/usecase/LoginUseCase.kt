/**
 * Login Use Case
 */

package com.vaultstadio.app.domain.auth.usecase

import com.vaultstadio.app.domain.auth.model.LoginResult
import com.vaultstadio.app.domain.result.Result

/**
 * Use case for user login.
 */
interface LoginUseCase {
    suspend operator fun invoke(email: String, password: String): Result<LoginResult>
}
