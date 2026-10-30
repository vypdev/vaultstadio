package com.vaultstadio.app.data.network

import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.utils.io.ByteReadChannel

/** Owned streaming type keeps the original channel/cause and has no business or saving policy. */
class DownloadBody(val channel: ByteReadChannel)

internal val DownloadBodyPlugin = createClientPlugin("OwnedDownloadBody") {
    transformResponseBody { _, content, requestedType ->
        if (requestedType.type == DownloadBody::class) DownloadBody(content) else null
    }
}
