package tech.dokus.database

import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import tech.dokus.database.tables.ai.ChatMessagesTable
import tech.dokus.database.tables.ai.DocumentChunksTable
import tech.dokus.database.tables.auth.AddressTable
import tech.dokus.database.tables.auth.PasswordResetTokensTable
import tech.dokus.database.tables.auth.RefreshTokensTable
import tech.dokus.database.tables.auth.TenantInvitationsTable
import tech.dokus.database.tables.auth.TenantMembersTable
import tech.dokus.database.tables.auth.TenantSettingsTable
import tech.dokus.database.tables.auth.TenantTable
import tech.dokus.database.tables.auth.UsersTable
import tech.dokus.database.tables.banking.BankConnectionsTable
import tech.dokus.database.tables.banking.BankTransactionsTable
import tech.dokus.database.tables.cashflow.BillsTable
import tech.dokus.database.tables.cashflow.DocumentDraftsTable
import tech.dokus.database.tables.cashflow.DocumentIngestionRunsTable
import tech.dokus.database.tables.cashflow.DocumentsTable
import tech.dokus.database.tables.cashflow.ExpensesTable
import tech.dokus.database.tables.cashflow.InvoiceItemsTable
import tech.dokus.database.tables.cashflow.InvoicesTable
import tech.dokus.database.tables.contacts.ContactNotesTable
import tech.dokus.database.tables.contacts.ContactsTable
import tech.dokus.database.tables.payment.PaymentsTable
import tech.dokus.database.tables.peppol.PeppolSettingsTable
import tech.dokus.database.tables.peppol.PeppolTransmissionsTable
import tech.dokus.foundation.backend.database.dbQuery
import tech.dokus.foundation.backend.utils.loggerFor

/**
 * Central database schema initializer for the modular monolith.
 *
 * This replaces per-service table bootstrap to avoid cross-service FK ordering issues.
 */
object DokusSchema {
    private val logger = loggerFor()

    /**
     * Creates all tables (idempotently) in a deterministic order.
     */
    suspend fun initialize() {
        logger.info("Initializing database schema...")

        dbQuery {
            SchemaUtils.createMissingTablesAndColumns(
                // ----------------------------
                // Auth (tenants, users, tokens)
                // ----------------------------
                TenantTable,
                TenantSettingsTable,
                UsersTable,
                TenantMembersTable,
                TenantInvitationsTable,
                RefreshTokensTable,
                PasswordResetTokensTable,
                AddressTable,

                // ----------------------------
                // Cashflow foundation (docs)
                // ----------------------------
                DocumentsTable,
                DocumentIngestionRunsTable,
                DocumentDraftsTable,

                // ----------------------------
                // Contacts (depends on docs/users)
                // ----------------------------
                ContactsTable,
                ContactNotesTable,

                // ----------------------------
                // Cashflow (depends on contacts)
                // ----------------------------
                InvoicesTable,
                InvoiceItemsTable,
                ExpensesTable,
                BillsTable,

                // ----------------------------
                // Payments / Banking
                // ----------------------------
                PaymentsTable,
                BankConnectionsTable,
                BankTransactionsTable,

                // ----------------------------
                // Peppol (depends on invoices/bills)
                // ----------------------------
                PeppolSettingsTable,
                PeppolTransmissionsTable,

                // ----------------------------
                // AI / RAG (depends on users/docs)
                // ----------------------------
                DocumentChunksTable,
                ChatMessagesTable,
            )
        }

        logger.info("Database schema initialized successfully")
    }
}
