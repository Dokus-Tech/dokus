package tech.dokus.database.tables.documents

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.datetime.CurrentDateTime
import org.jetbrains.exposed.v1.datetime.datetime
import tech.dokus.database.tables.auth.TenantTable
import tech.dokus.database.tables.cashflow.CashflowEntriesTable
import tech.dokus.database.tables.cashflow.InvoicesTable
import tech.dokus.domain.enums.AutoMatchStatus
import tech.dokus.domain.enums.PaymentCreatedBy
import tech.dokus.domain.enums.CashflowEntryStatus
import tech.dokus.domain.enums.InvoiceStatus
import tech.dokus.foundation.backend.database.dbEnumeration

object InvoiceBankMatchLinksTable : UUIDTable("invoice_bank_match_links") {
    val tenantId = uuid("tenant_id").references(TenantTable.id, onDelete = ReferenceOption.CASCADE)
    val invoiceId = uuid("invoice_id").references(InvoicesTable.id, onDelete = ReferenceOption.CASCADE)
    val cashflowEntryId = uuid("cashflow_entry_id").references(CashflowEntriesTable.id, onDelete = ReferenceOption.CASCADE)
    val importedBankTransactionId = uuid("imported_bank_transaction_id")
        .references(ImportedBankTransactionsTable.id, onDelete = ReferenceOption.CASCADE)

    val status = dbEnumeration<AutoMatchStatus>("status")
    val createdBy = dbEnumeration<PaymentCreatedBy>("created_by").default(PaymentCreatedBy.Auto)
    val confidenceScore = decimal("confidence_score", 5, 4).nullable()
    val scoreMargin = decimal("score_margin", 5, 4).nullable()
    val reasonsJson = text("reasons_json").nullable()
    val rulesJson = text("rules_json").nullable()

    val matchedAt = datetime("matched_at")
    val autoPaidAt = datetime("autopaid_at").nullable()
    val reversedAt = datetime("reversed_at").nullable()
    val reversedByUserId = uuid("reversed_by_user_id").nullable()
    val reversalReason = text("reversal_reason").nullable()

    val invoiceStatusBefore = dbEnumeration<InvoiceStatus>("invoice_status_before").nullable()
    val invoicePaidAmountBefore = decimal("invoice_paid_amount_before", 12, 2).nullable()
    val invoicePaidAtBefore = datetime("invoice_paid_at_before").nullable()
    val cashflowStatusBefore = dbEnumeration<CashflowEntryStatus>("cashflow_status_before").nullable()
    val cashflowRemainingBefore = decimal("cashflow_remaining_before", 12, 2).nullable()
    val cashflowPaidAtBefore = datetime("cashflow_paid_at_before").nullable()

    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    init {
        uniqueIndex("ux_invoice_bank_match_links_pair", tenantId, invoiceId, importedBankTransactionId)
        index(false, tenantId, cashflowEntryId)
        index(false, tenantId, status)
        index(false, tenantId, importedBankTransactionId)
    }
}
