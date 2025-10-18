package ai.dokus.foundation.database

import ai.dokus.foundation.database.tables.*
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * Test database factory for H2 in-memory database
 * Provides isolated database instances for unit tests
 */
object TestDatabaseFactory {
    private var database: Database? = null

    /**
     * Initialize H2 in-memory database with all tables
     */
    fun init() {
        if (database == null) {
            database = Database.connect(
                url = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
                driver = "org.h2.Driver",
                user = "sa",
                password = ""
            )

            transaction(database) {
                // Create all tables
                SchemaUtils.create(
                    TenantsTable,
                    TenantSettingsTable,
                    BusinessUsersTable,
                    ClientsTable,
                    InvoicesTable,
                    InvoiceItemsTable,
                    ExpensesTable,
                    PaymentsTable,
                    BankConnectionsTable,
                    BankTransactionsTable,
                    VatReturnsTable,
                    AuditLogsTable,
                    AttachmentsTable
                )
            }
        }
    }

    /**
     * Clean all data from tables (but keep schema)
     */
    fun clean() {
        transaction(database) {
            // Drop and recreate tables to ensure clean state
            SchemaUtils.drop(
                AttachmentsTable,
                AuditLogsTable,
                VatReturnsTable,
                BankTransactionsTable,
                BankConnectionsTable,
                PaymentsTable,
                ExpensesTable,
                InvoiceItemsTable,
                InvoicesTable,
                ClientsTable,
                BusinessUsersTable,
                TenantSettingsTable,
                TenantsTable
            )

            SchemaUtils.create(
                TenantsTable,
                TenantSettingsTable,
                BusinessUsersTable,
                ClientsTable,
                InvoicesTable,
                InvoiceItemsTable,
                ExpensesTable,
                PaymentsTable,
                BankConnectionsTable,
                BankTransactionsTable,
                VatReturnsTable,
                AuditLogsTable,
                AttachmentsTable
            )
        }
    }

    /**
     * Execute database query in test transaction
     */
    suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO, database) { block() }

    /**
     * Close database connection
     */
    fun close() {
        database = null
    }
}
