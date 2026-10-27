package com.vaultstadio.api.routes.storage

import com.vaultstadio.api.config.jsonConfig
import com.vaultstadio.domain.common.exception.ValidationException
import com.vaultstadio.domain.common.pagination.SortOrder
import com.vaultstadio.domain.storage.model.StorageSelection
import com.vaultstadio.domain.storage.repository.SortField
import io.ktor.http.Parameters
import io.ktor.server.application.ApplicationCall
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.ContentTransformationException
import io.ktor.server.request.receive
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.intOrNull
import java.util.Locale

internal enum class SelectionSurface(val defaultLimit: Int, val offset: Boolean = false, val sort: Boolean = false) {
    FOLDER(100, true, true),
    SEARCH(50, true),
    RECENT(20),
    SUGGESTIONS(10),
    METADATA(50),
}

internal data class StorageRequestSelection(
    val page: StorageSelection,
    val sortField: SortField,
    val sortOrder: SortOrder,
)

internal fun ApplicationCall.storageSelection(surface: SelectionSurface): StorageRequestSelection =
    parseStorageSelection(request.queryParameters, surface)

internal fun parseStorageSelection(parameters: Parameters, surface: SelectionSurface): StorageRequestSelection {
    val limit = queryInteger(parameters, "limit", surface.defaultLimit)
    val offset = if (surface.offset) {
        queryInteger(parameters, "offset", 0)
    } else {
        rejectUnsupported(parameters, "offset")
        0
    }
    val field = if (surface.sort) {
        sortToken(parameters, "sortBy", SortField.NAME)
    } else {
        rejectUnsupported(parameters, "sortBy")
        SortField.NAME
    }
    val order = if (surface.sort) {
        sortToken(parameters, "sortOrder", SortOrder.ASC)
    } else {
        rejectUnsupported(parameters, "sortOrder")
        SortOrder.ASC
    }
    return StorageRequestSelection(checkedBodySelection(limit, offset), field, order)
}

internal fun checkedBodySelection(limit: Int, offset: Int): StorageSelection =
    StorageSelection.create(limit, offset).fold(
        { throw ValidationException(field = it.parameter.name.lowercase()) },
        { it },
    )

internal suspend inline fun <reified T : Any> ApplicationCall.receiveSelectionRequest(): T = try {
    val body = receive<JsonObject>()
    // Validate the wire values before the shared serializer can coerce null to a default.
    for (field in listOf("limit", "offset")) {
        val value = body[field] ?: continue
        if (!isSelectionInteger(value)) {
            throw ValidationException(field = field)
        }
    }
    jsonConfig.decodeFromJsonElement<T>(body)
} catch (_: ContentTransformationException) {
    throw ValidationException(field = "selection")
} catch (_: BadRequestException) {
    throw ValidationException(field = "selection")
} catch (_: SerializationException) {
    throw ValidationException(field = "selection")
}

private fun queryInteger(parameters: Parameters, field: String, default: Int): Int {
    val value = singleValue(parameters, field) ?: return default
    if (value.isEmpty() || value.any { it !in '0'..'9' }) invalid(field)
    return value.toIntOrNull() ?: invalid(field)
}

private inline fun <reified T : Enum<T>> sortToken(parameters: Parameters, field: String, default: T): T {
    val value = singleValue(parameters, field) ?: return default
    if (!isAsciiSortToken(value)) invalid(field)
    return enumValues<T>().firstOrNull { it.name == value.uppercase(Locale.ROOT) } ?: invalid(field)
}

private fun singleValue(parameters: Parameters, field: String): String? {
    val values = parameters.getAll(field) ?: return null
    if (values.size != 1) invalid(field)
    return values.single()
}

private fun rejectUnsupported(parameters: Parameters, field: String) {
    if (parameters.contains(field)) invalid(field)
}

private fun invalid(field: String): Nothing = throw ValidationException(field = field)

internal fun isSelectionInteger(value: JsonElement): Boolean {
    if (value !is JsonPrimitive || value.isString) return false
    if (!Regex("-?(0|[1-9][0-9]*)").matches(value.content)) return false
    return value.intOrNull != null
}

private fun isAsciiSortToken(value: String): Boolean =
    value.isNotEmpty() && value.all { it in 'a'..'z' || it in 'A'..'Z' || it == '_' }
