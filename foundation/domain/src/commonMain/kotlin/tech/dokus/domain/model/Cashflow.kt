package tech.dokus.domain.model

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import tech.dokus.domain.Money
import tech.dokus.domain.enums.CashflowDirection
import tech.dokus.domain.enums.CashflowEntryStatus
import tech.dokus.domain.enums.CashflowSourceType
import tech.dokus.domain.enums.Currency
import tech.dokus.domain.enums.AutoMatchStatus
import tech.dokus.domain.enums.BankTransactionSource
import tech.dokus.domain.enums.BankTransactionStatus
import tech.dokus.domain.enums.IgnoredReason
import tech.dokus.domain.enums.MatchedBy
import tech.dokus.domain.enums.PaymentMethod
import tech.dokus.domain.enums.ResolutionType
import tech.dokus.domain.enums.StatementTrust
import tech.dokus.domain.ids.BankAccountId
import tech.dokus.domain.ids.BankTransactionId
import tech.dokus.domain.ids.CashflowEntryId
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.PaymentId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.contact.CounterpartySnapshot

/**
 * Cashflow overview data with Cash-In / Cash-Out structure.
 *
 * Cash-In: Money flowing INTO the business (outgoing invoices to clients)
 * Cash-Out: Money flowing OUT of the business (expenses, inbound invoices to pay)
 */
@Serializable
data class CashflowOverview(
    val period: CashflowPeriod,
    val cashIn: CashInSummary,
    val cashOut: CashOutSummary,
    val netCashflow: Money,
    val currency: Currency = Currency.Eur
)

@Serializable
data class CashflowPeriod(
    val from: LocalDate,
    val to: LocalDate
)

/**
 * Summary of Cash-In (money coming in from invoices).
 */
@Serializable
data class CashInSummary(
    val total: Money,
    val paid: Money,
    val pending: Money,
    val overdue: Money,
    val invoiceCount: Int
)

/**
 * Summary of Cash-Out (money going out for expenses and inbound invoices).
 */
@Serializable
data class CashOutSummary(
    val total: Money,
    val paid: Money,
    val pending: Money,
    val expenseCount: Int,
    val inboundInvoiceCount: Int
)

/**
 * Cashflow entry - projection of a financial fact (Invoice/Expense).
 *
 * This is the normalized representation used by the cashflow domain.
 * Created when financial facts are confirmed from documents.
 */
@Serializable
data class CashflowEntry(
    val id: CashflowEntryId,
    val tenantId: TenantId,
    val sourceType: CashflowSourceType,
    val sourceId: String, // UUID string of Invoice/Expense
    val documentId: DocumentId?,
    val direction: CashflowDirection,
    val eventDate: LocalDate,
    val amountGross: Money,
    val amountVat: Money,
    val remainingAmount: Money,
    val currency: Currency,
    val status: CashflowEntryStatus,
    val paidAt: LocalDateTime?, // UTC timestamp when entry became fully paid (null until PAID)
    val contact: CashflowContactRef? = null,
    val description: String? = null,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

// ============================================================================
// CASHFLOW ENTRY API REQUEST/RESPONSE MODELS
// ============================================================================

/**
 * Request to record a payment against a cashflow entry.
 */
@Serializable
data class CashflowPaymentRequest(
    val amount: Money,
    val paidAt: LocalDateTime,
    val note: String? = null,
    val bankTransactionId: BankTransactionId? = null,
    val dismissSuggestedMatch: Boolean = false,
    val paymentMethod: PaymentMethod = PaymentMethod.BankTransfer
)

@Serializable
data class BankTransactionDto(
    val id: BankTransactionId,
    val tenantId: TenantId,
    val bankAccountId: BankAccountId? = null,
    val documentId: DocumentId? = null,
    val source: BankTransactionSource = BankTransactionSource.PdfStatement,
    val transactionDate: LocalDate,
    val valueDate: LocalDate? = null,
    val signedAmount: Money,
    val currency: Currency = Currency.Eur,
    val counterparty: CounterpartySnapshot = CounterpartySnapshot(),
    val communication: TransactionCommunication? = null,
    val descriptionRaw: String? = null,
    val status: BankTransactionStatus,
    val resolutionType: ResolutionType? = null,
    val matchInfo: TransactionMatchInfo? = null,
    val ignoreInfo: TransactionIgnoreInfo? = null,
    val statementTrust: StatementTrust = StatementTrust.Low,
    val transferPairId: BankTransactionId? = null,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) {
    companion object
}

// ============================================================================
// Grouped field types — replace scattered nullable fields
// ============================================================================

/** Match info for a bank transaction — present when matched to a cashflow entry. */
@Serializable
data class TransactionMatchInfo(
    val cashflowEntryId: CashflowEntryId,
    val documentId: DocumentId? = null,
    val score: Double,
    val evidence: List<String> = emptyList(),
    val matchedBy: MatchedBy,
    val matchedAt: LocalDateTime,
)

/** Ignore info for a bank transaction — present when explicitly ignored. */
@Serializable
data class TransactionIgnoreInfo(
    val reason: IgnoredReason,
    val ignoredAt: LocalDateTime,
    val ignoredBy: String? = null,
)

/** Contact reference on a cashflow entry. */
@Serializable
data class CashflowContactRef(
    val id: ContactId,
    val name: String? = null,
)

/** Auto-payment status — typed states instead of 10 nullable fields. */
@Serializable
sealed interface AutoPaymentStatus {
    @Serializable
    @kotlinx.serialization.SerialName("AutoPaymentStatus.None")
    data object None : AutoPaymentStatus

    @Serializable
    @kotlinx.serialization.SerialName("AutoPaymentStatus.Matched")
    data class Matched(
        val bankTransactionId: BankTransactionId,
        val confidenceScore: Double,
        val scoreMargin: Double? = null,
        val reasons: List<String>,
        val matchSignals: List<String>,
        val matchedAt: LocalDateTime,
    ) : AutoPaymentStatus

    @Serializable
    @kotlinx.serialization.SerialName("AutoPaymentStatus.AutoPaid")
    data class AutoPaid(
        val paymentId: PaymentId,
        val bankTransactionId: BankTransactionId,
        val confidenceScore: Double,
        val reasons: List<String>,
        val autoPaidAt: LocalDateTime,
        val canUndo: Boolean,
    ) : AutoPaymentStatus

    @Serializable
    @kotlinx.serialization.SerialName("AutoPaymentStatus.Reversed")
    data object Reversed : AutoPaymentStatus
}

@Serializable
data class UndoAutoPaymentRequest(
    val reason: String? = null
)

/**
 * Request to cancel a cashflow entry.
 */
@Serializable
data class CancelEntryRequest(
    val reason: String? = null
)
