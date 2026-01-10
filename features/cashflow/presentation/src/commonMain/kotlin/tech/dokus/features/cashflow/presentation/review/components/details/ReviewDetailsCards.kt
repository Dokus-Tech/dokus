package tech.dokus.features.cashflow.presentation.review.components.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.action_change
import tech.dokus.aura.resources.cashflow_action_correct_contact
import tech.dokus.aura.resources.cashflow_action_ignore
import tech.dokus.aura.resources.cashflow_bill_details_section
import tech.dokus.aura.resources.cashflow_bound_to
import tech.dokus.aura.resources.cashflow_contact_name
import tech.dokus.aura.resources.cashflow_confidence_high
import tech.dokus.aura.resources.cashflow_confidence_label
import tech.dokus.aura.resources.cashflow_confidence_low
import tech.dokus.aura.resources.cashflow_confidence_medium
import tech.dokus.aura.resources.cashflow_contact_label
import tech.dokus.aura.resources.cashflow_contact_ai_extracted
import tech.dokus.aura.resources.cashflow_expense_details_section
import tech.dokus.aura.resources.cashflow_invoice_details_section
import tech.dokus.aura.resources.cashflow_invoice_number
import tech.dokus.aura.resources.cashflow_receipt_number
import tech.dokus.aura.resources.cashflow_processing_identifying_type
import tech.dokus.aura.resources.cashflow_select_document_type
import tech.dokus.aura.resources.cashflow_unknown_document_type
import tech.dokus.aura.resources.document_type_bill
import tech.dokus.aura.resources.document_type_expense
import tech.dokus.aura.resources.document_type_invoice
import tech.dokus.aura.resources.common_date
import tech.dokus.aura.resources.contacts_address
import tech.dokus.aura.resources.contacts_vat_number
import tech.dokus.aura.resources.invoice_due_date
import tech.dokus.aura.resources.invoice_issue_date
import tech.dokus.aura.resources.invoice_status_draft
import tech.dokus.domain.enums.DocumentType
import tech.dokus.features.cashflow.presentation.review.BillField
import tech.dokus.features.cashflow.presentation.review.DocumentReviewIntent
import tech.dokus.features.cashflow.presentation.review.DocumentReviewState
import tech.dokus.features.cashflow.presentation.review.EditableBillFields
import tech.dokus.features.cashflow.presentation.review.EditableExpenseFields
import tech.dokus.features.cashflow.presentation.review.EditableInvoiceFields
import tech.dokus.features.cashflow.presentation.review.ExpenseField
import tech.dokus.features.cashflow.presentation.review.InvoiceField
import tech.dokus.features.cashflow.presentation.review.components.forms.DetailBlock
import tech.dokus.features.cashflow.presentation.review.components.forms.DetailRow
import tech.dokus.foundation.aura.components.DokusCardSurface
import tech.dokus.foundation.aura.components.POutlinedButton
import tech.dokus.foundation.aura.components.PPrimaryButton
import tech.dokus.foundation.aura.components.StatusBadge
import tech.dokus.foundation.aura.components.fields.PDateField
import tech.dokus.foundation.aura.components.fields.PTextFieldStandard
import tech.dokus.foundation.aura.constrains.Constrains

// Confidence thresholds for AI extraction
private const val ConfidenceHighThreshold = 0.8
private const val ConfidenceMediumThreshold = 0.5
private const val ConfidenceMinimum = 0.0

// Badge styling
private const val ConfidenceBadgeBackgroundAlpha = 0.15f

