package tech.dokus.features.cashflow.presentation.cashflow.components.invoice

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
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.invoice_add_line_item
import tech.dokus.aura.resources.invoice_line_items
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.features.cashflow.mvi.model.InvoiceLineItem
import tech.dokus.foundation.aura.extensions.localized

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
    error: DokusException?,
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
                text = stringResource(Res.string.invoice_line_items),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            TextButton(onClick = onAddItem) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(Res.string.invoice_add_line_item),
                    modifier = Modifier.padding(end = 4.dp)
                )
                Text(stringResource(Res.string.invoice_add_line_item))
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

        error?.let { exception ->
            Text(
                text = exception.localized,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}
