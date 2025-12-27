package ai.dokus.foundation.domain.model

import tech.dokus.domain.Money
import ai.dokus.foundation.domain.enums.Currency
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

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
