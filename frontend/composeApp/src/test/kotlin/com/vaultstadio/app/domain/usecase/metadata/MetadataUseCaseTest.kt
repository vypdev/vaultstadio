/**
 * Unit tests for metadata use cases (GetSearchSuggestions, SearchByMetadata, GetFileMetadata,
 * AdvancedSearch, GetImageMetadata, GetVideoMetadata, GetDocumentMetadata).
 * Uses a fake MetadataRepository to avoid platform/DI.
 */

package com.vaultstadio.app.domain.usecase.metadata

import com.vaultstadio.app.data.metadata.usecase.AdvancedSearchUseCaseImpl
import com.vaultstadio.app.data.metadata.usecase.GetDocumentMetadataUseCaseImpl
import com.vaultstadio.app.data.metadata.usecase.GetFileMetadataUseCaseImpl
import com.vaultstadio.app.data.metadata.usecase.GetImageMetadataUseCaseImpl
import com.vaultstadio.app.data.metadata.usecase.GetSearchSuggestionsUseCaseImpl
import com.vaultstadio.app.data.metadata.usecase.GetVideoMetadataUseCaseImpl
import com.vaultstadio.app.data.metadata.usecase.SearchByMetadataUseCaseImpl
import com.vaultstadio.app.domain.metadata.MetadataRepository
import com.vaultstadio.app.domain.metadata.model.DocumentMetadata
import com.vaultstadio.app.domain.metadata.model.FileMetadata
import com.vaultstadio.app.domain.metadata.model.ImageMetadata
import com.vaultstadio.app.domain.metadata.model.MetadataSearchResult
import com.vaultstadio.app.domain.metadata.model.VideoMetadata
import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.domain.storage.model.ItemType
import com.vaultstadio.app.domain.storage.model.PaginatedResponse
import com.vaultstadio.app.domain.storage.model.StorageItem
import com.vaultstadio.app.domain.storage.model.Visibility
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

private fun testStorageItem(
    id: String = "item-1",
    name: String = "doc.pdf",
) = StorageItem(
    id = id,
    name = name,
    path = "/docs/$name",
    type = ItemType.FILE,
    parentId = "parent-1",
    size = 1024L,
    mimeType = "application/pdf",
    visibility = Visibility.PRIVATE,
    isStarred = false,
    isTrashed = false,
    createdAt = testInstant,
    updatedAt = testInstant,
    metadata = null,
)

private class FakeMetadataRepository(
    var getSearchSuggestionsResult: Result<List<String>> = Result.success(emptyList()),
    var searchByMetadataResult: Result<PaginatedResponse<MetadataSearchResult>> = Result.success(
        PaginatedResponse(emptyList(), 0L, 0, 50, 0, false),
    ),
    var getFileMetadataResult: Result<FileMetadata> = Result.success(testFileMetadata()),
    var advancedSearchResult: Result<PaginatedResponse<StorageItem>> = Result.success(
        PaginatedResponse(emptyList(), 0L, 0, 50, 0, false),
    ),
    var getImageMetadataResult: Result<ImageMetadata> = Result.success(ImageMetadata(width = 1920, height = 1080)),
    var getVideoMetadataResult: Result<VideoMetadata> = Result.success(
        VideoMetadata(width = 1280, height = 720, duration = 120.0),
    ),
    var getDocumentMetadataResult: Result<DocumentMetadata> = Result.success(
        DocumentMetadata(title = "Test", author = "Author", pageCount = 10),
    ),
) : MetadataRepository {

    override suspend fun getFileMetadata(itemId: String): Result<FileMetadata> = getFileMetadataResult

    override suspend fun getImageMetadata(itemId: String): Result<ImageMetadata> = getImageMetadataResult

    override suspend fun getVideoMetadata(itemId: String): Result<VideoMetadata> = getVideoMetadataResult

    override suspend fun getDocumentMetadata(itemId: String): Result<DocumentMetadata> = getDocumentMetadataResult

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
    ): Result<PaginatedResponse<StorageItem>> = advancedSearchResult

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

class AdvancedSearchUseCaseTest {

