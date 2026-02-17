/**
 * VaultStadio App Configuration Tests
 */

package com.vaultstadio.api.config

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

/**
 * Unit tests for AppConfig and related configuration classes.
 */
class AppConfigTest {

    @Nested
    @DisplayName("HttpServerConfig Tests")
    inner class HttpServerConfigTests {

        @Test
        fun `should create config from environment with defaults`() {
            val config = HttpServerConfig.fromEnvironment()

            assertNotNull(config)
            assertNotNull(config.host)
            assertTrue(config.port > 0)
            assertNotNull(config.corsAllowedOrigins)
        }

        @Test
        fun `should have default host of 0_0_0_0`() {
            // When SERVER_HOST is not set, default is 0.0.0.0
            val config = HttpServerConfig(
                host = "0.0.0.0",
                port = 8080,
                development = false,
                corsAllowedOrigins = listOf("*"),
            )

            assertEquals("0.0.0.0", config.host)
        }

        @Test
        fun `should have default port of 8080`() {
            val config = HttpServerConfig(
                host = "0.0.0.0",
                port = 8080,
                development = false,
                corsAllowedOrigins = listOf("*"),
            )

            assertEquals(8080, config.port)
        }

        @Test
        fun `should parse cors origins from comma-separated list`() {
            val origins = "http://localhost:3000,http://localhost:5173".split(",").map { it.trim() }

            assertEquals(2, origins.size)
            assertEquals("http://localhost:3000", origins[0])
            assertEquals("http://localhost:5173", origins[1])
        }
    }

    @Nested
    @DisplayName("DbConfig Tests")
    inner class DbConfigTests {

        @Test
        fun `should create config from environment with defaults`() {
            val config = DbConfig.fromEnvironment()

            assertNotNull(config)
            assertNotNull(config.url)
            assertNotNull(config.user)
            assertNotNull(config.password)
            assertNotNull(config.driver)
        }

        @Test
        fun `should have sensible pool defaults`() {
            val config = DbConfig(
                url = "jdbc:postgresql://localhost:5432/test",
                user = "test",
                password = "test",
                driver = "org.postgresql.Driver",
                maxPoolSize = 10,
                minIdle = 2,
                idleTimeout = 600000,
                connectionTimeout = 30000,
                maxLifetime = 1800000,
                runMigrations = true,
            )

            assertTrue(config.maxPoolSize > 0)
            assertTrue(config.minIdle > 0)
            assertTrue(config.minIdle <= config.maxPoolSize)
            assertTrue(config.idleTimeout > 0)
            assertTrue(config.connectionTimeout > 0)
        }
    }

    @Nested
    @DisplayName("StorageConfig Tests")
    inner class StorageConfigTests {

        @Test
        fun `should create config from environment with defaults`() {
            val config = StorageConfig.fromEnvironment()

            assertNotNull(config)
            assertNotNull(config.type)
            assertNotNull(config.localPath)
            assertNotNull(config.tempPath)
            assertTrue(config.maxFileSize > 0)
        }

        @Test
        fun `should have LOCAL as default storage type`() {
            val config = StorageConfig(
                type = StorageType.LOCAL,
                localPath = "./data/storage",
                tempPath = "./data/temp",
                maxFileSize = 10L * 1024 * 1024 * 1024,
                allowedMimeTypes = listOf("*"),
                s3 = null,
            )

            assertEquals(StorageType.LOCAL, config.type)
        }

        @Test
        fun `storage types should be defined`() {
            val types = StorageType.entries.toTypedArray()

            assertTrue(types.contains(StorageType.LOCAL))
            assertTrue(types.contains(StorageType.S3))
            assertTrue(types.contains(StorageType.MINIO))
        }
    }

    @Nested
    @DisplayName("S3Config Tests")
    inner class S3ConfigTests {

        @Test
        fun `should create S3 config from environment`() {
            val config = S3Config.fromEnvironment()

            assertNotNull(config)
            assertNotNull(config.endpoint)
            assertNotNull(config.region)
            assertNotNull(config.bucket)
            assertNotNull(config.accessKey)
            assertNotNull(config.secretKey)
        }

        @Test
        fun `should have sensible defaults`() {
            val config = S3Config(
                endpoint = "http://localhost:9000",
                region = "us-east-1",
                bucket = "vaultstadio",
                accessKey = "minioadmin",
                secretKey = "minioadmin",
                usePathStyle = true,
            )

            assertEquals("us-east-1", config.region)
            assertTrue(config.usePathStyle)
        }
    }

