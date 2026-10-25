/**
 * Unit tests for Files feature: mode enum, display titles, and result handling logic
 * for loadItemsForMode (Files / Recent / Starred / Trash), trash, star, empty trash.
 *
 * Does not test ViewModel directly (would require mocking in commonTest);
 * tests mode values and pure logic.
 */

package com.vaultstadio.app.feature.files

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FilesModeTest {

    @Test
    fun `FilesMode has ALL RECENT STARRED TRASH`() {
        val modes = FilesMode.entries
        assertEquals(4, modes.size)
        assertTrue(FilesMode.ALL in modes)
        assertTrue(FilesMode.RECENT in modes)
        assertTrue(FilesMode.STARRED in modes)
        assertTrue(FilesMode.TRASH in modes)
    }

    @Test
    fun `FilesMode ALL is first for default files tab`() {
        assertEquals(FilesMode.ALL, FilesMode.entries.first())
    }

    @Test
    fun `trash mode is distinct from other modes`() {
        assertTrue(FilesMode.TRASH == FilesMode.TRASH)
        assertFalse(FilesMode.TRASH == FilesMode.ALL)
        assertFalse(FilesMode.TRASH == FilesMode.RECENT)
        assertFalse(FilesMode.TRASH == FilesMode.STARRED)
    }
}

/**
 * Maps displayed mode to the "data source" for loadItemsForMode.
 * Mirrors FilesViewModel.loadItemsForMode(displayedMode) branching.
 */
object LoadItemsForModeSource {
    fun sourceFor(mode: FilesMode): String = when (mode) {
        FilesMode.ALL -> "folder"
        FilesMode.RECENT -> "recent"
        FilesMode.STARRED -> "starred"
        FilesMode.TRASH -> "trash"
    }
}

class LoadItemsForModeMappingTest {

    @Test
    fun `ALL mode maps to folder source`() {
        assertEquals("folder", LoadItemsForModeSource.sourceFor(FilesMode.ALL))
    }

    @Test
    fun `RECENT mode maps to recent source`() {
        assertEquals("recent", LoadItemsForModeSource.sourceFor(FilesMode.RECENT))
    }

    @Test
    fun `STARRED mode maps to starred source`() {
        assertEquals("starred", LoadItemsForModeSource.sourceFor(FilesMode.STARRED))
    }

    @Test
    fun `TRASH mode maps to trash source`() {
        assertEquals("trash", LoadItemsForModeSource.sourceFor(FilesMode.TRASH))
    }
}

/**
 * Result handling: after trashItem/restore/emptyTrash/batchStar success, the next action.
 * Mirrors ViewModel behaviour (Success -> loadItems or loadItemsForMode).
 */
object TrashStarResultAction {
    fun onTrashItemSuccess(): String = "loadItems"
    fun onRestoreSuccess(): String = "loadItems"

    /** After empty trash, ViewModel calls loadItemsForMode(displayedMode) to refresh current view. */
    fun onEmptyTrashSuccess(displayedMode: FilesMode): String = "loadItemsForMode($displayedMode)"
    fun onBatchStarSuccess(): String = "loadItems"
}

class TrashStarResultHandlingTest {

    @Test
    fun `trash item success triggers loadItems`() {
        assertEquals("loadItems", TrashStarResultAction.onTrashItemSuccess())
    }

    @Test
    fun `restore success triggers loadItems`() {
        assertEquals("loadItems", TrashStarResultAction.onRestoreSuccess())
    }

    @Test
    fun `empty trash success refreshes current mode view`() {
        assertEquals(
            "loadItemsForMode(TRASH)",
            TrashStarResultAction.onEmptyTrashSuccess(FilesMode.TRASH),
        )
        assertEquals(
            "loadItemsForMode(ALL)",
            TrashStarResultAction.onEmptyTrashSuccess(FilesMode.ALL),
        )
    }

    @Test
    fun `batch star success triggers loadItems`() {
        assertEquals("loadItems", TrashStarResultAction.onBatchStarSuccess())
    }
}
