/**
 * ResolveConflictUseCase unit tests.
 */

package com.vaultstadio.api.application.usecase.sync

import arrow.core.Either
import com.vaultstadio.core.domain.model.ChangeType
import com.vaultstadio.core.domain.model.ConflictResolution
import com.vaultstadio.core.domain.model.ConflictType
import com.vaultstadio.core.domain.model.SyncChange
import com.vaultstadio.core.domain.model.SyncConflict
import com.vaultstadio.core.domain.service.SyncService
import com.vaultstadio.core.exception.ItemNotFoundException
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ResolveConflictUseCaseTest {

    private val syncService: SyncService = mockk()
    private val useCase = ResolveConflictUseCaseImpl(syncService)

    @Test
    fun invokeDelegatesToSyncServiceAndReturnsRightSyncConflict() = runTest {
        val now = Clock.System.now()
        val localChange = SyncChange(
            itemId = "item-1",
            changeType = ChangeType.MODIFY,
            userId = "user-1",
            timestamp = now,
            cursor = 1L,
        )
        val remoteChange = SyncChange(
            itemId = "item-1",
            changeType = ChangeType.MODIFY,
            userId = "user-1",
            timestamp = now,
            cursor = 2L,
        )
        val conflict = SyncConflict(
            itemId = "item-1",
            localChange = localChange,
            remoteChange = remoteChange,
            conflictType = ConflictType.EDIT_CONFLICT,
            resolution = ConflictResolution.KEEP_LOCAL,
            resolvedAt = now,
            createdAt = now,
        )
        coEvery {
            syncService.resolveConflict("conflict-1", ConflictResolution.KEEP_LOCAL, "user-1")
        } returns Either.Right(conflict)

        val result = useCase("conflict-1", ConflictResolution.KEEP_LOCAL, "user-1")

        assertTrue(result.isRight())
        assertEquals("item-1", (result as Either.Right).value.itemId)
        assertEquals(ConflictResolution.KEEP_LOCAL, result.value.resolution)
    }

    @Test
    fun invokeReturnsLeftWhenSyncServiceReturnsLeft() = runTest {
        coEvery { syncService.resolveConflict(any(), any(), any()) } returns
            Either.Left(ItemNotFoundException(itemId = "x"))

        val result = useCase("conflict-1", ConflictResolution.KEEP_REMOTE, "user-1")

        assertTrue(result.isLeft())
        assertTrue((result as Either.Left).value is ItemNotFoundException)
    }
}
