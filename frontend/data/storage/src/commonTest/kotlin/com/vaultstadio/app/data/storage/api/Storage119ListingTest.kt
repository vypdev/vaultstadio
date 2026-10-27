package com.vaultstadio.app.data.storage.api

import com.vaultstadio.app.data.network.ApiResult
import com.vaultstadio.app.data.storage.dto.StorageItemDTO
import com.vaultstadio.app.domain.storage.model.SortField
import com.vaultstadio.app.domain.storage.model.SortOrder
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class Storage119ListingTest {
    @Test fun sto119RootUsesDefaultSortAndPaging() = runTest {
        storage119Request(HttpMethod.Get, "/api/v1/storage/folder", STORAGE119_PAGE,
            mapOf("sortBy" to "name", "sortOrder" to "asc", "limit" to "100", "offset" to "0")) { it.listFolder() }
    }
    @Test fun sto119ChildPreservesExplicitSortPageAndIdentity() = runTest {
        storage119Request(HttpMethod.Get, "/api/v1/storage/folder/child119", STORAGE119_PAGE,
            mapOf("sortBy" to "updated_at", "sortOrder" to "desc", "limit" to "25", "offset" to "50")) {
            it.listFolder("child119", SortField.UPDATED_AT, SortOrder.DESC, 25, 50)
        }
    }
    @Test fun sto119ItemUsesExactIdentityAndRealDeserialization() = runTest {
        val engine = MockEngine { respond(STORAGE119_ITEM, headers = headersOf(HttpHeaders.ContentType, "application/json")) }
        withStorage119Client(engine) { client ->
            val expected = assertIs<ApiResult.Success<StorageItemDTO>>(StorageApi(client).getItem("file119")).data
            assertEquals("file119", expected?.id); assertEquals("é.txt", expected?.name)
            assertEquals(4L, expected?.size); assertEquals("2026-10-27T12:00:00Z", expected?.createdAt.toString())
        }
    }
    @Test fun sto119CountsRetainInt64Precision() = runTest {
        val engine = MockEngine { respond(STORAGE119_PAGE, headers = headersOf(HttpHeaders.ContentType, "application/json")) }
        withStorage119Client(engine) { client ->
            val page = StorageApi(client).listFolder().getOrNull()
            assertEquals(9007199254740993L, page?.total); assertEquals(180143985094820L, page?.totalPages)
            assertEquals(2L, page?.page); assertEquals(true, page?.hasMore)
        }
    }
    @Test fun sto119BreadcrumbUsesTheItemPath() = runTest {
        storage119Request(HttpMethod.Get, "/api/v1/storage/item/file119/breadcrumbs", STORAGE119_LIST) { it.getBreadcrumbs("file119") }
    }
    @Test fun sto119RecentPreservesLimit() = runTest {
        storage119Request(HttpMethod.Get, "/api/v1/storage/recent", STORAGE119_LIST, mapOf("limit" to "7")) { it.getRecent(7) }
    }
    @Test fun sto119StarredAndTrashRemainDistinctReads() = runTest {
        storage119Request(HttpMethod.Get, "/api/v1/storage/starred", STORAGE119_LIST) { it.getStarred() }
        storage119Request(HttpMethod.Get, "/api/v1/storage/trash", STORAGE119_LIST) { it.getTrash() }
    }
    @Test fun sto119SearchPreservesUnicodeReservedCharactersAndPage() = runTest {
        storage119Request(HttpMethod.Get, "/api/v1/search", STORAGE119_PAGE,
            mapOf("q" to "é + & folder", "limit" to "10", "offset" to "20")) { it.search("é + & folder", 10, 20) }
    }
}
