package tech.dokus.features.cashflow.presentation.cashflow.components.invoice

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.cashflow_amount_with_currency
import tech.dokus.aura.resources.common_empty_value
import tech.dokus.aura.resources.currency_symbol_eur
import tech.dokus.aura.resources.invoice_amount
import tech.dokus.aura.resources.invoice_description
import tech.dokus.aura.resources.invoice_due_date
import tech.dokus.aura.resources.invoice_issue_date
import tech.dokus.aura.resources.invoice_no_items
import tech.dokus.aura.resources.invoice_price
import tech.dokus.aura.resources.invoice_qty
import tech.dokus.aura.resources.invoice_recipient
import tech.dokus.aura.resources.invoice_select_client
import tech.dokus.aura.resources.invoice_subtotal
import tech.dokus.aura.resources.invoice_total
import tech.dokus.aura.resources.invoice_vat
import tech.dokus.domain.enums.InvoiceStatus
import tech.dokus.features.cashflow.mvi.model.CreateInvoiceFormState
import tech.dokus.features.cashflow.mvi.model.InvoiceLineItem
import tech.dokus.foundation.aura.components.DokusCardSurface
import tech.dokus.foundation.aura.components.PDashedDivider
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

// Card spacing
private val CardOuterPadding = 8.dp
private val CardContentPadding = 16.dp
private val ContentSpacing = 16.dp
private val SectionSpacing = 4.dp
private val DateColumnSpacing = 24.dp
private val TotalsSpacing = 8.dp
private val DividerMargin = 4.dp
private val TotalRowPadding = 12.dp

// Typography
private val LabelLetterSpacing = 1.sp

// Layout weights
private const val DescriptionWeight = 2f
private const val QtyWeight = 0.5f
private const val PriceWeight = 1f
private const val AmountWeight = 1f

// Total row styling
private const val PrimaryContainerAlpha = 0.5f
private const val MaxDescriptionLines = 2

// Money formatting
private const val CentsMultiplier = 100
private const val RoundingOffset = 0.5
private const val DecimalPadLength = 2

/**
 * Displays an invoice preview that looks like a real paper invoice.
 * Features elevation, paper-like styling, and professional layout.
 */
@Composable
fun InvoiceSummaryCard(
    formState: CreateInvoiceFormState,
    modifier: Modifier = Modifier
) {
    DokusCardSurface(
        modifier = modifier.fillMaxWidth().padding(CardOuterPadding),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Invoice header
            InvoiceDocumentHeader(status = InvoiceStatus.Draft)

            // Invoice content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(CardContentPadding),
                verticalArrangement = Arrangement.spacedBy(ContentSpacing)
            ) {
                // Client section
                Column(
                    verticalArrangement = Arrangement.spacedBy(SectionSpacing)
                ) {
                    Text(
                        text = stringResource(Res.string.invoice_recipient).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = LabelLetterSpacing
                    )
                    val clientName = formState.selectedClient?.name?.value
                        ?: stringResource(Res.string.invoice_select_client)
                    Text(
                        text = clientName,
                        style = MaterialTheme.typography.titleLarge,
                        color = if (formState.selectedClient != null) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }

                // Dates section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(DateColumnSpacing)
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(SectionSpacing)
                    ) {
                        Text(
                            text = stringResource(Res.string.invoice_issue_date).uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = formState.issueDate?.toString()
                                ?: stringResource(Res.string.common_empty_value),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(SectionSpacing)
                    ) {
                        Text(
                            text = stringResource(Res.string.invoice_due_date).uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = LabelLetterSpacing
                        )
                        Text(
                            text = formState.dueDate?.toString()
                                ?: stringResource(Res.string.common_empty_value),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Divider before items
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // Line Items Table Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(Res.string.invoice_description).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = LabelLetterSpacing,
                        modifier = Modifier.weight(DescriptionWeight)
                    )
                    Text(
                        text = stringResource(Res.string.invoice_qty).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = LabelLetterSpacing,
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(QtyWeight)
                    )
                    Text(
                        text = stringResource(Res.string.invoice_price).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = LabelLetterSpacing,
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(PriceWeight)
                    )
                    Text(
                        text = stringResource(Res.string.invoice_amount).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = LabelLetterSpacing,
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(AmountWeight)
                    )
                }

                // Line Items
                val validItems = formState.items.filter { it.description.isNotBlank() || it.unitPriceDouble > 0 }
                if (validItems.isEmpty()) {
                    Text(
                        text = stringResource(Res.string.invoice_no_items),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = TotalsSpacing)
                    )
                } else {
                    validItems.forEach { item ->
                        InvoiceLineItemRow(item = item)
                    }
                }

                // Divider after items
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // Totals section
                Column(
                    verticalArrangement = Arrangement.spacedBy(TotalsSpacing)
                ) {
                    InvoiceTotalRow(
                        label = stringResource(Res.string.invoice_subtotal),
                        value = formState.subtotal
                    )
                    InvoiceTotalRow(
                        label = stringResource(Res.string.invoice_vat),
                        value = formState.vatAmount
                    )

                    Spacer(modifier = Modifier.height(DividerMargin))

                    // Dashed divider effect
                    PDashedDivider()

                    Spacer(modifier = Modifier.height(DividerMargin))

                    // Total amount - highlighted
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.small)
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = PrimaryContainerAlpha))
                            .padding(horizontal = TotalRowPadding, vertical = TotalRowPadding),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(Res.string.invoice_total).uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            letterSpacing = LabelLetterSpacing
                        )
                        Text(
                            text = formState.total,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InvoiceLineItemRow(
    item: InvoiceLineItem,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = SectionSpacing),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = item.description.ifBlank { stringResource(Res.string.common_empty_value) },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = MaxDescriptionLines,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(DescriptionWeight)
        )
        Text(
            text = item.quantity.toString(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(QtyWeight)
        )
        Text(
            text = if (item.unitPriceDouble > 0) {
                stringResource(
                    Res.string.cashflow_amount_with_currency,
                    stringResource(Res.string.currency_symbol_eur),
                    formatDecimal(item.unitPriceDouble)
                )
            } else {
                stringResource(Res.string.common_empty_value)
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(PriceWeight)
        )
        Text(
            text = if (item.lineTotalDouble > 0) {
                stringResource(
                    Res.string.cashflow_amount_with_currency,
                    stringResource(Res.string.currency_symbol_eur),
                    formatDecimal(item.lineTotalDouble)
                )
            } else {
                stringResource(Res.string.common_empty_value)
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(AmountWeight)
        )
    }
}

/**
 * Format a double to 2 decimal places.
 */
private fun formatDecimal(value: Double): String {
    val rounded = kotlin.math.round(value * CentsMultiplier) / CentsMultiplier
    val intPart = rounded.toLong()
    val decPart = ((kotlin.math.abs(rounded - intPart) * CentsMultiplier) + RoundingOffset).toInt()
    return "$intPart.${decPart.toString().padStart(DecimalPadLength, '0')}"
}

// =============================================================================
// Previews
// =============================================================================

@Preview
@Composable
private fun InvoiceSummaryCardPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        InvoiceSummaryCard(formState = Mocks.sampleFormState)
    }
}
