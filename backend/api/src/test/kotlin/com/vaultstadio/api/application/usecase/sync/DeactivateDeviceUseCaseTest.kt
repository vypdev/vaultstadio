/**
 * DeactivateDeviceUseCase unit tests.
 */

package com.vaultstadio.api.application.usecase.sync

import arrow.core.Either
import com.vaultstadio.application.usecase.sync.DeactivateDeviceUseCaseImpl
import com.vaultstadio.core.domain.service.SyncService
import com.vaultstadio.domain.common.exception.ItemNotFoundException
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DeactivateDeviceUseCaseTest {

    private val syncService: SyncService = mockk()
    private val useCase = DeactivateDeviceUseCaseImpl(syncService)

    @Test
    fun invokeDelegatesToSyncServiceAndReturnsRightUnit() = runTest {
        coEvery { syncService.deactivateDevice("dev-1", "user-1") } returns Either.Right(Unit)

        val result = useCase("dev-1", "user-1")

        assertTrue(result.isRight())
    }

    @Test
    fun invokeReturnsLeftWhenSyncServiceReturnsLeft() = runTest {
        coEvery { syncService.deactivateDevice(any(), any()) } returns
            Either.Left(ItemNotFoundException(itemId = "x"))

        val result = useCase("dev-1", "user-1")

        assertTrue(result.isLeft())
        assertTrue((result as Either.Left<ItemNotFoundException>).value is ItemNotFoundException)
    }
}
