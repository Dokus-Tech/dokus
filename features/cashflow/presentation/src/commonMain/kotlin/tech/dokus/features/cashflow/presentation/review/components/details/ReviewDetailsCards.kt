package tech.dokus.features.cashflow.presentation.review.components.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.cashflow_bill_details_section
import tech.dokus.aura.resources.cashflow_contact_label
import tech.dokus.aura.resources.cashflow_expense_details_section
import tech.dokus.aura.resources.cashflow_invoice_details_section
import tech.dokus.aura.resources.cashflow_invoice_number
import tech.dokus.aura.resources.cashflow_processing_identifying_type
import tech.dokus.aura.resources.cashflow_receipt_number
import tech.dokus.aura.resources.cashflow_select_document_type
import tech.dokus.aura.resources.common_date
import tech.dokus.aura.resources.contacts_address
import tech.dokus.aura.resources.contacts_vat_number
import tech.dokus.aura.resources.document_type_bill
import tech.dokus.aura.resources.document_type_expense
import tech.dokus.aura.resources.document_type_invoice
import tech.dokus.aura.resources.invoice_due_date
import tech.dokus.aura.resources.invoice_issue_date
import tech.dokus.domain.enums.DocumentType
import tech.dokus.features.cashflow.presentation.review.DocumentReviewIntent
import tech.dokus.features.cashflow.presentation.review.DocumentReviewState
import tech.dokus.features.cashflow.presentation.review.EditableBillFields
import tech.dokus.features.cashflow.presentation.review.EditableExpenseFields
import tech.dokus.features.cashflow.presentation.review.EditableInvoiceFields
import tech.dokus.foundation.aura.components.POutlinedButton
import tech.dokus.foundation.aura.constrains.Constrains

/**
 * Counterparty display section - shows extracted counterparty info as facts.
 * Fact validation pattern: display-by-default, click to edit via ContactBlock.
 *
 * @param state The document review state
 * @param onIntent Intent handler
 * @param onCorrectContact Callback to open contact picker/sheet
 */
@Composable
internal fun CounterpartyCard(
    state: DocumentReviewState.Content,
    onIntent: (DocumentReviewIntent) -> Unit,
    onCorrectContact: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val counterparty = tech.dokus.features.cashflow.presentation.review.models.counterpartyInfo(state)
    val isReadOnly = state.isDocumentConfirmed || state.isDocumentRejected

    Column(modifier = modifier.fillMaxWidth()) {
        // Subtle micro-label
        MicroLabel(text = stringResource(Res.string.cashflow_contact_label))

        // Use ContactBlock for unified display + edit behavior
        ContactBlock(
            contact = state.selectedContactSnapshot,
            onEditClick = onCorrectContact,
            isReadOnly = isReadOnly
        )

        // Show extracted data below if different from bound contact
        val hasExtractedData = counterparty.name != null ||
            counterparty.vatNumber != null ||
            counterparty.address != null
        val boundName = state.selectedContactSnapshot?.name

        if (hasExtractedData && counterparty.name != boundName) {
            // Show extracted data as secondary info
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Constrains.Spacing.xSmall)
            ) {
                counterparty.name?.let { name ->
                    if (name != boundName) {
                        FactField(
                            label = "Extracted name",
                            value = name
                        )
                    }
                }
                counterparty.vatNumber?.let { vat ->
                    FactField(
                        label = stringResource(Res.string.contacts_vat_number),
                        value = vat
                    )
                }
                counterparty.address?.let { address ->
                    FactField(
                        label = stringResource(Res.string.contacts_address),
                        value = address
                    )
                }
            }
        }
    }
}

/**
 * Document details section - shows document info as facts.
 * Fact validation pattern: display-by-default.
 */
@Composable
internal fun InvoiceDetailsCard(
    state: DocumentReviewState.Content,
    onIntent: (DocumentReviewIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val titleRes = when (state.editableData.documentType) {
        DocumentType.Invoice -> Res.string.cashflow_invoice_details_section
        DocumentType.Bill -> Res.string.cashflow_bill_details_section
        DocumentType.Expense -> Res.string.cashflow_expense_details_section
        else -> Res.string.cashflow_invoice_details_section
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // Subtle micro-label
        MicroLabel(text = stringResource(titleRes))

        when (state.editableData.documentType) {
            DocumentType.Invoice -> {
                val fields = state.editableData.invoice ?: EditableInvoiceFields()
                InvoiceDetailsFactDisplay(
                    invoiceNumber = fields.invoiceNumber.takeIf { it.isNotBlank() },
                    issueDate = fields.issueDate?.toString(),
                    dueDate = fields.dueDate?.toString()
                )
            }
            DocumentType.Bill -> {
                val fields = state.editableData.bill ?: EditableBillFields()
                BillDetailsFactDisplay(
                    invoiceNumber = fields.invoiceNumber.takeIf { it.isNotBlank() },
                    issueDate = fields.issueDate?.toString(),
                    dueDate = fields.dueDate?.toString()
                )
            }
            DocumentType.Expense -> {
                val fields = state.editableData.expense ?: EditableExpenseFields()
                ExpenseDetailsFactDisplay(
                    receiptNumber = fields.receiptNumber.takeIf { it.isNotBlank() },
                    date = fields.date?.toString()
                )
            }
            else -> {
                // Document type selector - only show when type is unknown and not processing
                if (state.isProcessing) {
                    Text(
                        text = stringResource(Res.string.cashflow_processing_identifying_type),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Text(
                        text = stringResource(Res.string.cashflow_select_document_type),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Constrains.Spacing.small),
                    ) {
                        POutlinedButton(
                            text = stringResource(Res.string.document_type_invoice),
                            modifier = Modifier.weight(1f),
                            onClick = { onIntent(DocumentReviewIntent.SelectDocumentType(DocumentType.Invoice)) },
                        )
                        POutlinedButton(
                            text = stringResource(Res.string.document_type_bill),
                            modifier = Modifier.weight(1f),
                            onClick = { onIntent(DocumentReviewIntent.SelectDocumentType(DocumentType.Bill)) },
                        )
                        POutlinedButton(
                            text = stringResource(Res.string.document_type_expense),
                            modifier = Modifier.weight(1f),
                            onClick = { onIntent(DocumentReviewIntent.SelectDocumentType(DocumentType.Expense)) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InvoiceDetailsFactDisplay(
    invoiceNumber: String?,
    issueDate: String?,
    dueDate: String?,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        FactField(
            label = stringResource(Res.string.cashflow_invoice_number),
            value = invoiceNumber
        )
        FactField(
            label = stringResource(Res.string.invoice_issue_date),
            value = issueDate
        )
        FactField(
            label = stringResource(Res.string.invoice_due_date),
            value = dueDate
        )
    }
}

@Composable
private fun BillDetailsFactDisplay(
    invoiceNumber: String?,
    issueDate: String?,
    dueDate: String?,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        FactField(
            label = stringResource(Res.string.cashflow_invoice_number),
            value = invoiceNumber
        )
        FactField(
            label = stringResource(Res.string.invoice_issue_date),
            value = issueDate
        )
        FactField(
            label = stringResource(Res.string.invoice_due_date),
            value = dueDate
        )
    }
}

@Composable
private fun ExpenseDetailsFactDisplay(
    receiptNumber: String?,
    date: String?,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        FactField(
            label = stringResource(Res.string.cashflow_receipt_number),
            value = receiptNumber
        )
        FactField(
            label = stringResource(Res.string.common_date),
            value = date
        )
    }
}
