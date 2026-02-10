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
import tech.dokus.domain.model.BillDraftData
import tech.dokus.domain.model.CreditNoteDraftData
import tech.dokus.domain.model.InvoiceDraftData
import tech.dokus.domain.model.ReceiptDraftData
import tech.dokus.features.cashflow.presentation.review.DocumentReviewIntent
import tech.dokus.features.cashflow.presentation.review.DocumentReviewState
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

        when (val draft = state.draftData) {
            is InvoiceDraftData -> {
                InvoiceAmountsDisplay(
                    subtotal = draft.subtotalAmount?.toString(),
                    vat = draft.vatAmount?.toString(),
                    total = draft.totalAmount?.toString()
                )
            }
            is BillDraftData -> {
                BillAmountsDisplay(
                    total = draft.totalAmount?.toString(),
                    vat = draft.vatAmount?.toString()
                )
            }
            is ReceiptDraftData -> {
                ReceiptAmountsDisplay(
                    total = draft.totalAmount?.toString(),
                    vat = draft.vatAmount?.toString()
                )
            }
            is CreditNoteDraftData -> {
                CreditNoteAmountsDisplay(
                    subtotal = draft.subtotalAmount?.toString(),
                    vat = draft.vatAmount?.toString(),
                    total = draft.totalAmount?.toString()
                )
            }
            null -> {
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

