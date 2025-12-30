package ai.dokus.app.cashflow.components.invoice

import ai.dokus.app.cashflow.viewmodel.model.CreateInvoiceFormState
import ai.dokus.app.cashflow.viewmodel.model.InvoiceLineItem
import ai.dokus.app.resources.generated.Res
import ai.dokus.app.resources.generated.cashflow_amount_with_currency
import ai.dokus.app.resources.generated.common_empty_value
import ai.dokus.app.resources.generated.currency_symbol_eur
import ai.dokus.app.resources.generated.invoice_amount
import ai.dokus.app.resources.generated.invoice_bill_to
import ai.dokus.app.resources.generated.invoice_description
import ai.dokus.app.resources.generated.invoice_due_date
import ai.dokus.app.resources.generated.invoice_issue_date
import ai.dokus.app.resources.generated.invoice_no_items
import ai.dokus.app.resources.generated.invoice_price
import ai.dokus.app.resources.generated.invoice_qty
import ai.dokus.app.resources.generated.invoice_select_client
import ai.dokus.app.resources.generated.invoice_subtotal
import ai.dokus.app.resources.generated.invoice_total
import ai.dokus.app.resources.generated.invoice_vat
import ai.dokus.foundation.design.components.PDashedDivider
import tech.dokus.domain.enums.InvoiceStatus
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.stringResource

/**
 * Displays an invoice preview that looks like a real paper invoice.
 * Features elevation, paper-like styling, and professional layout.
 */
@Composable
fun InvoiceSummaryCard(
    formState: CreateInvoiceFormState,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth().padding(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
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
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Client section
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = stringResource(Res.string.invoice_bill_to).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = formState.selectedClient?.name?.value ?: stringResource(Res.string.invoice_select_client),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
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
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
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
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = stringResource(Res.string.invoice_due_date).uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 1.sp
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
                        letterSpacing = 1.sp,
                        modifier = Modifier.weight(2f)
                    )
                    Text(
                        text = stringResource(Res.string.invoice_qty).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 1.sp,
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(0.5f)
                    )
                    Text(
                        text = stringResource(Res.string.invoice_price).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 1.sp,
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = stringResource(Res.string.invoice_amount).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 1.sp,
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Line Items
                val validItems = formState.items.filter { it.description.isNotBlank() || it.unitPriceDouble > 0 }
                if (validItems.isEmpty()) {
                    Text(
                        text = stringResource(Res.string.invoice_no_items),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
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
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    InvoiceTotalRow(
                        label = stringResource(Res.string.invoice_subtotal),
                        value = formState.subtotal
                    )
                    InvoiceTotalRow(
                        label = stringResource(Res.string.invoice_vat),
                        value = formState.vatAmount
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Dashed divider effect
                    PDashedDivider()

                    Spacer(modifier = Modifier.height(4.dp))

                    // Total amount - highlighted
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.small)
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(Res.string.invoice_total).uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = formState.total,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
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
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = item.description.ifBlank { stringResource(Res.string.common_empty_value) },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(2f)
        )
        Text(
            text = item.quantity.toString(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(0.5f)
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
            modifier = Modifier.weight(1f)
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
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * Format a double to 2 decimal places.
 */
private fun formatDecimal(value: Double): String {
    val rounded = kotlin.math.round(value * 100) / 100
    val intPart = rounded.toLong()
    val decPart = ((kotlin.math.abs(rounded - intPart) * 100) + 0.5).toInt()
    return "$intPart.${decPart.toString().padStart(2, '0')}"
}
