package com.vaultstadio.infrastructure.storage

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.vaultstadio.core.domain.service.ArchiveWriter
import com.vaultstadio.domain.common.exception.FileSizeLimitException
import com.vaultstadio.domain.common.exception.StorageBackendException
import com.vaultstadio.domain.common.exception.StorageException
import com.vaultstadio.domain.common.exception.ValidationException
import com.vaultstadio.domain.storage.model.ArchiveEntry
import com.vaultstadio.domain.storage.model.ArchiveLimits
import com.vaultstadio.domain.storage.model.ItemType
import com.vaultstadio.domain.storage.model.isSafeArchiveSegment
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Complete bounded output before returning HTTP200. Nothing partial can reach the caller. */
class BoundedZipArchiveWriter(private val limits: ArchiveLimits = ArchiveLimits()) : ArchiveWriter {
    override suspend fun write(
        entries: List<ArchiveEntry>,
        open: suspend (ArchiveEntry) -> Either<StorageException, InputStream>,
    ): Either<StorageException, ByteArray> = withContext(Dispatchers.IO) {
        try {
            if (entries.size > limits.entries ||
                entries.map { it.path }.distinct().size != entries.size ||
                entries.any { it.path.split('/').any { segment -> !isSafeArchiveSegment(segment) } }
            ) {
                return@withContext ValidationException("Invalid archive entries").left()
            }
            val bytes = ByteArrayOutputStream()
            var total = 0L
            ZipOutputStream(LimitedArchiveOutput(bytes, limits.outputBytes)).use { zip ->
                for (entry in entries) {
                    currentCoroutineContext().ensureActive()
                    val directory = entry.item.type == ItemType.FOLDER
                    zip.putNextEntry(ZipEntry(entry.path + if (directory) "/" else ""))
                    if (!directory) {
                        val stream = when (val result = open(entry)) {
                            is Either.Left -> throw ArchiveBuildFailure(result.value)
                            is Either.Right -> result.value
                        }
                        stream.use {
                            val buffer = ByteArray(8192)
                            var actual = 0L
                            while (true) {
                                currentCoroutineContext().ensureActive()
                                val count = runInterruptible { it.read(buffer) }
                                if (count < 0) break
                                if (count ==
                                    0
                                ) {
                                    throw ArchiveBuildFailure(
                                        ValidationException("Archive content did not advance"),
                                    )
                                }
                                if (count > limits.inputBytes - total) {
                                    throw ArchiveBuildFailure(FileSizeLimitException(total + count, limits.inputBytes))
                                }
                                actual += count
                                total += count
                                if (actual >
                                    entry.item.size
                                ) {
                                    throw ArchiveBuildFailure(ValidationException("Archive content size changed"))
                                }
                                zip.write(buffer, 0, count)
                            }
                            if (actual !=
                                entry.item.size
                            ) {
                                throw ArchiveBuildFailure(ValidationException("Archive content did not complete"))
                            }
                        }
                    }
                    zip.closeEntry()
                }
            }
            bytes.toByteArray().right()
        } catch (error: CancellationException) {
            throw error
        } catch (error: ArchiveBuildFailure) {
            error.failure.left()
        } catch (error: ArchiveOutputLimit) {
            FileSizeLimitException(
                limits.outputBytes.toLong() + 1,
                limits.outputBytes.toLong(),
                "Archive output exceeds its limit",
            ).left()
        } catch (error: Exception) {
            StorageBackendException("archive", "Archive could not be prepared", error).left()
        }
    }
}

private class ArchiveBuildFailure(val failure: StorageException) : RuntimeException()

private class ArchiveOutputLimit : RuntimeException()

private class LimitedArchiveOutput(private val target: OutputStream, private val limit: Int) : OutputStream() {
    private var size = 0
    override fun write(value: Int) {
        if (size >= limit) throw ArchiveOutputLimit()
        target.write(value)
        size++
    }
    override fun write(bytes: ByteArray, offset: Int, length: Int) {
        if (length > limit - size) throw ArchiveOutputLimit()
        target.write(bytes, offset, length)
        size += length
    }
}
