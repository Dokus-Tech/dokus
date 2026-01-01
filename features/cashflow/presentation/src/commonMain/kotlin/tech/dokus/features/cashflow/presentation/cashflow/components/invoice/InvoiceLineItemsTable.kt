package tech.dokus.features.cashflow.presentation.cashflow.components.invoice

import tech.dokus.features.cashflow.mvi.model.InvoiceLineItem
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.invoice_add_line_item
import tech.dokus.aura.resources.invoice_amount
import tech.dokus.aura.resources.invoice_description
import tech.dokus.aura.resources.invoice_price
import tech.dokus.aura.resources.invoice_qty
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.stringResource

/**
 * Table component for invoice line items.
 * Shows header, expandable item rows, and add item button.
 */
@Composable
fun InvoiceLineItemsTable(
    items: List<InvoiceLineItem>,
    expandedItemId: String?,
    onItemClick: (String) -> Unit,
    onItemCollapse: () -> Unit,
    onAddItem: () -> Unit,
    onRemoveItem: (String) -> Unit,
    onUpdateDescription: (String, String) -> Unit,
    onUpdateQuantity: (String, Double) -> Unit,
    onUpdateUnitPrice: (String, String) -> Unit,
    onUpdateVatRate: (String, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Table header
        LineItemsTableHeader()

        // Line items
        items.forEach { item ->
            ExpandableLineItemRow(
                item = item,
                isExpanded = item.id == expandedItemId,
                onExpand = { onItemClick(item.id) },
                onCollapse = onItemCollapse,
                onRemove = { onRemoveItem(item.id) },
                onUpdateDescription = { onUpdateDescription(item.id, it) },
                onUpdateQuantity = { onUpdateQuantity(item.id, it) },
                onUpdateUnitPrice = { onUpdateUnitPrice(item.id, it) },
                onUpdateVatRate = { onUpdateVatRate(item.id, it) },
                showRemove = items.size > 1 // Always keep at least one item
            )
        }

        // Add item button
        TextButton(
            onClick = onAddItem,
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = stringResource(Res.string.invoice_add_line_item),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

/**
 * Header row for the line items table.
 */
@Composable
private fun LineItemsTableHeader(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
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
            modifier = Modifier.width(48.dp)
        )
        Text(
            text = stringResource(Res.string.invoice_price).uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.sp,
            textAlign = TextAlign.End,
            modifier = Modifier.width(80.dp)
        )
        Text(
            text = stringResource(Res.string.invoice_amount).uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.sp,
            textAlign = TextAlign.End,
            modifier = Modifier.width(80.dp)
        )
        // Spacer for expand icon column
        Spacer(modifier = Modifier.width(32.dp))
    }
}
