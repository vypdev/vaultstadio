/**
 * Callback interface for authentication success.
 * Used instead of lambda to avoid KSP code generation issues with Function types.
 */

package com.vaultstadio.app.feature.auth

fun interface AuthSuccessCallback {
    fun onSuccess()
}
