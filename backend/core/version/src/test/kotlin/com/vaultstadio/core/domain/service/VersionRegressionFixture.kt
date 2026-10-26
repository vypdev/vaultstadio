package com.vaultstadio.core.domain.service

import arrow.core.right
import com.vaultstadio.core.domain.repository.FileVersionRepository
import com.vaultstadio.domain.storage.model.ItemType
import com.vaultstadio.domain.storage.model.StorageItem
import com.vaultstadio.domain.storage.repository.StorageItemRepository
import com.vaultstadio.domain.version.repository.VersionContentReferenceRepository
import com.vaultstadio.domain.version.repository.VersionItemScopeRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.datetime.Clock
import org.junit.jupiter.api.BeforeEach

abstract class VersionRegressionFixture {
    protected lateinit var versionRepository: FileVersionRepository
    protected lateinit var itemRepository: StorageItemRepository
    protected lateinit var storageBackend: StorageBackend
    protected lateinit var references: VersionContentReferenceRepository
    protected lateinit var scopes: VersionItemScopeRepository
    protected lateinit var service: FileVersionService

    @BeforeEach
    fun setup() {
        versionRepository = mockk()
        itemRepository = mockk()
        storageBackend = mockk()
        scopes = mockk()
        references = mockk()
        coEvery { references.hasOtherReferences(any(), any()) } returns false.right()
        service = FileVersionService(versionRepository, itemRepository, storageBackend, scopes, references)
        coEvery { scopes.findItemId(any()) } returns "item-1".right()
        coEvery { itemRepository.findById(any()) } answers {
            StorageItem(
                id = firstArg(),
                name = "test.txt",
                path = "/test.txt",
                type = ItemType.FILE,
                ownerId = "user-1",
                createdAt = Clock.System.now(),
                updatedAt = Clock.System.now(),
            ).right()
        }
    }
}
