package com.vaultstadio.app.data.network

import io.ktor.http.URLBuilder
import io.ktor.http.URLProtocol
import io.ktor.http.encodedPath

/** One adapter-owned interpretation of legacy origins, explicit roots and versioned aliases. */
internal class ApiEndpoint(baseUrl: String) {
    private val base = URLBuilder(baseUrl).build()
    val path: String = base.encodedPath.trimEnd('/').let {
        when {
            it.isEmpty() -> "/api/v1"
            it.endsWith("/v1") -> it
            else -> "$it/v1"
        }
    }

    fun owns(url: URLBuilder): Boolean = base.host.equals(url.host, ignoreCase = true) &&
        base.protocol == url.protocol &&
        base.port == url.build().port

    fun rebase(url: URLBuilder) {
        if (!owns(url)) return
        val requested = url.encodedPath
        if (requested == path || requested.startsWith("$path/")) return
        if (requested == "/api/v1" || requested.startsWith("/api/v1/")) {
            url.encodedPath = path + requested.removePrefix("/api/v1")
        }
    }

    fun isPublicCapability(url: URLBuilder): Boolean = url.encodedPath == "$path/share" ||
        url.encodedPath.startsWith("$path/share/")
}

/** Builds a prefixed socket location without adding identity credentials to its URL. */
fun apiWebSocketLocation(baseUrl: String, segments: List<String>, query: Map<String, String> = emptyMap()): String =
    URLBuilder(apiV1Location(baseUrl, segments, query)).apply {
        protocol = if (protocol == URLProtocol.HTTPS) URLProtocol.WSS else URLProtocol.WS
    }.buildString()
