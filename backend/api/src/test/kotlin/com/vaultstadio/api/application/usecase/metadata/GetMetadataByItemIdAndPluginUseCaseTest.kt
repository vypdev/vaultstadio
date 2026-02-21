/**
 * GetMetadataByItemIdAndPluginUseCase unit tests.
 */

package com.vaultstadio.api.application.usecase.metadata

import arrow.core.Either
import com.vaultstadio.core.domain.model.StorageItemMetadata
import com.vaultstadio.core.domain.repository.MetadataRepository
import com.vaultstadio.domain.common.exception.DatabaseException
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GetMetadataByItemIdAndPluginUseCaseTest {

    private val metadataRepository: MetadataRepository = mockk()
    private val useCase = GetMetadataByItemIdAndPluginUseCaseImpl(metadataRepository)

    @Test
    fun `invoke delegates to repository and returns Right list`() = runTest {
        val now = Clock.System.now()
        val metadata = listOf(
            StorageItemMetadata(
                id = "meta-1",
                itemId = "item-1",
                pluginId = "image-metadata",
                key = "height",
                value = "1080",
                createdAt = now,
                updatedAt = now,
            ),
        )
        coEvery { metadataRepository.findByItemIdAndPluginId("item-1", "image-metadata") } returns Either.Right(metadata)

        val result = useCase("item-1", "image-metadata")

        assertTrue(result.isRight())
        assertEquals(1, (result as Either.Right<*>).value.size)
        assertEquals("height", result.value[0].key)
    }

    @Test
    fun `invoke returns Left when repository returns Left`() = runTest {
        coEvery { metadataRepository.findByItemIdAndPluginId(any(), any()) } returns
            Either.Left(DatabaseException("DB error"))

        val result = useCase("item-1", "plugin")

        assertTrue(result.isLeft())
    }
}
