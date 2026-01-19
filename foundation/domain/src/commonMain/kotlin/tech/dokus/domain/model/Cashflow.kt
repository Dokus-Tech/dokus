package tech.dokus.domain.model

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import tech.dokus.domain.Money
import tech.dokus.domain.enums.CashflowDirection
import tech.dokus.domain.enums.CashflowEntryStatus
import tech.dokus.domain.enums.CashflowSourceType
import tech.dokus.domain.enums.Currency
import tech.dokus.domain.ids.CashflowEntryId
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.TenantId

/**
 * Cashflow overview data with Cash-In / Cash-Out structure.
 *
 * Cash-In: Money flowing INTO the business (outgoing invoices to clients)
 * Cash-Out: Money flowing OUT of the business (expenses, bills to pay)
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
 * Summary of Cash-Out (money going out for expenses and bills).
 */
@Serializable
data class CashOutSummary(
    val total: Money,
    val paid: Money,
    val pending: Money,
    val expenseCount: Int,
    val billCount: Int
)

/**
 * Cashflow entry - projection of a financial fact (Invoice/Bill/Expense).
 *
 * This is the normalized representation used by the cashflow domain.
 * Created when financial facts are confirmed from documents.
 */
@Serializable
data class CashflowEntry(
    val id: CashflowEntryId,
    val tenantId: TenantId,
    val sourceType: CashflowSourceType,
    val sourceId: String, // UUID string of Invoice/Bill/Expense
    val documentId: DocumentId?,
    val direction: CashflowDirection,
    val eventDate: LocalDate,
    val amountGross: Money,
    val amountVat: Money,
    val remainingAmount: Money,
    val currency: Currency,
    val status: CashflowEntryStatus,
    val paidAt: LocalDateTime?, // UTC timestamp when entry became fully paid (null until PAID)
    val contactId: ContactId?,
    val contactName: String? = null,
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
    val note: String? = null
)

/**
 * Request to cancel a cashflow entry.
 */
@Serializable
data class CancelEntryRequest(
    val reason: String? = null
)
