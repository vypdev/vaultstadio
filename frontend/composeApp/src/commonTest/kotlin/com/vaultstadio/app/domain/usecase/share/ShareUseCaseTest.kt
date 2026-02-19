/**
 * Unit tests for share use cases (CreateShare, GetMyShares, GetSharedWithMe, DeleteShare).
 * Uses a fake ShareRepository to avoid platform/DI.
 */

package com.vaultstadio.app.domain.usecase.share

import com.vaultstadio.app.data.network.ApiResult
import com.vaultstadio.app.data.repository.ShareRepository
import com.vaultstadio.app.domain.model.ShareLink
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private val testInstant = Instant.fromEpochMilliseconds(0L)

private fun testShareLink(
    id: String = "s1",
    itemId: String = "item-1",
    token: String = "tok",
    url: String = "https://example.com/share/tok",
) = ShareLink(
    id = id,
    itemId = itemId,
    token = token,
    url = url,
    expiresAt = testInstant,
    hasPassword = false,
    maxDownloads = null,
    downloadCount = 0,
    isActive = true,
    createdAt = testInstant,
    createdBy = "user1",
    sharedWithUsers = emptyList(),
)

private class FakeShareRepository(
    var getMySharesResult: ApiResult<List<ShareLink>> = ApiResult.success(emptyList()),
    var getSharedWithMeResult: ApiResult<List<ShareLink>> = ApiResult.success(emptyList()),
    var createShareResult: ApiResult<ShareLink> = ApiResult.success(testShareLink()),
    var deleteShareResult: ApiResult<Unit> = ApiResult.success(Unit),
) : ShareRepository {

    override suspend fun getMyShares(): ApiResult<List<ShareLink>> = getMySharesResult

    override suspend fun getSharedWithMe(): ApiResult<List<ShareLink>> = getSharedWithMeResult

    override suspend fun createShare(
        itemId: String,
        expiresInDays: Int?,
        password: String?,
        maxDownloads: Int?,
    ): ApiResult<ShareLink> = createShareResult

    override suspend fun deleteShare(shareId: String): ApiResult<Unit> = deleteShareResult
}

class CreateShareUseCaseTest {

    @Test
    fun invoke_returnsRepositoryCreateShareResult() = runTest {
        val share = testShareLink(id = "s2", itemId = "item-2")
        val repo = FakeShareRepository(createShareResult = ApiResult.success(share))
        val useCase = CreateShareUseCaseImpl(repo)
        val result = useCase("item-2", null, null, null)
        assertTrue(result.isSuccess())
        assertEquals(share, result.getOrNull())
    }

    @Test
    fun invoke_withOptionalParams_forwardsToRepository() = runTest {
        val share = testShareLink(id = "s3", token = "secret-token")
        val repo = FakeShareRepository(createShareResult = ApiResult.success(share))
        val useCase = CreateShareUseCaseImpl(repo)
        val result = useCase("item-3", expiresInDays = 7, password = "pwd", maxDownloads = 10)
        assertTrue(result.isSuccess())
        assertEquals("s3", result.getOrNull()?.id)
    }

    @Test
    fun invoke_propagatesError() = runTest {
        val repo = FakeShareRepository(createShareResult = ApiResult.error("FORBIDDEN", "No access"))
        val useCase = CreateShareUseCaseImpl(repo)
        val result = useCase("item-1")
        assertTrue(result.isError())
        assertNull(result.getOrNull())
    }
}

class GetMySharesUseCaseTest {

    @Test
    fun invoke_returnsRepositoryGetMySharesResult() = runTest {
        val list = listOf(testShareLink("s1"), testShareLink("s2"))
        val repo = FakeShareRepository(getMySharesResult = ApiResult.success(list))
        val useCase = GetMySharesUseCaseImpl(repo)
        val result = useCase()
        assertTrue(result.isSuccess())
        assertEquals(2, result.getOrNull()?.size)
        assertEquals("s1", result.getOrNull()?.get(0)?.id)
    }

    @Test
    fun invoke_propagatesError() = runTest {
        val repo = FakeShareRepository(getMySharesResult = ApiResult.error("UNAUTHORIZED", "Not logged in"))
        val useCase = GetMySharesUseCaseImpl(repo)
        val result = useCase()
        assertTrue(result.isError())
    }
}

class GetSharedWithMeUseCaseTest {

    @Test
    fun invoke_returnsRepositoryGetSharedWithMeResult() = runTest {
        val list = listOf(testShareLink("s1"))
        val repo = FakeShareRepository(getSharedWithMeResult = ApiResult.success(list))
        val useCase = GetSharedWithMeUseCaseImpl(repo)
        val result = useCase()
        assertTrue(result.isSuccess())
        assertEquals(1, result.getOrNull()?.size)
    }

    @Test
    fun invoke_propagatesError() = runTest {
        val repo = FakeShareRepository(getSharedWithMeResult = ApiResult.networkError("Offline"))
        val useCase = GetSharedWithMeUseCaseImpl(repo)
        val result = useCase()
        assertTrue(result.isError())
    }
}

class DeleteShareUseCaseTest {

    @Test
    fun invoke_returnsRepositoryDeleteShareResult() = runTest {
        val repo = FakeShareRepository(deleteShareResult = ApiResult.success(Unit))
        val useCase = DeleteShareUseCaseImpl(repo)
        val result = useCase("share-id")
        assertTrue(result.isSuccess())
    }

    @Test
    fun invoke_propagatesError() = runTest {
        val repo = FakeShareRepository(deleteShareResult = ApiResult.error("NOT_FOUND", "Share not found"))
        val useCase = DeleteShareUseCaseImpl(repo)
        val result = useCase("missing")
        assertTrue(result.isError())
    }
}
