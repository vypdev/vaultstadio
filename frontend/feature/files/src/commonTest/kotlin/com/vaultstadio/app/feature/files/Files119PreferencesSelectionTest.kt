@file:OptIn(ExperimentalCoroutinesApi::class)

package com.vaultstadio.app.feature.files

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.domain.storage.model.SortField
import com.vaultstadio.app.domain.storage.model.SortOrder
import com.vaultstadio.app.domain.storage.model.ViewMode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class Files119PreferencesSelectionTest {
    @Test fun file119KnownPreferencesRestoreWithoutWritingThemBack() = files119Test { h ->
        h.preferences.view = "LIST"
        h.preferences.field = "SIZE"
        h.preferences.order = "DESC"
        val vm = h.model()
        advanceUntilIdle()
        assertEquals(ViewMode.LIST, vm.viewMode)
        assertEquals(SortField.SIZE, vm.sortField)
        assertEquals(SortOrder.DESC, vm.sortOrder)
        assertTrue(h.preferences.writes.isEmpty())
    }

    @Test fun file119UnknownPreferencesRetainSupportedDefaults() = files119Test { h ->
        h.preferences.view = "retired-view"
        h.preferences.field = "unknown-sort"
        h.preferences.order = "invalid-order"
        val vm = h.model()
        advanceUntilIdle()
        assertEquals(ViewMode.GRID, vm.viewMode)
        assertEquals(SortField.TYPE, vm.sortField)
        assertEquals(SortOrder.ASC, vm.sortOrder)
    }

    @Test fun file119ViewChangesPersistWithoutReloading() = files119Test { h ->
        val vm = h.model()
        advanceUntilIdle()
        vm.updateViewMode(ViewMode.LIST)
        advanceUntilIdle()
        assertEquals(listOf("view" to "LIST"), h.preferences.writes)
        assertEquals(1, h.loading.queries.size)
    }

    @Test fun file119SortChangesReloadOnlyWhenValueChanges() = files119Test { h ->
        val vm = h.model()
        advanceUntilIdle()
        vm.updateSortField(SortField.TYPE)
        vm.updateSortOrder(SortOrder.ASC)
        advanceUntilIdle()
        assertEquals(1, h.loading.queries.size)
        vm.updateSortField(SortField.SIZE)
        advanceUntilIdle()
        vm.updateSortOrder(SortOrder.DESC)
        advanceUntilIdle()
        assertEquals(3, h.loading.queries.size)
        assertEquals(SortField.SIZE, h.loading.queries.last().sort)
        assertEquals(SortOrder.DESC, h.loading.queries.last().order)
        assertEquals(listOf("field" to "SIZE", "order" to "DESC"), h.preferences.writes)
    }

    @Test fun file119SelectionToggleAllAndClearUseDisplayedIdentities() = files119Test { h ->
        h.loading.folderResult = { Result.Success(files119Page(listOf(files119Item("a"), files119Item("b")))) }
        val vm = h.model()
        advanceUntilIdle()
        assertFalse(vm.isSelectionMode)
        vm.toggleItemSelection("a")
        assertEquals(setOf("a"), vm.selectedItems)
        assertTrue(vm.isSelectionMode)
        vm.toggleItemSelection("a")
        assertTrue(vm.selectedItems.isEmpty())
        vm.selectAll()
        assertEquals(setOf("a", "b"), vm.selectedItems)
        vm.selectAll()
        assertTrue(vm.selectedItems.isEmpty())
        vm.toggleItemSelection("b")
        vm.clearSelection()
        assertFalse(vm.isSelectionMode)
    }

    @Test fun file119DownloadAndShareUrlsComeFromSemanticPorts() = files119Test { h ->
        val vm = h.model()
        advanceUntilIdle()
        assertEquals("download:file119", vm.getDownloadUrl("file119"))
        assertEquals("preview:file119", vm.getPreviewUrl("file119"))
        assertEquals("thumbnail:file119:large", vm.getThumbnailUrl("file119", "large"))
        assertEquals("download-zip", vm.getDownloadZipUrl())
        assertEquals("share:${h.extra.shared.token}", vm.getShareUrl(h.extra.shared))
    }
}
