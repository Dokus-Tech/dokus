package ai.dokus.foundation.database.tables.banking

import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.datetime.CurrentDateTime
import org.jetbrains.exposed.v1.datetime.date
import org.jetbrains.exposed.v1.datetime.datetime

/**
 * Bank transactions synced from bank connections
 * Links to expenses or invoices for reconciliation
 */
object BankTransactionsTable : UUIDTable("bank_transactions") {
    val bankConnectionId = reference(
        name = "bank_connection_id",
        foreign = BankConnectionsTable,
        onDelete = org.jetbrains.exposed.v1.core.ReferenceOption.CASCADE
    )
    val tenantId = reference(
        name = "tenant_id",
        foreign = ai.dokus.foundation.database.tables.auth.TenantTable,
        onDelete = org.jetbrains.exposed.v1.core.ReferenceOption.CASCADE
    )

    val externalId = varchar("external_id", 255)
    val date = date("date")
    val amount = decimal("amount", 12, 2)
    val description = text("description")
    val merchantName = varchar("merchant_name", 255).nullable()
    val category = varchar("category", 100).nullable()

    val isPending = bool("is_pending").default(false)

    // Reconciliation
    val expenseId = reference(
        name = "expense_id",
        foreign = ai.dokus.foundation.database.tables.cashflow.ExpensesTable,
        onDelete = org.jetbrains.exposed.v1.core.ReferenceOption.SET_NULL
    ).nullable()
    val invoiceId = reference(
        name = "invoice_id",
        foreign = ai.dokus.foundation.database.tables.cashflow.InvoicesTable,
        onDelete = org.jetbrains.exposed.v1.core.ReferenceOption.SET_NULL
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
