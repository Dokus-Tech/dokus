package tech.dokus.features.cashflow.presentation.review.models

import androidx.compose.runtime.Immutable

@Immutable
data class BankStatementTransactionUiRow(
    val index: Int,
    val date: String,
    val description: String,
    val counterpartyName: String?,
    val communication: String?,
    val displayAmount: String,
    val amountMinor: Long,
    val isPositive: Boolean,
    val isExcluded: Boolean,
    val isDuplicate: Boolean,
)
