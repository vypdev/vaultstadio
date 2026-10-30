package com.vaultstadio.api.routes.collaboration

import com.vaultstadio.core.domain.model.CollaborationOperation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.assertEquals

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CollaborationOperationHistoryTest {
    private val f = CollaborationIntegrationFixture()

    @BeforeAll fun start() = f.start()

    @BeforeEach fun reset() = f.reset()

    @AfterAll fun close() = f.close()

    @ParameterizedTest(name = "C89-HISTORY since {0}")
    @ValueSource(longs = [0, 1, 2, 3])
    fun `since includes the operation that advances the requested version`(since: Long) = testApplication {
        application { f.install(this) }
        startApplication()
        for (base in listOf(1L, 2L)) {
            val operation = CollaborationOperation.Insert(
                userId = f.storage.alice.id,
                timestamp = f.item.createdAt,
                baseVersion = base,
                position = 0,
                text = "edit$base ",
            )
            f.service.applyOperation(f.item.id, operation, f.storage.alice.id).fold({ throw it }, {})
        }
        val before = f.snapshot()
        val response = client.get("/api/v1/collaboration/documents/${f.item.id}/operations?since=$since") {
            bearerAuth("owner")
        }
        assertEquals(HttpStatusCode.OK, response.status)
        val versions = Json.parseToJsonElement(response.bodyAsText()).jsonArray.map {
            it.jsonObject.getValue("baseVersion").jsonPrimitive.content.toLong()
        }
        val expected = when (since) {
            0L, 1L -> listOf(1L, 2L)
            2L -> listOf(2L)
            3L -> emptyList()
            else -> error("Unenumerated history boundary")
        }
        assertEquals(expected, versions)
        assertEquals(before, f.snapshot())
    }
}
