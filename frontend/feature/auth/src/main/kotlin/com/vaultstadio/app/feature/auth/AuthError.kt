/**
 * Validation error types for authentication.
 */

package com.vaultstadio.app.feature.auth

sealed class AuthError {
    data object EmailPasswordRequired : AuthError()
    data object AllFieldsRequired : AuthError()
    data object PasswordsDoNotMatch : AuthError()
    data object PasswordTooShort : AuthError()
    data class ApiError(val message: String) : AuthError()
}
