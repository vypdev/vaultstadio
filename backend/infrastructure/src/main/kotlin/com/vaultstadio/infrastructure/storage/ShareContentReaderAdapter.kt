package com.vaultstadio.infrastructure.storage

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.vaultstadio.core.domain.service.StorageBackend
import com.vaultstadio.domain.share.access.PreparedShareContent
import com.vaultstadio.domain.share.access.PublicShareFailure
import com.vaultstadio.domain.share.access.ShareContentReader
import com.vaultstadio.domain.storage.model.StorageItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InputStream

/** Opens and prepares a bounded prefix before a download allowance can be consumed. */
class ShareContentReaderAdapter(private val backend: StorageBackend) : ShareContentReader {
    override suspend fun prepare(item: StorageItem): Either<PublicShareFailure, PreparedShareContent> {
        val key = item.storageKey ?: return PublicShareFailure.NOT_FOUND.left()
        val retrieved = backend.retrieve(key)
        val source = when (val result = retrieved) {
            is Either.Left -> {
                currentCoroutineContext().ensureActive()
                return PublicShareFailure.UNAVAILABLE.left()
            }
            is Either.Right -> {
                result.value
            }
        }
        var transferred = false
        try {
            currentCoroutineContext().ensureActive()
            val prefix = withContext(Dispatchers.IO) {
                val bound = if (item.size < PREFIX_BYTES) (item.size + 1L).toInt() else PREFIX_BYTES
                source.readNBytes(bound)
            }
            currentCoroutineContext().ensureActive()
            if (prefix.size > item.size || item.size < PREFIX_BYTES && prefix.size.toLong() != item.size) {
                return PublicShareFailure.UNAVAILABLE.left()
            }
            val verified = VerifiedShareInputStream(prefix.inputStream(), source, item.size)
            transferred = true
            return object : PreparedShareContent {
                override val stream: InputStream = verified
                override fun close() = verified.close()
            }.right()
        } catch (_: IOException) {
            return PublicShareFailure.UNAVAILABLE.left()
        } finally {
            if (!transferred) source.closeSafely()
        }
    }

    companion object {
        private const val PREFIX_BYTES = 8192
    }
}

internal fun InputStream.closeSafely() {
    try {
        close()
    } catch (_: IOException) {
        // Closing a denied/failed transfer must not replace its semantic outcome.
    }
}
