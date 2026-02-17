/**
 * Unit tests for declarative routing (AppRoutes, RoutePaths, AppRoute).
 */

package com.vaultstadio.app.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for [AppRoute] data class and route definitions.
 */
class AppRouteTest {

    @Test
    fun `AppRoute holds destination path and parent`() {
        val route = AppRoute(
            destination = MainDestination.SETTINGS,
            path = "settings",
            parent = MainDestination.FILES,
        )
        assertEquals(MainDestination.SETTINGS, route.destination)
        assertEquals("settings", route.path)
        assertEquals(MainDestination.FILES, route.parent)
    }

    @Test
    fun `root route has null parent`() {
        val rootRoute = AppRoutes.all.find { it.destination == MainDestination.FILES }
        assertTrue(rootRoute != null)
        assertNull(rootRoute!!.parent)
        assertTrue(rootRoute.path == "files")
    }

    @Test
    fun `every MainDestination has exactly one AppRoute`() {
        val destinations = MainDestination.entries
        val routesByDest = AppRoutes.all.groupBy { it.destination }
        destinations.forEach { dest ->
            val list = routesByDest[dest]
            assertTrue(list != null && list.size == 1, "Missing or duplicate route for $dest")
        }
    }

    @Test
    fun `parent references point to existing destinations`() {
        AppRoutes.all.forEach { route ->
            route.parent?.let { parent ->
                assertTrue(
                    AppRoutes.all.any { it.destination == parent },
                    "Parent $parent of ${route.destination} is not in route list",
                )
            }
        }
    }
}

/**
 * Tests for [AppRoutes.fullPath] and [AppRoutes.toPath].
 */
class AppRoutesFullPathTest {

    @Test
    fun `fullPath of FILES is files base path`() {
        assertEquals("/files", AppRoutes.fullPath(MainDestination.FILES))
    }

    @Test
    fun `toPath of FILES is canonical files path`() {
        assertEquals("/files", AppRoutes.toPath(MainDestination.FILES))
        assertEquals(RoutePaths.DEFAULT_MAIN_PATH, AppRoutes.toPath(MainDestination.FILES))
    }

    @Test
    fun `toPath with path params appends segments`() {
        assertEquals(
            "/files/my_folder/other_folder/my-song.mp3",
            AppRoutes.toPath(
                MainDestination.FILES,
                mapOf("path" to listOf("my_folder", "other_folder", "my-song.mp3")),
            ),
        )
    }

    @Test
    fun `fullPath of top-level destinations is segment under root`() {
        assertEquals("/settings", AppRoutes.fullPath(MainDestination.SETTINGS))
        assertEquals("/profile", AppRoutes.fullPath(MainDestination.PROFILE))
        assertEquals("/recent", AppRoutes.fullPath(MainDestination.RECENT))
        assertEquals("/admin", AppRoutes.fullPath(MainDestination.ADMIN))
    }

    @Test
    fun `fullPath of nested destinations includes parent path`() {
        assertEquals("/settings/change-password", AppRoutes.fullPath(MainDestination.CHANGE_PASSWORD))
        assertEquals("/settings/licenses", AppRoutes.fullPath(MainDestination.LICENSES))
        assertEquals("/profile/security", AppRoutes.fullPath(MainDestination.SECURITY))
    }

    @Test
    fun `toPath equals fullPath for non-root destinations`() {
        assertEquals(AppRoutes.fullPath(MainDestination.SETTINGS), AppRoutes.toPath(MainDestination.SETTINGS))
        assertEquals(
            AppRoutes.fullPath(MainDestination.CHANGE_PASSWORD),
            AppRoutes.toPath(MainDestination.CHANGE_PASSWORD),
        )
    }
}

/**
 * Tests for [AppRoutes.fromPath] (path string -> destination).
 */
class AppRoutesFromPathTest {

    @Test
    fun `fromPath accepts root variants as FILES`() {
        assertEquals(MainDestination.FILES, AppRoutes.fromPath(""))
        assertEquals(MainDestination.FILES, AppRoutes.fromPath("/"))
        assertEquals(MainDestination.FILES, AppRoutes.fromPath("/files"))
    }

    @Test
    fun `fromPath normalizes trailing slash and case`() {
        assertEquals(MainDestination.SETTINGS, AppRoutes.fromPath("/settings/"))
        assertEquals(MainDestination.SETTINGS, AppRoutes.fromPath("  /SETTINGS  "))
    }

    @Test
    fun `fromPath resolves top-level routes`() {
        assertEquals(MainDestination.SETTINGS, AppRoutes.fromPath("/settings"))
        assertEquals(MainDestination.PROFILE, AppRoutes.fromPath("/profile"))
        assertEquals(MainDestination.RECENT, AppRoutes.fromPath("/recent"))
        assertEquals(MainDestination.SHARED_WITH_ME, AppRoutes.fromPath("/shared-with-me"))
    }

    @Test
    fun `fromPath resolves nested routes`() {
        assertEquals(MainDestination.CHANGE_PASSWORD, AppRoutes.fromPath("/settings/change-password"))
        assertEquals(MainDestination.LICENSES, AppRoutes.fromPath("/settings/licenses"))
        assertEquals(MainDestination.SECURITY, AppRoutes.fromPath("/profile/security"))
    }

