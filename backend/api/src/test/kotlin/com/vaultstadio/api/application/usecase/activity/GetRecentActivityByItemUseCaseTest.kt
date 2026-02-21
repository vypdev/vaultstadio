/**
 * GetRecentActivityByItemUseCase unit tests.
 */

package com.vaultstadio.api.application.usecase.activity

import arrow.core.Either
import com.vaultstadio.application.usecase.activity.GetRecentActivityByItemUseCaseImpl
import com.vaultstadio.domain.activity.model.Activity
import com.vaultstadio.domain.activity.model.ActivityType
import com.vaultstadio.domain.activity.repository.ActivityRepository
import com.vaultstadio.domain.common.exception.ItemNotFoundException
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GetRecentActivityByItemUseCaseTest {

    private val activityRepository: ActivityRepository = mockk()
    private val useCase = GetRecentActivityByItemUseCaseImpl(activityRepository)

    @Test
    fun invokeReturnsListWhenRepositorySucceeds() = runTest {
        val activities = listOf(
            Activity(
                id = "a1",
                type = ActivityType.FILE_UPLOADED,
                userId = "user-1",
                itemId = "item-1",
                createdAt = Clock.System.now(),
            ),
        )
        coEvery { activityRepository.getRecentByItem("item-1", 20) } returns Either.Right(activities)

        val result = useCase("item-1", 20)

        assertTrue(result.isRight())
        assertEquals(1, (result as Either.Right<List<Activity>>).value.size)
    }

    @Test
    fun invokeReturnsLeftWhenRepositoryFails() = runTest {
        coEvery { activityRepository.getRecentByItem("item-1", 10) } returns
            Either.Left(ItemNotFoundException(itemId = "item-1"))

        val result = useCase("item-1", 10)

        assertTrue(result.isLeft())
    }
}
