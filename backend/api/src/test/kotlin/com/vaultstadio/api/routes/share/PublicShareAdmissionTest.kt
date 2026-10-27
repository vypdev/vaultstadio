package com.vaultstadio.api.routes.share

import arrow.core.Either
import com.vaultstadio.domain.share.access.PublicShareFailure
import com.vaultstadio.infrastructure.persistence.entities.ShareLinksTable
import com.vaultstadio.infrastructure.persistence.entities.StorageItemsTable
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Instant
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.sql.DriverManager
import kotlin.test.assertEquals
import kotlin.test.assertIs

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PublicShareAdmissionTest {
    private val f = PublicShareIntegrationFixture()

    @BeforeAll fun start() = f.start()

    @BeforeEach fun reset() = f.reset()

    @AfterAll fun close() = f.close()

    @ParameterizedTest(name = "S90-SQL-revalidation {0}")
    @ValueSource(
        strings = [
            "owner",
            "trash",
            "deleted-item",
            "folder",
            "size",
            "key",
            "version",
            "name",
            "password",
            "revoked",
            "expired",
            "limit",
            "deleted-share",
            "token",
            "creator",
            "rebind",
        ],
    )
    fun staleSnapshotCannotConsumeAllowance(case: String) = runBlocking {
        val snapshot = f.repository.inspect(f.share).getOrNull()!!
        mutateSnapshot(case)
        val before = if (case == "deleted-share") null else f.count()
        val expected = when (case) {
            "owner", "trash", "deleted-item", "folder", "deleted-share" -> PublicShareFailure.NOT_FOUND
            "revoked" -> PublicShareFailure.REVOKED
            "expired" -> PublicShareFailure.EXPIRED
            "limit" -> PublicShareFailure.LIMIT_REACHED
            else -> PublicShareFailure.CHANGED
        }
        assertEquals(Either.Left(expected), f.repository.admit(snapshot))
        if (before != null) assertEquals(before, f.count())
    }

    @ParameterizedTest(name = "S90-SQL-last-allowance contenders={0}")
    @ValueSource(ints = [2, 8, 16])
    fun oneRemainingAllowanceAdmitsExactlyOneConcurrentRequest(contenders: Int) = runBlocking {
        f.update(f.share.copy(maxDownloads = 1))
        val snapshot = f.repository.inspect(f.share).getOrNull()!!
        val start = CompletableDeferred<Unit>()
        val jobs = List(contenders) {
            async(Dispatchers.IO) {
                start.await()
                f.repository.admit(snapshot)
            }
        }
        start.complete(Unit)
        val results = jobs.awaitAll()
        assertEquals(1, results.count { it.isRight() })
        assertEquals(contenders - 1, results.count { it == Either.Left(PublicShareFailure.LIMIT_REACHED) })
        assertEquals(1, f.count())
    }

    @Test
    fun `S90 stale lifecycle update cannot reset an admitted counter`() = runBlocking {
        val stale = f.share
        val snapshot = f.repository.inspect(stale).getOrNull()!!
        assertIs<Either.Right<*>>(f.repository.admit(snapshot))
        f.update(stale.copy(isActive = false))
        assertEquals(1, f.count())
        assertEquals(1, f.share.downloadCount)
        assertEquals(Either.Left(PublicShareFailure.REVOKED), f.repository.admit(snapshot))
    }

    @Test
    fun `S90 actual SQL failure rolls back admission counter`() = runBlocking {
        val snapshot = f.repository.inspect(f.share).getOrNull()!!
        sql(
            """CREATE FUNCTION reject_share_count() RETURNS trigger LANGUAGE plpgsql AS
            'BEGIN RAISE EXCEPTION ''deliberate counter failure''; END'""",
        )
        sql(
            "CREATE TRIGGER reject_share_count BEFORE UPDATE ON share_links " +
                "FOR EACH ROW EXECUTE FUNCTION reject_share_count()",
        )
        try {
            assertEquals(Either.Left(PublicShareFailure.UNAVAILABLE), f.repository.admit(snapshot))
            assertEquals(0, f.count())
        } finally {
            sql("DROP TRIGGER reject_share_count ON share_links")
            sql("DROP FUNCTION reject_share_count()")
        }
        assertIs<Either.Right<*>>(f.repository.admit(snapshot))
        assertEquals(1, f.count())
    }

    @Test
    fun `S90 equality expiry is checked at admission even after a successful inspection`() = runBlocking {
        f.update(f.share.copy(expiresAt = Instant.fromEpochSeconds(f.now.epochSeconds + 1)))
        val snapshot = f.repository.inspect(f.share).getOrNull()!!
        f.time.set(f.share.expiresAt!!)
        assertEquals(Either.Left(PublicShareFailure.EXPIRED), f.repository.admit(snapshot))
        assertEquals(0, f.count())
    }

    private suspend fun mutateSnapshot(case: String) {
        val otherItem = if (case == "rebind") f.storage.seedStoredFile("other bytes") else null
        transaction {
            when (case) {
                "deleted-share" -> ShareLinksTable.deleteWhere { ShareLinksTable.id eq f.share.id }
                "deleted-item" -> StorageItemsTable.deleteWhere { StorageItemsTable.id eq f.item.id }
                "password", "revoked", "expired", "limit", "token", "creator", "rebind" ->
                    ShareLinksTable.update({ ShareLinksTable.id eq f.share.id }) {
                        when (case) {
                            "password" -> it[password] = f.hasher.hash("changed")
                            "revoked" -> it[isActive] = false
                            "expired" -> it[expiresAt] = f.now
                            "limit" -> it[maxDownloads] = 1.also { _ -> it[downloadCount] = 1 }
                            "token" -> it[token] = "different"
                            "creator" -> it[createdBy] = f.storage.bob.id
                            "rebind" -> it[itemId] = otherItem!!.id
                        }
                    }
                else -> StorageItemsTable.update({ StorageItemsTable.id eq f.item.id }) {
                    when (case) {
                        "owner" -> it[ownerId] = f.storage.bob.id
                        "trash" -> it[isTrashed] = true
                        "folder" -> it[type] = "FOLDER"
                        "size" -> it[size] = f.item.size + 1
                        "key" -> it[storageKey] = "different-key"
                        "version" -> it[version] = f.item.version + 1
                        "name" -> it[name] = "renamed"
                    }
                }
            }
        }
    }

    private fun sql(statement: String) {
        DriverManager.getConnection(
            f.storage.postgres.jdbcUrl,
            f.storage.postgres.username,
            f.storage.postgres.password,
        )
            .use { connection -> connection.createStatement().use { it.execute(statement) } }
    }
}
