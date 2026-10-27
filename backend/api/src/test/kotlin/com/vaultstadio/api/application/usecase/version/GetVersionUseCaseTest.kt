/**
 * GetVersionUseCase unit tests.
 */

package com.vaultstadio.api.application.usecase.version

import arrow.core.Either
import com.vaultstadio.application.usecase.version.GetVersionUseCaseImpl
import com.vaultstadio.core.domain.model.FileVersion
import com.vaultstadio.core.domain.service.FileVersionService
import com.vaultstadio.domain.common.exception.ItemNotFoundException
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.test.assertIs

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
        coEvery { fileVersionService.getVersion("item-1", 1, "user-1") } returns Either.Right(version)

        val result = useCase("item-1", 1, "user-1")

        val actual = assertIs<Either.Right<FileVersion>>(result).value
        assertEquals(version, actual)
    }

    @Test
    fun invokeReturnsLeftWhenFileVersionServiceReturnsLeft() = runTest {
        coEvery { fileVersionService.getVersion(any(), any(), "user-1") } returns
            Either.Left(ItemNotFoundException(itemId = "item-1"))

        val result = useCase("item-1", 1, "user-1")

        assertIs<ItemNotFoundException>(assertIs<Either.Left<*>>(result).value)
    }
}
