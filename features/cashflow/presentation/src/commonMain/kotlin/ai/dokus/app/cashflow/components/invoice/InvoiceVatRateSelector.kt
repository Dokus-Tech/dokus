package ai.dokus.app.cashflow.components.invoice

import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.common_percent_value
import tech.dokus.aura.resources.invoice_vat_rate
import tech.dokus.foundation.aura.components.DokusCardSurface
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource

/**
 * VAT rate selector dropdown for invoice line items.
 * Supports Belgian VAT rates: 21%, 12%, 6%, 0%.
 */
@Composable
fun InvoiceVatRateSelector(
    selectedRatePercent: Int,
    onSelectRate: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val rates = listOf(21, 12, 6, 0)

    Column(modifier = modifier) {
        Text(
            text = stringResource(Res.string.invoice_vat_rate),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Box {
            DokusCardSurface(
                modifier = Modifier.fillMaxWidth(),
                onClick = { expanded = true },
            ) {
                Text(
                    text = stringResource(Res.string.common_percent_value, selectedRatePercent),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp)
                )
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                rates.forEach { rate ->
                    DropdownMenuItem(
                        text = { Text(stringResource(Res.string.common_percent_value, rate)) },
                        onClick = {
                            onSelectRate(rate)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
