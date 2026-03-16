package tech.dokus.database

import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import tech.dokus.database.tables.ai.ChatMessagesTable
import tech.dokus.database.tables.ai.DocumentChunksTable
import tech.dokus.database.tables.ai.DocumentExamplesTable
import tech.dokus.database.tables.auth.AddressTable
import tech.dokus.database.tables.auth.FirmAccessTable
import tech.dokus.database.tables.auth.FirmMembersTable
import tech.dokus.database.tables.auth.FirmsTable
import tech.dokus.database.tables.auth.PasswordResetTokensTable
import tech.dokus.database.tables.auth.RefreshTokensTable
import tech.dokus.database.tables.auth.TenantInvitationsTable
import tech.dokus.database.tables.auth.TenantMembersTable
import tech.dokus.database.tables.auth.TenantSettingsTable
import tech.dokus.database.tables.auth.TenantTable
import tech.dokus.database.tables.auth.UsersTable
import tech.dokus.database.tables.auth.WelcomeEmailJobsTable
import tech.dokus.database.tables.banking.BankAccountsTable
import tech.dokus.database.tables.banking.BankStatementsTable
import tech.dokus.database.tables.banking.BankTransactionsTable
import tech.dokus.database.tables.business.BusinessProfileEnrichmentJobsTable
import tech.dokus.database.tables.business.BusinessProfilesTable
import tech.dokus.database.tables.cashflow.CashflowEntriesTable
import tech.dokus.database.tables.cashflow.CreditNotesTable
import tech.dokus.database.tables.cashflow.ExpensesTable
import tech.dokus.database.tables.cashflow.InvoiceItemsTable
import tech.dokus.database.tables.cashflow.InvoiceNumberSequencesTable
import tech.dokus.database.tables.cashflow.InvoicesTable
import tech.dokus.database.tables.cashflow.RefundClaimsTable
import tech.dokus.database.tables.contacts.ContactAddressesTable
import tech.dokus.database.tables.contacts.ContactNotesTable
import tech.dokus.database.tables.contacts.ContactsTable
import tech.dokus.database.tables.documents.DocumentIngestionRunsTable
import tech.dokus.database.tables.documents.DocumentBlobsTable
import tech.dokus.database.tables.documents.DocumentMatchReviewsTable
import tech.dokus.database.tables.documents.DocumentPurposeExamplesTable
import tech.dokus.database.tables.documents.DocumentPurposeTemplatesTable
import tech.dokus.database.tables.documents.DocumentSourcesTable
import tech.dokus.database.tables.documents.DocumentLinksTable
import tech.dokus.database.tables.documents.DocumentsTable
import tech.dokus.database.tables.banking.MatchPatternsTable
import tech.dokus.database.tables.banking.RejectedMatchPairsTable
import tech.dokus.database.tables.documents.TransactionMatchLinksTable
import tech.dokus.database.tables.documents.AutoPaymentAuditEventsTable
import tech.dokus.database.tables.notifications.NotificationPreferencesTable
import tech.dokus.database.tables.notifications.NotificationsTable
import tech.dokus.database.tables.payment.PaymentsTable
import tech.dokus.database.tables.peppol.PeppolDirectoryCacheTable
import tech.dokus.database.tables.peppol.PeppolRegistrationTable
import tech.dokus.database.tables.peppol.PeppolSettingsTable
import tech.dokus.database.tables.peppol.PeppolTransmissionsTable
import tech.dokus.database.tables.search.SearchSignalStatsTable
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
                FirmsTable,
                TenantSettingsTable,
                UsersTable,
                WelcomeEmailJobsTable,
                TenantMembersTable,
                FirmMembersTable,
                FirmAccessTable,
                TenantInvitationsTable,
                RefreshTokensTable,
                PasswordResetTokensTable,
                AddressTable,
                NotificationsTable,
                NotificationPreferencesTable,

                // ----------------------------
                // Cashflow foundation (docs)
                // ----------------------------
                DocumentsTable,
                DocumentBlobsTable,
                DocumentSourcesTable,
                DocumentIngestionRunsTable,
                DocumentMatchReviewsTable,
                DocumentPurposeTemplatesTable,
                DocumentPurposeExamplesTable,
                BankTransactionsTable,
                TransactionMatchLinksTable,
                MatchPatternsTable,
                RejectedMatchPairsTable,
                AutoPaymentAuditEventsTable,
                DocumentLinksTable,

                // ----------------------------
                // Contacts (depends on docs/users/addresses)
                // ----------------------------
                ContactsTable,
                ContactAddressesTable,  // Join table: contacts -> addresses
                ContactNotesTable,
                BusinessProfilesTable,
                BusinessProfileEnrichmentJobsTable,

                // ----------------------------
                // Cashflow (depends on contacts)
                // ----------------------------
                InvoicesTable,
                InvoiceItemsTable,
                InvoiceNumberSequencesTable,
                ExpensesTable,
                CreditNotesTable,
                RefundClaimsTable,
                CashflowEntriesTable,

                // ----------------------------
                // Payments / Banking
                // ----------------------------
                PaymentsTable,
                BankAccountsTable,
                BankStatementsTable,

                // ----------------------------
                // Search telemetry
                // ----------------------------
                SearchSignalStatsTable,

                // ----------------------------
                // Peppol (depends on invoices)
                // ----------------------------
                PeppolRegistrationTable,
                PeppolSettingsTable,
                PeppolTransmissionsTable,
                PeppolDirectoryCacheTable,

                // ----------------------------
                // AI / RAG (depends on users/docs)
                // ----------------------------
                DocumentChunksTable,
                ChatMessagesTable,
                DocumentExamplesTable,
            )
        }

        // Partial unique index for bank transaction dedup
        dbQuery {
            val tx = org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager.current()
            tx.exec(
                """
                CREATE UNIQUE INDEX IF NOT EXISTS uq_bank_txn_dedup
                    ON bank_transactions (tenant_id, bank_account_id, dedup_hash)
                    WHERE bank_account_id IS NOT NULL
                """.trimIndent()
            )
        }

        logger.info("Database schema initialized successfully")
    }
}
