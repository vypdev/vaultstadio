package com.vaultstadio.app.data.network

import io.ktor.http.URLParserException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** Transport-level candidate; the Web composition root also applies its native URL parser. */
fun webApiRootCandidate(pageOrigin: String, document: String): String? = try {
    configurationObject(document)?.let { configuredApiRoot(pageOrigin, it) }
} catch (_: IllegalArgumentException) {
    null
} catch (_: URLParserException) {
    null
}

private fun configurationObject(document: String): JsonObject? {
    if (document.length > 16_384 || !flatConfigurationShape(document)) return null
    val json = Json.parseToJsonElement(document) as? JsonObject
    return json?.takeIf { it.keys.all { key -> key == "apiBaseUrl" } }
}

private fun configuredApiRoot(pageOrigin: String, json: JsonObject): String? {
    val page = safeApiRoot(pageOrigin, requireOrigin = true) ?: return null
    val value = json["apiBaseUrl"] ?: return "$page/api"
    if (value !is JsonPrimitive || !value.isString) return null
    return safeApiRoot(value.content)?.takeUnless {
        page.startsWith("https://") && it.startsWith("http://")
    }
}
