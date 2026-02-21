package tech.dokus.features.cashflow.presentation.cashflow.components.invoice

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.action_collapse
import tech.dokus.aura.resources.cashflow_amount_with_currency
import tech.dokus.aura.resources.common_empty_value
import tech.dokus.aura.resources.common_percent_value
import tech.dokus.aura.resources.currency_symbol_eur
import tech.dokus.aura.resources.invoice_add_description_hint
import tech.dokus.aura.resources.invoice_description
import tech.dokus.aura.resources.invoice_line_total
import tech.dokus.aura.resources.invoice_price_with_currency
import tech.dokus.aura.resources.invoice_qty
import tech.dokus.aura.resources.invoice_remove
import tech.dokus.aura.resources.invoice_vat_rate
import tech.dokus.aura.resources.invoice_vat_with_rate
import tech.dokus.features.cashflow.mvi.model.InvoiceLineItem
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import tech.dokus.foundation.aura.components.fields.PTextFieldStandard
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

private const val ExpandedBackgroundAlpha = 0.3f
private const val HoveredBackgroundAlpha = 0.5f
private val RowPadding = 12.dp
private val QuantityColumnWidth = 48.dp
private val PriceColumnWidth = 80.dp
private val ExpandButtonSize = 32.dp
private val ExpandedContentTopPadding = 16.dp
private val FieldSpacing = 16.dp
private val DeleteIconSize = 18.dp
private val ButtonSpacing = 4.dp
private val LabelBottomPadding = 4.dp
private const val DecimalMultiplier = 100
private const val DecimalPadLength = 2
private const val RoundingOffset = 0.5

// Belgian VAT rates
private const val VatRateStandard = 21
private const val VatRateReduced = 12
private const val VatRateSuperReduced = 6
private const val VatRateZero = 0
private val VatRateOptions = listOf(VatRateStandard, VatRateReduced, VatRateSuperReduced, VatRateZero)

/**
 * Expandable line item row for the interactive invoice.
 * Shows a compact summary when collapsed, expands to show editable fields.
 */
