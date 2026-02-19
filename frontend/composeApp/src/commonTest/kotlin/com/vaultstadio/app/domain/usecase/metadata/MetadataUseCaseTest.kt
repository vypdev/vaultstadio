/**
 * Unit tests for metadata use cases (GetSearchSuggestions, SearchByMetadata).
 * Uses a fake MetadataRepository to avoid platform/DI.
 */

package com.vaultstadio.app.domain.usecase.metadata

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.data.repository.MetadataRepository
import com.vaultstadio.app.domain.model.DocumentMetadata
import com.vaultstadio.app.domain.model.FileMetadata
import com.vaultstadio.app.domain.model.ImageMetadata
import com.vaultstadio.app.domain.model.MetadataSearchResult
import com.vaultstadio.app.domain.model.PaginatedResponse
import com.vaultstadio.app.domain.model.StorageItem
import com.vaultstadio.app.domain.model.VideoMetadata
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private val testInstant = Instant.fromEpochMilliseconds(0L)

private fun <T> stubResult(): Result<T> = Result.error("TEST", "Not implemented in fake")

private fun testMetadataSearchResult(
    itemId: String = "item-1",
    key: String = "camera",
    value: String = "Canon",
) = MetadataSearchResult(
    itemId = itemId,
    itemName = "photo.jpg",
    itemPath = "/photos/photo.jpg",
    pluginId = "image-metadata",
    key = key,
    value = value,
)

private fun testFileMetadata(
    itemId: String = "item-1",
    metadata: Map<String, String> = mapOf("mimeType" to "image/jpeg"),
) = FileMetadata(
    itemId = itemId,
    metadata = metadata,
    extractedBy = emptyList(),
)

private class FakeMetadataRepository(
    var getSearchSuggestionsResult: Result<List<String>> = Result.success(emptyList()),
    var searchByMetadataResult: Result<PaginatedResponse<MetadataSearchResult>> = Result.success(
        PaginatedResponse(emptyList(), 0L, 0, 50, 0, false),
    ),
    var getFileMetadataResult: Result<FileMetadata> = Result.success(testFileMetadata()),
) : MetadataRepository {

    override suspend fun getFileMetadata(itemId: String): Result<FileMetadata> = getFileMetadataResult

    override suspend fun getImageMetadata(itemId: String): Result<ImageMetadata> = stubResult()

    override suspend fun getVideoMetadata(itemId: String): Result<VideoMetadata> = stubResult()

    override suspend fun getDocumentMetadata(itemId: String): Result<DocumentMetadata> = stubResult()

    override suspend fun advancedSearch(
        query: String,
        searchContent: Boolean,
        fileTypes: List<String>?,
        minSize: Long?,
        maxSize: Long?,
        fromDate: Instant?,
        toDate: Instant?,
        limit: Int,
        offset: Int,
    ): Result<PaginatedResponse<StorageItem>> = stubResult()

    override suspend fun searchByMetadata(
        key: String,
        value: String?,
        pluginId: String?,
        limit: Int,
    ): Result<PaginatedResponse<MetadataSearchResult>> = searchByMetadataResult

    override suspend fun getSearchSuggestions(prefix: String, limit: Int): Result<List<String>> =
        getSearchSuggestionsResult
}

class GetSearchSuggestionsUseCaseTest {

    @Test
    fun invoke_returnsRepositoryGetSearchSuggestionsResult() = runTest {
        val suggestions = listOf("camera", "camera-model", "created-date")
        val repo = FakeMetadataRepository(getSearchSuggestionsResult = Result.success(suggestions))
        val useCase = GetSearchSuggestionsUseCaseImpl(repo)
        val result = useCase("cam", limit = 10)
        assertTrue(result.isSuccess())
        assertEquals(3, result.getOrNull()?.size)
        assertEquals("camera", result.getOrNull()?.get(0))
    }

    @Test
    fun invoke_propagatesError() = runTest {
        val repo = FakeMetadataRepository(getSearchSuggestionsResult = Result.error("UNAUTHORIZED", "Not logged in"))
        val useCase = GetSearchSuggestionsUseCaseImpl(repo)
        val result = useCase("x")
        assertTrue(result.isError())
        assertNull(result.getOrNull())
    }
}

class SearchByMetadataUseCaseTest {

    @Test
    fun invoke_returnsRepositorySearchByMetadataResult() = runTest {
        val results = listOf(testMetadataSearchResult("i1", "camera", "Canon"))
        val paged = PaginatedResponse(results, 1L, 0, 50, 1, false)
        val repo = FakeMetadataRepository(searchByMetadataResult = Result.success(paged))
        val useCase = SearchByMetadataUseCaseImpl(repo)
        val result = useCase("camera", value = "Canon", pluginId = null, limit = 50)
        assertTrue(result.isSuccess())
        assertEquals(1, result.getOrNull()?.items?.size)
        assertEquals("camera", result.getOrNull()?.items?.get(0)?.key)
    }

    @Test
    fun invoke_propagatesError() = runTest {
        val repo = FakeMetadataRepository(searchByMetadataResult = Result.error("BAD_REQUEST", "Invalid key"))
        val useCase = SearchByMetadataUseCaseImpl(repo)
        val result = useCase("key")
        assertTrue(result.isError())
    }
}

class GetFileMetadataUseCaseTest {

    @Test
    fun invoke_returnsRepositoryGetFileMetadataResult() = runTest {
        val metadata = testFileMetadata("file-1", mapOf("mimeType" to "application/pdf", "size" to "1024"))
        val repo = FakeMetadataRepository(getFileMetadataResult = Result.success(metadata))
        val useCase = GetFileMetadataUseCaseImpl(repo)
        val result = useCase("file-1")
        assertTrue(result.isSuccess())
        assertEquals("file-1", result.getOrNull()?.itemId)
        assertEquals("application/pdf", result.getOrNull()?.metadata?.get("mimeType"))
    }

    @Test
    fun invoke_propagatesError() = runTest {
        val repo = FakeMetadataRepository(getFileMetadataResult = Result.error("NOT_FOUND", "Item not found"))
        val useCase = GetFileMetadataUseCaseImpl(repo)
        val result = useCase("missing")
        assertTrue(result.isError())
        assertNull(result.getOrNull())
    }
}
