@file:OptIn(ExperimentalCoroutinesApi::class)

package com.vaultstadio.app.feature.files

import com.vaultstadio.app.domain.result.Result
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class Files119MutationTest {
    @Test fun file119CreateCallbackWaitsForActualSuccessThenRefreshes() = files119Test { h ->
        val vm = h.model()
        advanceUntilIdle()
        val pending = CompletableDeferred<Unit>()
        h.mutation.beforeItem = { pending.await() }
        var successful = 0
        vm.createFolder("Created") { successful++ }
        runCurrent()
        assertTrue(vm.isLoading)
        assertEquals(0, successful)
        h.loading.folderResult = { Result.Success(files119Page(listOf(files119Item("created119")))) }
        pending.complete(Unit)
        advanceUntilIdle()
        assertEquals(1, successful)
        assertFalse(vm.isLoading)
        assertEquals(listOf("created119"), vm.items.map { it.id })
        assertEquals(Files119Mutation("create", listOf("Created", null)), h.mutation.calls.single())
    }

    @Test fun file119CreateFailuresNeverInvokeSuccessOrDiscardDisplayedFiles() = files119Test { h ->
        val vm = h.model()
        advanceUntilIdle()
        var success = 0
        for (failure in listOf(Result.Error("DENIED", "private"), Result.NetworkError("offline"))) {
            h.mutation.itemResult = failure
            vm.createFolder("Rejected") { success++ }
            advanceUntilIdle()
            assertEquals(0, success)
            assertFalse(vm.isLoading)
            assertEquals(listOf("file119"), vm.items.map { it.id })
            assertTrue(vm.error in listOf("private", "offline"))
        }
        assertEquals(1, h.loading.queries.size)
        vm.clearError()
        assertNull(vm.error)
    }

    @Test fun file119SingleMutationsPreserveTheirIdentitiesAndRefreshOnSuccess() = files119Test { h ->
        val vm = h.model()
        advanceUntilIdle()
        vm.renameItem("file119", "renamed")
        advanceUntilIdle()
        vm.trashItem("file119")
        advanceUntilIdle()
        vm.restoreItem("file119")
        advanceUntilIdle()
        vm.deleteItemPermanently("file119")
        advanceUntilIdle()
        assertEquals(
            listOf(
                Files119Mutation("rename", listOf("file119", "renamed")),
                Files119Mutation("trash", listOf("file119")),
                Files119Mutation("restore", listOf("file119")),
                Files119Mutation("delete", listOf("file119")),
            ),
            h.mutation.calls,
        )
        assertEquals(5, h.loading.queries.size)
    }

    @Test fun file119FailedSingleMutationsKeepErrorAndSkipSuccessRefresh() = files119Test { h ->
        val vm = h.model()
        advanceUntilIdle()
        h.mutation.itemResult = Result.Error("DENIED", "not changed")
        h.mutation.unitResult = Result.NetworkError("offline")
        vm.renameItem("file119", "rejected")
        advanceUntilIdle()
        assertEquals("not changed", vm.error)
        vm.trashItem("file119")
        vm.restoreItem("file119")
        advanceUntilIdle()
        assertEquals("not changed", vm.error)
        vm.deleteItemPermanently("file119")
        advanceUntilIdle()
        assertEquals("offline", vm.error)
        assertEquals(1, h.loading.queries.size)
        assertEquals(listOf("file119"), vm.items.map { it.id })
    }

    @Test fun file119StarUpdatesTheSelectedInfoItemWithReturnedServerState() = files119Test { h ->
        val vm = h.model()
        advanceUntilIdle()
        val item = vm.items.single()
        vm.showItemInfo(item)
        advanceUntilIdle()
        val updated = item.copy(isStarred = true)
        h.mutation.itemResult = Result.Success(updated)
        h.loading.folderResult = { Result.Success(files119Page(listOf(updated))) }
        vm.toggleStar(item)
        advanceUntilIdle()
        assertEquals(true, vm.selectedInfoItem?.isStarred)
        assertEquals(true, vm.items.single().isStarred)
    }

    @Test fun file119FailedStarPreservesSelectionAndCause() = files119Test { h ->
        val vm = h.model()
        advanceUntilIdle()
        h.mutation.itemResult = Result.NetworkError("offline")
        vm.toggleStar(vm.items.single())
        advanceUntilIdle()
        assertEquals("offline", vm.error)
        assertEquals(false, vm.items.single().isStarred)
    }
}
