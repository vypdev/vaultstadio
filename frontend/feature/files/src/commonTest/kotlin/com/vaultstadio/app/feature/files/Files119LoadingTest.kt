@file:OptIn(ExperimentalCoroutinesApi::class)
package com.vaultstadio.app.feature.files

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.domain.storage.model.Breadcrumb
import com.vaultstadio.app.domain.storage.model.SortField
import com.vaultstadio.app.domain.storage.model.SortOrder
import com.vaultstadio.app.domain.storage.model.StorageItem
import com.vaultstadio.app.domain.storage.model.PaginatedResponse
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class Files119LoadingTest {
    @Test fun file119InitialLoadExposesProgressThenServerState() = files119Test { h ->
        val pending = CompletableDeferred<Result<PaginatedResponse<StorageItem>>>()
        h.loading.folderResult = { pending.await() }
        val vm = h.model(); runCurrent()
        assertTrue(vm.isLoading); assertTrue(vm.items.isEmpty())
        pending.complete(Result.Success(files119Page(total = 9007199254740993L, hasMore = true))); advanceUntilIdle()
        assertFalse(vm.isLoading); assertEquals(9007199254740993L, vm.folderTotalItems); assertTrue(vm.folderHasMore)
        assertEquals(listOf("file119"), vm.items.map { it.id })
    }
    @Test fun file119ExplicitRetryRecoversFromBothFailureKinds() = files119Test { h ->
        h.loading.folderResult = { Result.Error("DENIED", "private") }
        val vm = h.model(); advanceUntilIdle(); assertEquals("private", vm.error); assertFalse(vm.isLoading)
        h.loading.folderResult = { Result.NetworkError("offline") }
        vm.refresh(); advanceUntilIdle(); assertEquals("offline", vm.error)
        h.loading.folderResult = { Result.Success(files119Page()) }
        vm.refresh(); advanceUntilIdle(); assertNull(vm.error); assertEquals(1, vm.items.size)
    }
    @Test fun file119NavigationClearsSelectionAndTracksHomeParentAndFolder() = files119Test { h ->
        val vm = h.model(); advanceUntilIdle(); vm.toggleItemSelection("file119")
        vm.navigateToFolder("child", "Child"); advanceUntilIdle()
        assertEquals("child", vm.currentFolderId); assertEquals("Child", vm.currentFolderName)
        assertEquals(listOf(null, "child"), vm.breadcrumbs.map { it.id }); assertTrue(vm.selectedItems.isEmpty())
        vm.navigateUp(); advanceUntilIdle(); assertNull(vm.currentFolderId); assertNull(vm.currentFolderName)
        assertEquals(listOf(Breadcrumb(null, "Home", "/")), vm.breadcrumbs)
    }
    @Test fun file119ModesAndNavigationDoNotInventFolderPages() = files119Test { h ->
        for ((mode, id) in listOf(FilesMode.RECENT to "recent119", FilesMode.STARRED to "star119", FilesMode.TRASH to "trash119")) {
            val vm = h.model(mode); advanceUntilIdle(); assertEquals(listOf(id), vm.items.map { it.id })
            vm.openPathFromSegments(listOf("ignored")); vm.loadMoreFolderItems(); advanceUntilIdle()
        }
        assertTrue(h.loading.queries.isEmpty())
    }
    @Test fun file119LoadMoreAppendsAndStopsWhenServerIsDone() = files119Test { h ->
        h.loading.folderResult = { query -> Result.Success(if (query.offset == 0) files119Page(hasMore = true, total = 2)
            else files119Page(listOf(files119Item("next119")), total = 2)) }
        val vm = h.model(); advanceUntilIdle(); vm.loadMoreFolderItems(); advanceUntilIdle()
        assertEquals(listOf("file119", "next119"), vm.items.map { it.id }); assertFalse(vm.folderHasMore)
        vm.loadMoreFolderItems(); advanceUntilIdle(); assertEquals(listOf(0, 1), h.loading.queries.map { it.offset })
    }
    @Test fun file119LoadMoreFailurePreservesExistingItemsAndAllowsRetry() = files119Test { h ->
        h.loading.folderResult = { query -> if (query.offset == 0) Result.Success(files119Page(hasMore = true, total = 2)) else Result.NetworkError("offline") }
        val vm = h.model(); advanceUntilIdle(); vm.loadMoreFolderItems(); advanceUntilIdle()
        assertEquals(listOf("file119"), vm.items.map { it.id }); assertEquals("offline", vm.error); assertFalse(vm.isLoadingMore)
    }
    @Test fun file119LoadingUsesRestoredSortingOnTheFirstRequest() = files119Test { h ->
        h.preferences.field = "SIZE"; h.preferences.order = "DESC"
        h.model(); advanceUntilIdle()
        assertEquals(SortField.SIZE, h.loading.queries.single().sort); assertEquals(SortOrder.DESC, h.loading.queries.single().order)
    }
}
