/**
 * Unit tests for [AdminUser] model.
 */

package com.vaultstadio.domain.admin.model

import com.vaultstadio.domain.auth.model.UserRole
import com.vaultstadio.domain.auth.model.UserStatus
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AdminUserTest {

    private val now = Instant.fromEpochMilliseconds(1_600_000_000_000)

    @Test
    fun adminUserHoldsUserSummaryFields() {
        val adminUser = AdminUser(
            id = "u1",
            email = "a@example.com",
            username = "admin1",
            role = UserRole.ADMIN,
            status = UserStatus.ACTIVE,
            quotaBytes = 1_000_000L,
            usedBytes = 500_000L,
            createdAt = now,
        )
        assertEquals("u1", adminUser.id)
        assertEquals("a@example.com", adminUser.email)
        assertEquals(UserRole.ADMIN, adminUser.role)
        assertEquals(UserStatus.ACTIVE, adminUser.status)
        assertEquals(500_000L, adminUser.usedBytes)
    }
}
