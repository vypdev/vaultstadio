/**
 * SyncPullUseCase unit tests.
 */

package com.vaultstadio.api.application.usecase.sync

import arrow.core.Either
import com.vaultstadio.core.domain.model.SyncRequest
import com.vaultstadio.core.domain.model.SyncResponse
import com.vaultstadio.core.domain.service.SyncService
import com.vaultstadio.core.exception.ItemNotFoundException
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SyncPullUseCaseTest {

    private val syncService: SyncService = mockk()
    private val useCase = SyncPullUseCaseImpl(syncService)

    @Test
    fun invokeDelegatesToSyncServiceAndReturnsRightSyncResponse() = runTest {
        val now = Clock.System.now()
        val request = SyncRequest(
            deviceId = "dev-1",
            cursor = null,
            limit = 1000,
            includeDeleted = true,
        )
        val response = SyncResponse(
            changes = emptyList(),
            cursor = "cur-1",
            hasMore = false,
            conflicts = emptyList(),
            serverTime = now,
        )
        coEvery { syncService.sync(request, "user-1") } returns Either.Right(response)

        val result = useCase(request, "user-1")

        assertTrue(result.isRight())
        assertEquals("cur-1", (result as Either.Right).value.cursor)
    }

    @Test
    fun invokeReturnsLeftWhenSyncServiceReturnsLeft() = runTest {
        val request = SyncRequest(
            deviceId = "dev-1",
            cursor = null,
            limit = 1000,
            includeDeleted = true,
        )
        coEvery { syncService.sync(request, "user-1") } returns
            Either.Left(ItemNotFoundException(itemId = "x"))

        val result = useCase(request, "user-1")

        assertTrue(result.isLeft())
        assertTrue((result as Either.Left).value is ItemNotFoundException)
    }
}
