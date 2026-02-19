/**
 * VaultStadio AppViewModel Tests
 */

package com.vaultstadio.app.viewmodel

import com.vaultstadio.app.domain.storage.model.ViewMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NavDestinationTest {

    @Test
    fun shouldHaveAllDestinations() {
        val destinations = NavDestination.entries

        // Core + User + Admin + Advanced + Account & Security = 19
        assertEquals(19, destinations.size)
        // Core navigation
        assertTrue(destinations.contains(NavDestination.FILES))
        assertTrue(destinations.contains(NavDestination.RECENT))
        assertTrue(destinations.contains(NavDestination.STARRED))
        assertTrue(destinations.contains(NavDestination.SHARED))
        assertTrue(destinations.contains(NavDestination.SHARED_WITH_ME))
        assertTrue(destinations.contains(NavDestination.TRASH))
        // User settings
        assertTrue(destinations.contains(NavDestination.SETTINGS))
        assertTrue(destinations.contains(NavDestination.PROFILE))
        // Admin
        assertTrue(destinations.contains(NavDestination.ADMIN))
        assertTrue(destinations.contains(NavDestination.ACTIVITY))
        assertTrue(destinations.contains(NavDestination.PLUGINS))
        // Advanced features
        assertTrue(destinations.contains(NavDestination.AI))
        assertTrue(destinations.contains(NavDestination.SYNC))
        assertTrue(destinations.contains(NavDestination.FEDERATION))
        assertTrue(destinations.contains(NavDestination.COLLABORATION))
        assertTrue(destinations.contains(NavDestination.VERSION_HISTORY))
    }

    @Test
    fun filesIsFirstDestination() {
        assertEquals(NavDestination.FILES, NavDestination.entries.first())
    }
}

class ViewModeTest {

    @Test
    fun shouldHaveGridAndListModes() {
        val viewModes = ViewMode.entries

        assertEquals(2, viewModes.size)
        assertTrue(viewModes.contains(ViewMode.GRID))
        assertTrue(viewModes.contains(ViewMode.LIST))
    }
}

// Note: Full ViewModel tests require mocking which isn't available in commonTest
// These are basic structure tests - integration tests would be in platform-specific test sources

class AppViewModelStateTest {

    @Test
    fun navigationDestinationsAreCorrect() {
        // Verify all expected navigation destinations exist
        val expectedDestinations = listOf(
            "FILES", "RECENT", "STARRED", "SHARED", "SHARED_WITH_ME", "TRASH",
            "SETTINGS", "PROFILE",
            "ADMIN", "ACTIVITY", "PLUGINS",
            "AI", "SYNC", "FEDERATION", "COLLABORATION", "VERSION_HISTORY",
            "CHANGE_PASSWORD", "SECURITY", "LICENSES",
        )

        val actualDestinations = NavDestination.entries.map { it.name }

        assertEquals(expectedDestinations.sorted(), actualDestinations.sorted())
    }
}
