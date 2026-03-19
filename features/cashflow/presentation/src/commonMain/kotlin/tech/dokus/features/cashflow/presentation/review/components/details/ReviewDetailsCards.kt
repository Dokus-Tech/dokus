package tech.dokus.features.cashflow.presentation.review.components.details

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.cashflow_contact_label
import tech.dokus.aura.resources.cashflow_credit_note_details_section
import tech.dokus.aura.resources.cashflow_credit_note_number
import tech.dokus.aura.resources.cashflow_direction
import tech.dokus.aura.resources.cashflow_direction_in
import tech.dokus.aura.resources.cashflow_direction_out
import tech.dokus.aura.resources.cashflow_invoice_details_section
import tech.dokus.aura.resources.cashflow_invoice_number
import tech.dokus.aura.resources.cashflow_processing_identifying_type
import tech.dokus.aura.resources.cashflow_receipt_details_section
import tech.dokus.aura.resources.cashflow_receipt_number
import tech.dokus.aura.resources.cashflow_select_document_type
import tech.dokus.aura.resources.cashflow_bank_statement_details_section
import tech.dokus.aura.resources.common_date
import tech.dokus.aura.resources.contacts_address
import tech.dokus.aura.resources.document_type_classified_placeholder
import tech.dokus.aura.resources.document_type_credit_note
import tech.dokus.aura.resources.document_type_invoice
import tech.dokus.aura.resources.document_type_receipt
import tech.dokus.aura.resources.inspector_label_period
import tech.dokus.aura.resources.inspector_label_transactions
import tech.dokus.aura.resources.invoice_due_date
import tech.dokus.aura.resources.invoice_issue_date
import tech.dokus.aura.resources.workspace_iban
import tech.dokus.domain.enums.DocumentDirection
import tech.dokus.domain.enums.DocumentType
import tech.dokus.foundation.aura.extensions.localized
import tech.dokus.domain.model.contact.ResolvedContact
import tech.dokus.features.cashflow.presentation.review.DocumentReviewIntent
import tech.dokus.features.cashflow.presentation.review.DocumentReviewState
import tech.dokus.features.cashflow.presentation.review.EditableField
import tech.dokus.features.cashflow.presentation.review.models.DocumentUiData
import tech.dokus.foundation.aura.components.POutlinedButton
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.style.textMuted

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
    state: DocumentReviewState,
    onIntent: (DocumentReviewIntent) -> Unit,
    onCorrectContact: () -> Unit,
    onCreateContact: () -> Unit,
    isAccountantReadOnly: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val isReadOnly = isAccountantReadOnly || state.isDocumentConfirmed || state.isDocumentRejected
    val contact = state.effectiveContact

    Column(modifier = modifier.fillMaxWidth()) {
        // Subtle micro-label
        MicroLabel(text = stringResource(Res.string.cashflow_contact_label))

        // Use ContactBlock for unified display + edit behavior
        ContactBlock(
            displayState = contact,
            onEditClick = onCorrectContact,
            isReadOnly = isReadOnly
        )

        // Show extracted IBAN/address below only for Detected contacts
        if (contact is ResolvedContact.Detected) {
            val hasExtractedData = contact.iban != null || contact.address != null
            if (hasExtractedData) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.xSmall)
                ) {
                    contact.iban?.let { iban ->
                        FactField(
                            label = stringResource(Res.string.workspace_iban),
                            value = iban
                        )
                    }
                    contact.address?.let { address ->
                        FactField(
                            label = stringResource(Res.string.contacts_address),
                            value = address
                        )
                    }
                }
            }
        }
    }
}

// region Document details card — general entry-point + overloads

