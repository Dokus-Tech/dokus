package ai.dokus.foundation.database.tables

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.kotlin.datetime.CurrentDateTime
import org.jetbrains.exposed.sql.kotlin.datetime.date
import org.jetbrains.exposed.sql.kotlin.datetime.datetime

/**
 * Imported bank transactions
 * Reconcile with invoices and expenses
 */
object BankTransactionsTable : UUIDTable("bank_transactions") {
    val bankConnectionId = reference("bank_connection_id", BankConnectionsTable,
        onDelete = ReferenceOption.CASCADE)
    val tenantId = reference("tenant_id", TenantsTable, onDelete = ReferenceOption.CASCADE)

    val externalId = varchar("external_id", 255).uniqueIndex()  // Bank's ID
    val date = date("date")
    val amount = decimal("amount", 12, 2)
    val description = varchar("description", 500)
    val merchantName = varchar("merchant_name", 255).nullable()
    val category = varchar("category", 100).nullable()
    val isPending = bool("is_pending").default(false)

    // Reconciliation
    val expenseId = reference("expense_id", ExpensesTable,
        onDelete = ReferenceOption.SET_NULL).nullable()
    val invoiceId = reference("invoice_id", InvoicesTable,
        onDelete = ReferenceOption.SET_NULL).nullable()
    val isReconciled = bool("is_reconciled").default(false)

    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)

    init {
        index(false, tenantId)
        index(false, date)
        index(false, isReconciled)
        index(false, tenantId, isReconciled)
        index(false, tenantId, date)
    }
}