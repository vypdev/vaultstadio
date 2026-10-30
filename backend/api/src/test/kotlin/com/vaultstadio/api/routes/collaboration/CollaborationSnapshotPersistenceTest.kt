package com.vaultstadio.api.routes.collaboration

import arrow.core.Either
import com.vaultstadio.domain.common.exception.DatabaseException
import com.vaultstadio.domain.common.exception.InvalidOperationException
import com.vaultstadio.domain.common.exception.ItemNotFoundException
import com.vaultstadio.infrastructure.persistence.entities.DocumentStatesTable
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.sql.Connection
import java.sql.DriverManager
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CollaborationSnapshotPersistenceTest {
    private val f = CollaborationIntegrationFixture()

    @BeforeAll fun start() = f.start()

    @BeforeEach fun reset() = f.reset()

    @AfterAll fun close() = f.close()

    @Test
    fun `C89-SNAPSHOT a stale save cannot replace a concurrently committed working state`() = testApplication {
        application { f.install(this) }
        startApplication()
        coroutineScope {
            val pg = f.storage.postgres
            DriverManager.getConnection(pg.jdbcUrl, pg.username, pg.password).use { writer ->
                writer.autoCommit = false
                try {
                    writer.prepareStatement(
                        "UPDATE document_states SET version = 2, content = ? WHERE item_id = ?",
                    ).use {
                        it.setString(1, "concurrent edit retained")
                        it.setString(2, f.item.id)
                        assertEquals(1, it.executeUpdate())
                    }
                    val save = async(Dispatchers.IO) { f.service.saveDocument(f.item.id, f.storage.alice.id) }
                    awaitSaveLock()
                    writer.commit()
                    val result = save.await()
                    val state = f.repository.getDocumentState(f.item.id).fold({ throw it }, { checkNotNull(it) })
                    assertAll(
                        { assertTrue(result.isLeft(), "A stale snapshot must not acknowledge success") },
                        { assertEquals(Pair(2L, "concurrent edit retained"), Pair(state.version, state.content)) },
                    )
                } finally {
                    writer.rollback()
                }
            }
        }
    }

    @ParameterizedTest(name = "C89-SNAPSHOT {0}")
    @ValueSource(strings = ["exact-current", "changed-current", "deleted-current", "sql-failure"])
    fun `snapshot acknowledgment never recreates or overwrites working state`(case: String) = testApplication {
        application { f.install(this) }
        startApplication()
        val expected = f.repository.getDocumentState(f.item.id).fold({ throw it }, { checkNotNull(it) })
        when (case) {
            "changed-current" -> transaction {
                DocumentStatesTable.update({ DocumentStatesTable.itemId eq f.item.id }) {
                    it[version] = 2
                    it[content] = "later edit retained"
                }
            }
            "deleted-current" -> transaction { DocumentStatesTable.deleteWhere { itemId eq f.item.id } }
        }
        val before = f.snapshot()
        if (case ==
            "sql-failure"
        ) {
            transaction { exec("ALTER TABLE document_states RENAME TO document_states_unavailable") }
        }
        val result = try {
            f.repository.saveExistingSnapshot(expected, f.storage.alice.id)
        } finally {
            if (case == "sql-failure") {
                transaction { exec("ALTER TABLE document_states_unavailable RENAME TO document_states") }
            }
        }
        when (case) {
            "exact-current" -> assertEquals(expected, assertIs<Either.Right<*>>(result).value)
            "changed-current" -> assertIs<InvalidOperationException>(assertIs<Either.Left<*>>(result).value)
            "deleted-current" -> assertIs<ItemNotFoundException>(assertIs<Either.Left<*>>(result).value)
            "sql-failure" -> assertIs<DatabaseException>(assertIs<Either.Left<*>>(result).value)
        }
        assertEquals(before, f.snapshot())
    }

    private suspend fun awaitSaveLock() = withTimeout(5_000) {
        val pg = f.storage.postgres
        DriverManager.getConnection(pg.jdbcUrl, pg.username, pg.password).use { observer ->
            while (!waitingSave(observer)) delay(10)
        }
    }

    private fun waitingSave(observer: Connection): Boolean = observer.createStatement().use { statement ->
        statement.executeQuery(
            "SELECT count(*) FROM pg_stat_activity WHERE datname = current_database() " +
                "AND wait_event_type = 'Lock' AND query ILIKE '%document_states%' AND pid <> pg_backend_pid()",
        ).use { rows ->
            rows.next()
            rows.getInt(1) > 0
        }
    }
}
