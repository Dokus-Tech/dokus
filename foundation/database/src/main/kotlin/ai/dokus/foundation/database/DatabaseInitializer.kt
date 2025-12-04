package ai.dokus.foundation.database

import ai.dokus.foundation.database.tables.audit.AuditLogsTable
import ai.dokus.foundation.database.tables.auth.AddressTable
import ai.dokus.foundation.database.tables.auth.PasswordResetTokensTable
import ai.dokus.foundation.database.tables.auth.RefreshTokensTable
import ai.dokus.foundation.database.tables.auth.TenantMembersTable
import ai.dokus.foundation.database.tables.auth.TenantSettingsTable
import ai.dokus.foundation.database.tables.auth.TenantTable
import ai.dokus.foundation.database.tables.auth.UsersTable
import ai.dokus.foundation.database.tables.banking.BankConnectionsTable
import ai.dokus.foundation.database.tables.banking.BankTransactionsTable
import ai.dokus.foundation.database.tables.cashflow.AttachmentsTable
import ai.dokus.foundation.database.tables.cashflow.BillsTable
import ai.dokus.foundation.database.tables.cashflow.DocumentProcessingTable
import ai.dokus.foundation.database.tables.cashflow.DocumentsTable
import ai.dokus.foundation.database.tables.cashflow.ExpensesTable
import ai.dokus.foundation.database.tables.cashflow.InvoiceItemsTable
import ai.dokus.foundation.database.tables.cashflow.InvoicesTable
import ai.dokus.foundation.database.tables.media.MediaTable
import ai.dokus.foundation.database.tables.payment.PaymentsTable
import ai.dokus.foundation.database.tables.peppol.PeppolSettingsTable
import ai.dokus.foundation.database.tables.peppol.PeppolTransmissionsTable
import ai.dokus.foundation.database.tables.reporting.VatReturnsTable
import ai.dokus.foundation.ktor.database.dbQuery
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Centralized database initializer for all Dokus tables.
 *
 * This initializer creates all tables in the correct order, respecting
 * foreign key dependencies. It uses an atomic flag to ensure tables
 * are only created once, even with concurrent startup of multiple services.
 *
 * Table creation is idempotent - SchemaUtils.create() only creates tables
 * if they don't already exist.
 *
 * Usage:
 * ```kotlin
 * // In your service's DI configuration, after connecting to the database:
 * runBlocking {
 *     DatabaseInitializer.initializeAllTables()
 * }
 * ```
 */
object DatabaseInitializer {
    private val logger = LoggerFactory.getLogger(DatabaseInitializer::class.java)
    private val initialized = AtomicBoolean(false)

    /**
     * Initialize all database tables.
     *
     * Tables are created in dependency order:
     * 1. Auth tables (tenants, users, memberships, tokens)
     * 2. Cashflow tables (documents, invoices, expenses, bills)
     * 3. Other domain tables (payments, reports, audit, banking, peppol, media)
     *
     * This method is safe to call multiple times - it will only execute once
     * per JVM instance.
     */
    suspend fun initializeAllTables() {
        if (initialized.compareAndSet(false, true)) {
            logger.info("Initializing all database tables...")

            dbQuery {
                SchemaUtils.create(
                    // ===== Auth Tables (create first - other tables depend on these) =====
                    TenantTable,
                    TenantSettingsTable,  // depends on TenantTable
                    UsersTable,
                    TenantMembersTable,   // depends on TenantTable, UsersTable
                    RefreshTokensTable,   // depends on UsersTable
                    PasswordResetTokensTable, // depends on UsersTable
                    AddressTable,         // depends on TenantTable

                    // ===== Cashflow Tables (create in dependency order) =====
                    DocumentsTable,       // base document table
                    DocumentProcessingTable, // depends on DocumentsTable
                    InvoicesTable,        // depends on TenantTable, DocumentsTable
                    InvoiceItemsTable,    // depends on InvoicesTable
                    ExpensesTable,        // depends on TenantTable, DocumentsTable
                    AttachmentsTable,     // depends on TenantTable
                    BillsTable,           // depends on TenantTable, DocumentsTable

                    // ===== Payment Tables =====
                    PaymentsTable,        // depends on TenantTable, InvoicesTable

                    // ===== Reporting Tables =====
                    VatReturnsTable,      // depends on TenantTable

                    // ===== Audit Tables =====
                    AuditLogsTable,       // depends on TenantTable

                    // ===== Banking Tables =====
                    BankConnectionsTable, // depends on TenantTable
                    BankTransactionsTable, // depends on TenantTable, BankConnectionsTable

                    // ===== Peppol Tables =====
                    PeppolSettingsTable,  // depends on TenantTable
                    PeppolTransmissionsTable, // depends on TenantTable, InvoicesTable, BillsTable

                    // ===== Media Tables =====
                    MediaTable            // depends on TenantTable
                )
            }

            logger.info("All database tables initialized successfully")
        } else {
            logger.debug("Database tables already initialized, skipping...")
        }
    }

    /**
     * Reset the initialization flag.
     *
     * This is primarily for testing purposes - allows re-initialization
     * of tables in test scenarios.
     */
    fun reset() {
        initialized.set(false)
        logger.debug("DatabaseInitializer reset")
    }
}
