package ai.dokus.foundation.database.schema

import ai.dokus.foundation.database.tables.ai.ChatMessagesTable
import ai.dokus.foundation.database.tables.ai.DocumentChunksTable
import ai.dokus.foundation.database.tables.auth.AddressTable
import ai.dokus.foundation.database.tables.auth.PasswordResetTokensTable
import ai.dokus.foundation.database.tables.auth.RefreshTokensTable
import ai.dokus.foundation.database.tables.auth.TenantInvitationsTable
import ai.dokus.foundation.database.tables.auth.TenantMembersTable
import ai.dokus.foundation.database.tables.auth.TenantSettingsTable
import ai.dokus.foundation.database.tables.auth.TenantTable
import ai.dokus.foundation.database.tables.auth.UsersTable
import ai.dokus.foundation.database.tables.banking.BankConnectionsTable
import ai.dokus.foundation.database.tables.banking.BankTransactionsTable
import ai.dokus.foundation.database.tables.cashflow.BillsTable
import ai.dokus.foundation.database.tables.cashflow.DocumentProcessingTable
import ai.dokus.foundation.database.tables.cashflow.DocumentsTable
import ai.dokus.foundation.database.tables.cashflow.ExpensesTable
import ai.dokus.foundation.database.tables.cashflow.InvoiceItemsTable
import ai.dokus.foundation.database.tables.cashflow.InvoicesTable
import ai.dokus.foundation.database.tables.contacts.ContactNotesTable
import ai.dokus.foundation.database.tables.contacts.ContactsTable
import ai.dokus.foundation.database.tables.payment.PaymentsTable
import ai.dokus.foundation.database.tables.peppol.PeppolSettingsTable
import ai.dokus.foundation.database.tables.peppol.PeppolTransmissionsTable
import tech.dokus.foundation.ktor.database.dbQuery
import tech.dokus.foundation.ktor.utils.loggerFor
import org.jetbrains.exposed.v1.jdbc.SchemaUtils

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
                DocumentProcessingTable,

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

