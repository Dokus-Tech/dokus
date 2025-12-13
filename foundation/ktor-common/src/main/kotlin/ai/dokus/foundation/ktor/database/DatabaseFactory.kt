package ai.dokus.foundation.ktor.database

import ai.dokus.foundation.ktor.config.AppBaseConfig
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.slf4j.LoggerFactory
import javax.sql.DataSource

class DatabaseFactory(
    private val appConfig: AppBaseConfig,
    private val poolName: String
) {
    private val logger = LoggerFactory.getLogger(DatabaseFactory::class.java)
    private var dataSource: HikariDataSource? = null
    lateinit var database: Database

    /**
     * Connect to the database without creating any tables.
     * Tables should be initialized via service-specific initializers (e.g., AuthTables.initialize()).
     */
    fun connect(): Database {
        dataSource = createHikariDataSource()

        if (appConfig.flyway.enabled) {
            runMigrations(dataSource!!)
        }

        database = Database.connect(dataSource!!)

        return database
    }

    /**
     * Connect and create the specified tables.
     * @deprecated Use connect() + service-specific table initializers instead
     */
    @Deprecated("Use connect() + service-specific table initializers", level = DeprecationLevel.WARNING)
    suspend fun init(vararg tables: Table): Database {
        connect()
        createTables(*tables)
        return database
    }

    private fun createHikariDataSource(): HikariDataSource {
        val dbConfig = appConfig.database
        val poolConfig = dbConfig.pool

        val hikariConfig = HikariConfig().apply {
            jdbcUrl = dbConfig.url
            username = dbConfig.username
            password = dbConfig.password
            driverClassName = dbConfig.driver

            maximumPoolSize = poolConfig.maxSize
            minimumIdle = poolConfig.minSize
            connectionTimeout = poolConfig.acquisitionTimeout * 1000
            idleTimeout = poolConfig.idleTimeout * 1000
            maxLifetime = poolConfig.maxLifetime * 1000
            leakDetectionThreshold = poolConfig.leakDetectionThreshold * 1000

            poolName = this@DatabaseFactory.poolName

            addDataSourceProperty("cachePrepStmts", "true")
            addDataSourceProperty("prepStmtCacheSize", "250")
            addDataSourceProperty("prepStmtCacheSqlLimit", "2048")

            validate()
        }

        return HikariDataSource(hikariConfig)
    }

    private fun runMigrations(dataSource: DataSource) {
        val flywayConfig = appConfig.flyway

        val flyway = Flyway.configure()
            .dataSource(dataSource)
            .locations(*flywayConfig.locations.toTypedArray())
            .schemas(*flywayConfig.schemas.toTypedArray())
            .baselineOnMigrate(flywayConfig.baselineOnMigrate)
            .baselineVersion(flywayConfig.baselineVersion)
            .load()

        val result = flyway.migrate()
        logger.info("Migration completed: ${result.migrationsExecuted} migrations executed")
    }

    private suspend fun createTables(vararg table: Table) = withContext(Dispatchers.IO) {
        transaction {
            SchemaUtils.create(*table)
        }
    }

    fun close() {
        logger.info("Closing database connections...")
        dataSource?.close()
        logger.info("Database connections closed")
    }
}

suspend fun <T> dbQuery(block: () -> T): T = withContext(Dispatchers.IO) {
    transaction {
        TenantContextHolder.currentTenantId()?.let { tenant ->
            TransactionManager.current().exec("set local app.tenant_id = '$tenant'")
        }
        block()
    }
}

/**
 * Run a DB query with an explicit tenant context for RLS/guards.
 * This will set `app.tenant_id` for the current transaction.
 */
suspend fun <T> dbQuery(tenantId: String, block: () -> T): T =
    withTenantContext(tenantId) { dbQuery(block) }
