/**
 * Auth state for UI and tests.
 */

package com.vaultstadio.app.data.auth.repository

sealed class AuthState {
    data object Loading : AuthState()
    data object Unauthenticated : AuthState()
    data class Authenticated(val userId: String, val token: String) : AuthState()
    data class Error(val message: String) : AuthState()
}
