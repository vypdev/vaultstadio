package com.vaultstadio.app.data.network

import io.ktor.http.URLBuilder
import io.ktor.http.appendPathSegments
import io.ktor.http.encodedPath

/** Builds one canonical API prefix; public locations never inherit credentials or base query state. */
fun apiV1Location(baseUrl: String, segments: List<String>, query: Map<String, String> = emptyMap()): String =
    URLBuilder(baseUrl).apply {
        require(segments.none { it.isBlank() || it == "." || it == ".." || it.any { char -> char.code < 32 } })
        encodedPath = "/api/v1"
        appendPathSegments(*segments.toTypedArray(), encodeSlash = true)
        user = null
        password = null
        parameters.clear()
        query.forEach { (key, value) -> parameters.append(key, value) }
        fragment = ""
    }.buildString()