@Composable
internal fun DocumentDetailsCard(
    uiData: DocumentUiData,
    isReadOnly: Boolean,
    onDirectionSelected: (DocumentDirection) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (uiData) {
        is DocumentUiData.Invoice -> DocumentDetailsCard(uiData, isReadOnly, onDirectionSelected, modifier)
        is DocumentUiData.CreditNote -> DocumentDetailsCard(uiData, isReadOnly, onDirectionSelected, modifier)
        is DocumentUiData.Receipt -> DocumentDetailsCard(data = uiData, modifier = modifier)
        is DocumentUiData.BankStatement -> DocumentDetailsCard(data = uiData, modifier = modifier)
        // --- Classified-only document types ---
        is DocumentUiData.ProForma -> ClassifiedOnlyDetailsCard(uiData.documentType, modifier)
        is DocumentUiData.Quote -> ClassifiedOnlyDetailsCard(uiData.documentType, modifier)
        is DocumentUiData.OrderConfirmation -> ClassifiedOnlyDetailsCard(uiData.documentType, modifier)
        is DocumentUiData.DeliveryNote -> ClassifiedOnlyDetailsCard(uiData.documentType, modifier)
        is DocumentUiData.Reminder -> ClassifiedOnlyDetailsCard(uiData.documentType, modifier)
        is DocumentUiData.StatementOfAccount -> ClassifiedOnlyDetailsCard(uiData.documentType, modifier)
        is DocumentUiData.PurchaseOrder -> ClassifiedOnlyDetailsCard(uiData.documentType, modifier)
        is DocumentUiData.ExpenseClaim -> ClassifiedOnlyDetailsCard(uiData.documentType, modifier)
        is DocumentUiData.BankFee -> ClassifiedOnlyDetailsCard(uiData.documentType, modifier)
        is DocumentUiData.InterestStatement -> ClassifiedOnlyDetailsCard(uiData.documentType, modifier)
        is DocumentUiData.PaymentConfirmation -> ClassifiedOnlyDetailsCard(uiData.documentType, modifier)
        is DocumentUiData.VatReturn -> ClassifiedOnlyDetailsCard(uiData.documentType, modifier)
        is DocumentUiData.VatListing -> ClassifiedOnlyDetailsCard(uiData.documentType, modifier)
        is DocumentUiData.VatAssessment -> ClassifiedOnlyDetailsCard(uiData.documentType, modifier)
        is DocumentUiData.IcListing -> ClassifiedOnlyDetailsCard(uiData.documentType, modifier)
        is DocumentUiData.OssReturn -> ClassifiedOnlyDetailsCard(uiData.documentType, modifier)
        is DocumentUiData.CorporateTax -> ClassifiedOnlyDetailsCard(uiData.documentType, modifier)
        is DocumentUiData.CorporateTaxAdvance -> ClassifiedOnlyDetailsCard(uiData.documentType, modifier)
        is DocumentUiData.TaxAssessment -> ClassifiedOnlyDetailsCard(uiData.documentType, modifier)
        is DocumentUiData.PersonalTax -> ClassifiedOnlyDetailsCard(uiData.documentType, modifier)
        is DocumentUiData.WithholdingTax -> ClassifiedOnlyDetailsCard(uiData.documentType, modifier)
        is DocumentUiData.SocialContribution -> ClassifiedOnlyDetailsCard(uiData.documentType, modifier)
        is DocumentUiData.SocialFund -> ClassifiedOnlyDetailsCard(uiData.documentType, modifier)
        is DocumentUiData.SelfEmployedContribution -> ClassifiedOnlyDetailsCard(uiData.documentType, modifier)
        is DocumentUiData.Vapz -> ClassifiedOnlyDetailsCard(uiData.documentType, modifier)
        is DocumentUiData.SalarySlip -> ClassifiedOnlyDetailsCard(uiData.documentType, modifier)
        is DocumentUiData.PayrollSummary -> ClassifiedOnlyDetailsCard(uiData.documentType, modifier)
        is DocumentUiData.EmploymentContract -> ClassifiedOnlyDetailsCard(uiData.documentType, modifier)
        is DocumentUiData.Dimona -> ClassifiedOnlyDetailsCard(uiData.documentType, modifier)
        is DocumentUiData.C4 -> ClassifiedOnlyDetailsCard(uiData.documentType, modifier)
        is DocumentUiData.HolidayPay -> ClassifiedOnlyDetailsCard(uiData.documentType, modifier)
        is DocumentUiData.Contract -> ClassifiedOnlyDetailsCard(uiData.documentType, modifier)
        is DocumentUiData.Lease -> ClassifiedOnlyDetailsCard(uiData.documentType, modifier)
        is DocumentUiData.Loan -> ClassifiedOnlyDetailsCard(uiData.documentType, modifier)
        is DocumentUiData.Insurance -> ClassifiedOnlyDetailsCard(uiData.documentType, modifier)
        is DocumentUiData.Dividend -> ClassifiedOnlyDetailsCard(uiData.documentType, modifier)
        is DocumentUiData.ShareholderRegister -> ClassifiedOnlyDetailsCard(uiData.documentType, modifier)
        is DocumentUiData.CompanyExtract -> ClassifiedOnlyDetailsCard(uiData.documentType, modifier)
        is DocumentUiData.AnnualAccounts -> ClassifiedOnlyDetailsCard(uiData.documentType, modifier)
        is DocumentUiData.BoardMinutes -> ClassifiedOnlyDetailsCard(uiData.documentType, modifier)
        is DocumentUiData.Subsidy -> ClassifiedOnlyDetailsCard(uiData.documentType, modifier)
        is DocumentUiData.Fine -> ClassifiedOnlyDetailsCard(uiData.documentType, modifier)
        is DocumentUiData.Permit -> ClassifiedOnlyDetailsCard(uiData.documentType, modifier)
        is DocumentUiData.CustomsDeclaration -> ClassifiedOnlyDetailsCard(uiData.documentType, modifier)
        is DocumentUiData.Intrastat -> ClassifiedOnlyDetailsCard(uiData.documentType, modifier)
        is DocumentUiData.DepreciationSchedule -> ClassifiedOnlyDetailsCard(uiData.documentType, modifier)
        is DocumentUiData.Inventory -> ClassifiedOnlyDetailsCard(uiData.documentType, modifier)
        is DocumentUiData.Other -> ClassifiedOnlyDetailsCard(uiData.documentType, modifier)
    }
}

