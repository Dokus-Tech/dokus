package tech.dokus.features.cashflow.presentation.cashflow.components.invoice

import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.invoice_subtotal
import tech.dokus.aura.resources.invoice_title
import tech.dokus.aura.resources.invoice_total
import tech.dokus.aura.resources.invoice_vat
import tech.dokus.foundation.aura.components.PDashedDivider
import tech.dokus.foundation.aura.extensions.localized
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper
import tech.dokus.domain.enums.InvoiceStatus
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.jetbrains.compose.ui.tooling.preview.PreviewParameter

/**
 * Invoice header with INVOICE title and status badge.
 * Used in the interactive invoice document.
 *
 * @param status The invoice status to display
 * @param modifier Modifier for the header
 */
@Composable
internal fun InvoiceDocumentHeader(
    status: InvoiceStatus = InvoiceStatus.Draft,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary)
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(Res.string.invoice_title).uppercase(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimary,
                letterSpacing = 2.sp
            )
            Text(
                text = status.localized,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                letterSpacing = 1.sp
            )
        }
    }
}

/**
 * Totals section showing subtotal, VAT, and total.
 * Used in the interactive invoice document.
 */
@Composable
internal fun InvoiceTotalsSection(
    subtotal: String,
    vatAmount: String,
    total: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        InvoiceTotalRow(label = stringResource(Res.string.invoice_subtotal), value = subtotal)
        InvoiceTotalRow(label = stringResource(Res.string.invoice_vat), value = vatAmount)

        Spacer(modifier = Modifier.height(4.dp))

        // Dashed divider
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
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                letterSpacing = 1.sp
            )
            Text(
                text = total,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

/**
 * A single row in the totals section showing a label and value.
 */
@Composable
internal fun InvoiceTotalRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// =============================================================================
// Previews
// =============================================================================

@Preview
@Composable
internal fun InvoiceDocumentHeaderPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        Column(modifier = Modifier.fillMaxWidth()) {
            InvoiceStatus.entries.forEach { status ->
                InvoiceDocumentHeader(status = status)
            }
        }
    }
}

@Preview
@Composable
internal fun InvoiceTotalsSectionPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        InvoiceTotalsSection(
            subtotal = "€4,600.00",
            vatAmount = "€966.00",
            total = "€5,566.00",
            modifier = Modifier.padding(16.dp)
        )
    }
}
