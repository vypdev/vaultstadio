package com.vaultstadio.app.data.network

import kotlinx.serialization.Contextual
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Instant

@Serializable
private data class ContextualInstantEnvelope(@Contextual val at: Instant)

@Serializable
private data class BuiltInInstantEnvelope(val at: Instant)

@Serializable
private data class NullableInstantEnvelope(@Contextual val at: Instant?)

@Serializable
private data class InstantBatch(val items: List<ContextualInstantEnvelope>)

/** BOOT-SER-01/02 exercise the production factory, not a test-only Json/descriptor. */
class HttpClientFactoryInstantTest {
    @Test
    fun factorySupportsContextualAndBuiltInInstantTogether() {
        val json = HttpClientFactory.getJson()
        val instant = Instant.parse("2026-10-25T12:34:56Z")
        val contextual = json.encodeToString(ContextualInstantEnvelope.serializer(), ContextualInstantEnvelope(instant))
        val builtIn = json.encodeToString(BuiltInInstantEnvelope.serializer(), BuiltInInstantEnvelope(instant))
        assertEquals(contextual, builtIn)
        assertEquals(instant, json.decodeFromString(BuiltInInstantEnvelope.serializer(), builtIn).at)
    }

    @Test
    fun epochUsesTheExistingIsoStringWireShape() {
        assertEquals("""{"at":"1970-01-01T00:00:00Z"}""", encode("1970-01-01T00:00:00Z"))
    }

    @Test
    fun nanosecondPrecisionSurvivesRoundTrip() {
        roundTrip("2026-10-25T12:34:56.123456789Z")
    }

    @Test
    fun oneNanosecondIsNotRoundedToMilliseconds() {
        assertEquals("""{"at":"1970-01-01T00:00:00.000000001Z"}""", encode("1970-01-01T00:00:00.000000001Z"))
    }

    @Test
    fun preEpochFractionSurvivesRoundTrip() {
        roundTrip("1969-12-31T23:59:59.999999999Z")
    }

    @Test
    fun offsetInputNormalizesToUtcWithoutChangingTheInstant() {
        val decoded = HttpClientFactory.getJson().decodeFromString(
            ContextualInstantEnvelope.serializer(),
            """{"at":"2026-10-25T12:34:56.789+02:30"}""",
        )
        assertEquals(Instant.parse("2026-10-25T10:04:56.789Z"), decoded.at)
    }

    @Test
    fun nestedDtosUseTheProductionContextualModule() {
        val json = HttpClientFactory.getJson()
        val batch = InstantBatch(listOf(ContextualInstantEnvelope(Instant.parse("2000-02-29T00:00:00Z"))))
        assertEquals(
            batch,
            json.decodeFromString(InstantBatch.serializer(), json.encodeToString(InstantBatch.serializer(), batch)),
        )
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun contextualInstantListsPreserveTheIsoStringWireContract() {
        val json = HttpClientFactory.getJson()
        val instantSerializer = requireNotNull(json.serializersModule.getContextual(Instant::class))
        val serializer = ListSerializer(instantSerializer)
        val values = listOf(Instant.fromEpochSeconds(0), Instant.parse("2000-02-29T00:00:00.000000001Z"))
        val encoded = json.encodeToString(serializer, values)
        assertEquals("""["1970-01-01T00:00:00Z","2000-02-29T00:00:00.000000001Z"]""", encoded)
        assertEquals(values, json.decodeFromString(serializer, encoded))
    }

    @Test
    fun nullableInstantRetainsNull() {
        val json = HttpClientFactory.getJson()
        val value = NullableInstantEnvelope(null)
        val encoded = json.encodeToString(NullableInstantEnvelope.serializer(), value)
        assertEquals("""{"at":null}""", encoded)
        assertEquals(value, json.decodeFromString(NullableInstantEnvelope.serializer(), encoded))
    }

    @Test
    fun unknownFieldsDoNotChangeTheTimestamp() {
        val decoded = HttpClientFactory.getJson().decodeFromString(
            ContextualInstantEnvelope.serializer(),
            """{"at":"1970-01-01T00:00:00Z","futureField":true}""",
        )
        assertEquals(Instant.fromEpochSeconds(0), decoded.at)
    }

    @Test
    fun malformedTimestampIsRejected() = reject("""{"at":"not-an-instant"}""")

    @Test
    fun impossibleMonthIsRejected() = reject("""{"at":"2026-13-01T00:00:00Z"}""")

    @Test
    fun missingRequiredTimestampIsRejected() = reject("{}")

    @Test
    fun nullRequiredTimestampIsRejected() = reject("""{"at":null}""")

    @Test
    fun structuredTimestampIsRejected() = reject("""{"at":{"seconds":0}}""")

    private fun encode(iso: String): String = HttpClientFactory.getJson().encodeToString(
        ContextualInstantEnvelope.serializer(),
        ContextualInstantEnvelope(Instant.parse(iso)),
    )

    private fun roundTrip(iso: String) {
        val json = HttpClientFactory.getJson()
        val instant = Instant.parse(iso)
        assertEquals(instant, json.decodeFromString(ContextualInstantEnvelope.serializer(), encode(iso)).at)
    }

    private fun reject(payload: String) {
        val json = HttpClientFactory.getJson()
        assertFailsWith<IllegalArgumentException> {
            json.decodeFromString(ContextualInstantEnvelope.serializer(), payload)
        }
    }
}
