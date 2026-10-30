package com.vaultstadio.api.routes.share

import arrow.core.Either
import com.vaultstadio.domain.share.access.PublicShareFailure
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.datetime.Instant
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.sql.DriverManager
import kotlin.test.assertEquals

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PublicShareClockLockTest {
    private val f = PublicShareIntegrationFixture()

    @BeforeAll fun start() = f.start()

    @BeforeEach fun reset() = f.reset()

    @AfterAll fun close() = f.close()

    @Test
    fun `S90 expiry clock is read after the actual PostgreSQL lock wait`() = runBlocking {
        f.update(f.share.copy(expiresAt = Instant.fromEpochSeconds(f.now.epochSeconds + 1)))
        val snapshot = f.repository.inspect(f.share).getOrNull()!!
        connection().use { blocker ->
            blocker.autoCommit = false
            blocker.prepareStatement("SELECT id FROM storage_items WHERE id = ? FOR UPDATE").use {
                it.setString(1, f.item.id)
                it.executeQuery().close()
            }
            val admission = async(Dispatchers.IO) { f.repository.admit(snapshot) }
            try {
                withTimeout(10000) {
                    while (!hasBlockedStatement()) delay(10)
                }
                // The genuine admission is already blocked inside its item lock acquisition.
                f.time.set(f.share.expiresAt!!)
            } finally {
                blocker.commit()
            }
            assertEquals(Either.Left(PublicShareFailure.EXPIRED), withTimeout(10000) { admission.await() })
        }
        assertEquals(0, f.count())
    }

    private fun hasBlockedStatement(): Boolean = connection().use { connection ->
        connection.createStatement().use { statement ->
            statement.executeQuery("SELECT count(*) FROM pg_stat_activity WHERE wait_event_type = 'Lock'").use {
                it.next()
                it.getInt(1) > 0
            }
        }
    }

    private fun connection() = DriverManager.getConnection(
        f.storage.postgres.jdbcUrl,
        f.storage.postgres.username,
        f.storage.postgres.password,
    )
}
