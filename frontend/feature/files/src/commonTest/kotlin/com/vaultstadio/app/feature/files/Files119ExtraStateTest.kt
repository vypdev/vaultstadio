@file:OptIn(ExperimentalCoroutinesApi::class)
package com.vaultstadio.app.feature.files

import com.vaultstadio.app.domain.activity.model.Activity
import com.vaultstadio.app.domain.result.Result
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.withContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class Files119ExtraStateTest {
    private fun activity(id: String) = Activity("activity:$id", "read", "owner119", id, "/$id", null, files119Time)
    @Test fun file119InfoPanelShowsOnlySelectedItemActivityAndCanClose() = files119Test { h ->
        h.extra.activityResult = { Result.Success(listOf(activity(it))) }
        val vm = h.model(); advanceUntilIdle(); vm.showItemInfo(files119Item("a")); advanceUntilIdle()
        assertTrue(vm.showInfoPanel); assertEquals("a", vm.selectedInfoItem?.id); assertEquals("a", vm.itemActivity.single().itemId)
        assertFalse(vm.isLoadingActivity)
        vm.hideItemInfo(); assertFalse(vm.showInfoPanel); assertNull(vm.selectedInfoItem); assertTrue(vm.itemActivity.isEmpty())
    }
    @Test fun file119LateActivityCannotRepopulateAClosedPanel() = files119Test { h ->
        val pending = CompletableDeferred<Result<List<Activity>>>()
        h.extra.activityResult = { withContext(NonCancellable) { pending.await() } }
        val vm = h.model(); advanceUntilIdle(); vm.showItemInfo(files119Item("a")); runCurrent(); vm.hideItemInfo()
        pending.complete(Result.Success(listOf(activity("a")))); advanceUntilIdle()
        assertFalse(vm.showInfoPanel); assertTrue(vm.itemActivity.isEmpty()); assertFalse(vm.isLoadingActivity)
    }
    @Test fun file119LateActivityCannotReplaceTheNewSelection() = files119Test { h ->
        val old = CompletableDeferred<Result<List<Activity>>>()
        h.extra.activityResult = { id -> if (id == "a") withContext(NonCancellable) { old.await() } else Result.Success(listOf(activity(id))) }
        val vm = h.model(); advanceUntilIdle(); vm.showItemInfo(files119Item("a")); runCurrent()
        vm.showItemInfo(files119Item("b")); runCurrent(); old.complete(Result.Success(listOf(activity("a")))); advanceUntilIdle()
        assertEquals("b", vm.selectedInfoItem?.id); assertEquals("b", vm.itemActivity.single().itemId)
    }
    @Test fun file119ShareUsesExactArgumentsAndExposesProgressAndResult() = files119Test { h ->
        val pending = CompletableDeferred<Unit>(); h.extra.beforeShare = { pending.await() }
        val vm = h.model(); advanceUntilIdle()
        vm.createShare("file119", 3, "owned-ephemeral-password", 2); runCurrent()
        assertTrue(vm.isCreatingShare); assertNull(vm.lastCreatedShare); assertNull(vm.shareError)
        pending.complete(Unit); advanceUntilIdle()
        assertFalse(vm.isCreatingShare); assertEquals(h.extra.shared, vm.lastCreatedShare)
        assertEquals(Files119Mutation("share", listOf("file119", 3, "owned-ephemeral-password", 2)), h.extra.calls.single())
        vm.clearLastCreatedShare(); assertNull(vm.lastCreatedShare)
    }
    @Test fun file119ShareErrorsAndClearDoNotEraseFileErrors() = files119Test { h ->
        h.loading.folderResult = { Result.Error("DENIED", "file error") }
        val vm = h.model(); advanceUntilIdle()
        for (failure in listOf(Result.Error("DENIED", "share denied"), Result.NetworkError("offline"))) {
            h.extra.shareResult = failure; vm.createShare("file119"); advanceUntilIdle()
            assertFalse(vm.isCreatingShare); assertTrue(vm.shareError in listOf("share denied", "offline"))
            assertEquals("file error", vm.error); assertNull(vm.lastCreatedShare)
            vm.clearShareError(); assertNull(vm.shareError); assertEquals("file error", vm.error)
        }
    }
    @Test fun file119BasicSearchAndExplicitClearPreserveRequestScope() = files119Test { h ->
        val vm = h.model(); advanceUntilIdle(); vm.search("é + &"); advanceUntilIdle()
        assertEquals("é + &", vm.searchQuery); assertFalse(vm.isSearching); assertEquals(listOf("found119"), vm.items.map { it.id })
        assertEquals(Files119Mutation("search", listOf("é + &", 50, 0)), h.extra.calls.single())
        vm.clearSearch(); advanceUntilIdle(); assertEquals("", vm.searchQuery); assertEquals(listOf("file119"), vm.items.map { it.id })
    }
    @Test fun file119SearchFailuresRemainVisibleWithoutReplacingCurrentItems() = files119Test { h ->
        val vm = h.model(); advanceUntilIdle()
        h.extra.searchResult = Result.NetworkError("offline"); vm.search("query"); advanceUntilIdle()
        assertEquals("offline", vm.error); assertFalse(vm.isSearching); assertEquals(listOf("file119"), vm.items.map { it.id })
        h.extra.searchResult = Result.Error("DENIED", "private"); vm.search("query"); advanceUntilIdle(); assertEquals("private", vm.error)
        vm.search(" "); advanceUntilIdle(); assertNull(vm.error)
    }
    @Test fun file119SuggestionMinimumAndExplicitClearAreLocalStateOnly() = files119Test { h ->
        val vm = h.model(); advanceUntilIdle()
        vm.loadSearchSuggestions("a"); advanceUntilIdle(); assertTrue(h.extra.calls.isEmpty())
        vm.loadSearchSuggestions("ab"); advanceUntilIdle(); assertEquals(listOf("found119"), vm.searchSuggestions)
        h.extra.suggestionResult = Result.NetworkError("offline"); vm.loadSearchSuggestions("abc"); advanceUntilIdle()
        vm.clearSuggestions(); assertTrue(vm.searchSuggestions.isEmpty())
    }
}
