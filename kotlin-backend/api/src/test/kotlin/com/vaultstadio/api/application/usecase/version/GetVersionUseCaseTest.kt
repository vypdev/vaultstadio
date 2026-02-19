/**
 * GetVersionUseCase unit tests.
 */

package com.vaultstadio.api.application.usecase.version

import arrow.core.Either
import com.vaultstadio.core.domain.model.FileVersion
import com.vaultstadio.core.domain.service.FileVersionService
import com.vaultstadio.core.exception.ItemNotFoundException
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GetVersionUseCaseTest {

    private val fileVersionService: FileVersionService = mockk()
    private val useCase = GetVersionUseCaseImpl(fileVersionService)

    @Test
    fun invokeDelegatesToFileVersionServiceAndReturnsRightVersion() = runTest {
        val now = Clock.System.now()
        val version = FileVersion(
            itemId = "item-1",
            versionNumber = 1,
            size = 100L,
            checksum = "abc",
            storageKey = "key-1",
            createdBy = "user-1",
            createdAt = now,
            isLatest = true,
        )
        coEvery { fileVersionService.getVersion("item-1", 1) } returns Either.Right(version)

        val result = useCase("item-1", 1)

        assertTrue(result.isRight())
        assertNotNull((result as Either.Right).value)
        assertEquals(1, result.value!!.versionNumber)
        assertEquals("item-1", result.value!!.itemId)
    }

    @Test
    fun invokeReturnsLeftWhenFileVersionServiceReturnsLeft() = runTest {
        coEvery { fileVersionService.getVersion(any(), any()) } returns
            Either.Left(ItemNotFoundException(itemId = "item-1"))

        val result = useCase("item-1", 1)

        assertTrue(result.isLeft())
        assertTrue((result as Either.Left).value is ItemNotFoundException)
    }
}