@Composable
internal fun DocumentDetailsCard(
    data: DocumentUiData.Invoice,
    isReadOnly: Boolean,
    onDirectionSelected: (DocumentDirection) -> Unit,
    onIntent: (DocumentReviewIntent) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        MicroLabel(text = stringResource(Res.string.cashflow_invoice_details_section))
        InvoiceDetailsFactDisplay(
            direction = data.direction,
            isReadOnly = isReadOnly,
            onDirectionSelected = onDirectionSelected,
            onIntent = onIntent,
            invoiceNumber = data.invoiceNumber,
            issueDate = data.issueDate,
            dueDate = data.dueDate,
        )
    }
}

@Composable
internal fun DocumentDetailsCard(
    data: DocumentUiData.CreditNote,
    isReadOnly: Boolean,
    onDirectionSelected: (DocumentDirection) -> Unit,
    onIntent: (DocumentReviewIntent) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        MicroLabel(text = stringResource(Res.string.cashflow_credit_note_details_section))
        CreditNoteDetailsFactDisplay(
            direction = data.direction,
            isReadOnly = isReadOnly,
            onDirectionSelected = onDirectionSelected,
            onIntent = onIntent,
            creditNoteNumber = data.creditNoteNumber,
            issueDate = data.issueDate,
            originalInvoiceNumber = data.originalInvoiceNumber,
        )
    }
}

@Composable
internal fun DocumentDetailsCard(
    data: DocumentUiData.Receipt,
    isReadOnly: Boolean = false,
    onIntent: (DocumentReviewIntent) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        MicroLabel(text = stringResource(Res.string.cashflow_receipt_details_section))
        ReceiptDetailsFactDisplay(
            receiptNumber = data.receiptNumber,
            date = data.date,
            isReadOnly = isReadOnly,
            onIntent = onIntent,
        )
    }
}

