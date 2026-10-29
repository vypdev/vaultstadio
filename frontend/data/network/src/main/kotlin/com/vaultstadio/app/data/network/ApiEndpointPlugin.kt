package com.vaultstadio.app.data.network

import io.ktor.client.plugins.api.SendingRequest
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.http.HttpHeaders
import io.ktor.util.AttributeKey

private val applicationBearer = AttributeKey<Boolean>("VaultStadioApplicationBearer")

internal class ApiEndpointPluginConfig {
    lateinit var endpoint: ApiEndpoint
    lateinit var tokenProvider: TokenProvider
}

/** Runs after DefaultRequest resolution; prefix and bearer use the same immutable authority. */
internal val ApiEndpointPlugin = createClientPlugin("ApiEndpoint", ::ApiEndpointPluginConfig) {
    val endpoint = pluginConfig.endpoint
    val tokens = pluginConfig.tokenProvider
    onRequest { request, _ ->
        endpoint.rebase(request.url)
    }
    on(SendingRequest) { request, _ ->
        if (request.attributes.getOrNull(applicationBearer) == true) {
            request.headers.remove(HttpHeaders.Authorization)
            request.attributes.remove(applicationBearer)
        }
        if (endpoint.owns(request.url) && !request.headers.contains(HttpHeaders.Authorization)) {
            tokens.getToken()?.let {
                request.headers.append(HttpHeaders.Authorization, "Bearer $it")
                request.attributes.put(applicationBearer, true)
            }
        }
    }
}
