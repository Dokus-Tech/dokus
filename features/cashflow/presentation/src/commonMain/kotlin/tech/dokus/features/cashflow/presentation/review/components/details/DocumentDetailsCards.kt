package tech.dokus.features.cashflow.presentation.review.components.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.cashflow_bank_statement_details_section
import tech.dokus.aura.resources.cashflow_credit_note_details_section
import tech.dokus.aura.resources.cashflow_invoice_details_section
import tech.dokus.aura.resources.cashflow_processing_identifying_type
import tech.dokus.aura.resources.cashflow_receipt_details_section
import tech.dokus.aura.resources.cashflow_select_document_type
import tech.dokus.aura.resources.document_type_classified_placeholder
import tech.dokus.aura.resources.document_type_credit_note
import tech.dokus.aura.resources.document_type_invoice
import tech.dokus.aura.resources.document_type_receipt
import tech.dokus.domain.enums.DocumentDirection
import tech.dokus.domain.enums.DocumentType
import tech.dokus.foundation.aura.extensions.localized
import tech.dokus.features.cashflow.presentation.review.DocumentReviewIntent
import tech.dokus.features.cashflow.presentation.review.DocumentReviewState
import tech.dokus.features.cashflow.presentation.review.models.DocumentUiData
import tech.dokus.foundation.aura.components.POutlinedButton
import tech.dokus.foundation.aura.constrains.Constraints

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
