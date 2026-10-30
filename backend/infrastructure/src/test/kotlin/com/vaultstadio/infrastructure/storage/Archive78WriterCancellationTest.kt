package com.vaultstadio.infrastructure.storage

import arrow.core.right
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Test
import java.io.InputStream
import java.util.concurrent.CountDownLatch
import kotlin.test.assertTrue

class Archive78WriterCancellationTest {
    @Test fun `cancellation interrupts owned input and closes it without publishing ZIP bytes`() = runBlocking {
        val started = CountDownLatch(1)
        val release = CountDownLatch(1)
        var closed = false
        val input = object : InputStream() {
            override fun read(): Int {
                started.countDown()
                release.await()
                return -1
            }
            override fun close() {
                closed = true
                release.countDown()
            }
        }
        val job = launch(Dispatchers.Default) {
            BoundedZipArchiveWriter().write(listOf(archive78Entry())) { input.right() }
            error("Canceled archive must not publish bytes")
        }
        try {
            withTimeout(5000) { withContext(Dispatchers.IO) { started.await() } }
            withTimeout(5000) { job.cancelAndJoin() }
        } finally {
            release.countDown()
            job.cancelAndJoin()
        }
        assertTrue(job.isCancelled)
        assertTrue(closed)
    }
}
