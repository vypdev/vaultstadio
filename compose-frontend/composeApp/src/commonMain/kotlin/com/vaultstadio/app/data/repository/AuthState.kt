/**
 * Auth state for UI and tests.
 *
 * Represents the authentication state of the app (loading, unauthenticated, authenticated, error).
 * Used by tests and can be used by UI that needs a simple auth state representation.
 */
package com.vaultstadio.app.data.repository

sealed class AuthState {
    data object Loading : AuthState()
    data object Unauthenticated : AuthState()
    data class Authenticated(val userId: String, val token: String) : AuthState()
    data class Error(val message: String) : AuthState()
}
