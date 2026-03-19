package tech.dokus.features.cashflow.presentation.review.components.bankstatement

import androidx.compose.foundation.layout.fillMaxSize
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
            modifier = Modifier.width(1200.dp).height(800.dp),
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
            modifier = Modifier.width(1200.dp).height(800.dp),
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
            modifier = Modifier.width(1200.dp).height(800.dp),
        )
    }
}

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
private fun TransactionRowDuplicatePreview(
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
