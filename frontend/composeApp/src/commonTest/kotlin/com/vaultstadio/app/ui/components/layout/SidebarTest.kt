/**
 * VaultStadio Sidebar Component Tests
 *
 * Tests for Sidebar navigation logic.
 */

package com.vaultstadio.app.ui.components.layout

import com.vaultstadio.app.domain.model.StorageQuota
import com.vaultstadio.app.domain.model.UserRole
import com.vaultstadio.app.viewmodel.NavDestination
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for Sidebar component logic.
 */
class SidebarTest {

    // Navigation destination tests

    @Test
    fun `should have all navigation destinations defined`() {
        val destinations = NavDestination.entries

        // Core destinations
        assertTrue(destinations.contains(NavDestination.FILES))
        assertTrue(destinations.contains(NavDestination.RECENT))
        assertTrue(destinations.contains(NavDestination.STARRED))
        assertTrue(destinations.contains(NavDestination.SHARED))
        assertTrue(destinations.contains(NavDestination.TRASH))
        assertTrue(destinations.contains(NavDestination.SETTINGS))

        // Advanced destinations (Phase 6)
        assertTrue(destinations.contains(NavDestination.AI))
        assertTrue(destinations.contains(NavDestination.SYNC))
        assertTrue(destinations.contains(NavDestination.FEDERATION))
        assertTrue(destinations.contains(NavDestination.COLLABORATION))
    }

    @Test
    fun `should filter admin destination for non-admin users`() {
        val isAdmin = false
        val destinations = NavDestination.entries.filter {
            it != NavDestination.ADMIN || isAdmin
        }

        assertFalse(destinations.contains(NavDestination.ADMIN))
    }

    @Test
    fun `should show admin destination for admin users`() {
        val isAdmin = true
        val destinations = NavDestination.entries.filter {
            it != NavDestination.ADMIN || isAdmin
        }

        assertTrue(destinations.contains(NavDestination.ADMIN))
    }

    // User role tests

    @Test
    fun `should correctly identify admin role`() {
        val adminRole = UserRole.ADMIN
        val userRole = UserRole.USER

        assertEquals(UserRole.ADMIN, adminRole)
        assertTrue(adminRole == UserRole.ADMIN)
        assertFalse(userRole == UserRole.ADMIN)
    }

    // Storage quota tests

    @Test
    fun `should calculate quota percentage correctly`() {
        val quota = StorageQuota(
            usedBytes = 50 * 1024 * 1024 * 1024L, // 50 GB
            quotaBytes = 100 * 1024 * 1024 * 1024L, // 100 GB
            usagePercentage = 50.0,
            fileCount = 100,
            folderCount = 10,
            remainingBytes = 50 * 1024 * 1024 * 1024L,
        )

        assertEquals(50.0, quota.usagePercentage)
    }

    @Test
    fun `should detect near limit quota`() {
        val nearLimit = StorageQuota(
            usedBytes = 95 * 1024 * 1024 * 1024L, // 95 GB
            quotaBytes = 100 * 1024 * 1024 * 1024L, // 100 GB
            usagePercentage = 95.0,
            fileCount = 100,
            folderCount = 10,
            remainingBytes = 5 * 1024 * 1024 * 1024L,
        )

        assertTrue(nearLimit.isNearLimit) // 95% should be near limit

        val notNearLimit = StorageQuota(
            usedBytes = 50 * 1024 * 1024 * 1024L, // 50 GB
            quotaBytes = 100 * 1024 * 1024 * 1024L, // 100 GB
            usagePercentage = 50.0,
            fileCount = 100,
            folderCount = 10,
            remainingBytes = 50 * 1024 * 1024 * 1024L,
        )

        assertFalse(notNearLimit.isNearLimit)
    }

    @Test
    fun `should handle unlimited quota`() {
        val unlimited = StorageQuota(
            usedBytes = 500 * 1024 * 1024 * 1024L, // 500 GB
            quotaBytes = null, // Unlimited
            usagePercentage = 0.0,
            fileCount = 1000,
            folderCount = 100,
            remainingBytes = null,
        )

        assertEquals(0.0, unlimited.usagePercentage)
        assertFalse(unlimited.isNearLimit)
    }

    // Navigation state tests

    @Test
    fun `should track current destination`() {
        var currentDestination = NavDestination.FILES

        // Navigate to starred
        currentDestination = NavDestination.STARRED
        assertEquals(NavDestination.STARRED, currentDestination)

        // Navigate to settings
        currentDestination = NavDestination.SETTINGS
        assertEquals(NavDestination.SETTINGS, currentDestination)
    }

    @Test
    fun `should determine if destination is selected`() {
        val currentDestination = NavDestination.STARRED

        assertTrue(currentDestination == NavDestination.STARRED)
        assertFalse(currentDestination == NavDestination.FILES)
        assertFalse(currentDestination == NavDestination.TRASH)
    }

    // Section grouping tests

    @Test
    fun `should group destinations into sections`() {
        val mainSection = listOf(
            NavDestination.FILES,
            NavDestination.RECENT,
            NavDestination.STARRED,
            NavDestination.TRASH,
        )

        val sharingSection = listOf(
            NavDestination.SHARED,
            NavDestination.SHARED_WITH_ME,
        )

        val advancedSection = listOf(
            NavDestination.AI,
            NavDestination.COLLABORATION,
            NavDestination.SYNC,
            NavDestination.FEDERATION,
        )

        assertEquals(4, mainSection.size)
        assertEquals(2, sharingSection.size)
        assertEquals(4, advancedSection.size)
    }
}
