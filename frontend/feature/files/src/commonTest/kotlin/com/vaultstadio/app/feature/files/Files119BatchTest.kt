@file:OptIn(ExperimentalCoroutinesApi::class)
package com.vaultstadio.app.feature.files

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.domain.storage.model.BatchResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class Files119BatchTest {
    @Test fun file119EmptySelectionHasNoBatchEffects() = files119Test { h ->
        val vm = h.model(); advanceUntilIdle()
        vm.batchDelete(); vm.batchMove("target"); vm.batchCopy("target"); vm.batchStar(true); advanceUntilIdle()
        assertTrue(h.mutation.calls.isEmpty()); assertEquals(1, h.loading.queries.size)
    }
    @Test fun file119SuccessfulBatchesPassSelectionAndClearOnlyAfterSuccess() = files119Test { h ->
        val vm = h.model(); advanceUntilIdle()
        h.mutation.batchResult = Result.Success(BatchResult(2, 0))
        vm.toggleItemSelection("a"); vm.toggleItemSelection("b"); vm.batchDelete(true); advanceUntilIdle()
        assertTrue(vm.selectedItems.isEmpty())
        h.mutation.batchResult = Result.Success(BatchResult(1, 0))
        vm.toggleItemSelection("a"); vm.batchMove("target"); advanceUntilIdle()
        vm.toggleItemSelection("b"); vm.batchCopy(null); advanceUntilIdle()
        vm.toggleItemSelection("a"); vm.batchStar(false); advanceUntilIdle()
        assertEquals(listOf("batchDelete", "batchMove", "batchCopy", "batchStar"), h.mutation.calls.map { it.operation })
        assertEquals(listOf(listOf("a", "b"), true), h.mutation.calls[0].arguments)
        assertEquals(listOf(listOf("a"), "target"), h.mutation.calls[1].arguments)
        assertEquals(listOf(listOf("b"), null), h.mutation.calls[2].arguments)
        assertEquals(listOf(listOf("a"), false), h.mutation.calls[3].arguments)
        assertTrue(vm.selectedItems.isEmpty())
    }
    @Test fun file119BatchFailuresKeepTheRetrySelection() = files119Test { h ->
        val vm = h.model(); advanceUntilIdle(); vm.toggleItemSelection("a")
        h.mutation.batchResult = Result.NetworkError("offline")
        vm.batchDelete(); advanceUntilIdle(); assertEquals(setOf("a"), vm.selectedItems); assertEquals("offline", vm.error)
        h.mutation.batchResult = Result.Error("DENIED", "not copied")
        vm.batchCopy("target"); advanceUntilIdle(); assertEquals(setOf("a"), vm.selectedItems); assertEquals("not copied", vm.error)
        assertEquals(1, h.loading.queries.size)
    }
    @Test fun file119EmptyTrashReloadsTheDisplayedMode() = files119Test { h ->
        val vm = h.model(FilesMode.TRASH); advanceUntilIdle()
        vm.emptyTrash(FilesMode.TRASH); advanceUntilIdle()
        assertEquals(listOf("trash", "trash"), h.loading.modes)
        assertEquals(Files119Mutation("empty", emptyList()), h.mutation.calls.single())
    }
    @Test fun file119SuccessfulMoveReconcilesOptimisticRemoval() = files119Test { h ->
        val vm = h.model(); advanceUntilIdle()
        h.loading.folderResult = { Result.Success(files119Page(emptyList())) }
        vm.moveItemToFolder("file119", "target"); advanceUntilIdle()
        assertTrue(vm.items.isEmpty()); assertEquals(Files119Mutation("batchMove", listOf(listOf("file119"), "target")), h.mutation.calls.single())
    }
}
