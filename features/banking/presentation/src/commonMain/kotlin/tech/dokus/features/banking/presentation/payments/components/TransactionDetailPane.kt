package tech.dokus.features.banking.presentation.payments.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.X
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import androidx.compose.ui.text.style.TextAlign
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.banking_action_add_expense
import tech.dokus.aura.resources.banking_action_confirm_match
import tech.dokus.aura.resources.banking_action_ignore
import tech.dokus.aura.resources.banking_action_link
import tech.dokus.aura.resources.banking_detail_counterparty
import tech.dokus.aura.resources.banking_detail_description
import tech.dokus.aura.resources.banking_detail_evidence
import tech.dokus.aura.resources.banking_detail_iban
import tech.dokus.aura.resources.banking_detail_ignored_reason
import tech.dokus.aura.resources.banking_detail_matched_by
import tech.dokus.aura.resources.banking_detail_reference
import tech.dokus.aura.resources.banking_detail_resolution
import tech.dokus.aura.resources.banking_detail_title
import tech.dokus.aura.resources.banking_detail_trust
import tech.dokus.domain.Money
import tech.dokus.domain.enums.BankTransactionSource
import tech.dokus.domain.enums.BankTransactionStatus
import tech.dokus.domain.enums.Currency
import tech.dokus.domain.enums.IgnoredReason
import tech.dokus.domain.enums.MatchedBy
import tech.dokus.domain.enums.ResolutionType
import tech.dokus.domain.enums.StatementTrust
import tech.dokus.domain.ids.BankTransactionId
import tech.dokus.domain.ids.CashflowEntryId
import tech.dokus.domain.ids.Iban
import tech.dokus.domain.ids.StructuredCommunication
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.BankTransactionDto
import tech.dokus.domain.model.TransactionCommunicationDto
import tech.dokus.domain.model.TransactionMatchInfoDto
import tech.dokus.domain.model.contact.CounterpartySnapshotDto
import tech.dokus.foundation.aura.components.PButton
import tech.dokus.foundation.aura.components.PButtonVariant
import tech.dokus.foundation.aura.components.text.Amt
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.extensions.localized
import tech.dokus.foundation.aura.extensions.statusColor
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

