/**
 * CompareVersionsUseCase unit tests.
 */

package com.vaultstadio.api.application.usecase.version

import arrow.core.Either
import com.vaultstadio.core.domain.model.VersionDiff
import com.vaultstadio.core.domain.service.FileVersionService
import com.vaultstadio.core.exception.ItemNotFoundException
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CompareVersionsUseCaseTest {

    private val fileVersionService: FileVersionService = mockk()
    private val useCase = CompareVersionsUseCaseImpl(fileVersionService)

    @Test
    fun invokeDelegatesToFileVersionServiceAndReturnsRightVersionDiff() = runTest {
        val diff = VersionDiff(
            fromVersion = 1,
            toVersion = 2,
            sizeChange = 50L,
            additions = 10,
            deletions = 5,
            isBinary = false,
            patches = emptyList(),
        )
        coEvery { fileVersionService.compareVersions("item-1", 1, 2) } returns Either.Right(diff)

        val result = useCase("item-1", 1, 2)

        assertTrue(result.isRight())
        assertEquals(1, (result as Either.Right).value.fromVersion)
        assertEquals(2, result.value.toVersion)
        assertEquals(50L, result.value.sizeChange)
    }

    @Test
    fun invokeReturnsLeftWhenFileVersionServiceReturnsLeft() = runTest {
        coEvery { fileVersionService.compareVersions(any(), any(), any()) } returns
            Either.Left(ItemNotFoundException(itemId = "item-1"))

        val result = useCase("item-1", 1, 2)

        assertTrue(result.isLeft())
        assertTrue((result as Either.Left).value is ItemNotFoundException)
    }
}