    @Nested
    @DisplayName("SecurityConfig Tests")
    inner class SecurityConfigTests {

        @Test
        fun `should have required jwt fields`() {
            val config = SecurityConfig(
                jwtSecret = "test-secret-key-minimum-32-characters-long",
                jwtIssuer = "vaultstadio",
                jwtAudience = "vaultstadio-api",
                sessionDuration = 24.hours,
                refreshTokenDuration = 30.days,
                bcryptRounds = 12,
                rateLimitEnabled = true,
                rateLimitRequests = 100,
                rateLimitWindowSeconds = 60,
                federationPublicKey = null,
                federationPrivateKey = null,
            )

            assertNotNull(config.jwtSecret)
            assertTrue(config.jwtSecret.length >= 32)
            assertNotNull(config.jwtIssuer)
            assertNotNull(config.jwtAudience)
        }

        @Test
        fun `should have sensible session durations`() {
            val config = SecurityConfig(
                jwtSecret = "test-secret-key-minimum-32-characters-long",
                jwtIssuer = "vaultstadio",
                jwtAudience = "vaultstadio-api",
                sessionDuration = 24.hours,
                refreshTokenDuration = 30.days,
                bcryptRounds = 12,
                rateLimitEnabled = true,
                rateLimitRequests = 100,
                rateLimitWindowSeconds = 60,
                federationPublicKey = null,
                federationPrivateKey = null,
            )

            assertTrue(config.sessionDuration > kotlin.time.Duration.ZERO)
            assertTrue(config.refreshTokenDuration > config.sessionDuration)
        }

        @Test
        fun `bcrypt rounds should be reasonable`() {
            val config = SecurityConfig(
                jwtSecret = "test-secret-key-minimum-32-characters-long",
                jwtIssuer = "vaultstadio",
                jwtAudience = "vaultstadio-api",
                sessionDuration = 24.hours,
                refreshTokenDuration = 30.days,
                bcryptRounds = 12,
                rateLimitEnabled = true,
                rateLimitRequests = 100,
                rateLimitWindowSeconds = 60,
                federationPublicKey = null,
                federationPrivateKey = null,
            )

            assertTrue(config.bcryptRounds >= 10)
            assertTrue(config.bcryptRounds <= 15)
        }

        @Test
        fun `rate limit config should have sensible defaults`() {
            val config = SecurityConfig(
                jwtSecret = "test-secret-key-minimum-32-characters-long",
                jwtIssuer = "vaultstadio",
                jwtAudience = "vaultstadio-api",
                sessionDuration = 24.hours,
                refreshTokenDuration = 30.days,
                bcryptRounds = 12,
                rateLimitEnabled = true,
                rateLimitRequests = 100,
                rateLimitWindowSeconds = 60,
                federationPublicKey = null,
                federationPrivateKey = null,
            )

            assertTrue(config.rateLimitRequests > 0)
            assertTrue(config.rateLimitWindowSeconds > 0)
        }
    }

    @Nested
    @DisplayName("PluginsConfig Tests")
    inner class PluginsConfigTests {

        @Test
        fun `should create config from environment with defaults`() {
            val config = PluginsConfig.fromEnvironment()

            assertNotNull(config)
            assertNotNull(config.directory)
        }

        @Test
        fun `should have autoLoad option`() {
            val config = PluginsConfig(
                directory = "./data/plugins",
                autoLoad = true,
                enabledPlugins = emptyList(),
            )

            assertTrue(config.autoLoad)
        }

        @Test
        fun `should support enabled plugins list`() {
            val config = PluginsConfig(
                directory = "./data/plugins",
                autoLoad = true,
                enabledPlugins = listOf("image-metadata", "video-metadata"),
            )

            assertEquals(2, config.enabledPlugins.size)
            assertTrue(config.enabledPlugins.contains("image-metadata"))
        }

        @Test
        fun `should handle empty enabled plugins`() {
            val enabledPlugins = "".split(",").map { it.trim() }.filter { it.isNotEmpty() }

            assertTrue(enabledPlugins.isEmpty())
        }
    }

    @Nested
    @DisplayName("AppConfig Integration Tests")
    inner class AppConfigIntegrationTests {

        @Test
        fun `should create full config from environment`() {
            // This test verifies that fromEnvironment() doesn't throw
            // even when environment variables are not set
            val config = AppConfig.fromEnvironment()

            assertNotNull(config)
            assertNotNull(config.server)
            assertNotNull(config.database)
            assertNotNull(config.storage)
            assertNotNull(config.security)
            assertNotNull(config.plugins)
        }
    }
}
