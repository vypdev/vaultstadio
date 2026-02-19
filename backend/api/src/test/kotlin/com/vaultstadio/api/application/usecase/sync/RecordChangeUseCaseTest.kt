/**
 * RecordChangeUseCase unit tests.
 */

package com.vaultstadio.api.application.usecase.sync

import arrow.core.Either
import com.vaultstadio.core.domain.model.ChangeType
import com.vaultstadio.core.domain.model.SyncChange
import com.vaultstadio.core.domain.service.RecordChangeInput
import com.vaultstadio.core.domain.service.SyncService
import com.vaultstadio.core.exception.ItemNotFoundException
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RecordChangeUseCaseTest {

    private val syncService: SyncService = mockk()
    private val useCase = RecordChangeUseCaseImpl(syncService)

    @Test
    fun invokeDelegatesToSyncServiceAndReturnsRightSyncChange() = runTest {
        val now = Clock.System.now()
        val input = RecordChangeInput(
            itemId = "item-1",
            changeType = ChangeType.MODIFY,
            deviceId = "dev-1",
            oldPath = null,
            newPath = null,
            checksum = "abc",
            metadata = emptyMap(),
        )
        val change = SyncChange(
            itemId = "item-1",
            changeType = ChangeType.MODIFY,
            userId = "user-1",
            deviceId = "dev-1",
            timestamp = now,
            cursor = 1L,
        )
        coEvery { syncService.recordChange(input, "user-1") } returns Either.Right(change)

        val result = useCase(input, "user-1")

        assertTrue(result.isRight())
        assertEquals("item-1", (result as Either.Right).value.itemId)
        assertEquals(ChangeType.MODIFY, result.value.changeType)
    }

    @Test
    fun invokeReturnsLeftWhenSyncServiceReturnsLeft() = runTest {
        val input = RecordChangeInput(
            itemId = "item-1",
            changeType = ChangeType.MODIFY,
            deviceId = null,
            oldPath = null,
            newPath = null,
            checksum = null,
            metadata = emptyMap(),
        )
        coEvery { syncService.recordChange(input, "user-1") } returns
            Either.Left(ItemNotFoundException(itemId = "item-1"))

        val result = useCase(input, "user-1")

        assertTrue(result.isLeft())
        assertTrue((result as Either.Left).value is ItemNotFoundException)
    }
}
