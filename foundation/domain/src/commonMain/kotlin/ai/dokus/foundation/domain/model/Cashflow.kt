package ai.dokus.foundation.domain.model

import ai.dokus.foundation.domain.Money
import kotlinx.serialization.Serializable


/**
 * Cashflow overview data
 */
@Serializable
data class CashflowOverview(
    val totalIncome: Money,
    val totalExpenses: Money,
    val netCashflow: Money,
    val pendingInvoices: Money,
    val overdueInvoices: Money,
    val invoiceCount: Int,
    val expenseCount: Int
)