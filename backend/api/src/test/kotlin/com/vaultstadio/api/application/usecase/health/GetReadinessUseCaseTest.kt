/**
 * GetReadinessUseCase unit tests.
 */

package com.vaultstadio.api.application.usecase.health

import arrow.core.Either
import com.vaultstadio.core.domain.repository.UserRepository
import com.vaultstadio.core.domain.service.StorageBackend
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GetReadinessUseCaseTest {

    private val userRepository: UserRepository = mockk()
    private val storageBackend: StorageBackend = mockk()
    private val useCase = GetReadinessUseCaseImpl(userRepository, storageBackend)

    @Test
    fun invokeReturnsReadyWhenDatabaseAndStorageHealthy() = runTest {
        coEvery { userRepository.countAll() } returns Either.Right(5L)
        coEvery { storageBackend.isAvailable() } returns Either.Right(true)

        val result = useCase()

        assertTrue(result.ready)
        assertEquals("healthy", result.checks["database"]?.status)
        assertEquals("healthy", result.checks["storage"]?.status)
    }

    @Test
    fun invokeReturnsNotReadyWhenDatabaseFails() = runTest {
        coEvery { userRepository.countAll() } returns Either.Left(com.vaultstadio.domain.common.exception.DatabaseException("DB down"))
        coEvery { storageBackend.isAvailable() } returns Either.Right(true)

        val result = useCase()

        assertFalse(result.ready)
        assertEquals("unhealthy", result.checks["database"]?.status)
    }
}
