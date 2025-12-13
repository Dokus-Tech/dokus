package ai.dokus.payment.backend.database

import ai.dokus.foundation.database.tables.payment.PaymentsTable
import ai.dokus.foundation.ktor.database.dbQuery
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.slf4j.LoggerFactory

/**
 * Payment service table initializer.
 *
 * OWNER: payment service
 * Tables owned by this service:
 * - PaymentsTable
 *
 * DEPENDS ON (must exist first):
 * - TenantTable (auth service)
 * - InvoicesTable (cashflow service)
 */
object PaymentTables {
    private val logger = LoggerFactory.getLogger(PaymentTables::class.java)

    /**
     * Initialize payment-owned tables.
     * Uses createMissingTablesAndColumns for idempotent creation.
     */
    suspend fun initialize() {
        logger.info("Initializing payment tables...")

        dbQuery {
            SchemaUtils.createMissingTablesAndColumns(
                PaymentsTable
            )
        }

        logger.info("Payment tables initialized successfully")
    }
}