@Composable
internal fun DocumentDetailsCard(
    data: DocumentUiData.BankStatement,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        MicroLabel(text = stringResource(Res.string.cashflow_bank_statement_details_section))
        BankStatementDetailsFactDisplay(
            accountIban = data.accountIban,
            periodStart = data.periodStart,
            periodEnd = data.periodEnd,
            transactionCount = data.transactions.size,
        )
    }
}

@Composable
internal fun UnknownDocumentDetailsCard(
    state: DocumentReviewState,
    isAccountantReadOnly: Boolean,
    onIntent: (DocumentReviewIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        MicroLabel(text = stringResource(Res.string.cashflow_invoice_details_section))
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
            if (!isAccountantReadOnly) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.small),
                ) {
                    POutlinedButton(
                        text = stringResource(Res.string.document_type_invoice),
                        modifier = Modifier.weight(1f),
                        onClick = { onIntent(DocumentReviewIntent.SelectDocumentType(DocumentType.Invoice)) },
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = Constraints.Spacing.small),
                    horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.small),
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

@Composable
internal fun ClassifiedOnlyDetailsCard(
    documentType: DocumentType,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        MicroLabel(text = documentType.localized)
        Text(
            text = stringResource(Res.string.document_type_classified_placeholder),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// endregion

@Composable
private fun InvoiceDetailsFactDisplay(
    direction: DocumentDirection,
    isReadOnly: Boolean,
    onDirectionSelected: (DocumentDirection) -> Unit,
    onIntent: (DocumentReviewIntent) -> Unit,
    invoiceNumber: String?,
    issueDate: String?,
    dueDate: String?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.xSmall),
    ) {
        DirectionSelector(
            direction = direction,
            isReadOnly = isReadOnly,
            onDirectionSelected = onDirectionSelected
        )
        EditableFactField(
            label = stringResource(Res.string.cashflow_invoice_number),
            value = invoiceNumber,
            onValueChanged = { onIntent(DocumentReviewIntent.UpdateField(EditableField.InvoiceNumber, it)) },
            isReadOnly = isReadOnly,
        )
        EditableFactField(
            label = stringResource(Res.string.invoice_issue_date),
            value = issueDate,
            onValueChanged = { onIntent(DocumentReviewIntent.UpdateField(EditableField.IssueDate, it)) },
            isReadOnly = isReadOnly,
        )
        EditableFactField(
            label = stringResource(Res.string.invoice_due_date),
            value = dueDate,
            onValueChanged = { onIntent(DocumentReviewIntent.UpdateField(EditableField.DueDate, it)) },
            isReadOnly = isReadOnly,
        )
    }
}

@Composable
private fun ReceiptDetailsFactDisplay(
    receiptNumber: String?,
    date: String?,
    isReadOnly: Boolean = false,
    onIntent: (DocumentReviewIntent) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.xSmall),
    ) {
        EditableFactField(
            label = stringResource(Res.string.cashflow_receipt_number),
            value = receiptNumber,
            onValueChanged = { onIntent(DocumentReviewIntent.UpdateField(EditableField.ReceiptNumber, it)) },
            isReadOnly = isReadOnly,
        )
        EditableFactField(
            label = stringResource(Res.string.common_date),
            value = date,
            onValueChanged = { onIntent(DocumentReviewIntent.UpdateField(EditableField.ReceiptDate, it)) },
            isReadOnly = isReadOnly,
        )
    }
}

