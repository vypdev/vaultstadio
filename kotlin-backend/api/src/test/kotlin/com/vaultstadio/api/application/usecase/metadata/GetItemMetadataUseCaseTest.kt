/**
 * GetItemMetadataUseCase unit tests.
 */

package com.vaultstadio.api.application.usecase.metadata

import arrow.core.Either
import com.vaultstadio.api.application.usecase.storage.GetItemUseCase
import com.vaultstadio.core.domain.model.ItemType
import com.vaultstadio.core.domain.model.StorageItem
import com.vaultstadio.core.domain.model.StorageItemMetadata
import com.vaultstadio.core.domain.repository.MetadataRepository
import com.vaultstadio.core.exception.ItemNotFoundException
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GetItemMetadataUseCaseTest {

    private val getItemUseCase: GetItemUseCase = mockk()
    private val metadataRepository: MetadataRepository = mockk()
    private val useCase = GetItemMetadataUseCaseImpl(getItemUseCase, metadataRepository)

    @Test
    fun `invoke returns item and metadata when getItem and repository succeed`() = runTest {
        val item = StorageItem(
            id = "item-1",
            name = "f",
            path = "/f",
            type = ItemType.FILE,
            ownerId = "user-1",
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now(),
        )
        val now = Clock.System.now()
        val metadata = listOf(
            StorageItemMetadata(
                id = "meta-1",
                itemId = "item-1",
                pluginId = "image-metadata",
                key = "width",
                value = "1920",
                createdAt = now,
                updatedAt = now,
            ),
        )
        coEvery { getItemUseCase("item-1", "user-1") } returns Either.Right(item)
        coEvery { metadataRepository.findByItemId("item-1") } returns Either.Right(metadata)

        val result = useCase("item-1", "user-1")

        assertTrue(result.isRight())
        val (resultItem, resultMeta) = (result as Either.Right).value
        assertEquals(item.id, resultItem.id)
        assertEquals(1, resultMeta.size)
        assertEquals("width", resultMeta[0].key)
    }

    @Test
    fun `invoke returns Left when getItem returns Left`() = runTest {
        coEvery { getItemUseCase("item-1", "user-1") } returns
            Either.Left(ItemNotFoundException(itemId = "item-1"))

        val result = useCase("item-1", "user-1")

        assertTrue(result.isLeft())
    }
}
