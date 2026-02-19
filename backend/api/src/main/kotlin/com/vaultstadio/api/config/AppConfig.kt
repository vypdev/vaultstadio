/**
 * VaultStadio Application Configuration
 *
 * Centralized configuration management using environment variables
 * and application.yaml file.
 */

package com.vaultstadio.api.config

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

private val logger = KotlinLogging.logger {}

/**
 * Main application configuration.
 */
data class AppConfig(
    val server: HttpServerConfig,
    val database: DbConfig,
    val storage: StorageConfig,
    val security: SecurityConfig,
    val plugins: PluginsConfig,
) {
    companion object {
        /**
         * Loads configuration from environment variables.
         */
        fun fromEnvironment(): AppConfig {
            logger.info { "Loading configuration from environment" }

            return AppConfig(
                server = HttpServerConfig.fromEnvironment(),
                database = DbConfig.fromEnvironment(),
                storage = StorageConfig.fromEnvironment(),
                security = SecurityConfig.fromEnvironment(),
                plugins = PluginsConfig.fromEnvironment(),
            )
        }
    }
}

/**
 * HTTP Server configuration.
 */
data class HttpServerConfig(
    val host: String,
    val port: Int,
    val development: Boolean,
    val corsAllowedOrigins: List<String>,
) {
    companion object {
        fun fromEnvironment() = HttpServerConfig(
            host = getEnv("SERVER_HOST", "0.0.0.0"),
            port = getEnv("SERVER_PORT", "8080").toInt(),
            development = getEnv("DEVELOPMENT_MODE", "false").toBoolean(),
            corsAllowedOrigins = getEnv("CORS_ALLOWED_ORIGINS", "*").split(",").map { it.trim() },
        )
    }
}

/**
 * Database configuration for application settings.
 */
data class DbConfig(
    val url: String,
    val user: String,
    val password: String,
    val driver: String,
    val maxPoolSize: Int,
    val minIdle: Int,
    val idleTimeout: Long,
    val connectionTimeout: Long,
    val maxLifetime: Long,
    val runMigrations: Boolean,
) {
    companion object {
        fun fromEnvironment() = DbConfig(
            url = getEnv("DATABASE_URL", "jdbc:postgresql://localhost:5432/vaultstadio"),
            user = getEnv("DATABASE_USER", "vaultstadio"),
            password = getEnv("DATABASE_PASSWORD", "vaultstadio"),
            driver = getEnv("DATABASE_DRIVER", "org.postgresql.Driver"),
            maxPoolSize = getEnv("DATABASE_MAX_POOL_SIZE", "10").toInt(),
            minIdle = getEnv("DATABASE_MIN_IDLE", "2").toInt(),
            idleTimeout = getEnv("DATABASE_IDLE_TIMEOUT", "600000").toLong(),
            connectionTimeout = getEnv("DATABASE_CONNECTION_TIMEOUT", "30000").toLong(),
            maxLifetime = getEnv("DATABASE_MAX_LIFETIME", "1800000").toLong(),
            runMigrations = getEnv("DATABASE_RUN_MIGRATIONS", "true").toBoolean(),
        )
    }
}

/**
 * Storage configuration.
 */
data class StorageConfig(
    val type: StorageType,
    val localPath: String,
    val tempPath: String,
    val maxFileSize: Long,
    val allowedMimeTypes: List<String>,
    val s3: S3Config?,
) {
    companion object {
        fun fromEnvironment(): StorageConfig {
            val storageType = StorageType.valueOf(
                getEnv("STORAGE_TYPE", "LOCAL").uppercase(),
            )

            // Use sensible defaults based on environment
            // In Docker: /data/storage, locally: ./data/storage
            val defaultStoragePath = if (System.getenv("DOCKER_CONTAINER") != null) {
                "/data/storage"
            } else {
                "./data/storage"
            }
            val defaultTempPath = if (System.getenv("DOCKER_CONTAINER") != null) {
                "/data/temp"
            } else {
                "./data/temp"
            }

            return StorageConfig(
                type = storageType,
                localPath = getEnv("STORAGE_LOCAL_PATH", defaultStoragePath),
                tempPath = getEnv("STORAGE_TEMP_PATH", defaultTempPath),
                maxFileSize = getEnv("STORAGE_MAX_FILE_SIZE", "${10L * 1024 * 1024 * 1024}").toLong(), // 10GB
                allowedMimeTypes = getEnv("STORAGE_ALLOWED_MIME_TYPES", "*").split(",").map { it.trim() },
                s3 = if (storageType == StorageType.S3) S3Config.fromEnvironment() else null,
            )
        }
    }
}

