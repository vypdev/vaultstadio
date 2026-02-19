/**
 * ApplyRetentionPolicyUseCase unit tests.
 */

package com.vaultstadio.api.application.usecase.version

import arrow.core.Either
import com.vaultstadio.core.domain.model.VersionRetentionPolicy
import com.vaultstadio.core.domain.service.FileVersionService
import com.vaultstadio.core.exception.ItemNotFoundException
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ApplyRetentionPolicyUseCaseTest {

    private val fileVersionService: FileVersionService = mockk()
    private val useCase = ApplyRetentionPolicyUseCaseImpl(fileVersionService)

    @Test
    fun invokeDelegatesToFileVersionServiceAndReturnsRightList() = runTest {
        val policy = VersionRetentionPolicy(
            maxVersions = 10,
            maxAgeDays = 90,
            minVersionsToKeep = 1,
            excludePatterns = emptyList(),
        )
        val deletedIds = listOf("version-1", "version-2")
        coEvery { fileVersionService.applyRetentionPolicy("item-1", policy) } returns
            Either.Right(deletedIds)

        val result = useCase("item-1", policy)

        assertTrue(result.isRight())
        assertEquals(2, (result as Either.Right).value.size)
        assertEquals("version-1", result.value[0])
    }

    @Test
    fun invokeReturnsLeftWhenFileVersionServiceReturnsLeft() = runTest {
        val policy = VersionRetentionPolicy()
        coEvery { fileVersionService.applyRetentionPolicy(any(), any()) } returns
            Either.Left(ItemNotFoundException(itemId = "item-1"))

        val result = useCase("item-1", policy)

        assertTrue(result.isLeft())
        assertTrue((result as Either.Left).value is ItemNotFoundException)
    }
}
