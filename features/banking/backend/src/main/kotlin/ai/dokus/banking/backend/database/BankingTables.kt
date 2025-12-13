package ai.dokus.banking.backend.database

import ai.dokus.foundation.database.tables.banking.BankConnectionsTable
import ai.dokus.foundation.database.tables.banking.BankTransactionsTable
import ai.dokus.foundation.ktor.database.dbQuery
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.slf4j.LoggerFactory

/**
 * Banking service table initializer.
 *
 * OWNER: banking service
 * Tables owned by this service:
 * - BankConnectionsTable
 * - BankTransactionsTable
 *
 * DEPENDS ON (must exist first):
 * - TenantTable (auth service)
 */
object BankingTables {
    private val logger = LoggerFactory.getLogger(BankingTables::class.java)

    /**
     * Initialize banking-owned tables.
     * Uses createMissingTablesAndColumns for idempotent creation.
     */
    suspend fun initialize() {
        logger.info("Initializing banking tables...")

        dbQuery {
            SchemaUtils.createMissingTablesAndColumns(
                BankConnectionsTable,
                BankTransactionsTable
            )
        }

        logger.info("Banking tables initialized successfully")
    }
}
