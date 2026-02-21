/**
 * GetRecentActivityByUserUseCase unit tests.
 */

package com.vaultstadio.api.application.usecase.activity

import arrow.core.Either
import com.vaultstadio.core.domain.model.Activity
import com.vaultstadio.core.domain.model.ActivityType
import com.vaultstadio.core.domain.repository.ActivityRepository
import com.vaultstadio.domain.common.exception.DatabaseException
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GetRecentActivityByUserUseCaseTest {

    private val activityRepository: ActivityRepository = mockk()
    private val useCase = GetRecentActivityByUserUseCaseImpl(activityRepository)

    @Test
    fun `invoke delegates to repository and returns Right list`() = runTest {
        val userId = "user-1"
        val limit = 10
        val activities = listOf(
            Activity(
                id = "a1",
                type = ActivityType.FILE_UPLOADED,
                userId = userId,
                itemId = "item-1",
                details = null,
                createdAt = Clock.System.now(),
            ),
        )
        coEvery { activityRepository.getRecentByUser(userId, limit) } returns Either.Right(activities)

        val result = useCase(userId, limit)

        assertTrue(result.isRight())
        assertEquals(1, (result as Either.Right<*>).value.size)
    }

    @Test
    fun `invoke returns Left when repository returns Left`() = runTest {
        coEvery { activityRepository.getRecentByUser(any(), any()) } returns
            Either.Left(DatabaseException("DB error"))

        val result = useCase("user-1", 20)

        assertTrue(result.isLeft())
    }
}
