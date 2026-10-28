package com.vaultstadio.app.data.network

import io.ktor.client.HttpClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class Network78FatalTest {
    @Test fun transfer78FatalAssertionIsNotConvertedToNetworkError() = runTest {
        withNetwork119Client { client ->
            val failure = AssertionError("owned fatal fixture")
            assertSame(failure, assertFailsWith<AssertionError> { fatalRequest(client, failure) })
        }
    }

    @Test fun transfer78UnknownThrowableIsNotConvertedToNetworkError() = runTest {
        withNetwork119Client { client ->
            val failure = Throwable("owned unknown fixture")
            assertSame(failure, assertFailsWith<Throwable> { fatalRequest(client, failure) })
        }
    }

    @Test fun transfer78CallerCancellationIsStillControlFlow() = runTest {
        withNetwork119Client { client ->
            val failure = CancellationException("owned caller canceled")
            assertSame(failure, assertFailsWith<CancellationException> { fatalRequest(client, failure) })
        }
    }

    @Test fun transfer78OrdinaryFailureStillMapsToNetworkError() = runTest {
        withNetwork119Client { client ->
            kotlin.test.assertIs<ApiResult.NetworkError>(
                fatalRequest(client, IllegalStateException("owned network failure")),
            )
        }
    }

    private suspend fun fatalRequest(client: HttpClient, failure: Throwable): ApiResult<Unit> =
        object : BaseApi(client) {}.executeRequest(
            request = { throw failure },
            decode = { error("Failed request must not decode a response") },
            unitResult = true,
        )
}
