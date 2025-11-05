package ai.dokus.banking.backend.database.tables

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.kotlin.datetime.CurrentDateTime
import org.jetbrains.exposed.sql.kotlin.datetime.date
import org.jetbrains.exposed.sql.kotlin.datetime.datetime
import java.util.UUID as JavaUUID

/**
 * Bank transactions synced from bank connections
 * Links to expenses or invoices for reconciliation
 */
object BankTransactionsTable : UUIDTable("bank_transactions") {
    val bankConnectionId = uuid("bank_connection_id")
    val tenantId = uuid("tenant_id")

    val externalId = varchar("external_id", 255)
    val date = date("date")
    val amount = decimal("amount", 12, 2)
    val description = text("description")
    val merchantName = varchar("merchant_name", 255).nullable()
    val category = varchar("category", 100).nullable()

    val isPending = bool("is_pending").default(false)

    // Reconciliation
    val expenseId = uuid("expense_id").nullable()
    val invoiceId = uuid("invoice_id").nullable()
    val isReconciled = bool("is_reconciled").default(false)

    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)

    init {
        index(false, tenantId, date)
        index(false, bankConnectionId, date)
        index(false, tenantId, isReconciled)
        uniqueIndex(bankConnectionId, externalId)
    }
}
