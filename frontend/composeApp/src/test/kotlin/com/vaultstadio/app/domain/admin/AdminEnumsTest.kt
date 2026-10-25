/**
 * Unit tests for admin domain enums: UserStatus.
 */

package com.vaultstadio.app.domain.admin

import com.vaultstadio.app.domain.admin.model.UserStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UserStatusTest {

    @Test
    fun userStatus_hasExpectedValues() {
        val values = UserStatus.entries
        assertTrue(UserStatus.ACTIVE in values)
        assertTrue(UserStatus.INACTIVE in values)
        assertTrue(UserStatus.SUSPENDED in values)
        assertEquals(3, values.size)
    }

    @Test
    fun userStatus_names() {
        assertEquals("ACTIVE", UserStatus.ACTIVE.name)
        assertEquals("INACTIVE", UserStatus.INACTIVE.name)
        assertEquals("SUSPENDED", UserStatus.SUSPENDED.name)
    }
}
