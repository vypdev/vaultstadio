/**
 * Unit tests for admin domain models: AdminUser.
 */

package com.vaultstadio.app.domain.admin

import com.vaultstadio.app.domain.admin.model.AdminUser
import com.vaultstadio.app.domain.admin.model.UserStatus
import com.vaultstadio.app.domain.auth.model.UserRole
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.datetime.Instant

class AdminUserTest {

    private val testInstant = Instant.fromEpochMilliseconds(0L)

    @Test
    fun adminUser_construction() {
        val user = AdminUser(
            id = "u1",
            email = "admin@test.com",
            username = "admin",
            role = UserRole.ADMIN,
            status = UserStatus.ACTIVE,
            avatarUrl = null,
            quotaBytes = 1_000_000L,
            usedBytes = 500_000L,
            createdAt = testInstant,
            lastLoginAt = testInstant,
        )
        assertEquals("u1", user.id)
        assertEquals("admin@test.com", user.email)
        assertEquals(UserRole.ADMIN, user.role)
        assertEquals(UserStatus.ACTIVE, user.status)
        assertEquals(1_000_000L, user.quotaBytes)
        assertEquals(500_000L, user.usedBytes)
        assertNull(user.avatarUrl)
    }

    @Test
    fun adminUser_usagePercentageWhenQuotaSet() {
        val user = AdminUser(
            id = "u2",
            email = "u@t.com",
            username = "user",
            role = UserRole.USER,
            status = UserStatus.ACTIVE,
            avatarUrl = null,
            quotaBytes = 1000L,
            usedBytes = 250L,
            createdAt = testInstant,
            lastLoginAt = null,
        )
        assertEquals(25.0, user.usagePercentage)
    }

    @Test
    fun adminUser_usagePercentageWhenQuotaNull() {
        val user = AdminUser(
            id = "u3",
            email = "u3@t.com",
            username = "u3",
            role = UserRole.USER,
            status = UserStatus.ACTIVE,
            avatarUrl = null,
            quotaBytes = null,
            usedBytes = 100L,
            createdAt = testInstant,
            lastLoginAt = null,
        )
        assertEquals(0.0, user.usagePercentage)
    }

    @Test
    fun adminUser_withAvatarUrlAndLastLoginAt() {
        val lastLogin = Instant.fromEpochMilliseconds(1000L)
        val user = AdminUser(
            id = "u4",
            email = "u4@t.com",
            username = "u4",
            role = UserRole.USER,
            status = UserStatus.ACTIVE,
            avatarUrl = "https://cdn.example.com/avatar.png",
            quotaBytes = 2000L,
            usedBytes = 0L,
            createdAt = testInstant,
            lastLoginAt = lastLogin,
        )
        assertEquals("https://cdn.example.com/avatar.png", user.avatarUrl)
        assertEquals(lastLogin, user.lastLoginAt)
    }
}
