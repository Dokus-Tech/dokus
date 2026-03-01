package tech.dokus.features.cashflow.presentation.review.components

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tech.dokus.domain.Money
import tech.dokus.domain.model.FinancialLineItem
import tech.dokus.domain.model.InvoiceDraftData
import tech.dokus.foundation.aura.components.DokusCardSurface
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.*
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.style.textMuted

@Composable
internal fun CanonicalInvoiceDocumentCard(
    draft: InvoiceDraftData,
    counterpartyName: String,
    counterpartyAddress: String?,
    modifier: Modifier = Modifier,
) {
    val currencySign = draft.currency.displaySign
    val invoiceNumber = draft.invoiceNumber ?: "\u2014"
    val issueDate = draft.issueDate?.toString() ?: "\u2014"
    val dueDate = draft.dueDate?.toString() ?: "\u2014"
    val subtotal = draft.subtotalAmount?.toDisplayString()
    val vat = draft.vatAmount?.toDisplayString()
    val total = draft.totalAmount?.toDisplayString() ?: "\u2014"

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
                    verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.xSmall),
                ) {
                    Text(
                        text = counterpartyName,
                        style = MaterialTheme.typography.displaySmall.copy(fontSize = 20.sp),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    counterpartyAddress?.let { address ->
                        Text(
                            text = address,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.textMuted,
                        )
                    }
                }
                Text(
                    text = stringResource(Res.string.invoice_label),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.textMuted,
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
                CanonicalInvoiceMetaCell(stringResource(Res.string.invoice_issue), issueDate)
                CanonicalInvoiceMetaCell(stringResource(Res.string.invoice_due), dueDate)
                CanonicalInvoiceMetaCell(stringResource(Res.string.invoice_title), invoiceNumber)
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            CanonicalInvoiceLineItems(
                lineItems = draft.lineItems,
                currencySign = currencySign,
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            CanonicalInvoiceTotals(
                currencySign = currencySign,
                subtotal = subtotal,
                vat = vat,
                total = total,
            )

            draft.iban?.value?.takeIf { it.isNotBlank() }?.let { bank ->
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Text(
                    text = bank,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.textMuted,
                )
            }

            draft.notes?.takeIf { it.isNotBlank() }?.let { notes ->
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
private fun CanonicalInvoiceMetaCell(
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
private fun CanonicalInvoiceLineItems(
    lineItems: List<FinancialLineItem>,
    currencySign: String,
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
                        text = item.description.ifBlank { "\u2014" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = invoiceItemLineAmount(item, currencySign),
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
private fun CanonicalInvoiceTotals(
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
        CanonicalInvoiceTotalRow(stringResource(Res.string.invoice_subtotal), subtotal, currencySign)
        CanonicalInvoiceTotalRow(stringResource(Res.string.invoice_vat), vat, currencySign)
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
private fun CanonicalInvoiceTotalRow(
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

private fun invoiceItemLineAmount(item: FinancialLineItem, currencySign: String): String {
    val amountMinor = item.netAmount ?: item.unitPrice?.let { unit -> (item.quantity ?: 1L) * unit }
    return amountMinor?.let { "$currencySign${Money(it).toDisplayString()}" } ?: "\u2014"
}
