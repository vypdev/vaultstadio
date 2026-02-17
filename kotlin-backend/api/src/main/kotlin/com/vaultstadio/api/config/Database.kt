/**
 * VaultStadio Database Configuration
 *
 * Configures database connection pool and Flyway migrations.
 */

package com.vaultstadio.api.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.github.oshai.kotlinlogging.KotlinLogging
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database
import javax.sql.DataSource

private val logger = KotlinLogging.logger {}

/**
 * Database configuration properties.
 */
data class DatabaseConfig(
    val url: String,
    val user: String,
    val password: String,
    val driver: String = "org.postgresql.Driver",
    val maxPoolSize: Int = 10,
    val minIdle: Int = 2,
    val idleTimeout: Long = 600000,
    val connectionTimeout: Long = 30000,
    val maxLifetime: Long = 1800000,
    val runMigrations: Boolean = true,
)

/**
 * Initializes the database connection and runs migrations.
 */
class DatabaseInitializer(private val config: DatabaseConfig) {

    private var dataSource: HikariDataSource? = null

    /**
     * Initializes the database connection pool and runs migrations.
     */
    fun initialize(): Database {
        logger.info { "Initializing database connection to ${config.url}" }

        // Create HikariCP data source
        val hikariConfig = HikariConfig().apply {
            jdbcUrl = config.url
            username = config.user
            password = config.password
            driverClassName = config.driver
            maximumPoolSize = config.maxPoolSize
            minimumIdle = config.minIdle
            idleTimeout = config.idleTimeout
            connectionTimeout = config.connectionTimeout
            maxLifetime = config.maxLifetime

            // Performance optimizations
            addDataSourceProperty("cachePrepStmts", "true")
            addDataSourceProperty("prepStmtCacheSize", "250")
            addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
            addDataSourceProperty("useServerPrepStmts", "true")
        }

        dataSource = HikariDataSource(hikariConfig)

        // Run Flyway migrations
        if (config.runMigrations) {
            runMigrations(dataSource!!)
        }

        // Connect Exposed to the data source
        return Database.connect(dataSource!!)
    }

    /**
     * Runs Flyway database migrations.
     */
    private fun runMigrations(dataSource: DataSource) {
        logger.info { "Running database migrations..." }

        val flyway = Flyway.configure()
            .dataSource(dataSource)
            .locations("classpath:db/migration")
            .baselineOnMigrate(true)
            .validateMigrationNaming(true)
            .cleanDisabled(true) // Disable clean in production
            .load()

        val result = flyway.migrate()

        if (result.migrationsExecuted > 0) {
            logger.info {
                "Executed ${result.migrationsExecuted} migration(s). " +
                    "Database version: ${result.targetSchemaVersion}"
            }
        } else {
            logger.info { "Database is up to date. Version: ${result.targetSchemaVersion}" }
        }
    }

    /**
     * Closes the database connection pool.
     */
    fun close() {
        logger.info { "Closing database connection pool" }
        dataSource?.close()
    }

    /**
     * Gets the current migration info.
     */
    fun getMigrationInfo(): List<MigrationInfo> {
        val flyway = Flyway.configure()
            .dataSource(dataSource)
            .locations("classpath:db/migration")
            .load()

        return flyway.info().all().map { info ->
            MigrationInfo(
                version = info.version?.toString() ?: "baseline",
                description = info.description ?: "",
                type = info.type.toString(),
                state = info.state.toString(),
                installedOn = info.installedOn?.toString(),
            )
        }
    }
}

/**
 * Migration information.
 */
data class MigrationInfo(
    val version: String,
    val description: String,
    val type: String,
    val state: String,
    val installedOn: String?,
)

/**
 * Creates a DatabaseConfig from environment variables.
 */
fun createDatabaseConfigFromEnv(): DatabaseConfig {
    val url = System.getenv("DATABASE_URL")
        ?: "jdbc:postgresql://localhost:5432/vaultstadio"
    val user = System.getenv("DATABASE_USER")
        ?: "vaultstadio"
    val password = System.getenv("DATABASE_PASSWORD")
        ?: "vaultstadio"
    val runMigrations = System.getenv("RUN_MIGRATIONS")?.toBoolean()
        ?: true

    return DatabaseConfig(
        url = url,
        user = user,
        password = password,
        runMigrations = runMigrations,
    )
}
