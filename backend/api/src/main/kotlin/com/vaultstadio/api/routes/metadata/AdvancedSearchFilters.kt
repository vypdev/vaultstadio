package com.vaultstadio.api.routes.metadata

import com.vaultstadio.domain.storage.model.StorageItem
import kotlinx.datetime.Instant

internal fun AdvancedSearchRequest.searchDates() =
    SearchDateBounds(parseSearchDate(fromDate, "fromDate"), parseSearchDate(toDate, "toDate"))

private fun parseSearchDate(value: String?, field: String): Instant? {
    if (value == null) return null
    return try {
        Instant.parse(value)
    } catch (failure: IllegalArgumentException) {
        throw IllegalArgumentException("Invalid $field", failure)
    }
}

internal fun AdvancedSearchRequest.filterProjection(
    items: List<StorageItem>,
    dates: SearchDateBounds,
): List<StorageItem> {
    var filtered = items
    fileTypes?.let { types ->
        filtered = filtered.filter { item -> types.any { item.mimeType?.contains(it, ignoreCase = true) == true } }
    }
    minSize?.let { size -> filtered = filtered.filter { it.size >= size } }
    maxSize?.let { size -> filtered = filtered.filter { it.size <= size } }
    dates.from?.let { from -> filtered = filtered.filter { it.createdAt >= from } }
    dates.to?.let { to -> filtered = filtered.filter { it.createdAt <= to } }
    return filtered
}
