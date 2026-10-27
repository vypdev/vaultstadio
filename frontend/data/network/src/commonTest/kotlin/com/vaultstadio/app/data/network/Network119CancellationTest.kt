package com.vaultstadio.app.data.network

import io.ktor.client.engine.mock.MockEngine
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class Network119CancellationTest {
    private suspend fun cancelled(operation: suspend (Network119Probe) -> Unit) {
        val cause = CancellationException("owned cancellation")
        val engine = MockEngine { throw cause }
        withNetwork119Client(engine) { client ->
            assertFailsWith<CancellationException> { operation(Network119Probe(client)) }
        }
    }
    @Test fun net119GetCancellationPropagates() = runTest { cancelled { it.read() } }
    @Test fun net119PostCancellationPropagates() = runTest { cancelled { it.create() } }
    @Test fun net119PatchCancellationPropagates() = runTest { cancelled { it.update() } }
    @Test fun net119DeleteCancellationPropagates() = runTest { cancelled { it.remove() } }
    @Test fun net119CommandCancellationPropagates() = runTest { cancelled { it.command() } }
    @Test fun net119FormCancellationPropagates() = runTest { cancelled { it.form() } }
    @Test fun net119OrdinaryTransportFailureIsStillNetworkError() = runTest {
        val engine = MockEngine { throw IllegalStateException("offline") }
        withNetwork119Client(engine) { client ->
            val result = assertIs<ApiResult.NetworkError>(Network119Probe(client).read())
            assertEquals("offline", result.message)
        }
    }
}