    @Test
    fun `fromPath returns null for unknown path`() {
        assertNull(AppRoutes.fromPath("/unknown"))
        assertNull(AppRoutes.fromPath("/settings/unknown"))
    }

    @Test
    fun `parseRoute returns path params for files path`() {
        val m1 = AppRoutes.parseRoute("/files")
        assertTrue(m1 != null)
        assertEquals(MainDestination.FILES, m1!!.destination)
        assertTrue(m1.pathSegments().isEmpty())

        val m2 = AppRoutes.parseRoute("/files/my_folder/other_folder/my-song.mp3")
        assertTrue(m2 != null)
        assertEquals(MainDestination.FILES, m2!!.destination)
        assertEquals(listOf("my_folder", "other_folder", "my-song.mp3"), m2.pathSegments())
    }

    @Test
    fun `toPath then fromPath round-trips for every destination`() {
        MainDestination.entries.forEach { dest ->
            val path = AppRoutes.toPath(dest)
            val back = AppRoutes.fromPath(path)
            assertEquals(dest, back, "Round-trip failed for $dest: toPath=$path, fromPath=$back")
        }
    }
}

/**
 * Tests for [AppRoutes.getStack] (path -> navigation stack for deep links).
 */
class AppRoutesGetStackTest {

    @Test
    fun `getStack for root returns single FILES`() {
        assertEquals(listOf(MainDestination.FILES), AppRoutes.getStack(""))
        assertEquals(listOf(MainDestination.FILES), AppRoutes.getStack("/"))
        assertEquals(listOf(MainDestination.FILES), AppRoutes.getStack("/files"))
    }

    @Test
    fun `getStack for top-level destination returns FILES then destination`() {
        assertEquals(listOf(MainDestination.FILES, MainDestination.SETTINGS), AppRoutes.getStack("/settings"))
        assertEquals(listOf(MainDestination.FILES, MainDestination.PROFILE), AppRoutes.getStack("/profile"))
    }

    @Test
    fun `getStack for nested destination returns full ancestry`() {
        assertEquals(
            listOf(MainDestination.FILES, MainDestination.SETTINGS, MainDestination.CHANGE_PASSWORD),
            AppRoutes.getStack("/settings/change-password"),
        )
        assertEquals(
            listOf(MainDestination.FILES, MainDestination.SETTINGS, MainDestination.LICENSES),
            AppRoutes.getStack("/settings/licenses"),
        )
        assertEquals(
            listOf(MainDestination.FILES, MainDestination.PROFILE, MainDestination.SECURITY),
            AppRoutes.getStack("/profile/security"),
        )
    }

    @Test
    fun `getStack for unknown path returns default FILES only`() {
        assertEquals(listOf(MainDestination.FILES), AppRoutes.getStack("/unknown"))
    }
}

/**
 * Tests for [AppRoutes.isAuthPath].
 */
class AppRoutesAuthPathTest {

    @Test
    fun `isAuthPath returns true for login and auth paths`() {
        assertTrue(AppRoutes.isAuthPath("/login"))
        assertTrue(AppRoutes.isAuthPath("/auth"))
        assertTrue(AppRoutes.isAuthPath("  /login  "))
        assertTrue(AppRoutes.isAuthPath("/LOGIN"))
    }

    @Test
    fun `isAuthPath returns false for main area paths`() {
        assertFalse(AppRoutes.isAuthPath("/"))
        assertFalse(AppRoutes.isAuthPath("/files"))
        assertFalse(AppRoutes.isAuthPath("/settings"))
    }
}

/**
 * Tests for [RoutePaths] delegation to [AppRoutes].
 */
class RoutePathsTest {

    @Test
    fun `RoutePaths constants match AppRoutes`() {
        assertEquals(AppRoutes.AUTH_PATH, RoutePaths.AUTH_PATH)
        assertEquals(AppRoutes.DEFAULT_MAIN_PATH, RoutePaths.DEFAULT_MAIN_PATH)
    }

    @Test
    fun `RoutePaths toPath delegates to AppRoutes`() {
        MainDestination.entries.forEach { dest ->
            assertEquals(AppRoutes.toPath(dest), RoutePaths.toPath(dest))
        }
    }

    @Test
    fun `RoutePaths fromPath delegates to AppRoutes`() {
        listOf("", "/", "/files", "/settings", "/settings/change-password").forEach { path ->
            assertEquals(AppRoutes.fromPath(path), RoutePaths.fromPath(path))
        }
    }

    @Test
    fun `RoutePaths getStack delegates to AppRoutes`() {
        assertEquals(AppRoutes.getStack("/settings"), RoutePaths.getStack("/settings"))
        assertEquals(AppRoutes.getStack("/settings/change-password"), RoutePaths.getStack("/settings/change-password"))
    }

    @Test
    fun `RoutePaths isAuthPath delegates to AppRoutes`() {
        assertTrue(RoutePaths.isAuthPath("/login"))
        assertFalse(RoutePaths.isAuthPath("/settings"))
    }
}
