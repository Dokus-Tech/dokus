package tech.dokus.features.banking.presentation.payments.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import tech.dokus.domain.enums.BankTransactionStatus
import tech.dokus.domain.model.BankTransactionDto
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.extensions.localized
import tech.dokus.foundation.aura.extensions.statusColor

@Composable
internal fun TransactionRow(
    transaction: BankTransactionDto,
    isSelected: Boolean,
    onClick: () -> Unit,
    onIgnore: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedBg = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = 0f)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(selectedBg)
            .clickable(onClick = onClick)
            .padding(
                horizontal = Constraints.Spacing.large,
                vertical = Constraints.Spacing.medium,
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Left: date + counterparty + status
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.xxSmall),
        ) {
            Text(
                text = transaction.counterpartyName ?: transaction.descriptionRaw ?: "—",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.small),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = transaction.transactionDate.toString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                StatusDot(status = transaction.status)
                Text(
                    text = transaction.status.localized,
                    style = MaterialTheme.typography.bodySmall,
                    color = transaction.status.statusColor,
                )
            }
        }

        // Right: amount
        Text(
            text = transaction.signedAmount.toDisplayString(),
            style = MaterialTheme.typography.titleSmall,
            color = if (transaction.signedAmount.isNegative) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurface
            },
        )
    }
}

@Composable
private fun StatusDot(
    status: BankTransactionStatus,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(Constraints.Spacing.small)
            .clip(CircleShape)
            .background(status.statusColor),
    )
}
