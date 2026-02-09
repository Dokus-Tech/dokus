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
import tech.dokus.domain.enums.DocumentType
import tech.dokus.features.cashflow.presentation.review.DocumentReviewIntent
import tech.dokus.features.cashflow.presentation.review.DocumentReviewState
import tech.dokus.features.cashflow.presentation.review.EditableBillFields
import tech.dokus.features.cashflow.presentation.review.EditableCreditNoteFields
import tech.dokus.features.cashflow.presentation.review.EditableInvoiceFields
import tech.dokus.features.cashflow.presentation.review.EditableReceiptFields
import tech.dokus.foundation.aura.constrains.Constrains

/**
 * Amounts display section - shows amounts as facts with tabular numbers.
 * Fact validation pattern: display-by-default, no form inputs.
 */
@Composable
internal fun AmountsCard(
    state: DocumentReviewState.Content,
    onIntent: (DocumentReviewIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Subtle micro-label
        MicroLabel(text = stringResource(Res.string.cashflow_section_amounts))

        when (state.editableData.documentType) {
            DocumentType.Invoice -> {
                val fields = state.editableData.invoice ?: EditableInvoiceFields()
                InvoiceAmountsDisplay(
                    subtotal = fields.subtotalAmount.formatAmountDisplay(),
                    vat = fields.vatAmount.formatAmountDisplay(),
                    total = fields.totalAmount.formatAmountDisplay()
                )
            }
            DocumentType.Bill -> {
                val fields = state.editableData.bill ?: EditableBillFields()
                BillAmountsDisplay(
                    total = fields.totalAmount.formatAmountDisplay(),
                    vat = fields.vatAmount.formatAmountDisplay()
                )
            }
            DocumentType.Receipt -> {
                val fields = state.editableData.receipt ?: EditableReceiptFields()
                ReceiptAmountsDisplay(
                    total = fields.totalAmount.formatAmountDisplay(),
                    vat = fields.vatAmount.formatAmountDisplay()
                )
            }
            DocumentType.CreditNote -> {
                val fields = state.editableData.creditNote ?: EditableCreditNoteFields()
                CreditNoteAmountsDisplay(
                    subtotal = fields.subtotalAmount.formatAmountDisplay(),
                    vat = fields.vatAmount.formatAmountDisplay(),
                    total = fields.totalAmount.formatAmountDisplay()
                )
            }
            else -> {
                // Show neutral placeholder during processing, hint when type not selected
                Text(
                    text = stringResource(
                        if (state.isProcessing) {
                            Res.string.cashflow_processing_calculating_totals
                        } else {
                            Res.string.cashflow_select_document_type
                        }
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun InvoiceAmountsDisplay(
    subtotal: String?,
    vat: String?,
    total: String?,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        AmountRow(
            label = stringResource(Res.string.invoice_subtotal),
            value = subtotal
        )
        AmountRow(
            label = stringResource(Res.string.cashflow_vat_amount),
            value = vat
        )
        // Subtle divider before total
        HorizontalDivider(
            modifier = Modifier.padding(vertical = Constrains.Spacing.xSmall),
            color = MaterialTheme.colorScheme.outlineVariant
        )
        AmountRow(
            label = stringResource(Res.string.invoice_total_amount),
            value = total,
            isTotal = true
        )
    }
}

@Composable
private fun BillAmountsDisplay(
    total: String?,
    vat: String?,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        AmountRow(
            label = stringResource(Res.string.cashflow_vat_amount),
            value = vat
        )
        // Subtle divider before total
        HorizontalDivider(
            modifier = Modifier.padding(vertical = Constrains.Spacing.xSmall),
            color = MaterialTheme.colorScheme.outlineVariant
        )
        AmountRow(
            label = stringResource(Res.string.invoice_total_amount),
            value = total,
            isTotal = true
        )
    }
}

@Composable
private fun ReceiptAmountsDisplay(
    total: String?,
    vat: String?,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        AmountRow(
            label = stringResource(Res.string.cashflow_vat_amount),
            value = vat
        )
        // Subtle divider before total
        HorizontalDivider(
            modifier = Modifier.padding(vertical = Constrains.Spacing.xSmall),
            color = MaterialTheme.colorScheme.outlineVariant
        )
        AmountRow(
            label = stringResource(Res.string.invoice_total_amount),
            value = total,
            isTotal = true
        )
    }
}

@Composable
private fun CreditNoteAmountsDisplay(
    subtotal: String?,
    vat: String?,
    total: String?,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        AmountRow(
            label = stringResource(Res.string.invoice_subtotal),
            value = subtotal
        )
        AmountRow(
            label = stringResource(Res.string.cashflow_vat_amount),
            value = vat
        )
        HorizontalDivider(
            modifier = Modifier.padding(vertical = Constrains.Spacing.xSmall),
            color = MaterialTheme.colorScheme.outlineVariant
        )
        AmountRow(
            label = stringResource(Res.string.invoice_total_amount),
            value = total,
            isTotal = true
        )
    }
}

/**
 * Formats amount string for display.
 * Returns null if blank (shows as "—" in AmountRow).
 */
private fun String.formatAmountDisplay(): String? {
    return takeIf { it.isNotBlank() }
}
