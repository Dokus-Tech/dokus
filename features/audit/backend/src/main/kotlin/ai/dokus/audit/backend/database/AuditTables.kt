package ai.dokus.audit.backend.database

import ai.dokus.foundation.database.tables.audit.AuditLogsTable
import ai.dokus.foundation.ktor.database.dbQuery
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.slf4j.LoggerFactory

/**
 * Audit service table initializer.
 *
 * OWNER: audit service
 * Tables owned by this service:
 * - AuditLogsTable
 *
 * DEPENDS ON (must exist first):
 * - TenantTable (auth service)
 */
object AuditTables {
    private val logger = LoggerFactory.getLogger(AuditTables::class.java)

    /**
     * Initialize audit-owned tables.
     * Uses createMissingTablesAndColumns for idempotent creation.
     */
    suspend fun initialize() {
        logger.info("Initializing audit tables...")

        dbQuery {
            SchemaUtils.createMissingTablesAndColumns(
                AuditLogsTable
            )
        }

        logger.info("Audit tables initialized successfully")
    }
}
