package com.vaultstadio.infrastructure.persistence

import com.vaultstadio.domain.storage.model.ItemType
import com.vaultstadio.domain.storage.model.StorageItem
import com.vaultstadio.domain.storage.model.Visibility
import com.vaultstadio.infrastructure.persistence.entities.StorageItemsTable
import org.jetbrains.exposed.v1.core.ResultRow

internal fun ResultRow.toStorageItemModel() = StorageItem(
    id = this[StorageItemsTable.id],
    name = this[StorageItemsTable.name],
    path = this[StorageItemsTable.path],
    type = ItemType.valueOf(this[StorageItemsTable.type]),
    parentId = this[StorageItemsTable.parentId],
    ownerId = this[StorageItemsTable.ownerId],
    size = this[StorageItemsTable.size],
    mimeType = this[StorageItemsTable.mimeType],
    checksum = this[StorageItemsTable.checksum],
    storageKey = this[StorageItemsTable.storageKey],
    visibility = Visibility.valueOf(this[StorageItemsTable.visibility]),
    isTrashed = this[StorageItemsTable.isTrashed],
    isStarred = this[StorageItemsTable.isStarred],
    createdAt = this[StorageItemsTable.createdAt],
    updatedAt = this[StorageItemsTable.updatedAt],
    trashedAt = this[StorageItemsTable.trashedAt],
    version = this[StorageItemsTable.version],
)
