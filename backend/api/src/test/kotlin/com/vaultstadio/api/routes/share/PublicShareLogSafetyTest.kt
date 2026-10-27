package com.vaultstadio.api.routes.share

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.classic.spi.ThrowableProxyUtil
import ch.qos.logback.core.read.ListAppender
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.slf4j.Logger.ROOT_LOGGER_NAME
import org.slf4j.LoggerFactory
import java.sql.DriverManager
import java.util.Base64
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PublicShareLogSafetyTest {
    private val f = PublicShareIntegrationFixture()

    @BeforeAll fun start() = f.start()

    @BeforeEach fun reset() = f.reset()

    @AfterAll fun close() = f.close()

    @ParameterizedTest(name = "S90-privacy-actual-log {0}")
    @ValueSource(strings = ["query-denial", "password-success", "sql-failure"])
    fun publicRequestsCannotLogCapabilityOrCredentials(case: String) = testApplication {
        application { f.install(this) }
        startApplication()
        val secret = "SyntheticNeverLog90"
        if (case == "password-success") f.update(f.share.copy(password = f.hasher.hash(secret)))
        val logger = LoggerFactory.getLogger(ROOT_LOGGER_NAME) as Logger
        val appender = ListAppender<ILoggingEvent>().apply { start() }
        logger.addAppender(appender)
        try {
            if (case == "sql-failure") sql("ALTER TABLE share_links RENAME TO hidden_share_links_90")
            val response = client.get(
                "/api/v1/share/${f.share.token}" + if (case == "query-denial") "?password=$secret" else "",
            ) {
                header("X-Share-Password", Base64.getUrlEncoder().withoutPadding().encodeToString(secret.toByteArray()))
            }
            assertEquals(
                when (case) {
                    "query-denial" -> HttpStatusCode.BadRequest
                    "sql-failure" -> HttpStatusCode.InternalServerError
                    else -> HttpStatusCode.OK
                },
                response.status,
            )
            val body = response.bodyAsText()
            val events = appender.list.toList().joinToString("\n") {
                it.formattedMessage + (it.throwableProxy?.let(ThrowableProxyUtil::asString) ?: "")
            }
            assertTrue(events.contains("[redacted]"), "Actual request logging must record the safe route")
            assertPrivateValuesAbsent(body, events, secret)
        } finally {
            logger.detachAppender(appender)
            appender.stop()
            if (case == "sql-failure") sql("ALTER TABLE hidden_share_links_90 RENAME TO share_links")
        }
    }

    private fun assertPrivateValuesAbsent(body: String, events: String, secret: String) {
        for (privateValue in listOf(
            f.share.token,
            secret,
            Base64.getUrlEncoder().withoutPadding().encodeToString(secret.toByteArray()),
            f.item.storageKey!!,
            f.share.password.orEmpty(),
        )) {
            if (privateValue.isNotEmpty()) {
                assertFalse(events.contains(privateValue), "Private value reached actual logs")
                assertFalse(body.contains(privateValue), "Private value reached public response")
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
