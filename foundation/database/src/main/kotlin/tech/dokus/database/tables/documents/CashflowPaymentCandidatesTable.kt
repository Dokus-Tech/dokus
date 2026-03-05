package tech.dokus.database.tables.documents

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.datetime.CurrentDateTime
import org.jetbrains.exposed.v1.datetime.datetime
import tech.dokus.database.tables.auth.TenantTable
import tech.dokus.database.tables.cashflow.CashflowEntriesTable
import tech.dokus.domain.enums.PaymentCandidateTier
import tech.dokus.foundation.backend.database.dbEnumeration

object CashflowPaymentCandidatesTable : UUIDTable("cashflow_payment_candidates") {
    val tenantId = uuid("tenant_id").references(
        TenantTable.id,
        onDelete = ReferenceOption.CASCADE
    )
    val cashflowEntryId = uuid("cashflow_entry_id").references(
        CashflowEntriesTable.id,
        onDelete = ReferenceOption.CASCADE
    )
    val importedBankTransactionId = uuid("imported_bank_transaction_id").references(
        ImportedBankTransactionsTable.id,
        onDelete = ReferenceOption.CASCADE
    )
    val score = decimal("score", 5, 4)
    val tier = dbEnumeration<PaymentCandidateTier>("tier")
    val signalSnapshotJson = text("signal_snapshot_json").nullable()
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    init {
        uniqueIndex(tenantId, cashflowEntryId)
        index(false, tenantId, importedBankTransactionId)
        index(false, tenantId, tier)
    }
}
