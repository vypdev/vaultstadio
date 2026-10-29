package com.vaultstadio.app.data.network

import io.ktor.http.URLBuilder
import io.ktor.http.decodeURLPart
import io.ktor.http.encodedPath

/** Shared syntax checks; browser host/IP/IDNA canonicalization remains a platform responsibility. */
internal fun safeApiRoot(value: String, requireOrigin: Boolean = false): String? {
    if (!safeRawApiUrl(value)) return null
    if (!safeAuthority(value.substringAfter("://").substringBefore('/'))) return null
    val url = URLBuilder(value)
    if (!safeParsedAuthority(url) || !safeApiPath(url.encodedPath, requireOrigin)) return null
    url.encodedPath = if (requireOrigin) "" else url.encodedPath.removeSuffix("/").ifEmpty { "/api" }
    return url.buildString().removeSuffix("/")
}

private fun safeRawApiUrl(value: String): Boolean {
    val bounded = value.isNotEmpty() && value.length <= 2048
    val httpScheme = Regex("^https?://", RegexOption.IGNORE_CASE).containsMatchIn(value)
    val safeCharacters = validUnicodeScalars(value) && value.none(::unsafeRawUrlCharacter)
    return bounded && httpScheme && safeCharacters
}

private fun unsafeRawUrlCharacter(char: Char): Boolean =
    char.isWhitespace() || unsafePathCharacter(char) || char in setOf('?', '#')

private fun unsafePathCharacter(char: Char): Boolean = char == '\\' || char.code < 32 || char.code == 127

private fun safeAuthority(authority: String): Boolean {
    if (authority.isEmpty() || '@' in authority) return false
    val suffix = authorityPortSuffix(authority) ?: return false
    return suffix.isEmpty() || validPortSuffix(suffix)
}

private fun authorityPortSuffix(authority: String): String? = when {
    authority.startsWith('[') -> authority.indexOf(']').takeIf { it >= 0 }?.let { authority.substring(it + 1) }
    authority.count { it == ':' } > 1 -> null
    ':' in authority -> ":" + authority.substringAfter(':')
    else -> ""
}

private fun validPortSuffix(suffix: String): Boolean =
    suffix.startsWith(':') &&
        suffix.drop(1).all { it in '0'..'9' } &&
        suffix.drop(1).toIntOrNull()?.let { it in 1..65535 } == true

private fun safeParsedAuthority(url: URLBuilder): Boolean {
    val credentialsAbsent = url.user == null && url.password == null
    return url.host.isNotBlank() && credentialsAbsent && url.protocol.name in setOf("http", "https")
}

private fun safeApiPath(path: String, requireOrigin: Boolean): Boolean = when {
    requireOrigin -> path in setOf("", "/")
    "//" in path -> false
    else -> path.removeSuffix("/").split('/').drop(1).all(::safePathSegment)
}

private fun safePathSegment(segment: String): Boolean {
    if (segment.isEmpty() || !validEscapes(segment)) return false
    val decoded = segment.decodeURLPart()
    val containsUnsafeCharacter = decoded.any { it == '/' || unsafePathCharacter(it) || it == '\uFFFD' }
    return decoded !in setOf(".", "..") &&
        !containsUnsafeCharacter &&
        !Regex("%[0-9a-fA-F]{2}").containsMatchIn(decoded)
}

private fun validEscapes(value: String): Boolean {
    var index = 0
    while (index < value.length) {
        if (value[index] == '%') {
            val pair = value.substring(index + 1, minOf(index + 3, value.length))
            if (pair.length != 2 || pair.any { it.digitToIntOrNull(16) == null }) return false
            index += 3
        } else {
            index++
        }
    }
    return true
}
