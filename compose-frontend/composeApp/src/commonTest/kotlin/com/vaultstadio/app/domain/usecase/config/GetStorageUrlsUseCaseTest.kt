/**
 * Unit tests for GetStorageUrlsUseCase and GetShareUrlUseCase.
 * Uses a fake ConfigRepository to avoid platform/DI.
 */

package com.vaultstadio.app.domain.usecase.config

import com.vaultstadio.app.data.repository.ConfigRepository
import kotlin.test.Test
import kotlin.test.assertEquals

private class FakeConfigRepository(
    private val baseUrl: String = "https://api.example.com",
) : ConfigRepository {
    override fun getApiBaseUrl(): String = baseUrl
}

class GetStorageUrlsUseCaseTest {

    @Test
    fun downloadUrl_returnsCorrectPath() {
        val repo = FakeConfigRepository("https://api.test")
        val useCase = GetStorageUrlsUseCase(repo)
        assertEquals(
            "https://api.test/api/v1/storage/download/item-1",
            useCase.downloadUrl("item-1"),
        )
    }

    @Test
    fun thumbnailUrl_includesSizeParameter() {
        val repo = FakeConfigRepository("https://api.test")
        val useCase = GetStorageUrlsUseCase(repo)
        assertEquals(
            "https://api.test/api/v1/storage/item/item-1/thumbnail?size=medium",
            useCase.thumbnailUrl("item-1", "medium"),
        )
    }

    @Test
    fun thumbnailUrl_usesDefaultSize() {
        val repo = FakeConfigRepository("https://api.test")
        val useCase = GetStorageUrlsUseCase(repo)
        assertEquals(
            "https://api.test/api/v1/storage/item/item-1/thumbnail?size=medium",
            useCase.thumbnailUrl("item-1"),
        )
    }

    @Test
    fun previewUrl_returnsCorrectPath() {
        val repo = FakeConfigRepository("https://api.test")
        val useCase = GetStorageUrlsUseCase(repo)
        assertEquals(
            "https://api.test/api/v1/storage/item/item-1/preview",
            useCase.previewUrl("item-1"),
        )
    }

    @Test
    fun batchDownloadZipUrl_returnsCorrectPath() {
        val repo = FakeConfigRepository("https://api.test")
        val useCase = GetStorageUrlsUseCase(repo)
        assertEquals(
            "https://api.test/api/v1/storage/batch/download-zip",
            useCase.batchDownloadZipUrl(),
        )
    }
}

class GetShareUrlUseCaseTest {

    @Test
    fun invoke_returnsShareUrlWithToken() {
        val repo = FakeConfigRepository("https://api.test")
        val useCase = GetShareUrlUseCase(repo)
        assertEquals(
            "https://api.test/share/abc123token",
            useCase("abc123token"),
        )
    }
}

class GetVersionUrlsUseCaseTest {

    @Test
    fun downloadUrl_returnsCorrectPath() {
        val repo = FakeConfigRepository("https://api.test")
        val useCase = GetVersionUrlsUseCase(repo)
        assertEquals(
            "https://api.test/api/v1/versions/item-1/download/2",
            useCase.downloadUrl("item-1", 2),
        )
    }
}

class GetCollaborationUrlUseCaseTest {

    @Test
    fun invoke_returnsBaseUrl() {
        val repo = FakeConfigRepository("https://api.test")
        val useCase = GetCollaborationUrlUseCase(repo)
        assertEquals("https://api.test", useCase())
    }
}
