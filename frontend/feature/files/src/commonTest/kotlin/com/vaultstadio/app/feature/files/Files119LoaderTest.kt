package com.vaultstadio.app.feature.files

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.domain.storage.model.Breadcrumb
import com.vaultstadio.app.domain.storage.model.ItemType
import com.vaultstadio.app.domain.storage.model.SortField
import com.vaultstadio.app.domain.storage.model.SortOrder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class Files119LoaderTest {
    @Test fun file119RootUsesExactSortPageAndHomeScope() = files119Test { h ->
        val result = assertIs<LoadItemsResult.Success>(h.loading.loader().loadItemsForMode(FilesMode.ALL, null, SortField.SIZE, SortOrder.DESC, "stale"))
        assertEquals(listOf(Files119Query(null, SortField.SIZE, SortOrder.DESC, 2, 0)), h.loading.queries)
        assertEquals(listOf(Breadcrumb(null, "Home", "/")), result.breadcrumbs)
        assertNull(result.currentFolderName)
        assertEquals(listOf(files119Item()), result.items)
    }
    @Test fun file119ChildCombinesAncestorsAndCurrentFolder() = files119Test { h ->
        h.loading.breadcrumbResult = Result.Success(listOf(Breadcrumb("parent", "Parent", "/Parent")))
        val result = assertIs<LoadItemsResult.Success>(h.loading.loader().loadItemsForMode(FilesMode.ALL, "child", SortField.NAME, SortOrder.ASC, "Child"))
        assertEquals(listOf(null, "parent", "child"), result.breadcrumbs.map { it.id })
        assertEquals("Child", result.currentFolderName)
    }
    @Test fun file119BreadcrumbFailureFallsBackWithoutInventingAncestry() = files119Test { h ->
        h.loading.breadcrumbResult = Result.NetworkError("offline")
        val result = assertIs<LoadItemsResult.Success>(h.loading.loader().loadItemsForMode(FilesMode.ALL, "child", SortField.NAME, SortOrder.ASC, null))
        assertEquals(listOf(null, "child"), result.breadcrumbs.map { it.id })
    }
    @Test fun file119AllAlternateModesUseTheirOwnPorts() = files119Test { h ->
        val loader = h.loading.loader()
        for ((mode, id) in listOf(FilesMode.RECENT to "recent119", FilesMode.STARRED to "star119", FilesMode.TRASH to "trash119")) {
            val result = assertIs<LoadItemsResult.Success>(loader.loadItemsForMode(mode, "ignored", SortField.NAME, SortOrder.ASC, "ignored"))
            assertEquals(listOf(id), result.items.map { it.id })
            assertEquals(1L, result.total); assertEquals(false, result.hasMore); assertNull(result.currentFolderName)
        }
        assertEquals(listOf("recent", "starred", "trash"), h.loading.modes)
    }
    @Test fun file119LogicalAndNetworkErrorsRemainExplicit() = files119Test { h ->
        val loader = h.loading.loader()
        for (error in listOf(Result.Error("DENIED", "private"), Result.NetworkError("offline"))) {
            h.loading.folderResult = { error }; h.loading.recentResult = error
            assertEquals(LoadItemsResult.Error(error.message()), loader.loadItemsForMode(FilesMode.ALL, null, SortField.NAME, SortOrder.ASC, null))
            assertEquals(LoadItemsResult.Error(error.message()), loader.loadItemsForMode(FilesMode.RECENT, null, SortField.NAME, SortOrder.ASC, null))
            assertEquals(LoadMoreResult.Error(error.message()), loader.loadMoreFolderItems(null, SortField.NAME, SortOrder.ASC, 1))
        }
    }
    @Test fun file119LoadMorePreservesOffsetAndServerHasMore() = files119Test { h ->
        h.loading.folderResult = { Result.Success(files119Page(listOf(files119Item("next")), 9, true)) }
        val result = assertIs<LoadMoreResult.Success>(h.loading.loader().loadMoreFolderItems("child", SortField.TYPE, SortOrder.DESC, 4))
        assertEquals(listOf("next"), result.newItems.map { it.id }); assertEquals(true, result.hasMore)
        assertEquals(Files119Query("child", SortField.TYPE, SortOrder.DESC, 2, 4), h.loading.queries.single())
    }
    @Test fun file119PathResolutionVisitsNestedFoldersAndStopsBeforeAFile() = files119Test { h ->
        h.loading.folderResult = { query -> Result.Success(files119Page(when (query.folder) {
            null -> listOf(files119Item("a", ItemType.FOLDER, "A"))
            "a" -> listOf(files119Item("b", ItemType.FOLDER, "B"), files119Item("not-folder", name = "file"))
            else -> emptyList()
        })) }
        val loader = h.loading.loader()
        assertEquals("b" to "B", loader.resolvePathSegments(listOf("A", "B"), SortField.NAME, SortOrder.ASC))
        assertEquals("a" to "A", loader.resolvePathSegments(listOf("A", "file"), SortField.NAME, SortOrder.ASC))
        assertNull(loader.resolvePathSegments(emptyList(), SortField.NAME, SortOrder.ASC))
    }
    @Test fun file119PathResolutionSearchesLaterPages() = files119Test { h ->
        h.loading.folderResult = { query -> Result.Success(if (query.offset == 0)
            files119Page(listOf(files119Item("first", ItemType.FOLDER, "First")), 2, true)
        else files119Page(listOf(files119Item("target", ItemType.FOLDER, "Target")), 2)) }
        assertEquals("target" to "Target", h.loading.loader().resolvePathSegments(listOf("Target"), SortField.NAME, SortOrder.ASC))
        assertEquals(listOf(0, 1), h.loading.queries.map { it.offset })
    }
    @Test fun file119PathFailureRetainsOnlyResolvedAncestor() = files119Test { h ->
        h.loading.folderResult = { Result.NetworkError("offline") }
        assertEquals(null to null, h.loading.loader().resolvePathSegments(listOf("Missing"), SortField.NAME, SortOrder.ASC))
    }
    @Test fun file119StandaloneBreadcrumbsDistinguishFailureAndUnknownName() = files119Test { h ->
        assertEquals(listOf(Breadcrumb(null, "Home", "/")), h.loading.loader().loadBreadcrumbs("child", null))
        h.loading.breadcrumbResult = Result.Error("DENIED", "private")
        assertEquals(emptyList(), h.loading.loader().loadBreadcrumbs("child", "Child"))
    }
    private fun Result<Nothing>.message() = when (this) {
        is Result.Error -> message
        is Result.NetworkError -> message
        is Result.Success -> error("not an error")
    }
}
