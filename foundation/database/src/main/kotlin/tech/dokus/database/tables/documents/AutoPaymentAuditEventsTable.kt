package tech.dokus.database.tables.documents

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.datetime.CurrentDateTime
import org.jetbrains.exposed.v1.datetime.datetime
import tech.dokus.database.tables.auth.TenantTable
import tech.dokus.database.tables.cashflow.CashflowEntriesTable
import tech.dokus.database.tables.cashflow.InvoicesTable
import tech.dokus.domain.enums.AutoPaymentDecision
import tech.dokus.domain.enums.AutoPaymentTriggerSource
import tech.dokus.foundation.backend.database.dbEnumeration

object AutoPaymentAuditEventsTable : UUIDTable("auto_payment_audit_events") {
    val tenantId = uuid("tenant_id").references(TenantTable.id, onDelete = ReferenceOption.CASCADE)
    val invoiceId = uuid("invoice_id").references(InvoicesTable.id, onDelete = ReferenceOption.SET_NULL).nullable()
    val cashflowEntryId = uuid("cashflow_entry_id").references(CashflowEntriesTable.id, onDelete = ReferenceOption.SET_NULL).nullable()
    val importedBankTransactionId = uuid("imported_bank_transaction_id")
        .references(ImportedBankTransactionsTable.id, onDelete = ReferenceOption.SET_NULL)
        .nullable()

    val triggerSource = dbEnumeration<AutoPaymentTriggerSource>("trigger_source")
    val decision = dbEnumeration<AutoPaymentDecision>("decision")
    val score = decimal("score", 5, 4).nullable()
    val margin = decimal("margin", 5, 4).nullable()
    val reasonsJson = text("reasons_json").nullable()
    val rulesJson = text("rules_json").nullable()
    val actorUserId = uuid("actor_user_id").nullable()

    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)

    init {
        index(false, tenantId, createdAt)
        index(false, tenantId, invoiceId)
        index(false, tenantId, importedBankTransactionId)
        index(false, tenantId, cashflowEntryId)
    }
}
