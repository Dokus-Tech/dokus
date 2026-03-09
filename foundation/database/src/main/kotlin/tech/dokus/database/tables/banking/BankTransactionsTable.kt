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
import tech.dokus.domain.enums.IgnoredReason
import tech.dokus.domain.enums.MatchedBy
import tech.dokus.domain.enums.ResolutionType
import tech.dokus.domain.enums.StatementTrust
import tech.dokus.foundation.backend.database.dbEnumeration

private const val DedupHashLength = 64
private const val IbanLength = 34
private const val BicLength = 11
private const val NameLength = 255
private const val StructuredCommLength = 64
private const val NormalizedStructuredCommLength = 32

/**
 * Unified bank transactions table.
 *
 * Each row represents a single bank transaction imported from a statement
 * (PDF, CODA, MT940) or live-sync provider (Plaid, Tink).
 */
object BankTransactionsTable : UUIDTable("bank_transactions") {
    val tenantId = uuid("tenant_id").references(
        TenantTable.id,
        onDelete = ReferenceOption.CASCADE
    )

    // --- Source identification ---
    val txSource = dbEnumeration<BankTransactionSource>("source").default(BankTransactionSource.PdfStatement)

    val documentId = uuid("document_id").references(
        DocumentsTable.id,
        onDelete = ReferenceOption.CASCADE
    ).nullable()

    val bankAccountId = uuid("bank_account_id").references(
        BankAccountsTable.id,
        onDelete = ReferenceOption.SET_NULL
    ).nullable()

    // --- Dedup ---
    val dedupHash = varchar("dedup_hash", DedupHashLength)

    // --- Core transaction data ---
    val transactionDate = date("transaction_date")
    val valueDate = date("value_date").nullable()
    val signedAmount = decimal("signed_amount", 12, 2)
    val currency = dbEnumeration<Currency>("currency").default(Currency.Eur)

    // --- Counterparty ---
    val counterpartyName = varchar("counterparty_name", NameLength).nullable()
    val counterpartyIban = varchar("counterparty_iban", IbanLength).nullable()
    val counterpartyBic = varchar("counterparty_bic", BicLength).nullable()

    // --- Communication / description ---
    val structuredCommunicationRaw = varchar("structured_communication_raw", StructuredCommLength).nullable()
    val normalizedStructuredCommunication =
        varchar("normalized_structured_communication", NormalizedStructuredCommLength).nullable()
    val freeCommunication = text("free_communication").nullable()
    val descriptionRaw = text("description_raw").nullable()

    // --- Matching lifecycle ---
    val status = dbEnumeration<BankTransactionStatus>("status").default(BankTransactionStatus.Unmatched)
    val resolutionType = dbEnumeration<ResolutionType>("resolution_type").nullable()
    val matchedCashflowId = uuid("matched_cashflow_id")
        .references(CashflowEntriesTable.id, onDelete = ReferenceOption.SET_NULL)
        .nullable()
    val matchedDocumentId = uuid("matched_document_id")
        .references(DocumentsTable.id, onDelete = ReferenceOption.SET_NULL)
        .nullable()
    val matchScore = decimal("match_score", 5, 4).nullable()
    val matchEvidence = text("match_evidence").nullable() // JSON array of strings
    val matchedBy = dbEnumeration<MatchedBy>("matched_by").nullable()
    val matchedAt = datetime("matched_at").nullable()

    // --- Ignore metadata ---
    val ignoredReason = dbEnumeration<IgnoredReason>("ignored_reason").nullable()
    val ignoredAt = datetime("ignored_at").nullable()
    val ignoredBy = varchar("ignored_by", NameLength).nullable()

    // --- Trust & transfer ---
    val statementTrust = dbEnumeration<StatementTrust>("statement_trust").default(StatementTrust.Low)
    val transferPairId = uuid("transfer_pair_id").nullable()

    // --- Timestamps ---
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    init {
        index(false, tenantId, status, transactionDate)
        index(false, tenantId, matchedCashflowId)
        index(false, tenantId, normalizedStructuredCommunication)
        index(false, tenantId, dedupHash)
        index(false, tenantId, bankAccountId, transactionDate)
        index(false, tenantId, txSource)
    }
}