@Composable
internal fun CounterpartyCard(
    state: DocumentReviewState.Content,
    onIntent: (DocumentReviewIntent) -> Unit,
    onCorrectContact: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val counterparty = tech.dokus.features.cashflow.presentation.review.models.counterpartyInfo(state)
    val hasDraft = listOf(counterparty.name, counterparty.vatNumber, counterparty.address)
        .any { !it.isNullOrBlank() }
    val actionsEnabled = !state.isBindingContact &&
        !state.isDocumentConfirmed &&
        !state.isDocumentRejected &&
        !state.isProcessing
    val hasLinkedContact = state.selectedContactSnapshot != null
    val correctContactLabel = if (hasLinkedContact) {
        Res.string.action_change
    } else {
        Res.string.cashflow_action_correct_contact
    }

    val nameLabel = stringResource(Res.string.cashflow_contact_name)

    val confidence = (
        state.originalData?.overallConfidence
            ?: state.document.latestIngestion?.confidence
        )
        ?.takeIf { it > ConfidenceMinimum }
    val confidenceLabelRes = if (state.showConfidence) {
        confidence?.let {
            when {
                it >= ConfidenceHighThreshold -> Res.string.cashflow_confidence_high
                it >= ConfidenceMediumThreshold -> Res.string.cashflow_confidence_medium
                else -> Res.string.cashflow_confidence_low
            }
        }
    } else {
        null
    }
    val confidenceColor = when {
        confidence == null -> MaterialTheme.colorScheme.onSurfaceVariant
        confidence >= ConfidenceHighThreshold -> MaterialTheme.colorScheme.tertiary
        confidence >= ConfidenceMediumThreshold -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.error
    }

    DokusCardSurface(
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(Constrains.Spacing.medium),
            verticalArrangement = Arrangement.spacedBy(Constrains.Spacing.small),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(Res.string.cashflow_contact_ai_extracted),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                if (hasDraft) {
                    StatusBadge(
                        text = stringResource(Res.string.invoice_status_draft),
                        backgroundColor = MaterialTheme.colorScheme.secondaryContainer,
                        textColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }

            DetailRow(label = nameLabel, value = counterparty.name)

            counterparty.vatNumber?.let { vat ->
                DetailRow(
                    label = stringResource(Res.string.contacts_vat_number),
                    value = vat,
                )
            }

            counterparty.address?.let { address ->
                DetailBlock(
                    label = stringResource(Res.string.contacts_address),
                    value = address,
                )
            }

            state.selectedContactSnapshot?.let { snapshot ->
                Text(
                    text = stringResource(Res.string.cashflow_bound_to, snapshot.name),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (confidenceLabelRes != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(Res.string.cashflow_confidence_label),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    StatusBadge(
                        text = stringResource(confidenceLabelRes),
                        backgroundColor = confidenceColor.copy(alpha = ConfidenceBadgeBackgroundAlpha),
                        textColor = confidenceColor,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Constrains.Spacing.small),
            ) {
                PPrimaryButton(
                    text = stringResource(correctContactLabel),
                    modifier = Modifier.weight(1f),
                    enabled = actionsEnabled,
                    onClick = onCorrectContact,
                )
                POutlinedButton(
                    text = stringResource(Res.string.cashflow_action_ignore),
                    modifier = Modifier.weight(1f),
                    enabled = actionsEnabled,
                    onClick = { onIntent(DocumentReviewIntent.ClearSelectedContact) },
                )
            }
        }
    }
}

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

    DokusCardSurface(modifier = modifier) {
        Column(
            modifier = Modifier.padding(Constrains.Spacing.medium),
            verticalArrangement = Arrangement.spacedBy(Constrains.Spacing.small),
        ) {
            Text(
                text = stringResource(titleRes),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )

            when (state.editableData.documentType) {
                DocumentType.Invoice -> {
                    val fields = state.editableData.invoice ?: EditableInvoiceFields()
                    PTextFieldStandard(
                        fieldName = stringResource(Res.string.cashflow_invoice_number),
                        value = fields.invoiceNumber,
                        onValueChange = {
                            onIntent(DocumentReviewIntent.UpdateInvoiceField(InvoiceField.INVOICE_NUMBER, it))
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    PDateField(
                        label = stringResource(Res.string.invoice_issue_date),
                        value = fields.issueDate,
                        onValueChange = {
                            onIntent(DocumentReviewIntent.UpdateInvoiceField(InvoiceField.ISSUE_DATE, it))
                        },
                    )
                    PDateField(
                        label = stringResource(Res.string.invoice_due_date),
                        value = fields.dueDate,
                        onValueChange = {
                            onIntent(DocumentReviewIntent.UpdateInvoiceField(InvoiceField.DUE_DATE, it))
                        },
                    )
                }
                DocumentType.Bill -> {
                    val fields = state.editableData.bill ?: EditableBillFields()
                    PTextFieldStandard(
                        fieldName = stringResource(Res.string.cashflow_invoice_number),
                        value = fields.invoiceNumber,
                        onValueChange = {
                            onIntent(DocumentReviewIntent.UpdateBillField(BillField.INVOICE_NUMBER, it))
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    PDateField(
                        label = stringResource(Res.string.invoice_issue_date),
                        value = fields.issueDate,
                        onValueChange = {
                            onIntent(DocumentReviewIntent.UpdateBillField(BillField.ISSUE_DATE, it))
                        },
                    )
                    PDateField(
                        label = stringResource(Res.string.invoice_due_date),
                        value = fields.dueDate,
                        onValueChange = {
                            onIntent(DocumentReviewIntent.UpdateBillField(BillField.DUE_DATE, it))
                        },
                    )
                }
                DocumentType.Expense -> {
                    val fields = state.editableData.expense ?: EditableExpenseFields()
                    PTextFieldStandard(
                        fieldName = stringResource(Res.string.cashflow_receipt_number),
                        value = fields.receiptNumber,
                        onValueChange = {
                            onIntent(DocumentReviewIntent.UpdateExpenseField(ExpenseField.RECEIPT_NUMBER, it))
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    PDateField(
                        label = stringResource(Res.string.common_date),
                        value = fields.date,
                        onValueChange = {
                            onIntent(DocumentReviewIntent.UpdateExpenseField(ExpenseField.DATE, it))
                        },
                    )
                }
                else -> {
                    if (state.isProcessing) {
                        // Show neutral placeholder during processing
                        Text(
                            text = stringResource(Res.string.cashflow_processing_identifying_type),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        // Show document type selector when AI failed or type is unknown
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
}
