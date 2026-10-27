package com.vaultstadio.app.data.network

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.logging.Logger
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class Network119BoundaryTest {
    @Test fun net119BodyCommandAcceptsAnExplicitUnitResultWithoutData() = runTest {
        withNetwork119Client("""{"success":true}""") {
            assertEquals(ApiResult.Success(Unit), Network119Probe(it).createUnit())
        }
    }

    @Test fun net119FormCommandAcceptsAnExplicitUnitResultWithoutData() = runTest {
        withNetwork119Client("""{"success":true}""") {
            assertEquals(ApiResult.Success(Unit), Network119Probe(it).formUnit())
        }
    }

    @Test fun net119CancellationDuringHttpErrorDecodingIsNotAnErrorEnvelope() = runTest {
        val channel = ByteReadChannel(byteArrayOf())
        channel.cancel(CancellationException("owned decoding cancellation"))
        val engine =
            MockEngine {
                respond(channel, HttpStatusCode.BadGateway, headersOf(HttpHeaders.ContentType, "application/json"))
            }
        withNetwork119Client(engine) { assertFailsWith<CancellationException> { Network119Probe(it).read() } }
    }

    @Test fun net119ConfiguredTimeoutBoundsAnUnresponsiveTransport() = runTest {
        val engine = MockEngine { awaitCancellation() }
        val client = HttpClientFactory.create(
            ApiClientConfig("https://audit.invalid", 500),
            TokenProvider {
                null
            },
            engine,
        )
        try {
            withContext(Dispatchers.Default) {
                withTimeout(10_000) { assertIs<ApiResult.NetworkError>(Network119Probe(client).read()) }
            }
        } finally {
            client.close()
            engine.close()
        }
    }
    private suspend fun captured(path: String): List<String> {
        val messages = Channel<String>(Channel.UNLIMITED)
        val credential = "owned-ephemeral-network-credential"
        val logger = object : Logger {
            override fun log(message: String) {
                messages.trySend(message)
            }
        }
        val engine = MockEngine { request ->
            assertEquals("Bearer $credential", request.headers[HttpHeaders.Authorization])
            respond(NETWORK119_OK, headers = headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val client = HttpClientFactory.create(
            ApiClientConfig("https://audit.invalid"),
            TokenProvider {
                credential
            },
            engine,
            logger,
        )
        try {
            assertTrue(Network119Probe(client).readPath(path).isSuccess())
        } finally {
            client.close()
            engine.close()
        }
        messages.close()
        val captured = mutableListOf<String>()
        for (message in messages) captured += message
        assertFalse(captured.any { credential in it })
        return captured
    }

    @Test fun net119ActualHttpLoggerDoesNotExposeBearerHeaders() = runTest {
        val messages = captured("/record")
        assertTrue(messages.isNotEmpty(), "The actual logger must be exercised")
        assertTrue(messages.any { "audit.invalid" in it })
    }

    @Test fun net119PublicShareCapabilityDoesNotAppearInHttpLogs() = runTest {
        val capability = "owned-ephemeral-share-capability"
        assertFalse(captured("/api/v1/shares/$capability/download").any { capability in it })
    }
}
