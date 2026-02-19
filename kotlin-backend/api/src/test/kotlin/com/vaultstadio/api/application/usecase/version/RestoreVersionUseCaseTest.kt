/**
 * RestoreVersionUseCase unit tests.
 */

package com.vaultstadio.api.application.usecase.version

import arrow.core.Either
import com.vaultstadio.core.domain.model.FileVersion
import com.vaultstadio.core.domain.service.FileVersionService
import com.vaultstadio.core.domain.service.RestoreVersionInput
import com.vaultstadio.core.exception.ItemNotFoundException
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RestoreVersionUseCaseTest {

    private val fileVersionService: FileVersionService = mockk()
    private val useCase = RestoreVersionUseCaseImpl(fileVersionService)

    @Test
    fun invokeDelegatesToFileVersionServiceAndReturnsRightFileVersion() = runTest {
        val now = Clock.System.now()
        val input = RestoreVersionInput(
            itemId = "item-1",
            versionNumber = 1,
            comment = "Restore",
        )
        val version = FileVersion(
            itemId = "item-1",
            versionNumber = 2,
            size = 100L,
            checksum = "abc",
            storageKey = "key-2",
            createdBy = "user-1",
            createdAt = now,
            isLatest = true,
            restoredFrom = 1,
        )
        coEvery { fileVersionService.restoreVersion(input, "user-1") } returns Either.Right(version)

        val result = useCase(input, "user-1")

        assertTrue(result.isRight())
        assertEquals(2, (result as Either.Right).value.versionNumber)
        assertEquals(1, result.value.restoredFrom)
    }

    @Test
    fun invokeReturnsLeftWhenFileVersionServiceReturnsLeft() = runTest {
        val input = RestoreVersionInput(
            itemId = "item-1",
            versionNumber = 99,
            comment = null,
        )
        coEvery { fileVersionService.restoreVersion(input, "user-1") } returns
            Either.Left(ItemNotFoundException(itemId = "item-1"))

        val result = useCase(input, "user-1")

        assertTrue(result.isLeft())
        assertTrue((result as Either.Left).value is ItemNotFoundException)
    }
}
