/**
 * GetQuotaUseCase unit tests.
 */

package com.vaultstadio.api.application.usecase.user

import arrow.core.Either
import com.vaultstadio.application.usecase.user.GetQuotaUseCaseImpl
import com.vaultstadio.domain.storage.model.StorageQuota
import com.vaultstadio.core.domain.service.UserService
import com.vaultstadio.domain.common.exception.ItemNotFoundException
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GetQuotaUseCaseTest {

    private val userService: UserService = mockk()
    private val useCase = GetQuotaUseCaseImpl(userService)

    @Test
    fun `invoke delegates to userService and returns Right StorageQuota`() = runTest {
        val userId = "user-1"
        val quota = StorageQuota(
            userId = userId,
            usedBytes = 1024L,
            quotaBytes = 10_000_000L,
            fileCount = 5,
            folderCount = 2,
        )
        coEvery { userService.getQuota(userId) } returns Either.Right(quota)

        val result = useCase(userId)

        assertTrue(result.isRight())
        assertTrue((result as Either.Right<StorageQuota>).value.usedBytes == 1024L)
    }

    @Test
    fun `invoke returns Left when userService returns Left`() = runTest {
        coEvery { userService.getQuota(any()) } returns
            Either.Left(ItemNotFoundException("User not found"))

        val result = useCase("missing")

        assertTrue(result.isLeft())
    }
}
