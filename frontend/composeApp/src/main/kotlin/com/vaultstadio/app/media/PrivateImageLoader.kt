package com.vaultstadio.app.media

import coil3.ImageLoader
import coil3.PlatformContext
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.request.CachePolicy
import io.ktor.client.HttpClient

/** Use the authenticated API transport; URL-only caches must not cross account/session boundaries. */
internal fun privateImageLoader(context: PlatformContext, client: HttpClient): ImageLoader =
    ImageLoader.Builder(context)
        .components { add(KtorNetworkFetcherFactory(client)) }
        .memoryCachePolicy(CachePolicy.DISABLED)
        // Coil's network fetcher maps disabled disk reads/writes to Cache-Control: no-cache, no-store.
        .diskCachePolicy(CachePolicy.DISABLED)
        .build()
