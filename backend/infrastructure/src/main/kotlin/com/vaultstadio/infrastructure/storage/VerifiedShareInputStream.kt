package com.vaultstadio.infrastructure.storage

import java.io.IOException
import java.io.InputStream
import java.io.SequenceInputStream

/** Rejects provider truncation/extra bytes without ever forwarding bytes beyond the declared size. */
internal class VerifiedShareInputStream(prefix: InputStream, source: InputStream, private val expected: Long) :
    InputStream() {
    private val input = SequenceInputStream(prefix, source)
    private var readBytes = 0L
    private var closed = false

    override fun read(): Int {
        val single = ByteArray(1)
        return if (read(single, 0, 1) == -1) -1 else single[0].toInt() and 255
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        check(!closed) { "Shared stream is closed" }
        require(offset >= 0 && length >= 0 && offset <= buffer.size - length)
        if (length == 0) return 0
        if (readBytes == expected) {
            if (safeRead { input.read() } != -1) throw IOException("Shared content length mismatch")
            return -1
        }
        val count = safeRead { input.read(buffer, offset, minOf(length.toLong(), expected - readBytes).toInt()) }
        if (count == -1) throw IOException("Shared content length mismatch")
        readBytes += count
        return count
    }

    private fun <T> safeRead(read: () -> T): T = try {
        read()
    } catch (_: IOException) {
        throw IOException("Shared content transfer failed")
    }

    override fun close() {
        if (!closed) {
            closed = true
            input.closeSafely()
        }
    }
}
