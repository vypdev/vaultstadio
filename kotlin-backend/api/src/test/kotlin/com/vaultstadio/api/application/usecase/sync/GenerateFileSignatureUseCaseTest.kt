/**
 * GenerateFileSignatureUseCase unit tests.
 */

package com.vaultstadio.api.application.usecase.sync

import arrow.core.Either
import com.vaultstadio.core.domain.model.BlockChecksum
import com.vaultstadio.core.domain.model.FileSignature
import com.vaultstadio.core.domain.service.SyncService
import com.vaultstadio.core.exception.ItemNotFoundException
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GenerateFileSignatureUseCaseTest {

    private val syncService: SyncService = mockk()
    private val useCase = GenerateFileSignatureUseCaseImpl(syncService)

    @Test
    fun invokeDelegatesToSyncServiceAndReturnsRightFileSignature() = runTest {
        val signature = FileSignature(
            itemId = "item-1",
            versionNumber = 1,
            blockSize = 4096,
            blocks = listOf(
                BlockChecksum(index = 0, weakChecksum = 1L, strongChecksum = "abc"),
            ),
        )
        coEvery { syncService.generateFileSignature("item-1", 1, 4096) } returns Either.Right(signature)

        val result = useCase("item-1", 1, 4096)

        assertTrue(result.isRight())
        assertEquals("item-1", (result as Either.Right).value.itemId)
        assertEquals(1, result.value.blocks.size)
    }

    @Test
    fun invokeReturnsLeftWhenSyncServiceReturnsLeft() = runTest {
        coEvery { syncService.generateFileSignature(any(), any(), any()) } returns
            Either.Left(ItemNotFoundException(itemId = "item-1"))

        val result = useCase("item-1", 1, 4096)

        assertTrue(result.isLeft())
        assertTrue((result as Either.Left).value is ItemNotFoundException)
    }
}
