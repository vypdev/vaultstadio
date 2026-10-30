package com.vaultstadio.infrastructure.security

import com.vaultstadio.core.domain.service.PasswordHasher
import com.vaultstadio.domain.share.access.SharePasswordVerifier

class SharePasswordVerifierAdapter(private val hasher: PasswordHasher) : SharePasswordVerifier {
    override fun matches(password: String, hash: String): Boolean = try {
        hasher.verify(password, hash)
    } catch (_: IllegalArgumentException) {
        false
    }
}
