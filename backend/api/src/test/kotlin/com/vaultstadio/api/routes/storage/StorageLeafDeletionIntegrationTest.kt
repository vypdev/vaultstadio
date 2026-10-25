package com.vaultstadio.api.routes.storage

import arrow.core.Either
import com.vaultstadio.domain.common.exception.DatabaseException
import com.vaultstadio.domain.common.exception.InvalidOperationException
import com.vaultstadio.domain.storage.model.ItemType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StorageLeafDeletionIntegrationTest {
    private val f = StorageIntegrationFixture()

    @BeforeAll fun start() = f.start()

    @AfterAll fun close() = f.close()

    @BeforeEach fun reset() = f.reset()

    @Test
    fun `real cascade cannot delete owned or foreign children`() = runBlocking<Unit> {
        connection().use { connection ->
            connection.createStatement().use { statement ->
                statement.executeQuery(
                    "SELECT pg_get_constraintdef(oid) FROM pg_constraint WHERE conname = 'fk_storage_items_parent'",
                ).use {
                    assertTrue(it.next())
                    assertTrue(it.getString(1).contains("ON DELETE CASCADE"))
                }
            }
        }
        val parent = f.seed()
        val own = f.seed(parentId = parent.id)
        val foreign = f.seed(owner = f.bob, parentId = parent.id, type = ItemType.FILE)
        val key = f.backend.store("retained".byteInputStream(), 8, "text/plain").fold({ throw it }, { it })
        f.repository.update(foreign.copy(storageKey = key, size = 8)).fold({ throw it }, {})
        val result = f.service.deleteItem(parent.id, f.alice.id)
        assertIs<InvalidOperationException>(assertIs<Either.Left<*>>(result).value)
        assertEquals(3, f.rowCount())
        assertEquals(own, assertIs<Either.Right<*>>(f.repository.findById(own.id)).value)
        val bytes = f.backend.retrieve(key).fold({ throw it }, { it }).use { it.readBytes().decodeToString() }
        assertEquals("retained", bytes)
        assertEquals(1, f.byteFileCount())
    }

    @Test
    fun `empty leaf delete and missing replay succeed`() = runBlocking<Unit> {
        val parent = f.seed()
        assertIs<Either.Right<*>>(f.service.deleteItem(parent.id, f.alice.id))
        assertEquals(0, f.rowCount())
        assertIs<Either.Right<*>>(f.repository.delete(parent.id))
    }

    @Test
    fun `committing child first makes a racing delete fail without cascade`() = runBlocking<Unit> {
        val parent = f.seedStoredFile()
        connection().use { inserting ->
            inserting.autoCommit = false
            insertChild(inserting, parent.id)
            val deletion = async(Dispatchers.IO) { f.service.deleteItem(parent.id, f.alice.id) }
            try {
                awaitBlockedQuery("%FOR UPDATE%")
                inserting.commit()
            } finally {
                inserting.rollback()
            }
            assertIs<InvalidOperationException>(assertIs<Either.Left<*>>(withTimeout(10000) { deletion.await() }).value)
        }
        assertEquals(2, f.rowCount())
        assertEquals(1, f.byteFileCount())
        val retained = f.backend.retrieve(parent.storageKey!!).fold({ throw it }, { it }).use { it.readBytes() }
        assertEquals("retained", retained.decodeToString())
    }

    @Test
    fun `locking delete first rejects a racing child instead of cascading it`() = runBlocking<Unit> {
        val parent = f.seedStoredFile()
        connection().use { guard ->
            guard.createStatement().use { statement ->
                statement.execute("SELECT pg_advisory_lock(730001)")
                statement.execute(
                    """
                    CREATE FUNCTION hold_leaf_delete() RETURNS trigger LANGUAGE plpgsql AS ${'$'}${'$'}
                    BEGIN PERFORM pg_advisory_xact_lock(730001); RETURN OLD; END ${'$'}${'$'}
                    """.trimIndent(),
                )
                statement.execute(
                    "CREATE TRIGGER hold_leaf_delete BEFORE DELETE ON storage_items FOR EACH ROW EXECUTE FUNCTION hold_leaf_delete()",
                )
            }
            try {
                val deletion = async(Dispatchers.IO) { f.service.deleteItem(parent.id, f.alice.id) }
                awaitBlockedQuery("%DELETE%storage_items%")
                val insertion = async(Dispatchers.IO) {
                    try {
                        connection().use { insertChild(it, parent.id) }
                        "unexpected success"
                    } catch (error: SQLException) {
                        error.sqlState
                    }
                }
                try {
                    awaitBlockedQuery("%INSERT INTO storage_items%")
                } finally {
                    guard.createStatement().use { it.execute("SELECT pg_advisory_unlock(730001)") }
                }
                assertIs<Either.Right<*>>(withTimeout(10000) { deletion.await() })
                assertEquals("23503", withTimeout(10000) { insertion.await() })
            } finally {
                guard.createStatement().use {
                    it.execute("SELECT pg_advisory_unlock(730001)")
                    it.execute("DROP TRIGGER hold_leaf_delete ON storage_items")
                    it.execute("DROP FUNCTION hold_leaf_delete()")
                }
            }
        }
        assertEquals(0, f.rowCount())
        assertEquals(0, f.byteFileCount())
    }

    @Test
    fun `SQL failure returns a typed error without changing data`() = runBlocking<Unit> {
        val parent = f.seed()
        connection().use { connection ->
            connection.createStatement().use { statement ->
                statement.execute("ALTER TABLE storage_items RENAME TO storage_items_unavailable")
                try {
                    assertIs<DatabaseException>(assertIs<Either.Left<*>>(f.repository.delete(parent.id)).value)
                } finally {
                    statement.execute("ALTER TABLE storage_items_unavailable RENAME TO storage_items")
                }
            }
        }
        assertEquals(1, f.rowCount())
        assertEquals(parent, assertIs<Either.Right<*>>(f.repository.findById(parent.id)).value)
    }

    private fun connection(): Connection = DriverManager.getConnection(
        f.postgres.jdbcUrl,
        f.postgres.username,
        f.postgres.password,
    )

    private fun insertChild(connection: Connection, parentId: String) {
        connection.prepareStatement(
            """
            INSERT INTO storage_items
            (id,name,path,type,parent_id,owner_id,size,visibility,is_trashed,is_starred,created_at,updated_at,version)
            VALUES (?,?,?,'FOLDER',?,?,0,'PRIVATE',false,false,NOW(),NOW(),1)
            """.trimIndent(),
        ).use {
            val id = UUID.randomUUID().toString()
            it.setString(1, id)
            it.setString(2, id)
            it.setString(3, "/$id")
            it.setString(4, parentId)
            it.setString(5, f.alice.id)
            it.executeUpdate()
        }
    }

    private suspend fun awaitBlockedQuery(pattern: String) = withTimeout(10000) {
        while (true) {
            val waiting = connection().use { connection ->
                connection.prepareStatement(
                    "SELECT count(*) FROM pg_stat_activity WHERE datname = current_database() AND wait_event_type = 'Lock' AND query ILIKE ?",
                ).use { query ->
                    query.setString(1, pattern)
                    query.executeQuery().use {
                        it.next()
                        it.getInt(1) > 0
                    }
                }
            }
            if (waiting) break
            delay(20)
        }
    }
}
