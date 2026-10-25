/**
 * Unit tests for ActivityViewModel: loadActivities, onActivityClick, clearSelectedActivity, clearError.
 */

package com.vaultstadio.app.feature.activity

import com.vaultstadio.app.domain.activity.model.Activity
import com.vaultstadio.app.domain.activity.usecase.GetRecentActivityUseCase
import com.vaultstadio.app.domain.result.Result
import com.vaultstadio.app.feature.ViewModelTestBase
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private val testInstant = Instant.fromEpochMilliseconds(0L)

private fun testActivity(id: String = "act-1") = Activity(
    id = id,
    type = "FILE_UPLOADED",
    userId = "user-1",
    itemId = "item-1",
    itemPath = "/file.txt",
    details = "Uploaded file",
    createdAt = testInstant,
)

private class FakeGetRecentActivityUseCase(
    var result: Result<List<Activity>> = Result.success(emptyList()),
) : GetRecentActivityUseCase {
    override suspend fun invoke(limit: Int): Result<List<Activity>> = result
}

class ActivityViewModelTest {

    private fun createViewModel(
        getRecentActivityResult: Result<List<Activity>> = Result.success(emptyList()),
    ): ActivityViewModel = ActivityViewModel(
        getRecentActivityUseCase = FakeGetRecentActivityUseCase(getRecentActivityResult),
    )

    @Test
    fun loadActivities_success_populatesActivities() = ViewModelTestBase.runTestWithMain {
        val activities = listOf(testActivity("a1"), testActivity("a2"))
        val vm = createViewModel(getRecentActivityResult = Result.success(activities))
        vm.loadActivities()
        assertEquals(activities, vm.activities)
        assertNull(vm.error)
    }

    @Test
    fun loadActivities_error_setsError() = ViewModelTestBase.runTestWithMain {
        val vm = createViewModel(getRecentActivityResult = Result.error("ERR", "Load failed"))
        vm.loadActivities()
        assertTrue(vm.activities.isEmpty())
        assertEquals("Load failed", vm.error)
    }

    @Test
    fun onActivityClick_setsSelectedActivity() = ViewModelTestBase.runTestWithMain {
        val activity = testActivity()
        val vm = createViewModel()
        vm.onActivityClick(activity)
        assertEquals(activity, vm.selectedActivity)
    }

    @Test
    fun clearSelectedActivity_clearsSelection() = ViewModelTestBase.runTestWithMain {
        val vm = createViewModel()
        vm.onActivityClick(testActivity())
        assertEquals(testActivity(), vm.selectedActivity)
        vm.clearSelectedActivity()
        assertNull(vm.selectedActivity)
    }

    @Test
    fun clearError_clearsErrorMessage() = ViewModelTestBase.runTestWithMain {
        val vm = createViewModel(getRecentActivityResult = Result.error("ERR", "Oops"))
        vm.loadActivities()
        assertEquals("Oops", vm.error)
        vm.clearError()
        assertNull(vm.error)
    }

    @Test
    fun loadActivities_withLimit_invokesUseCaseWithLimit() = ViewModelTestBase.runTestWithMain {
        var capturedLimit: Int? = null
        val useCase = object : GetRecentActivityUseCase {
            override suspend fun invoke(limit: Int): Result<List<Activity>> {
                capturedLimit = limit
                return Result.success(emptyList())
            }
        }
        val vm = ActivityViewModel(getRecentActivityUseCase = useCase)
        vm.loadActivities(limit = 100)
        assertEquals(100, capturedLimit)
    }
}
