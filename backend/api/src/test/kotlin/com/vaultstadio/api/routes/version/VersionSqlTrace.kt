package com.vaultstadio.api.routes.version

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.testcontainers.containers.PostgreSQLContainer
import java.sql.DriverManager
import java.util.UUID

/** Request SQL boundaries are observed from PostgreSQL itself; Docker log arrival is asynchronous. */
internal class VersionSqlTrace(private val postgres: PostgreSQLContainer<*>) {
    suspend fun <T> capture(request: suspend () -> T): Pair<T, String> {
        val start = "vaultstadio_sql_start_${UUID.randomUUID()}"
        val end = "vaultstadio_sql_end_${UUID.randomUUID()}"
        mark(start)
        awaitMarker(start)
        val result = request()
        mark(end)
        val lines = awaitMarker(end).lines()
        val from = lines.indexOfFirst { it.contains(start) }
        val to = lines.indexOfFirst { it.contains(end) }
        check(from >= 0 && to > from) { "PostgreSQL SQL boundaries must be observed in order" }
        return result to lines.subList(from + 1, to).joinToString("\n")
    }

    private suspend fun mark(marker: String) = withContext(Dispatchers.IO) {
        DriverManager.getConnection(postgres.jdbcUrl, postgres.username, postgres.password).use { connection ->
            connection.createStatement().use { statement ->
                statement.executeQuery("SELECT '$marker'").use { rows ->
                    check(rows.next() && rows.getString(1) == marker) { "PostgreSQL SQL marker failed" }
                }
            }
        }
    }

    private suspend fun awaitMarker(marker: String): String = withTimeout(5_000) {
        var logs = postgres.logs
        while (!logs.contains(marker)) {
            delay(10)
            logs = postgres.logs
        }
        logs
    }
}
