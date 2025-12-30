package ai.dokus.app.cashflow.components.invoice

import ai.dokus.app.cashflow.viewmodel.model.InvoiceLineItem
import ai.dokus.app.resources.generated.Res
import ai.dokus.app.resources.generated.invoice_description
import ai.dokus.app.resources.generated.invoice_item_number
import ai.dokus.app.resources.generated.invoice_line_total
import ai.dokus.app.resources.generated.invoice_price
import ai.dokus.app.resources.generated.invoice_qty
import ai.dokus.app.resources.generated.invoice_remove
import tech.dokus.foundation.aura.components.fields.PTextFieldStandard
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource

/**
 * Card displaying a single invoice line item with editable fields.
 */
@Composable
fun InvoiceLineItemCard(
    item: InvoiceLineItem,
    itemNumber: Int,
    canDelete: Boolean,
    onDelete: () -> Unit,
    onUpdateDescription: (String) -> Unit,
    onUpdateQuantity: (Double) -> Unit,
    onUpdateUnitPrice: (String) -> Unit,
    onUpdateVatRate: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedCard(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(Res.string.invoice_item_number, itemNumber),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (canDelete) {
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(Res.string.invoice_remove),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            PTextFieldStandard(
                fieldName = stringResource(Res.string.invoice_description),
                value = item.description,
                onValueChange = onUpdateDescription,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PTextFieldStandard(
                    fieldName = stringResource(Res.string.invoice_qty),
                    value = if (item.quantity == 0.0) "" else item.quantity.toString(),
                    onValueChange = { value ->
                        value.toDoubleOrNull()?.let { onUpdateQuantity(it) }
                    },
                    modifier = Modifier.weight(1f)
                )

                PTextFieldStandard(
                    fieldName = stringResource(Res.string.invoice_price),
                    value = item.unitPrice,
                    onValueChange = onUpdateUnitPrice,
                    modifier = Modifier.weight(1f)
                )
            }

            InvoiceVatRateSelector(
                selectedRatePercent = item.vatRatePercent,
                onSelectRate = onUpdateVatRate
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = stringResource(Res.string.invoice_line_total, item.lineTotal),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
