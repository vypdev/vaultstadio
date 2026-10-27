package com.vaultstadio.api.routes.collaboration

import arrow.core.Either
import com.vaultstadio.core.domain.model.CollaborationSession
import com.vaultstadio.core.domain.model.CursorPosition
import com.vaultstadio.core.domain.service.JoinSessionInput
import com.vaultstadio.domain.common.exception.AuthorizationException
import com.vaultstadio.domain.common.exception.DatabaseException
import com.vaultstadio.domain.common.exception.ItemNotFoundException
import com.vaultstadio.infrastructure.persistence.ExposedCollaborationMembershipRepository
import com.vaultstadio.infrastructure.persistence.entities.CollaborationSessionsTable
import com.vaultstadio.infrastructure.persistence.entities.StorageItemsTable
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import kotlinx.datetime.Clock
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.sql.Connection
import java.sql.DriverManager
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CollaborationMembershipRaceTest {
    private val f = CollaborationIntegrationFixture()

    @BeforeAll fun start() = f.start()

    @AfterAll fun close() = f.close()

    @ParameterizedTest(name = "C89-RACE {0}")
    @ValueSource(
        strings = [
            "concurrent-admissions",
            "join-before-leave",
            "leave-before-join",
            "concurrent-leaves",
            "cursor-after-leave",
            "foreign-participant",
            "transferred-owner",
            "sql-rollback",
            "cancellation",
            "owner-scoped-expiry",
            "owner-change-while-waiting-for-item-lock",
        ],
    )
    fun `membership effects use authoritative locks and atomic state`(case: String) {
        f.reset()
        testApplication {
            application { f.install(this) }
            startApplication()
            when (case) {
                "concurrent-admissions" -> concurrentAdmissions()
                "join-before-leave" -> joinBeforeLeave()
                "leave-before-join" -> leaveBeforeJoin()
                "concurrent-leaves" -> concurrentLeaves()
                "cursor-after-leave" -> cursorAfterLeave()
                "foreign-participant" -> foreignParticipant()
                "transferred-owner" -> transferredOwner()
                "sql-rollback" -> rollback()
                "cancellation" -> cancellation()
                "owner-scoped-expiry" -> expiry()
                "owner-change-while-waiting-for-item-lock" -> ownerChangeWhileLocked()
                else -> error("Unenumerated race")
            }
        }
    }

    private suspend fun join(name: String = "Owner") = f.service.joinSession(
        JoinSessionInput(f.item.id),
        f.storage.alice.id,
        name,
    ).value()

    private suspend fun concurrentAdmissions() = coroutineScope {
        val joined = List(2) { index -> async(Dispatchers.Default) { join("Device $index") } }.map { it.await() }
        assertEquals(1, joined.map { it.first.id }.toSet().size)
        assertEquals(2, f.service.getParticipants(joined.first().first.id, f.storage.alice.id).value().size)
        assertEquals(1, transaction { CollaborationSessionsTable.selectAll().count() })
    }

    private suspend fun joinBeforeLeave() {
        val first = join()
        val second = join("Second device")
        f.service.leaveSession(first.first.id, first.second.id, f.storage.alice.id).value()
        assertEquals(first.first.id, second.first.id)
        assertEquals(
            listOf(second.second.id),
            f.service.getParticipants(first.first.id, f.storage.alice.id).value().map {
                it.id
            },
        )
    }

    private suspend fun leaveBeforeJoin() {
        val first = join()
        f.service.leaveSession(first.first.id, first.second.id, f.storage.alice.id).value()
        val second = join("New session")
        assertNotEquals(first.first.id, second.first.id)
        assertEquals(
            listOf(second.second.id),
            f.service.getParticipants(second.first.id, f.storage.alice.id).value().map {
                it.id
            },
        )
    }

    private suspend fun concurrentLeaves() = coroutineScope {
        val first = join()
        val second = join("Second")
        listOf(first, second).map { (_, participant) ->
            async(Dispatchers.Default) {
                f.service.leaveSession(first.first.id, participant.id, f.storage.alice.id).value()
            }
        }.forEach { it.await() }
        val closed =
            transaction { CollaborationSessionsTable.selectAll().single()[CollaborationSessionsTable.closedAt] }
        assertTrue(closed != null)
        assertEquals(emptyList(), f.repository.getParticipants(first.first.id).value())
    }

    private suspend fun cursorAfterLeave() {
        val first = join()
        join("Keeper")
        f.service.leaveSession(first.first.id, first.second.id, f.storage.alice.id).value()
        val before = f.snapshot()
        val result = f.service.updateCursor(
            first.first.id,
            first.second.id,
            CursorPosition(1, 2, 3),
            f.storage.alice.id,
        )
        assertIs<ItemNotFoundException>(assertIs<Either.Left<*>>(result).value)
        assertEquals(before, f.snapshot())
    }

    private suspend fun foreignParticipant() {
        val first = join()
        val foreign = first.second.copy(id = UUID.randomUUID().toString(), userId = f.storage.bob.id)
        f.repository.addParticipant(first.first.id, foreign).value()
        val before = f.snapshot()
        val result = f.service.updateCursor(first.first.id, foreign.id, CursorPosition(1, 2, 3), f.storage.alice.id)
        assertIs<AuthorizationException>(assertIs<Either.Left<*>>(result).value)
        assertEquals(before, f.snapshot())
    }

    private suspend fun transferredOwner() {
        transaction {
            StorageItemsTable.update({ StorageItemsTable.id eq f.item.id }) { it[ownerId] = f.storage.bob.id }
        }
        val before = f.snapshot()
        assertIs<AuthorizationException>(
            assertIs<Either.Left<*>>(
                f.service.joinSession(JoinSessionInput(f.item.id), f.storage.alice.id, "stale owner"),
            ).value,
        )
        assertEquals(before, f.snapshot())
    }

    private suspend fun rollback() {
        transaction {
            exec(
                "ALTER TABLE collaboration_participants ADD CONSTRAINT reject_test_name " +
                    "CHECK (user_name <> 'forbidden')",
            )
        }
        try {
            val before = f.snapshot()
            val result = f.service.joinSession(JoinSessionInput(f.item.id), f.storage.alice.id, "forbidden")
            assertIs<DatabaseException>(assertIs<Either.Left<*>>(result).value)
            assertEquals(before, f.snapshot())
        } finally {
            transaction { exec("ALTER TABLE collaboration_participants DROP CONSTRAINT reject_test_name") }
        }
    }

    private suspend fun cancellation() {
        val before = f.snapshot()
        assertFailsWith<CancellationException> {
            ExposedCollaborationMembershipRepository().admit(f.item.id, f.storage.alice.id, f.item.createdAt) {
                throw CancellationException("Deliberate admission cancellation")
            }
        }
        assertEquals(before, f.snapshot())
    }

    private suspend fun ownerChangeWhileLocked() = coroutineScope {
        val before = f.snapshot().drop(1)
        val pg = f.storage.postgres
        DriverManager.getConnection(pg.jdbcUrl, pg.username, pg.password).use { blocker ->
            blocker.autoCommit = false
            try {
                blocker.prepareStatement("UPDATE storage_items SET owner_id = ? WHERE id = ?").use {
                    it.setString(1, f.storage.bob.id)
                    it.setString(2, f.item.id)
                    assertEquals(1, it.executeUpdate())
                }
                val admission = async(Dispatchers.IO) {
                    f.service.joinSession(JoinSessionInput(f.item.id), f.storage.alice.id, "Stale precheck")
                }
                awaitItemLockWait()
                blocker.commit()
                assertIs<AuthorizationException>(assertIs<Either.Left<*>>(admission.await()).value)
                assertEquals(before, f.snapshot().drop(1))
                assertEquals(f.storage.bob.id, f.storage.repository.findById(f.item.id).value()?.ownerId)
            } finally {
                blocker.rollback()
            }
        }
    }

    private suspend fun awaitItemLockWait() = withTimeout(5_000) {
        val pg = f.storage.postgres
        DriverManager.getConnection(pg.jdbcUrl, pg.username, pg.password).use { observer ->
            while (!isWaitingForItemLock(observer)) delay(10)
        }
    }

    private fun isWaitingForItemLock(observer: Connection): Boolean = observer.createStatement().use { statement ->
        statement.executeQuery(
            "SELECT count(*) FROM pg_stat_activity WHERE datname = current_database() " +
                "AND wait_event_type = 'Lock' AND query ILIKE '%storage_items%FOR UPDATE%'",
        ).use { rows ->
            rows.next()
            rows.getInt(1) > 0
        }
    }

    private suspend fun expiry() {
        val now = Clock.System.now()
        val other = f.storage.seed(owner = f.storage.bob)
        for ((id, item, expires) in listOf(
            Triple("own-expired", f.item.id, now - 1.hours),
            Triple(
                "other-expired",
                other.id,
                now - 1.hours,
            ),
            Triple("own-live", f.item.id, now + 1.hours),
        )) {
            f.repository.createSession(
                CollaborationSession(id = id, itemId = item, createdAt = now - 2.hours, expiresAt = expires),
            ).value()
        }
        assertEquals(1, f.service.cleanupExpiredSessions(f.storage.alice.id).value())
        val rows = transaction {
            CollaborationSessionsTable.selectAll().associate {
                it[CollaborationSessionsTable.id] to
                    it[CollaborationSessionsTable.closedAt]
            }
        }
        assertTrue(rows.getValue("own-expired") != null)
        assertEquals(null, rows.getValue("other-expired"))
        assertEquals(null, rows.getValue("own-live"))
    }
}

private fun <A, B> Either<A, B>.value(): B = fold({ error("Unexpected failure: $it") }, { it })
