package tech.dokus.features.cashflow.presentation.detail.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tech.dokus.features.cashflow.presentation.detail.models.DocumentUiData
import tech.dokus.features.cashflow.presentation.detail.models.LineItemUiData
import tech.dokus.foundation.aura.components.DokusCardSurface
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.invoice_amount
import tech.dokus.aura.resources.invoice_description
import tech.dokus.aura.resources.invoice_due
import tech.dokus.aura.resources.invoice_issue
import tech.dokus.aura.resources.invoice_label
import tech.dokus.aura.resources.invoice_no_line_items
import tech.dokus.aura.resources.invoice_notes
import tech.dokus.aura.resources.invoice_subtotal
import tech.dokus.aura.resources.invoice_title
import tech.dokus.aura.resources.invoice_total
import tech.dokus.aura.resources.invoice_vat
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.style.textMuted

@Composable
internal fun CanonicalInvoiceDocumentCard(
    data: DocumentUiData.Invoice,
    counterpartyName: String,
    counterpartyAddress: String?,
    modifier: Modifier = Modifier,
) {
    val currencySign = data.currencySign
    val invoiceNumber = data.invoiceNumber ?: "\u2014"
    val issueDate = data.issueDate ?: "\u2014"
    val dueDate = data.dueDate ?: "\u2014"
    val subtotal = data.subtotalAmount?.toDisplayString()
    val vat = data.vatAmount?.toDisplayString()
    val total = data.totalAmount?.toDisplayString() ?: "\u2014"

    DokusCardSurface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = Constraints.Spacing.xxxLarge,
                    vertical = Constraints.Spacing.xxxLarge
                ),
            verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.medium),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = Constraints.Spacing.small),
                    verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.xSmall),
                ) {
                    Text(
                        text = counterpartyName,
                        style = MaterialTheme.typography.displaySmall.copy(fontSize = 20.sp),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    counterpartyAddress?.let { address ->
                        Text(
                            text = address,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.textMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Text(
                    text = stringResource(Res.string.invoice_label),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Text(
                text = "$currencySign$total",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.large),
            ) {
                CanonicalMetaCell(stringResource(Res.string.invoice_issue), issueDate)
                CanonicalMetaCell(stringResource(Res.string.invoice_due), dueDate)
                CanonicalMetaCell(stringResource(Res.string.invoice_title), invoiceNumber)
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            CanonicalLineItems(
                lineItems = data.lineItems,
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            CanonicalTotals(
                currencySign = currencySign,
                subtotal = subtotal,
                vat = vat,
                total = total,
            )

            data.iban?.takeIf { it.isNotBlank() }?.let { bank ->
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Text(
                    text = bank,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.textMuted,
                )
            }

            data.notes?.takeIf { it.isNotBlank() }?.let { notes ->
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(Res.string.invoice_notes),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.textMuted,
                    )
                    Text(
                        text = notes,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

@Composable
internal fun CanonicalMetaCell(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.textMuted,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
internal fun CanonicalLineItems(
    lineItems: List<LineItemUiData>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.small),
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(Res.string.invoice_description),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.textMuted,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(Res.string.invoice_amount),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.textMuted,
                textAlign = TextAlign.End,
                modifier = Modifier.width(96.dp),
            )
        }

        if (lineItems.isEmpty()) {
            Text(
                text = stringResource(Res.string.invoice_no_line_items),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.textMuted,
            )
        } else {
            lineItems.forEach { item ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = item.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = item.displayAmount,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.End,
                        modifier = Modifier.width(96.dp),
                    )
                }
            }
        }
    }
}

@Composable
internal fun CanonicalTotals(
    currencySign: String,
    subtotal: String?,
    vat: String?,
    total: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        CanonicalTotalRow(stringResource(Res.string.invoice_subtotal), subtotal, currencySign)
        CanonicalTotalRow(stringResource(Res.string.invoice_vat), vat, currencySign)
        HorizontalDivider(
            modifier = Modifier.width(220.dp),
            color = MaterialTheme.colorScheme.onSurface
        )
        Row(
            modifier = Modifier.width(220.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(Res.string.invoice_total),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "$currencySign$total",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun CanonicalTotalRow(
    label: String,
    value: String?,
    currencySign: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.width(220.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.textMuted,
        )
        Text(
            text = if (value != null) "$currencySign$value" else "\u2014",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
