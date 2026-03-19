package tech.dokus.features.cashflow.presentation.review.components.bankstatement

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.bank_statement_account
import tech.dokus.aura.resources.document_type_bank_statement
import org.jetbrains.compose.resources.stringResource
import tech.dokus.features.cashflow.presentation.review.models.DocumentUiData
import tech.dokus.foundation.aura.constrains.Constraints

@Composable
internal fun CanonicalBankStatementView(
    data: DocumentUiData.BankStatement,
    onToggleTransaction: (Int) -> Unit,
    onReject: () -> Unit,
    onConfirm: () -> Unit,
    isConfirming: Boolean,
    isReadOnly: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = Constraints.Spacing.large,
                    vertical = Constraints.Spacing.medium,
                ),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column {
                Text(
                    text = buildString {
                        append(data.institutionName ?: stringResource(Res.string.document_type_bank_statement))
                        val period = listOfNotNull(data.periodStart, data.periodEnd).joinToString(" – ")
                        if (period.isNotEmpty()) {
                            append("  ")
                            append(period)
                        }
                    },
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            data.accountIban?.let { iban ->
                Text(
                    text = "${stringResource(Res.string.bank_statement_account)}: $iban",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Duplicate banner (only when duplicates exist)
        if (data.hasDuplicates) {
            BankStatementDuplicateBanner(
                duplicateCount = data.duplicateCount,
                totalCount = data.transactions.size,
                includedCount = data.includedCount,
                excludedCount = data.excludedCount,
                modifier = Modifier.padding(horizontal = Constraints.Spacing.large),
            )
        }

        // Balance row
        if (data.openingBalance != null || data.closingBalance != null) {
            BankStatementBalanceRow(
                openingBalance = data.openingBalance,
                closingBalance = data.closingBalance,
                movement = data.movement,
                currencyPrefix = "€",
                modifier = Modifier.padding(
                    horizontal = Constraints.Spacing.large,
                    vertical = Constraints.Spacing.medium,
                ),
            )
        }

        HorizontalDivider()

        // Transaction table
        BankStatementTransactionTable(
            transactions = data.transactions,
            onToggle = onToggleTransaction,
            isReadOnly = isReadOnly,
            modifier = Modifier.weight(1f),
        )

        // Action bar (only when editable)
        if (!isReadOnly && data.transactions.isNotEmpty()) {
            HorizontalDivider()
            BankStatementActionBar(
                includedCount = data.includedCount,
                netAmount = "€${data.netAmountDisplay}",
                onReject = onReject,
                onConfirm = onConfirm,
                isConfirming = isConfirming,
            )
        }
    }
}