@Composable
internal fun TransactionDetailPane(
    transaction: BankTransactionDto,
    onClose: () -> Unit,
    onLinkDocument: () -> Unit,
    onIgnore: () -> Unit,
    onConfirmMatch: () -> Unit,
    onCreateExpense: () -> Unit,
    onMarkTransfer: () -> Unit,
    onUndoTransfer: () -> Unit,
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
                    imageVector = Lucide.X,
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
        transaction.counterparty.name?.let {
            DetailRow(
                label = stringResource(Res.string.banking_detail_counterparty),
                value = it,
            )
        }
        transaction.counterparty.iban?.let {
            DetailRow(
                label = stringResource(Res.string.banking_detail_iban),
                value = it.toString(),
            )
        }
        val referenceText = when (val comm = transaction.communication) {
            is TransactionCommunicationDto.Structured -> comm.raw
            is TransactionCommunicationDto.FreeForm -> comm.text
            null -> null
        }
        referenceText?.let {
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

        // Trust badge
        if (transaction.statementTrust != StatementTrust.High) {
            Spacer(Modifier.height(Constraints.Spacing.small))
            DetailRow(
                label = stringResource(Res.string.banking_detail_trust),
                value = transaction.statementTrust.localized,
            )
        }

        // Match metadata (when matched)
        if (transaction.status == BankTransactionStatus.Matched) {
            transaction.matchInfo?.matchedBy?.let {
                DetailRow(
                    label = stringResource(Res.string.banking_detail_matched_by),
                    value = it.localized,
                )
            }
            transaction.resolutionType?.let {
                DetailRow(
                    label = stringResource(Res.string.banking_detail_resolution),
                    value = it.localized,
                )
            }
            transaction.matchInfo?.evidence?.takeIf { it.isNotEmpty() }?.let { evidence ->
                DetailRow(
                    label = stringResource(Res.string.banking_detail_evidence),
                    value = evidence.joinToString(", "),
                )
            }
        }

        // Ignored reason (when ignored)
        if (transaction.status == BankTransactionStatus.Ignored) {
            transaction.ignoreInfo?.reason?.let {
                DetailRow(
                    label = stringResource(Res.string.banking_detail_ignored_reason),
                    value = it.localized,
                )
            }
        }

        Spacer(Modifier.height(Constraints.Spacing.xLarge))

        // Action buttons
        when (transaction.status) {
            BankTransactionStatus.Unmatched -> {
                PButton(
                    text = stringResource(Res.string.banking_action_link),
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onLinkDocument,
                )
                Spacer(Modifier.height(Constraints.Spacing.small))
                PButton(
                    text = "Mark as transfer",
                    variant = PButtonVariant.Outline,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onMarkTransfer,
                )
                if (transaction.signedAmount.isNegative) {
                    Spacer(Modifier.height(Constraints.Spacing.small))
                    PButton(
                        text = stringResource(Res.string.banking_action_add_expense),
                        variant = PButtonVariant.Outline,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onCreateExpense,
                    )
                }
                Spacer(Modifier.height(Constraints.Spacing.small))
                PButton(
                    text = stringResource(Res.string.banking_action_ignore),
                    variant = PButtonVariant.OutlineMuted,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onIgnore,
                )
            }
            BankTransactionStatus.NeedsReview -> {
                PButton(
                    text = stringResource(Res.string.banking_action_confirm_match),
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onConfirmMatch,
                )
                Spacer(Modifier.height(Constraints.Spacing.small))
                PButton(
                    text = stringResource(Res.string.banking_action_link),
                    variant = PButtonVariant.Outline,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onLinkDocument,
                )
                Spacer(Modifier.height(Constraints.Spacing.small))
                PButton(
                    text = "Mark as transfer",
                    variant = PButtonVariant.Outline,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onMarkTransfer,
                )
                if (transaction.signedAmount.isNegative) {
                    Spacer(Modifier.height(Constraints.Spacing.small))
                    PButton(
                        text = stringResource(Res.string.banking_action_add_expense),
                        variant = PButtonVariant.Outline,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onCreateExpense,
                    )
                }
                Spacer(Modifier.height(Constraints.Spacing.small))
                PButton(
                    text = stringResource(Res.string.banking_action_ignore),
                    variant = PButtonVariant.OutlineMuted,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onIgnore,
                )
            }
            BankTransactionStatus.Matched -> {
                if (transaction.resolutionType == ResolutionType.Transfer) {
                    PButton(
                        text = "Undo transfer",
                        variant = PButtonVariant.OutlineMuted,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onUndoTransfer,
                    )
                }
            }
            BankTransactionStatus.Ignored -> {
                // No actions for ignored transactions
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

// =============================================================================
// Previews
// =============================================================================

private val PreviewDateTime = LocalDateTime(2026, 2, 15, 10, 0)
private val PreviewTenantId = TenantId.generate()

private val PreviewUnmatchedTx = BankTransactionDto(
    id = BankTransactionId.generate(),
    tenantId = PreviewTenantId,
    source = BankTransactionSource.PdfStatement,
    transactionDate = LocalDate(2026, 2, 14),
    signedAmount = Money.parseOrThrow("-1250.00"),
    counterparty = CounterpartySnapshotDto(
        name = "Coolblue Belgi\u00EB NV",
        iban = Iban("BE68539007547034"),
    ),
    communication = TransactionCommunicationDto.Structured(
        raw = "+++090/9337/55493+++",
        normalized = StructuredCommunication("+++090/9337/55493+++"),
    ),
    descriptionRaw = "Payment for order #12345",
    status = BankTransactionStatus.Unmatched,
    currency = Currency.Eur,
    createdAt = PreviewDateTime,
    updatedAt = PreviewDateTime,
)

private val PreviewMatchedTx = BankTransactionDto(
    id = BankTransactionId.generate(),
    tenantId = PreviewTenantId,
    source = BankTransactionSource.PdfStatement,
    transactionDate = LocalDate(2026, 2, 12),
    signedAmount = Money.parseOrThrow("-89.99"),
    counterparty = CounterpartySnapshotDto(
        name = "DigitalOcean",
        iban = Iban("BE71096123456769"),
    ),
    communication = TransactionCommunicationDto.FreeForm(text = "DO Invoice #12345"),
    status = BankTransactionStatus.Matched,
    resolutionType = ResolutionType.Document,
    matchInfo = TransactionMatchInfoDto(
        cashflowEntryId = CashflowEntryId.generate(),
        matchedBy = MatchedBy.Auto,
        score = 1.0,
        evidence = listOf("exact_amount", "structured_comm_match"),
        matchedAt = PreviewDateTime,
    ),
    statementTrust = StatementTrust.High,
    currency = Currency.Eur,
    createdAt = PreviewDateTime,
    updatedAt = PreviewDateTime,
)

@Preview(name = "Detail Pane — Unmatched", widthDp = 280, heightDp = 700)
@Composable
private fun TransactionDetailPaneUnmatchedPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters,
) {
    TestWrapper(parameters) {
        TransactionDetailPane(
            transaction = PreviewUnmatchedTx,
            onClose = {},
            onLinkDocument = {},
            onIgnore = {},
            onConfirmMatch = {},
            onCreateExpense = {},
            onMarkTransfer = {},
            onUndoTransfer = {},
            modifier = Modifier.width(280.dp),
        )
    }
}

@Preview(name = "Detail Pane — Matched", widthDp = 280, heightDp = 700)
@Composable
private fun TransactionDetailPaneMatchedPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters,
) {
    TestWrapper(parameters) {
        TransactionDetailPane(
            transaction = PreviewMatchedTx,
            onClose = {},
            onLinkDocument = {},
            onIgnore = {},
            onConfirmMatch = {},
            onCreateExpense = {},
            onMarkTransfer = {},
            onUndoTransfer = {},
            modifier = Modifier.width(280.dp),
        )
    }
}

