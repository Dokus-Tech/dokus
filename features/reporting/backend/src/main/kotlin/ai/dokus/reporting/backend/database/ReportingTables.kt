package ai.dokus.reporting.backend.database

import ai.dokus.foundation.database.tables.reporting.VatReturnsTable
import ai.dokus.foundation.ktor.database.dbQuery
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.slf4j.LoggerFactory

/**
 * Reporting service table initializer.
 *
 * OWNER: reporting service
 * Tables owned by this service:
 * - VatReturnsTable
 *
 * DEPENDS ON (must exist first):
 * - TenantTable (auth service)
 */
object ReportingTables {
    private val logger = LoggerFactory.getLogger(ReportingTables::class.java)

    /**
     * Initialize reporting-owned tables.
     * Uses createMissingTablesAndColumns for idempotent creation.
     */
    suspend fun initialize() {
        logger.info("Initializing reporting tables...")

        dbQuery {
            SchemaUtils.createMissingTablesAndColumns(
                VatReturnsTable
            )
        }

        logger.info("Reporting tables initialized successfully")
    }
}
