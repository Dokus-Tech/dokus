package tech.dokus.features.cashflow.presentation.review.components.bankstatement

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

private val PreviewSize = Modifier.width(1200.dp).height(800.dp)

// ═══════════════════════════════════════════════════════════════════
// FULL VIEW PREVIEWS
// ═══════════════════════════════════════════════════════════════════

@Preview
@Composable
private fun BankStatementWithDuplicatesPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        CanonicalBankStatementView(
            data = previewBankStatementUiData(),
            onToggleTransaction = {},
            onReject = {},
            onConfirm = {},
            isConfirming = false,
            isReadOnly = false,
            modifier = PreviewSize,
        )
    }
}

@Preview
@Composable
private fun BankStatementNoDuplicatesPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        CanonicalBankStatementView(
            data = previewBankStatementUiDataNoDuplicates(),
            onToggleTransaction = {},
            onReject = {},
            onConfirm = {},
            isConfirming = false,
            isReadOnly = false,
            modifier = PreviewSize,
        )
    }
}

@Preview
@Composable
private fun BankStatementReadOnlyPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        CanonicalBankStatementView(
            data = previewBankStatementUiDataNoDuplicates(),
            onToggleTransaction = {},
            onReject = {},
            onConfirm = {},
            isConfirming = false,
            isReadOnly = true,
            modifier = PreviewSize,
        )
    }
}

@Preview
@Composable
private fun BankStatementEmptyPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        CanonicalBankStatementView(
            data = previewBankStatementEmpty(),
            onToggleTransaction = {},
            onReject = {},
            onConfirm = {},
            isConfirming = false,
            isReadOnly = false,
            modifier = PreviewSize,
        )
    }
}

@Preview
@Composable
private fun BankStatementAllExcludedPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        CanonicalBankStatementView(
            data = previewBankStatementAllExcluded(),
            onToggleTransaction = {},
            onReject = {},
            onConfirm = {},
            isConfirming = false,
            isReadOnly = false,
            modifier = PreviewSize,
        )
    }
}

@Preview
@Composable
private fun BankStatementManyTransactionsPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        CanonicalBankStatementView(
            data = previewBankStatementManyTransactions(),
            onToggleTransaction = {},
            onReject = {},
            onConfirm = {},
            isConfirming = false,
            isReadOnly = false,
            modifier = PreviewSize,
        )
    }
}

@Preview
@Composable
private fun BankStatementNoBalancesPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        CanonicalBankStatementView(
            data = previewBankStatementNoBalances(),
            onToggleTransaction = {},
            onReject = {},
            onConfirm = {},
            isConfirming = false,
            isReadOnly = false,
            modifier = PreviewSize,
        )
    }
}

@Preview
@Composable
private fun BankStatementSingleTransactionPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        CanonicalBankStatementView(
            data = previewBankStatementSingleTransaction(),
            onToggleTransaction = {},
            onReject = {},
            onConfirm = {},
            isConfirming = false,
            isReadOnly = false,
            modifier = PreviewSize,
        )
    }
}

@Preview
@Composable
private fun BankStatementConfirmingPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        CanonicalBankStatementView(
            data = previewBankStatementUiDataNoDuplicates(),
            onToggleTransaction = {},
            onReject = {},
            onConfirm = {},
            isConfirming = true,
            isReadOnly = false,
            modifier = PreviewSize,
        )
    }
}

// ═══════════════════════════════════════════════════════════════════
// COMPONENT PREVIEWS
// ═══════════════════════════════════════════════════════════════════

@Preview
@Composable
private fun DuplicateBannerPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        BankStatementDuplicateBanner(
            duplicateCount = 7,
            totalCount = 12,
            includedCount = 5,
            excludedCount = 7,
        )
    }
}

@Preview
@Composable
private fun DuplicateBannerFewPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        BankStatementDuplicateBanner(
            duplicateCount = 1,
            totalCount = 20,
            includedCount = 19,
            excludedCount = 1,
        )
    }
}

@Preview
@Composable
private fun BalanceRowPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        BankStatementBalanceRow(
            openingBalance = "14,523.61",
            closingBalance = "12,310.42",
            movement = "-2,213.19",
            currencyPrefix = "€",
        )
    }
}

@Preview
@Composable
private fun BalanceRowPartialPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        BankStatementBalanceRow(
            openingBalance = "14,523.61",
            closingBalance = null,
            movement = null,
            currencyPrefix = "€",
        )
    }
}

@Preview
@Composable
private fun ActionBarPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        BankStatementActionBar(
            includedCount = 5,
            netAmount = "€-2,620.91",
            onReject = {},
            onConfirm = {},
            isConfirming = false,
        )
    }
}

@Preview
@Composable
private fun ActionBarConfirmingPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        BankStatementActionBar(
            includedCount = 5,
            netAmount = "€-2,620.91",
            onReject = {},
            onConfirm = {},
            isConfirming = true,
        )
    }
}

@Preview
@Composable
private fun ActionBarZeroPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        BankStatementActionBar(
            includedCount = 0,
            netAmount = "€0.00",
            onReject = {},
            onConfirm = {},
            isConfirming = false,
        )
    }
}

@Preview
@Composable
private fun TransactionRowIncludedPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        BankStatementTransactionRow(
            row = previewBankStatementTransactions()[0],
            onToggle = {},
            isReadOnly = false,
        )
    }
}

@Preview
@Composable
private fun TransactionRowDuplicateExcludedPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        BankStatementTransactionRow(
            row = previewBankStatementTransactions()[1],
            onToggle = {},
            isReadOnly = false,
        )
    }
}

@Preview
@Composable
private fun TransactionRowPositiveAmountPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        BankStatementTransactionRow(
            row = previewBankStatementTransactions()[3],
            onToggle = {},
            isReadOnly = false,
        )
    }
}

@Preview
@Composable
private fun TransactionRowReadOnlyPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        BankStatementTransactionRow(
            row = previewBankStatementTransactions()[0],
            onToggle = {},
            isReadOnly = true,
        )
    }
}

@Preview
@Composable
private fun DuplicateBadgePreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        DuplicateBadge()
    }
}
