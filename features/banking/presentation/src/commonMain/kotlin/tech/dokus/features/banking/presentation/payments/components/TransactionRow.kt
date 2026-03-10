package tech.dokus.features.banking.presentation.payments.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.HorizontalDivider
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.banking_action_link
import tech.dokus.aura.resources.banking_col_account
import tech.dokus.aura.resources.banking_col_amount
import tech.dokus.aura.resources.banking_col_counterparty
import tech.dokus.aura.resources.banking_col_date
import tech.dokus.aura.resources.banking_col_description
import tech.dokus.aura.resources.banking_col_status
import tech.dokus.domain.Money
import tech.dokus.domain.enums.BankTransactionSource
import tech.dokus.domain.enums.BankTransactionStatus
import tech.dokus.domain.enums.Currency
import tech.dokus.domain.enums.MatchedBy
import tech.dokus.domain.enums.ResolutionType
import tech.dokus.domain.enums.StatementTrust
import tech.dokus.domain.ids.BankTransactionId
import tech.dokus.domain.ids.Iban
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.BankTransactionDto
import tech.dokus.foundation.aura.components.DokusCardSurface
import tech.dokus.foundation.aura.components.layout.DokusHeaderColumn
import tech.dokus.foundation.aura.components.layout.DokusTableCell
import tech.dokus.foundation.aura.components.layout.DokusTableColumnSpec
import tech.dokus.foundation.aura.components.layout.DokusTableHeader
import tech.dokus.foundation.aura.components.layout.DokusTableRow
import tech.dokus.foundation.aura.components.text.Amt
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.extensions.iconized
import tech.dokus.foundation.aura.extensions.localized
import tech.dokus.foundation.aura.extensions.statusColor
import tech.dokus.foundation.aura.style.amberSoft
import tech.dokus.foundation.aura.style.redSoft
import tech.dokus.foundation.aura.style.surfaceHover
import tech.dokus.foundation.aura.style.textMuted
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

/**
 * Column layout: Date(60dp) | Description(flex) | Counterparty(flex 0.7) | Acct(50dp) | Status(90dp) | Document(70dp) | Amount(100dp)
 */
@Immutable
internal object PaymentsTableColumns {
    val Date = DokusTableColumnSpec(width = 60.dp)
    val Description = DokusTableColumnSpec(weight = 1f)
    val Counterparty = DokusTableColumnSpec(weight = 0.7f)
    val Account = DokusTableColumnSpec(width = 50.dp)
    val Status = DokusTableColumnSpec(width = 90.dp)
    val Document = DokusTableColumnSpec(width = 70.dp)
    val Amount = DokusTableColumnSpec(width = 100.dp, horizontalAlignment = Alignment.End)
}

private val TableRowHeight = Constraints.Height.input

@Composable
internal fun TransactionHeaderRow(
    modifier: Modifier = Modifier,
) {
    DokusTableHeader(
        columns = listOf(
            DokusHeaderColumn(label = stringResource(Res.string.banking_col_date), width = 60.dp),
            DokusHeaderColumn(label = stringResource(Res.string.banking_col_description), weight = 1f),
            DokusHeaderColumn(label = stringResource(Res.string.banking_col_counterparty), weight = 0.7f),
            DokusHeaderColumn(label = stringResource(Res.string.banking_col_account), width = 50.dp),
            DokusHeaderColumn(label = stringResource(Res.string.banking_col_status), width = 90.dp),
            DokusHeaderColumn(label = "", width = 70.dp),
            DokusHeaderColumn(
                label = stringResource(Res.string.banking_col_amount),
                width = 100.dp,
                alignment = Alignment.End,
            ),
        ),
        modifier = modifier,
    )
}

