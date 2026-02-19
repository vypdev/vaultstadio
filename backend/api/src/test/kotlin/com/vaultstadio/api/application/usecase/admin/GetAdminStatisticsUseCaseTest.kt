/**
 * GetAdminStatisticsUseCase unit tests.
 */

package com.vaultstadio.api.application.usecase.admin

import arrow.core.Either
import com.vaultstadio.core.domain.model.StorageStatistics
import com.vaultstadio.core.domain.repository.ActivityRepository
import com.vaultstadio.core.exception.ItemNotFoundException
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GetAdminStatisticsUseCaseTest {

    private val activityRepository: ActivityRepository = mockk()
    private val useCase = GetAdminStatisticsUseCaseImpl(activityRepository)

    @Test
    fun invokeReturnsStatisticsWhenRepositorySucceeds() = runTest {
        val stats = StorageStatistics(
            totalFiles = 10L,
            totalFolders = 2L,
            totalSize = 1000L,
            totalUsers = 1L,
            activeUsers = 1L,
            totalShares = 0L,
            uploadsToday = 0L,
            downloadsToday = 0L,
            collectedAt = Clock.System.now(),
        )
        coEvery { activityRepository.getStatistics() } returns Either.Right(stats)

        val result = useCase()

        assertTrue(result.isRight())
        assertEquals(10L, (result as Either.Right).value.totalFiles)
    }

    @Test
    fun invokeReturnsLeftWhenRepositoryFails() = runTest {
        coEvery { activityRepository.getStatistics() } returns
            Either.Left(ItemNotFoundException(itemId = "x"))

        val result = useCase()

        assertTrue(result.isLeft())
    }
}
