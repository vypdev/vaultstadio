@file:OptIn(ExperimentalCoroutinesApi::class)

package com.vaultstadio.app.feature.files

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.domain.storage.model.BatchError
import com.vaultstadio.app.domain.storage.model.BatchResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class Files119PartialBatchTest {
    private fun runPartial(operation: (FilesViewModel) -> Unit) = files119Test { h ->
        val vm = h.model()
        advanceUntilIdle()
        vm.toggleItemSelection("a")
        vm.toggleItemSelection("b")
        h.mutation.batchResult = Result.Success(BatchResult(1, 1, listOf(BatchError("b", "No grant"))))
        operation(vm)
        advanceUntilIdle()
        assertEquals(setOf("b"), vm.selectedItems)
        assertNotNull(vm.error)
        assertTrue(vm.error!!.contains("No grant"))
    }

    @Test fun file119PartialDeleteRetainsFailedSelectionAndError() = runPartial { it.batchDelete() }

    @Test fun file119PartialMoveRetainsFailedSelectionAndError() = runPartial { it.batchMove("target") }

    @Test fun file119PartialCopyRetainsFailedSelectionAndError() = runPartial { it.batchCopy("target") }

    @Test fun file119PartialStarRetainsFailedSelectionAndError() = runPartial { it.batchStar(true) }

    @Test fun file119IncompleteFailureDetailsDoNotInventSuccessfulIdentities() = files119Test { h ->
        val vm = h.model()
        advanceUntilIdle()
        vm.toggleItemSelection("a")
        vm.toggleItemSelection("b")
        h.mutation.batchResult = Result.Success(BatchResult(1, 1))
        vm.batchMove("target")
        advanceUntilIdle()
        assertEquals(setOf("a", "b"), vm.selectedItems)
        assertNotNull(vm.error)
    }

    @Test fun file119FailedBatchDoesNotSelectAnUnrequestedIdentityFromAProviderError() = files119Test { h ->
        val vm = h.model()
        advanceUntilIdle()
        vm.toggleItemSelection("a")
        h.mutation.batchResult =
            Result.Success(BatchResult(0, 1, listOf(BatchError("foreign-unrequested", "No grant"))))
        vm.batchDelete()
        advanceUntilIdle()
        assertEquals(setOf("a"), vm.selectedItems)
        assertNotNull(vm.error)
    }
}
