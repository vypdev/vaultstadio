/**
 * Update Profile Use Case
 */

package com.vaultstadio.app.domain.auth.usecase

import com.vaultstadio.app.domain.auth.model.User
import com.vaultstadio.app.domain.result.Result

/**
 * Use case for updating the current user's profile.
 */
interface UpdateProfileUseCase {
    suspend operator fun invoke(username: String?, avatarUrl: String?): Result<User>
}
