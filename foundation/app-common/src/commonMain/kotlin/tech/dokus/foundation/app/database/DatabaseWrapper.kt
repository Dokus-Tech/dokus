package tech.dokus.foundation.app.database

import tech.dokus.domain.model.common.Feature
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Interface for database wrappers that handles thread-safe initialization.
 *
 * @param T The database type (e.g., AuthDatabase, InvoicingDatabase)
 */
interface DatabaseWrapper<T> {
    /**
     * Initializes the database. Safe to call multiple times - subsequent calls are no-ops.
     * Must be called before [get].
     */
    suspend fun initialize()

    /**
     * Returns the initialized database instance.
     *
     * @throws IllegalStateException if [initialize] hasn't been called
     */
    fun get(): T
}

/**
 * Creates a DatabaseWrapper implementation with thread-safe initialization.
 *
 * Handles schema migration failures by dropping and recreating the database.
 * Since these databases are used for caching, data loss is acceptable.
 *
 * @param feature The feature this database belongs to (e.g., Feature.Auth)
 * @param schema The async schema for this database (e.g., AuthDatabase.Schema)
 * @param createDatabase Factory function to create the database instance from a driver
 */
fun <T> DatabaseWrapper(
    feature: Feature,
    schema: SqlSchema<QueryResult.AsyncValue<Unit>>,
    createDatabase: (SqlDriver) -> T
): DatabaseWrapper<T> = object : DatabaseWrapper<T> {
    private val mutex = Mutex()
    private var driver: SqlDriver? = null
    private var database: T? = null
    private var isInitialized = false

    override suspend fun initialize() {
        mutex.withLock {
            if (isInitialized) return

            try {
                val newDriver = feature.createSqlDriver(schema)
                val newDatabase = createDatabase(newDriver)

                driver = newDriver
                database = newDatabase
                isInitialized = true
            } catch (e: Exception) {
                // Schema mismatch or corruption - delete and retry
                println("Database initialization failed for ${feature.frontendDbName}, dropping and recreating: ${e.message}")

                try {
                    feature.deleteSqlDatabase()
                } catch (deleteError: Exception) {
                    println("Failed to delete database ${feature.frontendDbName}: ${deleteError.message}")
                }

                // Retry initialization after deletion
                val newDriver = feature.createSqlDriver(schema)
                val newDatabase = createDatabase(newDriver)

                driver = newDriver
                database = newDatabase
                isInitialized = true
            }
        }
    }

    override fun get(): T {
        check(isInitialized) { "Database not initialized. Call initialize() first." }
        return database!!
    }
}
