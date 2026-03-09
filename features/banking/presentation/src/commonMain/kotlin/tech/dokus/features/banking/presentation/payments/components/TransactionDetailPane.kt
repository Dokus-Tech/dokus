package tech.dokus.features.banking.presentation.payments.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.banking_action_confirm_match
import tech.dokus.aura.resources.banking_action_ignore
import tech.dokus.aura.resources.banking_action_link
import tech.dokus.aura.resources.banking_detail_counterparty
import tech.dokus.aura.resources.banking_detail_description
import tech.dokus.aura.resources.banking_detail_iban
import tech.dokus.aura.resources.banking_detail_reference
import tech.dokus.aura.resources.banking_detail_title
import tech.dokus.domain.enums.BankTransactionStatus
import tech.dokus.domain.model.BankTransactionDto
import tech.dokus.foundation.aura.components.text.Amt
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.extensions.localized
import tech.dokus.foundation.aura.extensions.statusColor

@Composable
internal fun TransactionDetailPane(
    transaction: BankTransactionDto,
    onClose: () -> Unit,
    onLinkDocument: () -> Unit,
    onIgnore: () -> Unit,
    onConfirmMatch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(Constraints.Spacing.large)) {
        // Header: title + close
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(Res.string.banking_detail_title),
                style = MaterialTheme.typography.titleMedium,
            )
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(Constraints.Spacing.large))

        // Hero amount + date
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Constraints.Spacing.large),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Amt(
                minorUnits = transaction.signedAmount.minor,
                size = MaterialTheme.typography.headlineMedium.fontSize,
                weight = FontWeight.Bold,
            )
            Spacer(Modifier.height(Constraints.Spacing.xSmall))
            Text(
                text = formatShortDate(transaction.transactionDate),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(Modifier.height(Constraints.Spacing.large))

        // Detail rows
        transaction.counterpartyName?.let {
            DetailRow(
                label = stringResource(Res.string.banking_detail_counterparty),
                value = it,
            )
        }
        transaction.counterpartyIban?.let {
            DetailRow(
                label = stringResource(Res.string.banking_detail_iban),
                value = it.toString(),
            )
        }
        transaction.structuredCommunicationRaw?.let {
            DetailRow(
                label = stringResource(Res.string.banking_detail_reference),
                value = it,
            )
        }
        transaction.descriptionRaw?.let {
            Spacer(Modifier.height(Constraints.Spacing.medium))
            Text(
                text = stringResource(Res.string.banking_detail_description).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(Constraints.Spacing.xSmall))
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        Spacer(Modifier.height(Constraints.Spacing.large))

        // Status
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "\u2022 ",
                color = transaction.status.statusColor,
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = transaction.status.localized,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
        }

        Spacer(Modifier.height(Constraints.Spacing.xLarge))

        // Action buttons
        when (transaction.status) {
            BankTransactionStatus.Unmatched -> {
                Button(
                    onClick = onLinkDocument,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(Res.string.banking_action_link))
                }
                Spacer(Modifier.height(Constraints.Spacing.small))
                OutlinedButton(
                    onClick = onIgnore,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(Res.string.banking_action_ignore))
                }
            }
            BankTransactionStatus.NeedsReview -> {
                Button(
                    onClick = onConfirmMatch,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(Res.string.banking_action_confirm_match))
                }
                Spacer(Modifier.height(Constraints.Spacing.small))
                OutlinedButton(
                    onClick = onLinkDocument,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(Res.string.banking_action_link))
                }
                Spacer(Modifier.height(Constraints.Spacing.small))
                OutlinedButton(
                    onClick = onIgnore,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(Res.string.banking_action_ignore))
                }
            }
            BankTransactionStatus.Matched,
            BankTransactionStatus.Ignored -> {
                // No actions for resolved transactions
            }
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Constraints.Spacing.xSmall),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
        )
    }
}

