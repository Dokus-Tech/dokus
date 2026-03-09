package tech.dokus.database.tables.banking

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.datetime.CurrentDateTime
import org.jetbrains.exposed.v1.datetime.date
import org.jetbrains.exposed.v1.datetime.datetime
import tech.dokus.database.tables.auth.TenantTable
import tech.dokus.database.tables.cashflow.CashflowEntriesTable
import tech.dokus.database.tables.documents.DocumentsTable
import tech.dokus.domain.enums.BankTransactionSource
import tech.dokus.domain.enums.BankTransactionStatus
import tech.dokus.domain.enums.Currency
import tech.dokus.domain.enums.PaymentCandidateTier
import tech.dokus.foundation.backend.database.dbEnumeration

private const val HashLength = 64
private const val FingerprintLength = 64
private const val IbanLength = 34
private const val NameLength = 255
private const val ExternalIdLength = 255
private const val StructuredCommLength = 64
private const val NormalizedStructuredCommLength = 32

/**
 * Unified bank transactions table.
 *
 * Merges the former `imported_bank_transactions` (file imports) and `bank_transactions` (live sync)
 * into a single table. The `source` column distinguishes the origin.
 */
object BankTransactionsTable : UUIDTable("bank_transactions") {
    val tenantId = uuid("tenant_id").references(
        TenantTable.id,
        onDelete = ReferenceOption.CASCADE
    )

    // --- Source identification ---
    val txSource = dbEnumeration<BankTransactionSource>("source").default(BankTransactionSource.BankImport)

    // File import fields (nullable for live sync)
    val documentId = uuid("document_id").references(
        DocumentsTable.id,
        onDelete = ReferenceOption.CASCADE
    ).nullable()
    val rowHash = varchar("row_hash", HashLength).nullable()

    // Live sync fields (nullable for file imports)
    val bankConnectionId = uuid("bank_connection_id").references(
        BankConnectionsTable.id,
        onDelete = ReferenceOption.SET_NULL
    ).nullable()
    val externalId = varchar("external_id", ExternalIdLength).nullable()

    // --- Core transaction data ---
    val transactionFingerprint = varchar("transaction_fingerprint", FingerprintLength)
    val transactionDate = date("transaction_date")
    val signedAmount = decimal("signed_amount", 12, 2)
    val currency = dbEnumeration<Currency>("currency").default(Currency.Eur)
    val isPending = bool("is_pending").default(false)

    // --- Counterparty ---
    val counterpartyName = varchar("counterparty_name", NameLength).nullable()
    val counterpartyIban = varchar("counterparty_iban", IbanLength).nullable()

    // --- Communication / description ---
    val structuredCommunicationRaw = varchar("structured_communication_raw", StructuredCommLength).nullable()
    val normalizedStructuredCommunication =
        varchar("normalized_structured_communication", NormalizedStructuredCommLength).nullable()
    val descriptionRaw = text("description_raw").nullable()

    // --- Quality signals ---
    val rowConfidence = decimal("row_confidence", 5, 4).nullable()
    val largeAmountFlag = bool("large_amount_flag").default(false)

    // --- Matching lifecycle ---
    val status = dbEnumeration<BankTransactionStatus>("status").default(BankTransactionStatus.Unmatched)
    val linkedCashflowEntryId = uuid("linked_cashflow_entry_id")
        .references(CashflowEntriesTable.id, onDelete = ReferenceOption.SET_NULL)
        .nullable()
    val suggestedCashflowEntryId = uuid("suggested_cashflow_entry_id")
        .references(CashflowEntriesTable.id, onDelete = ReferenceOption.SET_NULL)
        .nullable()
    val suggestedScore = decimal("suggested_score", 5, 4).nullable()
    val suggestedTier = dbEnumeration<PaymentCandidateTier>("suggested_tier").nullable()

    // --- Timestamps ---
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    init {
        // Regular indexes
        index(false, tenantId, status, transactionDate)
        index(false, tenantId, suggestedCashflowEntryId)
        index(false, tenantId, linkedCashflowEntryId)
        index(false, tenantId, normalizedStructuredCommunication)
        index(false, tenantId, transactionFingerprint)
        index(false, tenantId, bankConnectionId, transactionDate)
        index(false, tenantId, txSource)
    }
}
