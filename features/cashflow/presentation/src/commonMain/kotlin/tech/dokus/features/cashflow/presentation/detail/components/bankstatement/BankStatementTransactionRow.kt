package tech.dokus.features.cashflow.presentation.detail.components.bankstatement

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import tech.dokus.features.cashflow.presentation.detail.models.BankStatementTransactionUiRow
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.style.positionNegative
import tech.dokus.foundation.aura.style.positionPositive

@Composable
internal fun BankStatementTransactionRow(
    row: BankStatementTransactionUiRow,
    onToggle: () -> Unit,
    isReadOnly: Boolean,
    modifier: Modifier = Modifier,
) {
    val dimmed = row.isExcluded
    val contentAlpha = if (dimmed) 0.4f else 1f
    val textDecoration = if (dimmed) TextDecoration.LineThrough else TextDecoration.None

    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (!isReadOnly) Modifier.clickable { onToggle() } else Modifier)
            .padding(
                horizontal = Constraints.Spacing.large,
                vertical = Constraints.Spacing.small,
            )
            .alpha(contentAlpha),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.small),
    ) {
        if (!isReadOnly) {
            Checkbox(
                checked = !row.isExcluded,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary,
                ),
            )
        }

        // Date
        Text(
            text = row.date,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(BankStatementColumnWidths.Date),
            textDecoration = textDecoration,
        )

        // Description + duplicate badge
        Row(
            modifier = Modifier.weight(BankStatementColumnWeights.Description),
            horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = row.counterpartyName ?: row.description,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textDecoration = textDecoration,
                modifier = Modifier.weight(1f, fill = false),
            )
            if (row.isDuplicate) {
                DuplicateBadge()
            }
        }

        // Counterparty
        Text(
            text = row.counterpartyIban.orEmpty(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(BankStatementColumnWeights.Counterparty),
            textDecoration = textDecoration,
        )

        // Communication
        Text(
            text = row.communication.orEmpty(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(BankStatementColumnWeights.Communication),
            textDecoration = textDecoration,
        )

        // Amount
        Text(
            text = row.displayAmount,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
            color = if (row.amountMinor > 0) MaterialTheme.colorScheme.positionPositive else MaterialTheme.colorScheme.positionNegative,
            modifier = Modifier.width(BankStatementColumnWidths.Amount),
            textDecoration = textDecoration,
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
        )
    }
}

internal object BankStatementColumnWidths {
    val Date = 64.dp
    val Amount = 80.dp
    val Checkbox = 48.dp
}

internal object BankStatementColumnWeights {
    const val Description = 2f
    const val Counterparty = 1.5f
    const val Communication = 1.5f
}
