package com.vaultstadio.api.routes.share

import com.vaultstadio.domain.common.exception.ValidationException
import io.ktor.http.HttpHeaders
import io.ktor.server.application.ApplicationCall
import java.net.URI

internal fun getBaseUrl(call: ApplicationCall): String {
    val local = call.request.local
    val scheme = call.request.headers["X-Forwarded-Proto"] ?: local.scheme
    val host = local.serverHost.removeSurrounding("[", "]")
    val fallbackHost = if (':' in host) "[$host]" else host
    val fallbackAuthority = "$fallbackHost:${local.serverPort}"
    val authority =
        call.request.headers["X-Forwarded-Host"] ?: call.request.headers[HttpHeaders.Host] ?: fallbackAuthority
    return validatedShareOrigin(scheme, authority)
}

/** Proxy trust remains an edge configuration concern; never append a private port to public authority. */
internal fun validatedShareOrigin(scheme: String, authority: String): String {
    val normalizedScheme = scheme.lowercase()
    if (normalizedScheme !in setOf("http", "https")) throw invalidOrigin()
    val uri = parseOrigin(normalizedScheme, authority)
    val valid = uri.host != null &&
        uri.rawUserInfo == null &&
        uri.rawPath.isNullOrEmpty() &&
        uri.rawQuery == null &&
        uri.rawFragment == null &&
        (uri.port == -1 || uri.port in 1..65535) &&
        !authority.endsWith(':')
    if (!valid) throw invalidOrigin()
    return "$normalizedScheme://${uri.rawAuthority}"
}

private fun parseOrigin(scheme: String, authority: String): URI = try {
    URI.create("$scheme://$authority")
} catch (_: IllegalArgumentException) {
    throw invalidOrigin()
}

private fun invalidOrigin() = ValidationException(field = "shareOrigin", message = "Invalid public share origin")
