package tech.dokus.database.tables.documents

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.datetime.CurrentDateTime
import org.jetbrains.exposed.v1.datetime.date
import org.jetbrains.exposed.v1.datetime.datetime
import tech.dokus.database.tables.auth.TenantTable
import tech.dokus.database.tables.cashflow.CashflowEntriesTable
import tech.dokus.domain.enums.ImportedBankTransactionStatus
import tech.dokus.domain.enums.PaymentCandidateTier
import tech.dokus.foundation.backend.database.dbEnumeration

private const val HashLength = 64
private const val FingerprintLength = 64
private const val IbanLength = 34
private const val NameLength = 255
private const val StructuredCommLength = 64
private const val NormalizedStructuredCommLength = 32

object ImportedBankTransactionsTable : UUIDTable("imported_bank_transactions") {
    val tenantId = uuid("tenant_id").references(
        TenantTable.id,
        onDelete = ReferenceOption.CASCADE
    )

    val documentId = uuid("document_id").references(
        DocumentsTable.id,
        onDelete = ReferenceOption.CASCADE
    )

    val rowHash = varchar("row_hash", HashLength)
    val transactionFingerprint = varchar("transaction_fingerprint", FingerprintLength)
    val transactionDate = date("transaction_date")
    val signedAmount = decimal("signed_amount", 12, 2)
    val counterpartyName = varchar("counterparty_name", NameLength).nullable()
    val counterpartyIban = varchar("counterparty_iban", IbanLength).nullable()
    val structuredCommunicationRaw = varchar("structured_communication_raw", StructuredCommLength).nullable()
    val normalizedStructuredCommunication =
        varchar("normalized_structured_communication", NormalizedStructuredCommLength).nullable()
    val descriptionRaw = text("description_raw").nullable()
    val rowConfidence = decimal("row_confidence", 5, 4).nullable()
    val largeAmountFlag = bool("large_amount_flag").default(false)
    val status = dbEnumeration<ImportedBankTransactionStatus>("status").default(ImportedBankTransactionStatus.Unmatched)
    val linkedCashflowEntryId = uuid("linked_cashflow_entry_id")
        .references(CashflowEntriesTable.id, onDelete = ReferenceOption.SET_NULL)
        .nullable()
    val suggestedCashflowEntryId = uuid("suggested_cashflow_entry_id")
        .references(CashflowEntriesTable.id, onDelete = ReferenceOption.SET_NULL)
        .nullable()
    val suggestedScore = decimal("suggested_score", 5, 4).nullable()
    val suggestedTier = dbEnumeration<PaymentCandidateTier>("suggested_tier").nullable()
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    init {
        uniqueIndex(tenantId, documentId, rowHash)
        index(false, tenantId, status, transactionDate)
        index(false, tenantId, suggestedCashflowEntryId)
        index(false, tenantId, linkedCashflowEntryId)
        index(false, tenantId, normalizedStructuredCommunication)
        index(false, tenantId, transactionFingerprint)
    }
}
