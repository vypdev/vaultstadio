package com.vaultstadio.core.domain.service

import arrow.core.Either
import arrow.core.right
import com.vaultstadio.core.domain.event.EventBus
import com.vaultstadio.domain.common.exception.StorageException
import com.vaultstadio.domain.storage.model.ItemType
import com.vaultstadio.domain.storage.model.StorageItem
import com.vaultstadio.domain.storage.model.Visibility
import com.vaultstadio.domain.storage.repository.StorageItemRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.datetime.Clock

internal const val ACTOR = "alice"
internal const val FOREIGN = "bob"

internal class StorageAuthorizationFixture {
    val repository = mockk<StorageItemRepository>()
    val backend = mockk<StorageBackend>()
    val events = mockk<EventBus>(relaxed = true)
    val service = StorageService(repository, backend, events)

    fun source(parentId: String? = null) {
        coEvery { repository.findById("source") } returns item("source", parentId = parentId).right()
    }

    suspend fun parentWrite(operation: String): Either<StorageException, *> = when (operation) {
        "create" -> service.createFolder(CreateFolderInput("child", "parent", ACTOR))
        "get-or-create" -> service.getOrCreateFolder("child", "parent", ACTOR)
        "upload" -> service.uploadFile(
            UploadFileInput("child", "parent", ACTOR, "text/plain", 1, byteArrayOf(1).inputStream()),
        )
        "move" -> service.moveItem(MoveItemInput("source", "parent", userId = ACTOR))
        "copy" -> service.copyItem(CopyItemInput("source", "parent", userId = ACTOR))
        "restore" -> service.restoreItem("source", ACTOR)
        else -> error("Unknown operation: $operation")
    }

    fun assertNoEffects() {
        coVerify(exactly = 0) {
            repository.create(any())
            repository.update(any())
            repository.delete(any())
            backend.store(any(), any(), any())
            backend.copy(any())
            backend.delete(any())
            backend.retrieve(any())
            events.publish(any())
        }
    }

    fun assertNoNameLookup() {
        coVerify(exactly = 0) {
            repository.existsByPath(any(), any())
            repository.findByPath(any(), any())
        }
    }
}

internal fun item(
    id: String = "parent",
    owner: String = ACTOR,
    visibility: Visibility = Visibility.PRIVATE,
    type: ItemType = ItemType.FOLDER,
    trashed: Boolean = false,
    parentId: String? = null,
): StorageItem {
    val now = Clock.System.now()
    return StorageItem(
        id = id, name = id, path = "/$id", ownerId = owner, type = type,
        parentId = parentId, visibility = visibility, isTrashed = trashed,
        createdAt = now, updatedAt = now,
    )
}