enum class StorageType {
    LOCAL,
    S3,
    MINIO,
}

/**
 * S3/MinIO configuration.
 */
data class S3Config(
    val endpoint: String,
    val region: String,
    val bucket: String,
    val accessKey: String,
    val secretKey: String,
    val usePathStyle: Boolean,
) {
    companion object {
        fun fromEnvironment() = S3Config(
            endpoint = getEnv("S3_ENDPOINT", "http://localhost:9000"),
            region = getEnv("S3_REGION", "us-east-1"),
            bucket = getEnv("S3_BUCKET", "vaultstadio"),
            accessKey = getEnv("S3_ACCESS_KEY", "minioadmin"),
            secretKey = getEnv("S3_SECRET_KEY", "minioadmin"),
            usePathStyle = getEnv("S3_USE_PATH_STYLE", "true").toBoolean(),
        )
    }
}

/**
 * Security configuration.
 */
data class SecurityConfig(
    val jwtSecret: String,
    val jwtIssuer: String,
    val jwtAudience: String,
    val sessionDuration: Duration,
    val refreshTokenDuration: Duration,
    val bcryptRounds: Int,
    val rateLimitEnabled: Boolean,
    val rateLimitRequests: Int,
    val rateLimitWindowSeconds: Int,
    val federationPublicKey: String?,
    val federationPrivateKey: String?,
) {
    companion object {
        fun fromEnvironment() = SecurityConfig(
            jwtSecret = getEnvRequired("JWT_SECRET"),
            jwtIssuer = getEnv("JWT_ISSUER", "vaultstadio"),
            jwtAudience = getEnv("JWT_AUDIENCE", "vaultstadio-api"),
            sessionDuration = getEnv("SESSION_DURATION_HOURS", "24").toInt().hours,
            refreshTokenDuration = getEnv("REFRESH_TOKEN_DURATION_DAYS", "30").toInt().days,
            bcryptRounds = getEnv("BCRYPT_ROUNDS", "12").toInt(),
            rateLimitEnabled = getEnv("RATE_LIMIT_ENABLED", "true").toBoolean(),
            rateLimitRequests = getEnv("RATE_LIMIT_REQUESTS", "100").toInt(),
            rateLimitWindowSeconds = getEnv("RATE_LIMIT_WINDOW_SECONDS", "60").toInt(),
            federationPublicKey = getEnvOptional("FEDERATION_PUBLIC_KEY"),
            federationPrivateKey = getEnvOptional("FEDERATION_PRIVATE_KEY"),
        )
    }
}

/**
 * Plugins configuration.
 */
data class PluginsConfig(
    val directory: String,
    val autoLoad: Boolean,
    val enabledPlugins: List<String>,
) {
    companion object {
        fun fromEnvironment(): PluginsConfig {
            // Use sensible defaults based on environment
            val defaultPluginsPath = if (System.getenv("DOCKER_CONTAINER") != null) {
                "/data/plugins"
            } else {
                "./data/plugins"
            }

            return PluginsConfig(
                directory = getEnv("PLUGINS_DIRECTORY", defaultPluginsPath),
                autoLoad = getEnv("PLUGINS_AUTO_LOAD", "true").toBoolean(),
                enabledPlugins = getEnv("PLUGINS_ENABLED", "").split(",")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() },
            )
        }
    }
}

/**
 * Get environment variable with default value.
 */
private fun getEnv(name: String, default: String): String {
    return System.getenv(name) ?: default
}

/**
 * Get optional environment variable (returns null if not set).
 */
private fun getEnvOptional(name: String): String? {
    return System.getenv(name)?.takeIf { it.isNotBlank() }
}

/**
 * Get required environment variable.
 */
private fun getEnvRequired(name: String): String {
    return System.getenv(name) ?: run {
        // In development, provide a default for required secrets
        val isDev = System.getenv("DEVELOPMENT_MODE")?.toBoolean() ?: true
        if (isDev) {
            logger.warn { "Required environment variable $name not set, using development default" }
            when (name) {
                "JWT_SECRET" -> "dev-secret-key-change-in-production-minimum-32-chars"
                else -> throw IllegalStateException("Required environment variable $name is not set")
            }
        } else {
            throw IllegalStateException("Required environment variable $name is not set")
        }
    }
}
