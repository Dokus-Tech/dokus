package tech.dokus.features.cashflow.presentation.review.components.bankstatement

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.bank_statement_col_amount
import tech.dokus.aura.resources.bank_statement_col_communication
import tech.dokus.aura.resources.bank_statement_col_date
import tech.dokus.aura.resources.bank_statement_col_description
import org.jetbrains.compose.resources.stringResource
import tech.dokus.features.cashflow.presentation.review.models.BankStatementTransactionUiRow
import tech.dokus.foundation.aura.constrains.Constraints

@Composable
internal fun BankStatementTransactionTable(
    transactions: List<BankStatementTransactionUiRow>,
    onToggle: (Int) -> Unit,
    isReadOnly: Boolean,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier.fillMaxWidth()) {
        item {
            TransactionTableHeader(isReadOnly = isReadOnly)
            HorizontalDivider()
        }
        itemsIndexed(
            items = transactions,
            key = { _, row -> row.index },
        ) { _, row ->
            BankStatementTransactionRow(
                row = row,
                onToggle = { onToggle(row.index) },
                isReadOnly = isReadOnly,
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        }
    }
}

@Composable
private fun TransactionTableHeader(
    isReadOnly: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = Constraints.Spacing.large,
                vertical = Constraints.Spacing.small,
            ),
        horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.small),
    ) {
        if (!isReadOnly) {
            // Checkbox column spacer
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(48.dp))
        }
        HeaderCell(stringResource(Res.string.bank_statement_col_date), Modifier.width(64.dp))
        HeaderCell(stringResource(Res.string.bank_statement_col_description), Modifier.weight(1f))
        HeaderCell(stringResource(Res.string.bank_statement_col_communication), Modifier.width(160.dp))
        HeaderCell(
            stringResource(Res.string.bank_statement_col_amount),
            Modifier.width(100.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
        )
    }
}

@Composable
private fun HeaderCell(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: androidx.compose.ui.text.style.TextAlign = androidx.compose.ui.text.style.TextAlign.Start,
) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
        textAlign = textAlign,
    )
}
