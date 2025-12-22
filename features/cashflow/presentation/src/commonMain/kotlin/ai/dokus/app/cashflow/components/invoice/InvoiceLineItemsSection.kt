package ai.dokus.app.cashflow.components.invoice

import ai.dokus.app.cashflow.viewmodel.model.InvoiceLineItem
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Section containing all invoice line items with add/remove functionality.
 */
@Composable
fun InvoiceLineItemsSection(
    items: List<InvoiceLineItem>,
    onAddItem: () -> Unit,
    onRemoveItem: (String) -> Unit,
    onUpdateDescription: (String, String) -> Unit,
    onUpdateQuantity: (String, Double) -> Unit,
    onUpdateUnitPrice: (String, String) -> Unit,
    onUpdateVatRate: (String, Int) -> Unit,
    error: String?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Line Items",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            TextButton(onClick = onAddItem) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add item",
                    modifier = Modifier.padding(end = 4.dp)
                )
                Text("Add Item")
            }
        }

        items.forEachIndexed { index, item ->
            InvoiceLineItemCard(
                item = item,
                itemNumber = index + 1,
                canDelete = items.size > 1,
                onDelete = { onRemoveItem(item.id) },
                onUpdateDescription = { onUpdateDescription(item.id, it) },
                onUpdateQuantity = { onUpdateQuantity(item.id, it) },
                onUpdateUnitPrice = { onUpdateUnitPrice(item.id, it) },
                onUpdateVatRate = { onUpdateVatRate(item.id, it) }
            )
        }

        error?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}
