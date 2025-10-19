package ai.dokus.foundation.database

import ai.dokus.foundation.database.tables.*
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * Test Database Factory using H2 in-memory database
 * Provides database setup and cleanup utilities for integration tests
 */
object TestDatabaseFactory {
    private var database: Database? = null
    private var dataSource: HikariDataSource? = null

    /**
     * Initialize H2 in-memory database with all tables
     */
    fun init() {
        if (database != null) return

        val config = HikariConfig().apply {
            driverClassName = "org.h2.Driver"
            jdbcUrl = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL"
            username = "sa"
            password = ""
            maximumPoolSize = 3
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            validate()
        }

        dataSource = HikariDataSource(config)
        database = Database.connect(dataSource!!)

        // Create all tables (order matters for foreign keys)
        transaction {
            SchemaUtils.create(
                TenantsTable,
                TenantSettingsTable,
                UsersTable,
                RefreshTokensTable,
                ClientsTable,
                InvoicesTable,
                InvoiceItemsTable,
                ExpensesTable,
                PaymentsTable,
                AttachmentsTable,
                AuditLogsTable,
                VatReturnsTable,
                BankConnectionsTable,
                BankTransactionsTable
            )
        }
    }

    /**
     * Clean all tables (but keep schema)
     */
    fun clean() {
        transaction {
            // Drop in reverse order of creation
            SchemaUtils.drop(
                BankTransactionsTable,
                BankConnectionsTable,
                VatReturnsTable,
                AuditLogsTable,
                AttachmentsTable,
                PaymentsTable,
                ExpensesTable,
                InvoiceItemsTable,
                InvoicesTable,
                ClientsTable,
                RefreshTokensTable,
                UsersTable,
                TenantSettingsTable,
                TenantsTable
            )
            // Recreate in proper order
            SchemaUtils.create(
                TenantsTable,
                TenantSettingsTable,
                UsersTable,
                RefreshTokensTable,
                ClientsTable,
                InvoicesTable,
                InvoiceItemsTable,
                ExpensesTable,
                PaymentsTable,
                AttachmentsTable,
                AuditLogsTable,
                VatReturnsTable,
                BankConnectionsTable,
                BankTransactionsTable
            )
        }
    }

    /**
     * Close database connection
     */
    fun close() {
        dataSource?.close()
        database = null
        dataSource = null
    }

    /**
     * Execute a database query in a transaction
     */
    suspend fun <T> dbQuery(block: suspend (Transaction) -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block(this) }
}
