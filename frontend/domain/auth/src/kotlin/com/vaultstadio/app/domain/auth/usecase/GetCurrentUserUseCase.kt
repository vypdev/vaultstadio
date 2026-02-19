/**
 * Get Current User Use Case
 */

package com.vaultstadio.app.domain.auth.usecase

import com.vaultstadio.app.domain.auth.model.User
import com.vaultstadio.app.domain.result.Result
import kotlinx.coroutines.flow.StateFlow

/**
 * Use case for getting the current user.
 */
interface GetCurrentUserUseCase {
    val currentUserFlow: StateFlow<User?>
    suspend operator fun invoke(): Result<User>
    suspend fun refresh()
    fun isLoggedIn(): Boolean
}
