package ai.dokus.cashflow.backend.database

import ai.dokus.foundation.database.tables.ai.ChatMessagesTable
import ai.dokus.foundation.database.tables.ai.DocumentChunksTable
import ai.dokus.foundation.database.tables.cashflow.BillsTable
import ai.dokus.foundation.database.tables.cashflow.DocumentProcessingTable
import ai.dokus.foundation.database.tables.cashflow.DocumentsTable
import ai.dokus.foundation.database.tables.cashflow.ExpensesTable
import ai.dokus.foundation.database.tables.cashflow.InvoiceItemsTable
import ai.dokus.foundation.database.tables.cashflow.InvoicesTable
import ai.dokus.foundation.database.tables.peppol.PeppolSettingsTable
import ai.dokus.foundation.database.tables.peppol.PeppolTransmissionsTable
import ai.dokus.foundation.ktor.database.dbQuery
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.slf4j.LoggerFactory

/**
 * Cashflow service table initializer.
 *
 * OWNER: cashflow service
 * Tables owned by this service:
 * - DocumentsTable (base document storage)
 * - DocumentProcessingTable
 * - InvoicesTable
 * - InvoiceItemsTable
 * - ExpensesTable
 * - AttachmentsTable
 * - BillsTable
 * - PeppolSettingsTable
 * - PeppolTransmissionsTable
 * - DocumentChunksTable (AI/RAG)
 * - ChatMessagesTable (AI/RAG)
 *
 * DEPENDS ON (must exist first):
 * - TenantTable (auth service)
 * - ContactsTable (contacts service)
 * - UsersTable (auth service) - for ChatMessagesTable
 */
object CashflowTables {
    private val logger = LoggerFactory.getLogger(CashflowTables::class.java)

    /**
     * Initialize cashflow-owned tables.
     * Uses createMissingTablesAndColumns for idempotent creation.
     */
    suspend fun initialize() {
        logger.info("Initializing cashflow tables...")

        dbQuery {
            SchemaUtils.createMissingTablesAndColumns(
                // Document tables
                DocumentsTable,
                DocumentProcessingTable,

                // Invoice tables
                InvoicesTable,
                InvoiceItemsTable,

                // Expense and bill tables
                ExpensesTable,
                BillsTable,

                // Peppol e-invoicing tables
                PeppolSettingsTable,
                PeppolTransmissionsTable,

                // AI/RAG tables
                DocumentChunksTable,
                ChatMessagesTable
            )
        }

        logger.info("Cashflow tables initialized successfully")
    }
}
