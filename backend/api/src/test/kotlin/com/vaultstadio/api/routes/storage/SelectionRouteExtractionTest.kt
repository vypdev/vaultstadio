package com.vaultstadio.api.routes.storage

import arrow.core.right
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import io.mockk.coEvery
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SelectionRouteExtractionTest {
    @Test fun extractedDownloadKeepsTheStreamOpenUntilTransmissionAndClosesItAfterwards() = testApplication {
        val f = SelectionIntegrationFixture()
        try {
            f.start()
            f.reset()
            val item = f.storage.seedStoredFile("stream lifetime")
            val stream = TrackingStream("stream lifetime".toByteArray())
            coEvery { f.backend.retrieve(item.storageKey!!) } returns stream.right()
            application { f.install(this) }
            val response = client.get("/api/v1/storage/download/${item.id}") { bearerAuth("owner") }
            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals("stream lifetime", response.bodyAsText())
            assertTrue(stream.closed)
            assertEquals(1, stream.closeCount)
        } finally {
            f.close()
        }
    }

    private class TrackingStream(bytes: ByteArray) : ByteArrayInputStream(bytes) {
        var closed = false
        var closeCount = 0

        override fun read(bytes: ByteArray, offset: Int, length: Int): Int {
            check(!closed) { "Stream closed before deferred transmission" }
            return super.read(bytes, offset, length)
        }

        override fun close() {
            closed = true
            closeCount++
            super.close()
        }
    }
}
