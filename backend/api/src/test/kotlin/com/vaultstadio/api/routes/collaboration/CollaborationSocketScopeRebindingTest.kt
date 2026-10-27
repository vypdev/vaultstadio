package com.vaultstadio.api.routes.collaboration

import com.vaultstadio.domain.storage.model.ItemType
import com.vaultstadio.infrastructure.persistence.entities.CollaborationSessionsTable
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import kotlin.test.assertEquals

class CollaborationSocketScopeRebindingTest {
    @Test
    fun `C89-BIND live session rebinding cannot redirect an already bound socket`() {
        val f = CollaborationIntegrationFixture()
        f.start()
        try {
            f.reset()
            testApplication {
                application { f.install(this) }
                val wsClient = createClient { install(WebSockets) }
                startApplication()
                val s = CollaborationSocketFixture(f, wsClient)
                s.prepare()
                try {
                    val socket = s.joined(s.first)
                    val other = f.storage.seed(type = ItemType.FILE)
                    transaction {
                        CollaborationSessionsTable.update({ CollaborationSessionsTable.id eq s.session }) {
                            it[itemId] = other.id
                        }
                    }
                    val before = f.snapshot()
                    socket.sendMessage(
                        WebSocketMessage(
                            "operation",
                            itemId = f.item.id,
                            operation = OperationData("INSERT", 0, "must not apply", baseVersion = 1),
                        ),
                    )
                    val response = socket.packet()
                    assertAll(
                        { assertEquals(403, response["code"]?.jsonPrimitive?.intOrNull) },
                        { assertEquals(before, f.snapshot()) },
                    )
                } finally {
                    s.close()
                }
            }
        } finally {
            f.close()
        }
    }
}
