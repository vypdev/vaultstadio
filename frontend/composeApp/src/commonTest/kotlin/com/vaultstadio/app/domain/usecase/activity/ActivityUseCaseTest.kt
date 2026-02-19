/**
 * Unit tests for activity use cases (GetRecentActivity, GetItemActivity).
 * Uses a fake ActivityRepository to avoid platform/DI.
 */

package com.vaultstadio.app.domain.usecase.activity

import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.data.repository.ActivityRepository
import com.vaultstadio.app.domain.model.Activity
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private val testInstant = Instant.fromEpochMilliseconds(0L)

private fun testActivity(
    id: String = "act-1",
    type: String = "FILE_UPLOAD",
    itemId: String? = "item-1",
) = Activity(
    id = id,
    type = type,
    userId = "user-1",
    itemId = itemId,
    itemPath = "/path",
    details = null,
    createdAt = testInstant,
)

private class FakeActivityRepository(
    var getRecentActivityResult: Result<List<Activity>> = Result.success(emptyList()),
    var getItemActivityResult: Result<List<Activity>> = Result.success(emptyList()),
) : ActivityRepository {

    override suspend fun getRecentActivity(limit: Int): Result<List<Activity>> = getRecentActivityResult

    override suspend fun getItemActivity(itemId: String, limit: Int): Result<List<Activity>> = getItemActivityResult
}

class GetRecentActivityUseCaseTest {

    @Test
    fun invoke_returnsRepositoryGetRecentActivityResult() = runTest {
        val activities = listOf(testActivity("a1"), testActivity("a2"))
        val repo = FakeActivityRepository(getRecentActivityResult = Result.success(activities))
        val useCase = GetRecentActivityUseCaseImpl(repo)
        val result = useCase(limit = 20)
        assertTrue(result.isSuccess())
        assertEquals(2, result.getOrNull()?.size)
    }

    @Test
    fun invoke_propagatesError() = runTest {
        val repo = FakeActivityRepository(getRecentActivityResult = Result.error("UNAUTHORIZED", "Not logged in"))
        val useCase = GetRecentActivityUseCaseImpl(repo)
        val result = useCase()
        assertTrue(result.isError())
        assertNull(result.getOrNull())
    }
}

class GetItemActivityUseCaseTest {

    @Test
    fun invoke_returnsRepositoryGetItemActivityResult() = runTest {
        val activities = listOf(testActivity("a1", itemId = "item-1"))
        val repo = FakeActivityRepository(getItemActivityResult = Result.success(activities))
        val useCase = GetItemActivityUseCaseImpl(repo)
        val result = useCase("item-1", limit = 20)
        assertTrue(result.isSuccess())
        assertEquals(1, result.getOrNull()?.size)
        assertEquals("item-1", result.getOrNull()?.get(0)?.itemId)
    }

    @Test
    fun invoke_propagatesError() = runTest {
        val repo = FakeActivityRepository(getItemActivityResult = Result.error("NOT_FOUND", "Item not found"))
        val useCase = GetItemActivityUseCaseImpl(repo)
        val result = useCase("missing")
        assertTrue(result.isError())
    }
}