@Composable
internal fun TransactionRow(
    transaction: BankTransactionDto,
    isSelected: Boolean,
    onClick: () -> Unit,
    accountName: String? = null,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val backgroundColor by animateColorAsState(
        targetValue = when {
            isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            isHovered -> MaterialTheme.colorScheme.surfaceHover
            else -> MaterialTheme.colorScheme.surface
        },
        animationSpec = tween(durationMillis = 100),
        label = "tx-row-bg",
    )

    DokusTableRow(
        modifier = modifier
            .hoverable(interactionSource = interactionSource)
            .pointerHoverIcon(PointerIcon.Hand),
        minHeight = TableRowHeight,
        backgroundColor = backgroundColor,
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = Constraints.Spacing.large),
    ) {
        // Date + trust dot
        DokusTableCell(PaymentsTableColumns.Date) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.xxSmall),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (transaction.statementTrust != StatementTrust.High) {
                    Box(
                        modifier = Modifier
                            .size(Constraints.Spacing.xSmall)
                            .clip(CircleShape)
                            .background(transaction.statementTrust.statusColor),
                    )
                }
                Text(
                    text = formatShortDate(transaction.transactionDate),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.sp,
                        fontFamily = MaterialTheme.typography.labelLarge.fontFamily,
                    ),
                    color = MaterialTheme.colorScheme.textMuted,
                    maxLines = 1,
                )
            }
        }

        // Description
        DokusTableCell(PaymentsTableColumns.Description) {
            Text(
                text = transaction.descriptionRaw ?: transaction.counterpartyName ?: "\u2014",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.5.sp,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        // Counterparty
        DokusTableCell(PaymentsTableColumns.Counterparty) {
            Text(
                text = transaction.counterpartyName ?: "\u2014",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        // Account (short label)
        DokusTableCell(PaymentsTableColumns.Account) {
            Text(
                text = accountName ?: "\u2014",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        // Status badge + resolution icon
        DokusTableCell(PaymentsTableColumns.Status) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.xxSmall),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TransactionStatusBadge(status = transaction.status)
                transaction.resolutionType?.let { resolution ->
                    Icon(
                        imageVector = resolution.iconized,
                        contentDescription = null,
                        modifier = Modifier.size(Constraints.IconSize.xSmall),
                        tint = MaterialTheme.colorScheme.textMuted,
                    )
                }
            }
        }

        // Document action
        DokusTableCell(PaymentsTableColumns.Document) {
            if (transaction.status == BankTransactionStatus.Unmatched ||
                transaction.status == BankTransactionStatus.NeedsReview
            ) {
                Text(
                    text = "+ ${stringResource(Res.string.banking_action_link)}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                )
            }
        }

        // Amount
        DokusTableCell(PaymentsTableColumns.Amount) {
            Amt(
                minorUnits = transaction.signedAmount.minor,
                size = 12.sp,
            )
        }
    }
}

/**
 * Mobile-friendly card layout: two lines (counterparty + date/status) with amount on the right.
 */
@Composable
internal fun TransactionCard(
    transaction: BankTransactionDto,
    isSelected: Boolean,
    onClick: () -> Unit,
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
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.xxSmall),
        ) {
            Text(
                text = transaction.counterpartyName ?: transaction.descriptionRaw ?: "\u2014",
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
                    text = formatShortDate(transaction.transactionDate),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TransactionStatusBadge(status = transaction.status)
            }
        }

        Amt(minorUnits = transaction.signedAmount.minor)
    }
}

@Composable
private fun TransactionStatusBadge(
    status: BankTransactionStatus,
    modifier: Modifier = Modifier,
) {
    val bgColor = when (status) {
        BankTransactionStatus.Unmatched -> MaterialTheme.colorScheme.amberSoft
        BankTransactionStatus.NeedsReview -> MaterialTheme.colorScheme.redSoft
        BankTransactionStatus.Matched -> MaterialTheme.colorScheme.surface
        BankTransactionStatus.Ignored -> MaterialTheme.colorScheme.surface
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(3.dp))
            .background(bgColor)
            .padding(horizontal = 7.dp, vertical = 2.dp),
    ) {
        Text(
            text = status.localized,
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = MaterialTheme.typography.labelLarge.fontFamily,
            color = status.statusColor,
            maxLines = 1,
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
    counterpartyName = "Coolblue Belgi\u00EB NV",
    counterpartyIban = Iban("BE68539007547034"),
    structuredCommunicationRaw = "+++090/9337/55493+++",
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
    counterpartyName = "DigitalOcean",
    descriptionRaw = "DO Invoice #12345",
    status = BankTransactionStatus.Matched,
    matchedBy = MatchedBy.Auto,
    resolutionType = ResolutionType.Document,
    matchScore = 1.0,
    matchEvidence = listOf("exact_amount", "structured_comm_match"),
    matchedAt = PreviewDateTime,
    statementTrust = StatementTrust.High,
    currency = Currency.Eur,
    createdAt = PreviewDateTime,
    updatedAt = PreviewDateTime,
)

private val PreviewLowTrustTx = BankTransactionDto(
    id = BankTransactionId.generate(),
    tenantId = PreviewTenantId,
    source = BankTransactionSource.PdfStatement,
    transactionDate = LocalDate(2026, 2, 13),
    signedAmount = Money.parseOrThrow("3500.00"),
    counterpartyName = "Acme Corp",
    status = BankTransactionStatus.NeedsReview,
    statementTrust = StatementTrust.Medium,
    currency = Currency.Eur,
    createdAt = PreviewDateTime,
    updatedAt = PreviewDateTime,
)

@Preview(name = "Transaction Row — Desktop", widthDp = 900)
@Composable
private fun TransactionRowPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters,
) {
    TestWrapper(parameters) {
        DokusCardSurface(modifier = Modifier.fillMaxWidth()) {
            Column {
                TransactionHeaderRow()
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                TransactionRow(
                    transaction = PreviewUnmatchedTx,
                    isSelected = false,
                    onClick = {},
                    accountName = "KBC Business",
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                TransactionRow(
                    transaction = PreviewMatchedTx,
                    isSelected = true,
                    onClick = {},
                    accountName = "Belfius",
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                TransactionRow(
                    transaction = PreviewLowTrustTx,
                    isSelected = false,
                    onClick = {},
                    accountName = "KBC Business",
                )
            }
        }
    }
}

@Preview(name = "Transaction Card — Mobile", widthDp = 390)
@Composable
private fun TransactionCardPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters,
) {
    TestWrapper(parameters) {
        DokusCardSurface(modifier = Modifier.fillMaxWidth()) {
            Column {
                TransactionCard(
                    transaction = PreviewUnmatchedTx,
                    isSelected = false,
                    onClick = {},
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                TransactionCard(
                    transaction = PreviewMatchedTx,
                    isSelected = true,
                    onClick = {},
                )
            }
        }
    }
}

