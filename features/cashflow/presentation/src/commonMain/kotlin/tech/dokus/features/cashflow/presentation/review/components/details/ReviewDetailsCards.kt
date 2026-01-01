package tech.dokus.features.cashflow.presentation.review.components.details

import tech.dokus.features.cashflow.presentation.review.BillField
import tech.dokus.features.cashflow.presentation.review.DocumentReviewIntent
import tech.dokus.features.cashflow.presentation.review.DocumentReviewState
import tech.dokus.features.cashflow.presentation.review.ExpenseField
import tech.dokus.features.cashflow.presentation.review.InvoiceField
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
import tech.dokus.aura.resources.cashflow_action_ignore_for_now
import tech.dokus.aura.resources.cashflow_action_link_contact
import tech.dokus.aura.resources.cashflow_action_save_new_contact
import tech.dokus.aura.resources.cashflow_bill_details_section
import tech.dokus.aura.resources.cashflow_bound_to
import tech.dokus.aura.resources.cashflow_confidence_high
import tech.dokus.aura.resources.cashflow_confidence_label
import tech.dokus.aura.resources.cashflow_confidence_low
import tech.dokus.aura.resources.cashflow_confidence_medium
import tech.dokus.aura.resources.cashflow_client_name
import tech.dokus.aura.resources.cashflow_contact_label
import tech.dokus.aura.resources.cashflow_counterparty_ai_extracted
import tech.dokus.aura.resources.cashflow_expense_details_section
import tech.dokus.aura.resources.cashflow_invoice_details_section
import tech.dokus.aura.resources.cashflow_invoice_number
import tech.dokus.aura.resources.cashflow_merchant
import tech.dokus.aura.resources.cashflow_receipt_number
import tech.dokus.aura.resources.cashflow_supplier_name
import tech.dokus.aura.resources.cashflow_unknown_document_type
import tech.dokus.aura.resources.common_date
import tech.dokus.aura.resources.contacts_address
import tech.dokus.aura.resources.contacts_vat_number
import tech.dokus.aura.resources.invoice_issue_date
import tech.dokus.aura.resources.invoice_due_date
import tech.dokus.aura.resources.invoice_status_draft
import tech.dokus.domain.enums.DocumentType
import tech.dokus.foundation.aura.components.DokusCardSurface
import tech.dokus.foundation.aura.components.POutlinedButton
import tech.dokus.foundation.aura.components.PPrimaryButton
import tech.dokus.foundation.aura.components.StatusBadge
import tech.dokus.foundation.aura.components.fields.PDateField
import tech.dokus.foundation.aura.components.fields.PTextFieldStandard
import tech.dokus.foundation.aura.constrains.Constrains
import tech.dokus.features.cashflow.presentation.review.components.forms.DetailBlock
import tech.dokus.features.cashflow.presentation.review.components.forms.DetailRow

@Composable
internal fun CounterpartyCard(
    state: DocumentReviewState.Content,
    onIntent: (DocumentReviewIntent) -> Unit,
    onLinkExistingContact: () -> Unit,
    onCreateNewContact: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val counterparty = tech.dokus.features.cashflow.presentation.review.models.counterpartyInfo(state)
    val hasDraft = listOf(counterparty.name, counterparty.vatNumber, counterparty.address)
        .any { !it.isNullOrBlank() }
    val actionsEnabled = !state.isBindingContact && !state.isDocumentConfirmed
    val hasLinkedContact = state.selectedContactSnapshot != null
    val linkLabel = if (hasLinkedContact) {
        Res.string.action_change
    } else {
        Res.string.cashflow_action_link_contact
    }

    val nameLabel = when (state.editableData.documentType) {
        DocumentType.Invoice -> stringResource(Res.string.cashflow_client_name)
        DocumentType.Bill -> stringResource(Res.string.cashflow_supplier_name)
        DocumentType.Expense -> stringResource(Res.string.cashflow_merchant)
        else -> stringResource(Res.string.cashflow_contact_label)
    }

    val confidence = (state.originalData?.overallConfidence
        ?: state.document.latestIngestion?.confidence)
        ?.takeIf { it > 0.0 }
    val confidenceLabelRes = confidence?.let {
        when {
            it >= 0.8 -> Res.string.cashflow_confidence_high
            it >= 0.5 -> Res.string.cashflow_confidence_medium
            else -> Res.string.cashflow_confidence_low
        }
    }
    val confidenceColor = when {
        confidence == null -> MaterialTheme.colorScheme.onSurfaceVariant
        confidence >= 0.8 -> MaterialTheme.colorScheme.tertiary
        confidence >= 0.5 -> MaterialTheme.colorScheme.secondary
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
                    text = stringResource(Res.string.cashflow_counterparty_ai_extracted),
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
                        backgroundColor = confidenceColor.copy(alpha = 0.15f),
                        textColor = confidenceColor,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Constrains.Spacing.small),
            ) {
                POutlinedButton(
                    text = stringResource(linkLabel),
                    modifier = Modifier.weight(1f),
                    enabled = actionsEnabled,
                    onClick = onLinkExistingContact,
                )
                PPrimaryButton(
                    text = stringResource(Res.string.cashflow_action_save_new_contact),
                    modifier = Modifier.weight(1f),
                    enabled = actionsEnabled,
                    onClick = onCreateNewContact,
                )
            }

            androidx.compose.material3.TextButton(
                onClick = {
                    onIntent(DocumentReviewIntent.ClearSelectedContact)
                },
                enabled = actionsEnabled,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) {
                Text(stringResource(Res.string.cashflow_action_ignore_for_now))
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
                    val fields = state.editableData.invoice ?: tech.dokus.features.cashflow.presentation.review.EditableInvoiceFields()
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
                    val fields = state.editableData.bill ?: tech.dokus.features.cashflow.presentation.review.EditableBillFields()
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
                    val fields = state.editableData.expense ?: tech.dokus.features.cashflow.presentation.review.EditableExpenseFields()
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
