package com.vaultstadio.api.routes.collaboration

import arrow.core.Either
import com.vaultstadio.core.domain.model.CollaborationOperation
import com.vaultstadio.core.domain.model.DocumentState
import com.vaultstadio.domain.common.exception.AuthorizationException
import com.vaultstadio.domain.common.exception.DatabaseException
import com.vaultstadio.domain.common.exception.InvalidOperationException
import com.vaultstadio.domain.common.exception.ItemNotFoundException
import com.vaultstadio.infrastructure.persistence.entities.CollaborationOperationsTable
import com.vaultstadio.infrastructure.persistence.entities.DocumentStatesTable
import com.vaultstadio.infrastructure.persistence.entities.StorageItemsTable
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Duration.Companion.seconds

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CollaborationDocumentWriteContractTest {
    private val f = CollaborationIntegrationFixture()

    @BeforeAll fun start() = f.start()

    @BeforeEach fun reset() = f.reset()

    @AfterAll fun close() = f.close()

    @ParameterizedTest(name = "C89-WRITE {0}")
    @ValueSource(
        strings = [
            "exact", "stale-version", "stale-content", "stale-time", "missing-document", "forged-actor",
            "wrong-item", "new-owner", "blank-actor", "trashed-item", "folder-item", "missing-item", "rollback",
        ],
    )
    fun `document adapter checks actor item snapshot and atomicity`(case: String) = testApplication {
        application { f.install(this) }
        startApplication()
        val expected = f.repository.getDocumentState(f.item.id).fold({ throw it }, { checkNotNull(it) })
        arrange(case, expected)
        val before = f.snapshot()
        val result = try {
            applyScenario(case, expected)
        } finally {
            if (case ==
                "rollback"
            ) {
                transaction { exec("ALTER TABLE document_states DROP CONSTRAINT reject_new_version") }
            }
        }
        assertOutcome(case, before, result)
    }

    private suspend fun applyScenario(case: String, expected: DocumentState): Either<*, *> {
        val target = if (case == "missing-item") "absent" else f.item.id
        val actor = if (case == "blank-actor") " " else f.storage.alice.id
        val operation = operation().let { if (case == "forged-actor") it.copy(userId = f.storage.bob.id) else it }
        val bound = if (case == "wrong-item" || case == "missing-item") expected.copy(itemId = "absent") else expected
        return f.repository.applyOperation(target, operation, actor, bound)
    }

    private fun assertOutcome(case: String, before: List<Any>, result: Either<*, *>) {
        if (case == "exact") {
            val updated = assertIs<DocumentState>(assertIs<Either.Right<*>>(result).value)
            assertEquals(Pair(2L, "editowned text\n"), Pair(updated.version, updated.content))
            assertEquals(1, transaction { CollaborationOperationsTable.selectAll().count() })
            assertEquals(before.last(), f.snapshot().last())
        } else {
            val error = assertIs<Either.Left<*>>(result).value
            when (case) {
                "forged-actor", "new-owner", "blank-actor" -> assertIs<AuthorizationException>(error)
                "missing-document", "missing-item" -> assertIs<ItemNotFoundException>(error)
                "wrong-item", "rollback" -> assertIs<DatabaseException>(error)
                else -> assertIs<InvalidOperationException>(error)
            }
            assertEquals(before, f.snapshot())
        }
    }

    private fun arrange(case: String, expected: DocumentState) = transaction {
        when (case) {
            "stale-version" -> DocumentStatesTable.update({
                DocumentStatesTable.itemId eq f.item.id
            }) { it[version] = 2 }
            "stale-content" -> DocumentStatesTable.update({ DocumentStatesTable.itemId eq f.item.id }) {
                it[content] =
                    "changed"
            }
            "stale-time" -> DocumentStatesTable.update({ DocumentStatesTable.itemId eq f.item.id }) {
                it[lastModified] = expected.lastModified + 1.seconds
            }
            "missing-document" -> DocumentStatesTable.deleteWhere { itemId eq f.item.id }
            "new-owner" -> StorageItemsTable.update({ StorageItemsTable.id eq f.item.id }) {
                it[ownerId] =
                    f.storage.bob.id
            }
            "trashed-item" -> StorageItemsTable.update({ StorageItemsTable.id eq f.item.id }) { it[isTrashed] = true }
            "folder-item" -> StorageItemsTable.update({ StorageItemsTable.id eq f.item.id }) { it[type] = "FOLDER" }
            "rollback" -> exec("ALTER TABLE document_states ADD CONSTRAINT reject_new_version CHECK (version < 2)")
        }
    }

    @Test
    fun `C89-WRITE simultaneous transformed writes cannot overwrite one another`() = testApplication {
        application { f.install(this) }
        startApplication()
        val expected = f.repository.getDocumentState(f.item.id).fold({ throw it }, { checkNotNull(it) })
        val results = coroutineScope {
            List(2) {
                async(Dispatchers.IO) {
                    f.repository.applyOperation(f.item.id, operation(), f.storage.alice.id, expected)
                }
            }.map { it.await() }
        }
        assertEquals(1, results.count { it.isRight() })
        assertIs<InvalidOperationException>(assertIs<Either.Left<*>>(results.single { it.isLeft() }).value)
        assertEquals(1, transaction { CollaborationOperationsTable.selectAll().count() })
        val state = f.repository.getDocumentState(f.item.id).fold({ throw it }, { checkNotNull(it) })
        assertEquals(Pair(2L, "editowned text\n"), Pair(state.version, state.content))
    }

    private fun operation() = CollaborationOperation.Insert(
        userId = f.storage.alice.id,
        timestamp = f.item.createdAt,
        baseVersion = 1,
        position = 0,
        text = "edit",
    )
}
