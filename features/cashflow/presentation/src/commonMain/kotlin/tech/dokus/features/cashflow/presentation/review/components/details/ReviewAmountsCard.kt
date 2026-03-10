package tech.dokus.features.cashflow.presentation.review.components.details

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.cashflow_processing_calculating_totals
import tech.dokus.aura.resources.cashflow_section_amounts
import tech.dokus.aura.resources.cashflow_select_document_type
import tech.dokus.aura.resources.cashflow_vat_amount
import tech.dokus.aura.resources.invoice_subtotal
import tech.dokus.aura.resources.invoice_total_amount
import tech.dokus.features.cashflow.presentation.review.models.DocumentUiData
import tech.dokus.foundation.aura.constrains.Constraints

@Composable
internal fun AmountsCard(
    uiData: DocumentUiData?,
    isProcessing: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        MicroLabel(text = stringResource(Res.string.cashflow_section_amounts))

        when (uiData) {
            is DocumentUiData.Invoice -> FinancialAmountsDisplay(
                subtotal = uiData.subtotalAmount?.toString(),
                vat = uiData.vatAmount?.toString(),
                total = uiData.totalAmount?.toString(),
            )
            is DocumentUiData.CreditNote -> FinancialAmountsDisplay(
                subtotal = uiData.subtotalAmount?.toString(),
                vat = uiData.vatAmount?.toString(),
                total = uiData.totalAmount?.toString(),
            )
            is DocumentUiData.Receipt -> ReceiptAmountsDisplay(
                total = uiData.totalAmount?.toString(),
                vat = uiData.vatAmount?.toString(),
            )
            is DocumentUiData.BankStatement -> Text(
                text = "Amounts are available per transaction in bank statements.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            null -> Text(
                text = stringResource(
                    if (isProcessing) Res.string.cashflow_processing_calculating_totals
                    else Res.string.cashflow_select_document_type
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun FinancialAmountsDisplay(
    subtotal: String?,
    vat: String?,
    total: String?,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        AmountRow(label = stringResource(Res.string.invoice_subtotal), value = subtotal)
        AmountRow(label = stringResource(Res.string.cashflow_vat_amount), value = vat)
        HorizontalDivider(
            modifier = Modifier.padding(vertical = Constraints.Spacing.xSmall),
            color = MaterialTheme.colorScheme.outlineVariant
        )
        AmountRow(label = stringResource(Res.string.invoice_total_amount), value = total, isTotal = true)
    }
}

@Composable
private fun ReceiptAmountsDisplay(
    total: String?,
    vat: String?,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        AmountRow(label = stringResource(Res.string.cashflow_vat_amount), value = vat)
        HorizontalDivider(
            modifier = Modifier.padding(vertical = Constraints.Spacing.xSmall),
            color = MaterialTheme.colorScheme.outlineVariant
        )
        AmountRow(label = stringResource(Res.string.invoice_total_amount), value = total, isTotal = true)
    }
}
