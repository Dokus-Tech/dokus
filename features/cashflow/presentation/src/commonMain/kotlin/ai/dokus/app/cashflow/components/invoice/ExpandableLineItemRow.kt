package ai.dokus.app.cashflow.components.invoice

import ai.dokus.app.cashflow.viewmodel.model.InvoiceLineItem
import ai.dokus.foundation.design.components.fields.PTextFieldStandard
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
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
                    isExpanded -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    isHovered -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
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
            .padding(12.dp)
    ) {
        // Compact row (always visible)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Description
            Text(
                text = item.description.ifBlank { "Click to add description" },
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
                modifier = Modifier.width(48.dp)
            )

            // Unit price
            Text(
                text = if (item.unitPriceDouble > 0) "€${formatDecimal(item.unitPriceDouble)}" else "-",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.End,
                modifier = Modifier.width(80.dp)
            )

            // Line total
            Text(
                text = if (item.lineTotalDouble > 0) "€${formatDecimal(item.lineTotalDouble)}" else "-",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.End,
                modifier = Modifier.width(80.dp)
            )

            // Expand/collapse icon
            if (isExpanded) {
                IconButton(
                    onClick = onCollapse,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ExpandLess,
                        contentDescription = "Collapse",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Box(modifier = Modifier.width(32.dp)) // Placeholder for alignment
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
                    .padding(top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Description field
                PTextFieldStandard(
                    fieldName = "Description",
                    value = item.description,
                    onValueChange = onUpdateDescription,
                    modifier = Modifier.fillMaxWidth()
                )

                // Row: Quantity, Unit Price, VAT Rate
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Quantity
                    PTextFieldStandard(
                        fieldName = "Quantity",
                        value = formatQuantity(item.quantity),
                        onValueChange = { value ->
                            value.toDoubleOrNull()?.let { onUpdateQuantity(it) }
                        },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )

                    // Unit Price
                    PTextFieldStandard(
                        fieldName = "Unit Price (€)",
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
                            text = "Line Total: €${formatDecimal(item.lineTotalDouble)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "VAT (${item.vatRatePercent}%): €${formatDecimal(item.vatAmountDouble)}",
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
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Remove",
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
    val rates = listOf(21, 12, 6, 0)

    Box(modifier = modifier) {
        Column {
            Text(
                text = "VAT Rate",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            OutlinedButton(
                onClick = { expanded = true },
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "$selectedRate%")
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            rates.forEach { rate ->
                DropdownMenuItem(
                    text = { Text("$rate%") },
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
    val rounded = kotlin.math.round(value * 100) / 100
    val intPart = rounded.toLong()
    val decPart = ((kotlin.math.abs(rounded - intPart) * 100) + 0.5).toInt()
    return "$intPart.${decPart.toString().padStart(2, '0')}"
}
