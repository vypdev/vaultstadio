/**
 * Unit tests for [User] (sanitized) and auth domain models.
 */

package com.vaultstadio.domain.auth.model

import kotlinx.datetime.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class UserTest {

    private val now = Instant.fromEpochMilliseconds(1_600_000_000_000)

    @Test
    fun sanitizedExcludesSensitiveFields() {
        val user = User(
            id = "user-1",
            email = "u@example.com",
            username = "user1",
            passwordHash = "secret-hash",
            role = UserRole.USER,
            status = UserStatus.ACTIVE,
            quotaBytes = 1_000_000L,
            avatarUrl = null,
            preferences = "{}",
            lastLoginAt = now,
            createdAt = now,
            updatedAt = now,
        )
        val info = user.sanitized()
        assertEquals("user-1", info.id)
        assertEquals("u@example.com", info.email)
        assertEquals("user1", info.username)
        assertEquals(UserRole.USER, info.role)
        assertEquals(UserStatus.ACTIVE, info.status)
        assertEquals(1_000_000L, info.quotaBytes)
        assertNull(info.avatarUrl)
        assertEquals(now, info.createdAt)
    }

    @Test
    fun userRoleAndStatusEnums() {
        assertEquals(UserRole.ADMIN, UserRole.entries.first { it == UserRole.ADMIN })
        assertEquals(UserStatus.ACTIVE, UserStatus.entries.first { it == UserStatus.ACTIVE })
    }
}
