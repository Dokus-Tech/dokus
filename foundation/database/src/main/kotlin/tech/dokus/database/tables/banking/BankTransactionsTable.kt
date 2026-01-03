package tech.dokus.database.tables.banking

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.datetime.CurrentDateTime
import org.jetbrains.exposed.v1.datetime.date
import org.jetbrains.exposed.v1.datetime.datetime
import tech.dokus.database.tables.auth.TenantTable
import tech.dokus.database.tables.cashflow.ExpensesTable
import tech.dokus.database.tables.cashflow.InvoicesTable

/**
 * Bank transactions synced from bank connections
 * Links to expenses or invoices for reconciliation
 */
object BankTransactionsTable : UUIDTable("bank_transactions") {
    val bankConnectionId = uuid("bank_connection_id").references(
        BankConnectionsTable.id,
        onDelete = ReferenceOption.CASCADE
    )
    val tenantId = uuid("tenant_id").references(
        TenantTable.id,
        onDelete = ReferenceOption.CASCADE
    )

    val externalId = varchar("external_id", 255)
    val date = date("date")
    val amount = decimal("amount", 12, 2)
    val description = text("description")
    val merchantName = varchar("merchant_name", 255).nullable()
    val category = varchar("category", 100).nullable()

    val isPending = bool("is_pending").default(false)

    // Reconciliation
    val expenseId = uuid("expense_id").references(
        ExpensesTable.id,
        onDelete = ReferenceOption.SET_NULL
    ).nullable()
    val invoiceId = uuid("invoice_id").references(
        InvoicesTable.id,
        onDelete = ReferenceOption.SET_NULL
    ).nullable()
    val isReconciled = bool("is_reconciled").default(false)

    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)

    init {
        index(false, tenantId, date)
        index(false, bankConnectionId, date)
        index(false, tenantId, isReconciled)
        uniqueIndex(bankConnectionId, externalId)
    }
}
