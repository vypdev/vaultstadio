/**
 * DeleteVersionUseCase unit tests.
 */

package com.vaultstadio.api.application.usecase.version

import arrow.core.Either
import com.vaultstadio.application.usecase.version.DeleteVersionUseCaseImpl
import com.vaultstadio.core.domain.service.FileVersionService
import com.vaultstadio.domain.common.exception.ItemNotFoundException
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DeleteVersionUseCaseTest {

    private val fileVersionService: FileVersionService = mockk()
    private val useCase = DeleteVersionUseCaseImpl(fileVersionService)

    @Test
    fun invokeDelegatesToFileVersionServiceAndReturnsRightUnit() = runTest {
        coEvery { fileVersionService.deleteVersion("version-1", "user-1") } returns Either.Right(Unit)

        val result = useCase("version-1", "user-1")

        assertTrue(result.isRight())
    }

    @Test
    fun invokeReturnsLeftWhenFileVersionServiceReturnsLeft() = runTest {
        coEvery { fileVersionService.deleteVersion(any(), any()) } returns
            Either.Left(ItemNotFoundException(itemId = "version-1"))

        val result = useCase("version-1", "user-1")

        assertTrue(result.isLeft())
        assertTrue((result as Either.Left<ItemNotFoundException>).value is ItemNotFoundException)
    }
}
