package tech.dokus.features.cashflow.presentation.detail.models

import androidx.compose.runtime.Immutable

@Immutable
data class BankStatementTransactionUiRow(
    val index: Int,
    val date: String,
    val description: String,
    val counterpartyName: String?,
    val counterpartyIban: String?,
    val communication: String?,
    val displayAmount: String,
    val amountMinor: Long,
    val isExcluded: Boolean,
    val isDuplicate: Boolean,
)
