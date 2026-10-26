package com.vaultstadio.api.routes.storage

import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Instant
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StorageFixturePrecisionIntegrationTest {
    private val fixture = StorageIntegrationFixture()
    private val nanosecondTimestamp = Instant.parse("2026-10-26T12:00:00.123456789Z")

    @BeforeAll fun start() = fixture.start()

    @AfterAll fun close() = fixture.close()

    @BeforeEach fun reset() = fixture.reset()

    @Test
    fun `metadata seed returns the complete persisted baseline for nanosecond input`() = runBlocking<Unit> {
        val baseline = fixture.seed(timestamp = nanosecondTimestamp)
        val persisted = fixture.repository.findById(baseline.id).fold({ throw it }, { assertNotNull(it) })

        assertNotEquals(nanosecondTimestamp, persisted.createdAt)
        assertNotEquals(nanosecondTimestamp, persisted.updatedAt)
        assertEquals(persisted, baseline)
        assertEquals(1, fixture.rowCount())
        assertEquals(0, fixture.byteFileCount())
    }

    @Test
    fun `byte seed returns persisted nanosecond baseline and retains content`() = runBlocking<Unit> {
        val content = "retained precision fixture Ω"
        val baseline = fixture.seedStoredFile(content, timestamp = nanosecondTimestamp)
        val persisted = fixture.repository.findById(baseline.id).fold({ throw it }, { assertNotNull(it) })

        assertNotEquals(nanosecondTimestamp, persisted.createdAt)
        assertNotEquals(nanosecondTimestamp, persisted.updatedAt)
        assertEquals(persisted, baseline)
        val bytes = fixture.backend.retrieve(assertNotNull(baseline.storageKey))
            .fold({ throw it }, { it }).use { it.readBytes() }
        assertEquals(content, bytes.decodeToString())
        assertEquals(bytes.size.toLong(), baseline.size)
        assertEquals(1, fixture.rowCount())
        assertEquals(1, fixture.byteFileCount())
    }
}
