package tech.dokus.features.cashflow.presentation.cashflow.components.invoice.create.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import tech.dokus.features.cashflow.mvi.model.InvoiceLineItem
import tech.dokus.foundation.aura.style.textFaint
import tech.dokus.foundation.aura.style.textMuted
import tech.dokus.foundation.aura.style.thText

private val TableRowHeight = 44.dp
private val CellPadding = 8.dp
private val QtyWidth = 72.dp
private val PriceWidth = 130.dp
private val VatWidth = 88.dp
private val AmountWidth = 132.dp
private val RemoveWidth = 28.dp

@Composable
internal fun InvoiceLineItemsGrid(
    items: List<InvoiceLineItem>,
    subtotal: String,
    vatAmount: String,
    total: String,
    onAddItem: () -> Unit,
    onRemoveItem: (String) -> Unit,
    onDescription: (String, String) -> Unit,
    onQuantity: (String, Double) -> Unit,
    onUnitPrice: (String, String) -> Unit,
    onVatRate: (String, Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        LineItemsHeader()
        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface)

        items.forEach { item ->
            LineItemRow(
                item = item,
                onEnter = onAddItem,
                onRemove = { onRemoveItem(item.id) },
                onDescription = { onDescription(item.id, it) },
                onQuantity = { onQuantity(item.id, it) },
                onUnitPrice = { onUnitPrice(item.id, it) },
                onVatRate = { onVatRate(item.id, it) }
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }

        Text(
            text = "+ Add line",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .padding(top = 4.dp)
                .clickable(onClick = onAddItem)
        )
        Text(
            text = "Enter = new \u00b7 Backspace empty = remove",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.textFaint
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp),
            horizontalArrangement = Arrangement.End
        ) {
            Column(
                modifier = Modifier.width(320.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TotalsRow("Subtotal", subtotal)
                TotalsRow("VAT 21%", vatAmount, labelMuted = true)
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface)
                TotalsRow("Total", total, emphasized = true)
            }
        }
    }
}

@Composable
private fun LineItemsHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HeaderCell("DESCRIPTION", Modifier.weight(1f), TextAlign.Start)
        HeaderCell("QTY", Modifier.width(QtyWidth), TextAlign.Center)
        HeaderCell("PRICE", Modifier.width(PriceWidth), TextAlign.End)
        HeaderCell("VAT", Modifier.width(VatWidth), TextAlign.Center)
        HeaderCell("AMOUNT", Modifier.width(AmountWidth), TextAlign.End)
        Box(modifier = Modifier.width(RemoveWidth))
    }
}

@Composable
private fun HeaderCell(
    text: String,
    modifier: Modifier,
    align: TextAlign
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.thText,
        textAlign = align,
        modifier = modifier
    )
}

@Composable
private fun LineItemRow(
    item: InvoiceLineItem,
    onEnter: () -> Unit,
    onRemove: () -> Unit,
    onDescription: (String) -> Unit,
    onQuantity: (Double) -> Unit,
    onUnitPrice: (String) -> Unit,
    onVatRate: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(TableRowHeight),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PlainTextCell(
            value = item.description,
            placeholder = "Start typing...",
            modifier = Modifier.weight(1f),
            onEnter = onEnter,
            onBackspaceEmpty = {
                if (item.description.isBlank() && item.unitPrice.isBlank()) {
                    onRemove()
                }
            },
            onValueChange = onDescription
        )

        VerticalDivider()

        PlainTextCell(
            value = formatQuantity(item.quantity),
            placeholder = "1",
            modifier = Modifier.width(QtyWidth),
            textAlign = TextAlign.Center,
            onValueChange = { value ->
                value.toDoubleOrNull()?.let(onQuantity)
            }
        )

        VerticalDivider()

        PlainTextCell(
            value = item.unitPrice,
            placeholder = "€",
            modifier = Modifier.width(PriceWidth),
            textAlign = TextAlign.End,
            onValueChange = onUnitPrice
        )

        VerticalDivider()

        VatCell(
            selected = item.vatRatePercent,
            onSelect = onVatRate,
            modifier = Modifier.width(VatWidth)
        )

        VerticalDivider()

        Text(
            text = item.lineTotal,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
            modifier = Modifier.width(AmountWidth)
        )

        Box(
            modifier = Modifier
                .width(RemoveWidth)
                .clickable(onClick = onRemove),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.textMuted
            )
        }
    }
}

@Composable
private fun PlainTextCell(
    value: String,
    placeholder: String,
    modifier: Modifier,
    textAlign: TextAlign = TextAlign.Start,
    onEnter: (() -> Unit)? = null,
    onBackspaceEmpty: (() -> Unit)? = null,
    onValueChange: (String) -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .padding(horizontal = CellPadding),
        contentAlignment = when (textAlign) {
            TextAlign.End -> Alignment.CenterEnd
            TextAlign.Center -> Alignment.Center
            else -> Alignment.CenterStart
        }
    ) {
        if (value.isBlank()) {
            Text(
                text = placeholder,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.textFaint,
                textAlign = textAlign,
                modifier = Modifier.fillMaxWidth()
            )
        }

        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = MaterialTheme.typography.bodyLarge.merge(TextStyle(textAlign = textAlign)),
            singleLine = true,
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            modifier = Modifier
                .fillMaxWidth()
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    when (event.key) {
                        Key.Enter -> {
                            onEnter?.invoke()
                            onEnter != null
                        }

                        Key.Backspace -> {
                            if (value.isBlank()) {
                                onBackspaceEmpty?.invoke()
                                onBackspaceEmpty != null
                            } else {
                                false
                            }
                        }

                        else -> false
                    }
                }
        )
    }
}

@Composable
private fun VerticalDivider() {
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(1.dp)
            .background(MaterialTheme.colorScheme.outlineVariant)
    )
}

@Composable
private fun VatCell(
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val rates = listOf(21, 12, 6, 0)

    Row(
        modifier = modifier
            .fillMaxHeight()
            .clickable { expanded = true }
            .padding(horizontal = CellPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = "$selected%",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Icon(
            imageVector = Icons.Default.KeyboardArrowDown,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.textMuted
        )

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            rates.forEach { rate ->
                DropdownMenuItem(
                    text = { Text("$rate%") },
                    onClick = {
                        onSelect(rate)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun TotalsRow(
    label: String,
    value: String,
    emphasized: Boolean = false,
    labelMuted: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = if (emphasized) MaterialTheme.typography.displaySmall else MaterialTheme.typography.titleLarge,
            color = if (labelMuted) MaterialTheme.colorScheme.textMuted else MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = value,
            style = if (emphasized) MaterialTheme.typography.displaySmall else MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = if (emphasized) FontWeight.Bold else FontWeight.Medium
        )
    }
}

private fun formatQuantity(quantity: Double): String {
    if (quantity % 1.0 == 0.0) return quantity.toInt().toString()
    return quantity.toString()
}
