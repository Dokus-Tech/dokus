package tech.dokus.features.cashflow.presentation.review.components.details

import tech.dokus.features.cashflow.presentation.review.BillField
import tech.dokus.features.cashflow.presentation.review.DocumentReviewIntent
import tech.dokus.features.cashflow.presentation.review.DocumentReviewState
import tech.dokus.features.cashflow.presentation.review.ExpenseField
import tech.dokus.features.cashflow.presentation.review.InvoiceField
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.cashflow_section_amounts
import tech.dokus.aura.resources.cashflow_unknown_document_type
import tech.dokus.aura.resources.cashflow_vat_amount
import tech.dokus.aura.resources.invoice_subtotal
import tech.dokus.aura.resources.invoice_total_amount
import tech.dokus.domain.enums.DocumentType
import tech.dokus.foundation.aura.components.DokusCardSurface
import tech.dokus.foundation.aura.components.fields.PTextFieldStandard
import tech.dokus.foundation.aura.constrains.Constrains

@Composable
internal fun AmountsCard(
    state: DocumentReviewState.Content,
    onIntent: (DocumentReviewIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    DokusCardSurface(modifier = modifier) {
        Column(
            modifier = Modifier.padding(Constrains.Spacing.medium),
            verticalArrangement = Arrangement.spacedBy(Constrains.Spacing.small),
        ) {
            Text(
                text = stringResource(Res.string.cashflow_section_amounts),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )

            when (state.editableData.documentType) {
                DocumentType.Invoice -> {
                    val fields = state.editableData.invoice
                        ?: tech.dokus.features.cashflow.presentation.review.EditableInvoiceFields()
                    PTextFieldStandard(
                        fieldName = stringResource(Res.string.invoice_subtotal),
                        value = fields.subtotalAmount,
                        onValueChange = {
                            onIntent(DocumentReviewIntent.UpdateInvoiceField(InvoiceField.SUBTOTAL_AMOUNT, it))
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    PTextFieldStandard(
                        fieldName = stringResource(Res.string.cashflow_vat_amount),
                        value = fields.vatAmount,
                        onValueChange = {
                            onIntent(DocumentReviewIntent.UpdateInvoiceField(InvoiceField.VAT_AMOUNT, it))
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    PTextFieldStandard(
                        fieldName = stringResource(Res.string.invoice_total_amount),
                        value = fields.totalAmount,
                        onValueChange = {
                            onIntent(DocumentReviewIntent.UpdateInvoiceField(InvoiceField.TOTAL_AMOUNT, it))
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                DocumentType.Bill -> {
                    val fields = state.editableData.bill
                        ?: tech.dokus.features.cashflow.presentation.review.EditableBillFields()
                    PTextFieldStandard(
                        fieldName = stringResource(Res.string.invoice_total_amount),
                        value = fields.amount,
                        onValueChange = {
                            onIntent(DocumentReviewIntent.UpdateBillField(BillField.AMOUNT, it))
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    PTextFieldStandard(
                        fieldName = stringResource(Res.string.cashflow_vat_amount),
                        value = fields.vatAmount,
                        onValueChange = {
                            onIntent(DocumentReviewIntent.UpdateBillField(BillField.VAT_AMOUNT, it))
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                DocumentType.Expense -> {
                    val fields = state.editableData.expense
                        ?: tech.dokus.features.cashflow.presentation.review.EditableExpenseFields()
                    PTextFieldStandard(
                        fieldName = stringResource(Res.string.invoice_total_amount),
                        value = fields.amount,
                        onValueChange = {
                            onIntent(DocumentReviewIntent.UpdateExpenseField(ExpenseField.AMOUNT, it))
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    PTextFieldStandard(
                        fieldName = stringResource(Res.string.cashflow_vat_amount),
                        value = fields.vatAmount,
                        onValueChange = {
                            onIntent(DocumentReviewIntent.UpdateExpenseField(ExpenseField.VAT_AMOUNT, it))
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                else -> {
                    Text(
                        text = stringResource(Res.string.cashflow_unknown_document_type),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}
