@file:OptIn(ExperimentalCoroutinesApi::class)
package com.vaultstadio.app.feature.files

import androidx.lifecycle.viewModelScope
import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.domain.storage.model.PaginatedResponse
import com.vaultstadio.app.domain.storage.model.StorageItem
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.withContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class Files119OrderingTest {
    @Test fun file119OlderFolderCannotReplaceNewerNavigationEvenIfProviderIgnoresCancellation() = files119Test { h ->
        val old = CompletableDeferred<Result<PaginatedResponse<StorageItem>>>()
        h.loading.folderResult = { query -> if (query.folder == null) withContext(NonCancellable) { old.await() }
            else Result.Success(files119Page(listOf(files119Item("new119")))) }
        val vm = h.model(); runCurrent()
        vm.navigateToFolder("new-folder", "New"); runCurrent()
        assertEquals(listOf("new119"), vm.items.map { it.id })
        old.complete(Result.Success(files119Page(listOf(files119Item("old119"))))); advanceUntilIdle()
        assertEquals("new-folder", vm.currentFolderId); assertEquals(listOf("new119"), vm.items.map { it.id })
        assertEquals("New", vm.currentFolderName); assertFalse(vm.isLoading); assertNull(vm.error)
    }
    @Test fun file119OlderPageCannotAppendIntoAnotherFolder() = files119Test { h ->
        val oldPage = CompletableDeferred<Result<PaginatedResponse<StorageItem>>>()
        h.loading.folderResult = { query -> when {
            query.folder == "new-folder" -> Result.Success(files119Page(listOf(files119Item("new119"))))
            query.offset == 0 -> Result.Success(files119Page(hasMore = true, total = 2))
            else -> withContext(NonCancellable) { oldPage.await() }
        } }
        val vm = h.model(); advanceUntilIdle(); vm.loadMoreFolderItems(); runCurrent()
        vm.navigateToFolder("new-folder", "New"); runCurrent()
        oldPage.complete(Result.Success(files119Page(listOf(files119Item("old-page119"))))); advanceUntilIdle()
        assertEquals(listOf("new119"), vm.items.map { it.id }); assertFalse(vm.isLoadingMore)
    }
    @Test fun file119CancellationReleasesOnlyOwnedLoadingProgress() = files119Test { h ->
        val pending = CompletableDeferred<Result<PaginatedResponse<StorageItem>>>()
        h.loading.folderResult = { pending.await() }
        val vm = h.model(); runCurrent(); vm.viewModelScope.cancel(); runCurrent()
        assertFalse(vm.isLoading)
    }
    @Test fun file119FailedMoveRestoresItemsAndPreservesTheCauseUntilExplicitRetry() = files119Test { h ->
        val vm = h.model(); advanceUntilIdle()
        h.mutation.batchResult = Result.Error("DENIED", "not moved")
        vm.moveItemToFolder("file119", "target"); advanceUntilIdle()
        assertEquals(listOf("file119"), vm.items.map { it.id }); assertEquals("not moved", vm.error)
        vm.refresh(); advanceUntilIdle(); assertNull(vm.error)
    }
}
