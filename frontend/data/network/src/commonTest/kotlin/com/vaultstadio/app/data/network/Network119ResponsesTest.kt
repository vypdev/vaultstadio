package com.vaultstadio.app.data.network

import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class Network119ResponsesTest {
    @Test fun net119TypedPayloadSurvivesDeserialization() = runTest {
        withNetwork119Client() { client ->
            assertEquals(ApiResult.Success(Network119Record("kept", "é + &")), Network119Probe(client).read())
        }
    }
    @Test fun net119MissingTypedPayloadIsNotUnitSuccess() = runTest {
        withNetwork119Client("""{"success":true}""") { client ->
            assertIs<ApiResult.Error>(Network119Probe(client).read())
        }
    }
    @Test fun net119NullTypedPayloadIsNotUnitSuccess() = runTest {
        withNetwork119Client("""{"success":true,"data":null}""") { client ->
            assertIs<ApiResult.Error>(Network119Probe(client).read())
        }
    }
    @Test fun net119UnitCommandAcceptsNoData() = runTest {
        withNetwork119Client("""{"success":true}""") { client ->
            assertEquals(ApiResult.Success(Unit), Network119Probe(client).removeUnit())
        }
    }
    @Test fun net119SemanticErrorRetainsCodeAndMessage() = runTest {
        val body = """{"success":false,"error":{"code":"QUOTA","message":"No space"}}"""
        withNetwork119Client(body) { client ->
            assertEquals(ApiResult.Error("QUOTA", "No space"), Network119Probe(client).read())
        }
    }
    @Test fun net119AbsentErrorUsesBoundedFallback() = runTest {
        withNetwork119Client("""{"success":false}""") { client ->
            assertEquals(ApiResult.Error("UNKNOWN_ERROR", "Unknown error"), Network119Probe(client).read())
        }
    }
    @Test fun net119HttpErrorRetainsSemanticEnvelope() = runTest {
        val body = """{"success":false,"error":{"code":"FORBIDDEN","message":"No grant"}}"""
        withNetwork119Client(body, HttpStatusCode.Forbidden) { client ->
            assertEquals(ApiResult.Error("FORBIDDEN", "No grant"), Network119Probe(client).read())
        }
    }
    @Test fun net119NonJsonErrorDoesNotExposeResponseBody() = runTest {
        withNetwork119Client("private upstream diagnostic", HttpStatusCode.BadGateway) { client ->
            assertEquals(ApiResult.Error("HTTP_502", "Bad Gateway"), Network119Probe(client).read())
        }
    }
    @Test fun net119ErrorWithoutDetailsRetainsHttpStatus() = runTest {
        withNetwork119Client("{}", HttpStatusCode.ServiceUnavailable) { client ->
            assertEquals(ApiResult.Error("HTTP_503", "Service Unavailable"), Network119Probe(client).read())
        }
    }
    @Test fun net119MalformedSuccessRemainsFailure() = runTest {
        withNetwork119Client("not json") { client ->
            val result = Network119Probe(client).read()
            assertTrue(result.isError())
            assertNull(result.getOrNull())
        }
    }
    @Test fun net119BodyAndFormRequireTypedData() = runTest {
        withNetwork119Client("""{"success":true}""") { client ->
            assertIs<ApiResult.Error>(Network119Probe(client).create())
            assertIs<ApiResult.Error>(Network119Probe(client).form())
        }
    }
}
