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
import tech.dokus.aura.resources.cashflow_contact_create_new
import tech.dokus.aura.resources.cashflow_no_contact_selected
import tech.dokus.aura.resources.cashflow_select_contact
import tech.dokus.aura.resources.cashflow_suggested_contact
import tech.dokus.aura.resources.cashflow_use_this_contact
import tech.dokus.aura.resources.cashflow_choose_different
import tech.dokus.aura.resources.cashflow_credit_note_details_section
import tech.dokus.aura.resources.cashflow_credit_note_number
import tech.dokus.aura.resources.cashflow_extracted_name
import tech.dokus.aura.resources.cashflow_invoice_details_section
import tech.dokus.aura.resources.cashflow_invoice_number
import tech.dokus.aura.resources.cashflow_processing_identifying_type
import tech.dokus.aura.resources.cashflow_receipt_details_section
import tech.dokus.aura.resources.cashflow_receipt_number
import tech.dokus.aura.resources.cashflow_select_document_type
import tech.dokus.aura.resources.common_date
import tech.dokus.aura.resources.contacts_address
import tech.dokus.aura.resources.contacts_vat_number
import tech.dokus.aura.resources.workspace_iban
import tech.dokus.aura.resources.document_type_bill
import tech.dokus.aura.resources.document_type_credit_note
import tech.dokus.aura.resources.document_type_invoice
import tech.dokus.aura.resources.document_type_receipt
import tech.dokus.aura.resources.invoice_due_date
import tech.dokus.aura.resources.invoice_issue_date
import tech.dokus.domain.enums.DocumentType
import tech.dokus.features.cashflow.presentation.review.DocumentReviewIntent
import tech.dokus.features.cashflow.presentation.review.DocumentReviewState
import tech.dokus.features.cashflow.presentation.review.ContactSelectionState
import tech.dokus.features.cashflow.presentation.review.EditableBillFields
import tech.dokus.features.cashflow.presentation.review.EditableCreditNoteFields
import tech.dokus.features.cashflow.presentation.review.EditableInvoiceFields
import tech.dokus.features.cashflow.presentation.review.EditableReceiptFields
import tech.dokus.foundation.aura.components.POutlinedButton
import tech.dokus.foundation.aura.components.PPrimaryButton
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
    onCreateContact: () -> Unit,
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
            counterparty.iban != null ||
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
                            label = stringResource(Res.string.cashflow_extracted_name),
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
                counterparty.iban?.let { iban ->
                    FactField(
                        label = stringResource(Res.string.workspace_iban),
                        value = iban
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

        when (val selection = state.contactSelectionState) {
            is ContactSelectionState.Suggested -> {
                SuggestedContactCard(
                    name = selection.name,
                    vatNumber = selection.vatNumber,
                    onAccept = { onIntent(DocumentReviewIntent.AcceptSuggestedContact) },
                    onChooseDifferent = onCorrectContact,
                    modifier = Modifier.padding(top = Constrains.Spacing.small),
                )
            }
            ContactSelectionState.NoContact -> {
                if (hasExtractedData && state.selectedContactSnapshot == null) {
                    PendingContactCard(
                        name = counterparty.name,
                        vatNumber = counterparty.vatNumber,
                        iban = counterparty.iban,
                        onLinkExisting = onCorrectContact,
                        onCreateNew = onCreateContact,
                        modifier = Modifier.padding(top = Constrains.Spacing.small),
                    )
                }
            }
            ContactSelectionState.Selected -> Unit
        }
    }
}

@Composable
private fun SuggestedContactCard(
    name: String,
    vatNumber: String?,
    onAccept: () -> Unit,
    onChooseDifferent: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Constrains.Spacing.xSmall),
    ) {
        MicroLabel(text = stringResource(Res.string.cashflow_suggested_contact))
        FactField(
            label = stringResource(Res.string.cashflow_contact_label),
            value = name
        )
        vatNumber?.let { vat ->
            FactField(
                label = stringResource(Res.string.contacts_vat_number),
                value = vat
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Constrains.Spacing.small)) {
            PPrimaryButton(
                text = stringResource(Res.string.cashflow_use_this_contact),
                onClick = onAccept,
                modifier = Modifier.weight(1f)
            )
            POutlinedButton(
                text = stringResource(Res.string.cashflow_choose_different),
                onClick = onChooseDifferent,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun PendingContactCard(
    name: String?,
    vatNumber: String?,
    iban: String?,
    onLinkExisting: () -> Unit,
    onCreateNew: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Constrains.Spacing.xSmall),
    ) {
        MicroLabel(text = stringResource(Res.string.cashflow_no_contact_selected))
        name?.let { FactField(label = stringResource(Res.string.cashflow_extracted_name), value = it) }
        vatNumber?.let { FactField(label = stringResource(Res.string.contacts_vat_number), value = it) }
        iban?.let { FactField(label = stringResource(Res.string.workspace_iban), value = it) }
        Row(horizontalArrangement = Arrangement.spacedBy(Constrains.Spacing.small)) {
            PPrimaryButton(
                text = stringResource(Res.string.cashflow_select_contact),
                onClick = onLinkExisting,
                modifier = Modifier.weight(1f)
            )
            POutlinedButton(
                text = stringResource(Res.string.cashflow_contact_create_new),
                onClick = onCreateNew,
                modifier = Modifier.weight(1f)
            )
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
        DocumentType.Receipt -> Res.string.cashflow_receipt_details_section
        DocumentType.CreditNote -> Res.string.cashflow_credit_note_details_section
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
            DocumentType.Receipt -> {
                val fields = state.editableData.receipt ?: EditableReceiptFields()
                ReceiptDetailsFactDisplay(
                    receiptNumber = fields.receiptNumber.takeIf { it.isNotBlank() },
                    date = fields.date?.toString()
                )
            }
            DocumentType.CreditNote -> {
                val fields = state.editableData.creditNote ?: EditableCreditNoteFields()
                CreditNoteDetailsFactDisplay(
                    creditNoteNumber = fields.creditNoteNumber.takeIf { it.isNotBlank() },
                    issueDate = fields.issueDate?.toString(),
                    originalInvoiceNumber = fields.originalInvoiceNumber.takeIf { it.isNotBlank() }
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
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = Constrains.Spacing.small),
                        horizontalArrangement = Arrangement.spacedBy(Constrains.Spacing.small),
                    ) {
                        POutlinedButton(
                            text = stringResource(Res.string.document_type_receipt),
                            modifier = Modifier.weight(1f),
                            onClick = { onIntent(DocumentReviewIntent.SelectDocumentType(DocumentType.Receipt)) },
                        )
                        POutlinedButton(
                            text = stringResource(Res.string.document_type_credit_note),
                            modifier = Modifier.weight(1f),
                            onClick = { onIntent(DocumentReviewIntent.SelectDocumentType(DocumentType.CreditNote)) },
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
private fun ReceiptDetailsFactDisplay(
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

@Composable
private fun CreditNoteDetailsFactDisplay(
    creditNoteNumber: String?,
    issueDate: String?,
    originalInvoiceNumber: String?,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        FactField(
            label = stringResource(Res.string.cashflow_credit_note_number),
            value = creditNoteNumber
        )
        FactField(
            label = stringResource(Res.string.invoice_issue_date),
            value = issueDate
        )
        if (!originalInvoiceNumber.isNullOrBlank()) {
            FactField(
                label = stringResource(Res.string.cashflow_invoice_number),
                value = originalInvoiceNumber
            )
        }
    }
}