    @Test
    fun invoke_returnsRepositoryAdvancedSearchResult() = runTest {
        val items = listOf(testStorageItem("i1", "photo.jpg"), testStorageItem("i2", "doc.pdf"))
        val paged = PaginatedResponse(items, 2L, 0, 50, 2, false)
        val repo = FakeMetadataRepository(advancedSearchResult = Result.success(paged))
        val useCase = AdvancedSearchUseCaseImpl(repo)
        val result = useCase("query", searchContent = true, fileTypes = listOf("image"), limit = 50, offset = 0)
        assertTrue(result.isSuccess())
        assertEquals(2, result.getOrNull()?.items?.size)
        assertEquals("photo.jpg", result.getOrNull()?.items?.get(0)?.name)
    }

    @Test
    fun invoke_propagatesError() = runTest {
        val repo = FakeMetadataRepository(advancedSearchResult = Result.error("UNAUTHORIZED", "Not logged in"))
        val useCase = AdvancedSearchUseCaseImpl(repo)
        val result = useCase("q")
        assertTrue(result.isError())
        assertNull(result.getOrNull())
    }
}

class GetImageMetadataUseCaseTest {

    @Test
    fun invoke_returnsRepositoryGetImageMetadataResult() = runTest {
        val meta = ImageMetadata(width = 1920, height = 1080, cameraMake = "Canon", cameraModel = "EOS R5")
        val repo = FakeMetadataRepository(getImageMetadataResult = Result.success(meta))
        val useCase = GetImageMetadataUseCaseImpl(repo)
        val result = useCase("item-1")
        assertTrue(result.isSuccess())
        assertEquals(1920, result.getOrNull()?.width)
        assertEquals("Canon", result.getOrNull()?.cameraMake)
    }

    @Test
    fun invoke_propagatesError() = runTest {
        val repo = FakeMetadataRepository(getImageMetadataResult = Result.error("NOT_FOUND", "Item not found"))
        val useCase = GetImageMetadataUseCaseImpl(repo)
        val result = useCase("missing")
        assertTrue(result.isError())
        assertNull(result.getOrNull())
    }
}

class GetVideoMetadataUseCaseTest {

    @Test
    fun invoke_returnsRepositoryGetVideoMetadataResult() = runTest {
        val meta = VideoMetadata(width = 1920, height = 1080, duration = 90.5, title = "Clip")
        val repo = FakeMetadataRepository(getVideoMetadataResult = Result.success(meta))
        val useCase = GetVideoMetadataUseCaseImpl(repo)
        val result = useCase("video-1")
        assertTrue(result.isSuccess())
        assertEquals(1920, result.getOrNull()?.width)
        assertEquals(90.5, result.getOrNull()?.duration)
    }

    @Test
    fun invoke_propagatesError() = runTest {
        val repo = FakeMetadataRepository(getVideoMetadataResult = Result.error("NOT_FOUND", "Item not found"))
        val useCase = GetVideoMetadataUseCaseImpl(repo)
        val result = useCase("missing")
        assertTrue(result.isError())
        assertNull(result.getOrNull())
    }
}

class GetDocumentMetadataUseCaseTest {

    @Test
    fun invoke_returnsRepositoryGetDocumentMetadataResult() = runTest {
        val meta = DocumentMetadata(title = "Report", author = "Jane", pageCount = 42, wordCount = 5000)
        val repo = FakeMetadataRepository(getDocumentMetadataResult = Result.success(meta))
        val useCase = GetDocumentMetadataUseCaseImpl(repo)
        val result = useCase("doc-1")
        assertTrue(result.isSuccess())
        assertEquals("Report", result.getOrNull()?.title)
        assertEquals(42, result.getOrNull()?.pageCount)
    }

    @Test
    fun invoke_propagatesError() = runTest {
        val repo = FakeMetadataRepository(getDocumentMetadataResult = Result.error("NOT_FOUND", "Item not found"))
        val useCase = GetDocumentMetadataUseCaseImpl(repo)
        val result = useCase("missing")
        assertTrue(result.isError())
        assertNull(result.getOrNull())
    }
}
