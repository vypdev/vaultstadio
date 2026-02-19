/**
 * Unit tests for Breadcrumbs component logic (display name, home, last, clickable).
 * The actual UI is in Breadcrumbs.kt; this tests the rules used there without Compose.
 */

package com.vaultstadio.app.ui.components.layout

import com.vaultstadio.app.domain.storage.model.Breadcrumb
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BreadcrumbsLogicTest {

    private fun displayName(breadcrumb: Breadcrumb, homeLabel: String): String =
        if (breadcrumb.id == null) homeLabel else breadcrumb.name

    private fun isHome(breadcrumb: Breadcrumb): Boolean = breadcrumb.id == null

    private fun isLast(index: Int, size: Int): Boolean = index == size - 1

    private fun isClickable(index: Int, size: Int): Boolean = !isLast(index, size)

    @Test
    fun breadcrumb_idNull_isHome() {
        val home = Breadcrumb(id = null, name = "Home", path = "/")
        assertTrue(isHome(home))
    }

    @Test
    fun breadcrumb_idNotNull_isNotHome() {
        val folder = Breadcrumb(id = "f1", name = "Documents", path = "/Documents")
        assertFalse(isHome(folder))
    }

    @Test
    fun displayName_homeUsesHomeLabel() {
        val homeLabel = "Home"
        val home = Breadcrumb(id = null, name = "Root", path = "/")
        assertEquals(homeLabel, displayName(home, homeLabel))
    }

    @Test
    fun displayName_nonHomeUsesBreadcrumbName() {
        val folder = Breadcrumb(id = "f1", name = "My Folder", path = "/My Folder")
        assertEquals("My Folder", displayName(folder, "Home"))
    }

    @Test
    fun isLast_firstOfOne() {
        assertTrue(isLast(0, 1))
    }

    @Test
    fun isLast_lastOfMany() {
        assertTrue(isLast(2, 3))
    }

    @Test
    fun isLast_notLastWhenMoreItems() {
        assertFalse(isLast(0, 3))
        assertFalse(isLast(1, 3))
    }

    @Test
    fun isClickable_lastItemNotClickable() {
        assertFalse(isClickable(0, 1))
        assertFalse(isClickable(2, 3))
    }

    @Test
    fun isClickable_nonLastClickable() {
        assertTrue(isClickable(0, 3))
        assertTrue(isClickable(1, 3))
    }

    @Test
    fun breadcrumb_holdsIdNamePath() {
        val b = Breadcrumb(id = "id1", name = "Folder", path = "/a/b")
        assertEquals("id1", b.id)
        assertEquals("Folder", b.name)
        assertEquals("/a/b", b.path)
    }
}
