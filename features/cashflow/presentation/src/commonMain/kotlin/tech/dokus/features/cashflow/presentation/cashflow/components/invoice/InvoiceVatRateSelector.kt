package tech.dokus.features.cashflow.presentation.cashflow.components.invoice

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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.common_percent_value
import tech.dokus.aura.resources.invoice_vat_rate
import tech.dokus.foundation.aura.components.DokusCardSurface
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

// Belgian VAT rates
private const val VatRateStandard = 21
private const val VatRateReduced = 12
private const val VatRateSuperReduced = 6
private const val VatRateZero = 0

// Layout constants
private val LabelSpacing = 8.dp
private val DropdownPadding = 16.dp

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
    val rates = listOf(VatRateStandard, VatRateReduced, VatRateSuperReduced, VatRateZero)

    Column(modifier = modifier) {
        Text(
            text = stringResource(Res.string.invoice_vat_rate),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(LabelSpacing))

        Box {
            DokusCardSurface(
                modifier = Modifier.fillMaxWidth(),
                onClick = { expanded = true },
            ) {
                Text(
                    text = stringResource(Res.string.common_percent_value, selectedRatePercent),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(DropdownPadding)
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

// =============================================================================
// Previews
// =============================================================================

@Preview
@Composable
private fun InvoiceVatRateSelectorPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        InvoiceVatRateSelector(
            selectedRatePercent = 21,
            onSelectRate = {}
        )
    }
}