@Composable
fun ExpandableLineItemRow(
    item: InvoiceLineItem,
    isExpanded: Boolean,
    onExpand: () -> Unit,
    onCollapse: () -> Unit,
    onRemove: () -> Unit,
    onUpdateDescription: (String) -> Unit,
    onUpdateQuantity: (Double) -> Unit,
    onUpdateUnitPrice: (String) -> Unit,
    onUpdateVatRate: (Int) -> Unit,
    showRemove: Boolean,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(
                when {
                    isExpanded -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = ExpandedBackgroundAlpha)
                    isHovered -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = HoveredBackgroundAlpha)
                    else -> MaterialTheme.colorScheme.surface
                }
            )
            .then(
                if (!isExpanded) {
                    Modifier
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = onExpand
                        )
                        .pointerHoverIcon(PointerIcon.Hand)
                } else {
                    Modifier
                }
            )
            .padding(RowPadding)
    ) {
        // Compact row (always visible)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Description
            Text(
                text = item.description.ifBlank {
                    stringResource(Res.string.invoice_add_description_hint)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = if (item.description.isNotBlank()) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(2f)
            )

            // Quantity
            Text(
                text = formatQuantity(item.quantity),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.End,
                modifier = Modifier.width(QuantityColumnWidth)
            )

            // Unit price
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
                modifier = Modifier.width(PriceColumnWidth)
            )

            // Line total
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
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.End,
                modifier = Modifier.width(PriceColumnWidth)
            )

            // Expand/collapse icon
            if (isExpanded) {
                IconButton(
                    onClick = onCollapse,
                    modifier = Modifier.size(ExpandButtonSize)
                ) {
                    Icon(
                        imageVector = Icons.Default.ExpandLess,
                        contentDescription = stringResource(Res.string.action_collapse),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Box(modifier = Modifier.width(ExpandButtonSize)) // Placeholder for alignment
            }
        }

        // Expanded content
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = ExpandedContentTopPadding),
                verticalArrangement = Arrangement.spacedBy(FieldSpacing)
            ) {
                // Description field
                PTextFieldStandard(
                    fieldName = stringResource(Res.string.invoice_description),
                    value = item.description,
                    onValueChange = onUpdateDescription,
                    modifier = Modifier.fillMaxWidth()
                )

                // Row: Quantity, Unit Price, VAT Rate
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(FieldSpacing)
                ) {
                    // Quantity
                    PTextFieldStandard(
                        fieldName = stringResource(Res.string.invoice_qty),
                        value = formatQuantity(item.quantity),
                        onValueChange = { value ->
                            value.toDoubleOrNull()?.let { onUpdateQuantity(it) }
                        },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )

                    // Unit Price
                    PTextFieldStandard(
                        fieldName = stringResource(
                            Res.string.invoice_price_with_currency,
                            stringResource(Res.string.currency_symbol_eur)
                        ),
                        value = item.unitPrice,
                        onValueChange = onUpdateUnitPrice,
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )

                    // VAT Rate dropdown
                    VatRateSelector(
                        selectedRate = item.vatRatePercent,
                        onRateSelected = onUpdateVatRate,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Actions row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Line total summary
                    Column {
                        Text(
                            text = stringResource(
                                Res.string.invoice_line_total,
                                stringResource(
                                    Res.string.cashflow_amount_with_currency,
                                    stringResource(Res.string.currency_symbol_eur),
                                    formatDecimal(item.lineTotalDouble)
                                )
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(
                                Res.string.invoice_vat_with_rate,
                                item.vatRatePercent,
                                stringResource(
                                    Res.string.cashflow_amount_with_currency,
                                    stringResource(Res.string.currency_symbol_eur),
                                    formatDecimal(item.vatAmountDouble)
                                )
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Remove button
                    if (showRemove) {
                        OutlinedButton(
                            onClick = onRemove,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                modifier = Modifier.size(DeleteIconSize),
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(ButtonSpacing))
                            Text(
                                text = stringResource(Res.string.invoice_remove),
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Dropdown selector for VAT rate.
 */
@Composable
private fun VatRateSelector(
    selectedRate: Int,
    onRateSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Column {
            Text(
                text = stringResource(Res.string.invoice_vat_rate),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = LabelBottomPadding)
            )
            OutlinedButton(
                onClick = { expanded = true },
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(Res.string.common_percent_value, selectedRate))
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            VatRateOptions.forEach { rate ->
                DropdownMenuItem(
                    text = { Text(stringResource(Res.string.common_percent_value, rate)) },
                    onClick = {
                        onRateSelected(rate)
                        expanded = false
                    }
                )
            }
        }
    }
}

/**
 * Format quantity - show integer if whole number, otherwise show decimal.
 */
private fun formatQuantity(value: Double): String {
    return if (value == value.toLong().toDouble()) {
        value.toLong().toString()
    } else {
        formatDecimal(value)
    }
}

/**
 * Format a double to 2 decimal places.
 */
private fun formatDecimal(value: Double): String {
    val rounded = kotlin.math.round(value * DecimalMultiplier) / DecimalMultiplier
    val intPart = rounded.toLong()
    val decPart = ((kotlin.math.abs(rounded - intPart) * DecimalMultiplier) + RoundingOffset).toInt()
    return "$intPart.${decPart.toString().padStart(DecimalPadLength, '0')}"
}

// =============================================================================
// Previews
// =============================================================================

@Preview
@Composable
private fun ExpandableLineItemRowPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        ExpandableLineItemRow(
            item = Mocks.sampleLineItems.first(),
            isExpanded = false,
            onExpand = {},
            onCollapse = {},
            onRemove = {},
            onUpdateDescription = {},
            onUpdateQuantity = {},
            onUpdateUnitPrice = {},
            onUpdateVatRate = {},
            showRemove = true
        )
    }
}
