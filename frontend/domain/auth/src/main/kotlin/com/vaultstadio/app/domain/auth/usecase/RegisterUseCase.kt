/**
 * Register Use Case
 */

package com.vaultstadio.app.domain.auth.usecase

import com.vaultstadio.app.domain.auth.model.User
import com.vaultstadio.app.domain.result.Result

/**
 * Use case for user registration.
 */
interface RegisterUseCase {
    suspend operator fun invoke(email: String, username: String, password: String): Result<User>
}
