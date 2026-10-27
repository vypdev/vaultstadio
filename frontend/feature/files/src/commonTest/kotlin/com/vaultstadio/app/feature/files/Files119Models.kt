package com.vaultstadio.app.feature.files

import com.vaultstadio.app.domain.storage.model.ItemType
import com.vaultstadio.app.domain.storage.model.PaginatedResponse
import com.vaultstadio.app.domain.storage.model.StorageItem
import com.vaultstadio.app.domain.storage.model.Visibility
import kotlin.time.Instant

internal val files119Time = Instant.parse("2026-10-27T12:00:00Z")
internal fun files119Item(id: String = "file119", type: ItemType = ItemType.FILE, name: String = id) = StorageItem(
    id = id, name = name, path = "/$name", type = type, parentId = null, size = 4,
    mimeType = "text/plain", visibility = Visibility.PRIVATE, isStarred = false, isTrashed = false,
    createdAt = files119Time, updatedAt = files119Time,
)
internal fun files119Page(items: List<StorageItem> = listOf(files119Item()), total: Long = items.size.toLong(), hasMore: Boolean = false) =
    PaginatedResponse(items, total, 1, 50, if (hasMore) 2 else 1, hasMore)

internal class Files119Preferences : FilesViewPreferences {
    var view: String? = null
    var field: String? = null
    var order: String? = null
    val writes = mutableListOf<Pair<String, String>>()
    override fun getViewMode() = view
    override fun setViewMode(value: String) { view = value; writes += "view" to value }
    override fun getSortField() = field
    override fun setSortField(value: String) { field = value; writes += "field" to value }
    override fun getSortOrder() = order
    override fun setSortOrder(value: String) { order = value; writes += "order" to value }
}
