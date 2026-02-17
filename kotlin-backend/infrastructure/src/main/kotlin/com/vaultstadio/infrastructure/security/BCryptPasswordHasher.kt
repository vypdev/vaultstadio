/**
 * VaultStadio BCrypt Password Hasher
 */

package com.vaultstadio.infrastructure.security

import at.favre.lib.crypto.bcrypt.BCrypt
import com.vaultstadio.core.domain.service.PasswordHasher

/**
 * BCrypt implementation of password hashing.
 */
class BCryptPasswordHasher(
    private val cost: Int = 12,
) : PasswordHasher {

    override fun hash(password: String): String {
        return BCrypt.withDefaults().hashToString(cost, password.toCharArray())
    }

    override fun verify(password: String, hash: String): Boolean {
        return BCrypt.verifyer().verify(password.toCharArray(), hash).verified
    }
}