@Composable
private fun CreditNoteDetailsFactDisplay(
    direction: DocumentDirection,
    isReadOnly: Boolean,
    onDirectionSelected: (DocumentDirection) -> Unit,
    onIntent: (DocumentReviewIntent) -> Unit,
    creditNoteNumber: String?,
    issueDate: String?,
    originalInvoiceNumber: String?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.xSmall),
    ) {
        DirectionSelector(
            direction = direction,
            isReadOnly = isReadOnly,
            onDirectionSelected = onDirectionSelected
        )
        EditableFactField(
            label = stringResource(Res.string.cashflow_credit_note_number),
            value = creditNoteNumber,
            onValueChanged = { onIntent(DocumentReviewIntent.UpdateField(EditableField.CreditNoteNumber, it)) },
            isReadOnly = isReadOnly,
        )
        EditableFactField(
            label = stringResource(Res.string.invoice_issue_date),
            value = issueDate,
            onValueChanged = { onIntent(DocumentReviewIntent.UpdateField(EditableField.IssueDate, it)) },
            isReadOnly = isReadOnly,
        )
        if (!originalInvoiceNumber.isNullOrBlank()) {
            EditableFactField(
                label = stringResource(Res.string.cashflow_invoice_number),
                value = originalInvoiceNumber,
                onValueChanged = { onIntent(DocumentReviewIntent.UpdateField(EditableField.OriginalInvoiceNumber, it)) },
                isReadOnly = isReadOnly,
            )
        }
    }
}

@Composable
private fun BankStatementDetailsFactDisplay(
    accountIban: String?,
    periodStart: String?,
    periodEnd: String?,
    transactionCount: Int,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.xSmall),
    ) {
        accountIban?.let { iban ->
            FactField(
                label = stringResource(Res.string.workspace_iban),
                value = iban,
            )
        }
        formatPeriodRange(periodStart, periodEnd)?.let { range ->
            FactField(
                label = stringResource(Res.string.inspector_label_period),
                value = range,
            )
        }
        FactField(
            label = stringResource(Res.string.inspector_label_transactions),
            value = transactionCount.toString(),
        )
    }
}

private fun formatPeriodRange(start: String?, end: String?): String? = when {
    start != null && end != null -> "$start \u2013 $end"
    start != null -> start
    end != null -> end
    else -> null
}

@Composable
private fun DirectionSelector(
    direction: DocumentDirection,
    isReadOnly: Boolean,
    onDirectionSelected: (DocumentDirection) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Show toggle only when direction is Unknown and editable; otherwise read-only text
    if (isReadOnly || direction != DocumentDirection.Unknown) {
        FactField(
            label = stringResource(Res.string.cashflow_direction),
            value = cashflowDirectionLabel(direction),
            modifier = modifier,
        )
        return
    }

    val cashflowInSelected = direction == DocumentDirection.Outbound
    val cashflowOutSelected = direction == DocumentDirection.Inbound

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.xSmall)
    ) {
        MicroLabel(text = stringResource(Res.string.cashflow_direction))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(12.dp),
                )
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(12.dp),
                )
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DirectionChoice(
                text = stringResource(Res.string.cashflow_direction_in),
                selected = cashflowInSelected,
                onClick = { onDirectionSelected(DocumentDirection.Outbound) },
                modifier = Modifier.weight(1f),
            )
            DirectionChoice(
                text = stringResource(Res.string.cashflow_direction_out),
                selected = cashflowOutSelected,
                onClick = { onDirectionSelected(DocumentDirection.Inbound) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun DirectionChoice(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        textAlign = TextAlign.Center,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = FontWeight.Medium,
        color = if (selected) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.textMuted
        },
        modifier = modifier
            .background(
                color = if (selected) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.92f)
                } else {
                    MaterialTheme.colorScheme.surface.copy(alpha = 0f)
                },
                shape = RoundedCornerShape(9.dp),
            )
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = Constraints.Spacing.small),
    )
}

@Composable
private fun cashflowDirectionLabel(direction: DocumentDirection): String = when (direction) {
    DocumentDirection.Inbound -> stringResource(Res.string.cashflow_direction_out)
    DocumentDirection.Outbound -> stringResource(Res.string.cashflow_direction_in)
    DocumentDirection.Neutral,
    DocumentDirection.Unknown -> "—"
}
