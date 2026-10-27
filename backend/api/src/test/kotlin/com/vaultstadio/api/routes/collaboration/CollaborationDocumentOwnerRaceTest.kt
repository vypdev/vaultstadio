package com.vaultstadio.api.routes.collaboration

import arrow.core.Either
import com.vaultstadio.core.domain.model.CollaborationOperation
import com.vaultstadio.core.domain.service.CreateCommentInput
import com.vaultstadio.domain.common.exception.AuthorizationException
import com.vaultstadio.domain.common.exception.StorageException
import com.vaultstadio.domain.storage.model.StorageItem
import com.vaultstadio.domain.storage.repository.StorageItemRepository
import com.vaultstadio.infrastructure.persistence.entities.StorageItemsTable
import io.ktor.server.testing.testApplication
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.assertEquals
import kotlin.test.assertIs

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CollaborationDocumentOwnerRaceTest {
    private val f = CollaborationIntegrationFixture()

    @BeforeAll fun start() = f.start()

    @BeforeEach fun reset() = f.reset()

    @AfterAll fun close() = f.close()

    @ParameterizedTest(name = "C89-WRITE-RACE {0}")
    @ValueSource(
        strings = ["apply", "snapshot", "comment-create", "comment-resolve", "comment-reply", "comment-delete"],
    )
    fun `a committed transfer after the actual precheck read revokes every document write`(case: String) {
        val reads = object : StorageItemRepository by f.storage.repository {
            override suspend fun findById(id: String): Either<StorageException, StorageItem?> {
                val row = f.storage.repository.findById(id)
                // Deterministic interleaving: retain the genuine precheck row, commit transfer, then resume.
                transaction {
                    StorageItemsTable.update({ StorageItemsTable.id eq id }) { it[ownerId] = f.storage.bob.id }
                }
                return row
            }
        }
        testApplication {
            application { f.install(this, reads) }
            startApplication()
            val comment = f.seedComment()
            val before = f.snapshot().drop(1)
            val result = when (case) {
                "apply" -> f.service.applyOperation(
                    f.item.id,
                    CollaborationOperation.Insert(
                        userId = f.storage.alice.id,
                        timestamp = f.item.createdAt,
                        baseVersion = 1,
                        position = 0,
                        text = "revoked edit",
                    ),
                    f.storage.alice.id,
                )
                "snapshot" -> f.service.saveDocument(f.item.id, f.storage.alice.id)
                "comment-create" -> f.service.createComment(
                    CreateCommentInput(f.item.id, "revoked comment", 0, 0, 1, 1),
                    f.storage.alice.id,
                )
                "comment-resolve" -> f.service.resolveComment(f.item.id, comment.id, f.storage.alice.id)
                "comment-reply" -> f.service.addReply(f.item.id, comment.id, "revoked reply", f.storage.alice.id)
                "comment-delete" -> f.service.deleteComment(f.item.id, comment.id, f.storage.alice.id)
                else -> error("Unenumerated write race")
            }
            val ownerAfter = f.storage.repository.findById(f.item.id).fold({ throw it }, { it?.ownerId })
            assertAll(
                { assertIs<AuthorizationException>(assertIs<Either.Left<*>>(result).value) },
                { assertEquals(before, f.snapshot().drop(1)) },
                { assertEquals(f.storage.bob.id, ownerAfter) },
            )
        }
    }
}
