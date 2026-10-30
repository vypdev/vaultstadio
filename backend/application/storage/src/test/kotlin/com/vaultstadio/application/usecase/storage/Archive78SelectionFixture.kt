package com.vaultstadio.application.usecase.storage

import arrow.core.right
import com.vaultstadio.core.domain.service.StorageService
import com.vaultstadio.domain.common.pagination.PagedResult
import com.vaultstadio.domain.storage.model.ArchiveLimits
import com.vaultstadio.domain.storage.model.ItemType
import com.vaultstadio.domain.storage.model.StorageItem
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.datetime.Instant

internal abstract class Archive78SelectionFixture {
    protected val storage = mockk<StorageService>()
    protected fun file(id: String = "file78", name: String = id, size: Long = 1, parent: String? = null) = StorageItem(
        id = id, name = name, size = size, parentId = parent, path = "/$name", type = ItemType.FILE,
        ownerId = "owner78", storageKey = id,
        createdAt = Instant.fromEpochMilliseconds(
            0,
        ),
        updatedAt = Instant.fromEpochMilliseconds(0),
    )
    protected fun folder(
        id: String = "folder78",
        parent: String? = null,
    ) = file(id, parent = parent).copy(type = ItemType.FOLDER, size = 0)
    protected fun metadata(vararg items: StorageItem) {
        items.forEach { item -> coEvery { storage.getItem(item.id, "owner78") } returns item.right() }
    }
    protected fun children(parent: String, vararg items: StorageItem) {
        coEvery { storage.listFolder(parent, "owner78", any()) } returns
            PagedResult(items.toList(), items.size.toLong(), 0, 256).right()
    }
    protected suspend fun select(
        ids: List<String>,
        limits: ArchiveLimits = ArchiveLimits(),
    ) = ArchiveSelection(storage, "owner78", limits).collect(ids)
}
