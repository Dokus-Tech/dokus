package tech.dokus.features.cashflow.presentation.review.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.document_detail_confirmed
import tech.dokus.domain.enums.CashflowEntryStatus
import tech.dokus.domain.enums.DocumentStatus
import tech.dokus.features.cashflow.presentation.review.DocumentReviewIntent
import tech.dokus.features.cashflow.presentation.review.DocumentReviewState
import tech.dokus.features.cashflow.presentation.review.ReviewFinancialStatus
import tech.dokus.features.cashflow.presentation.review.compressedStatusDetailLocalized
import tech.dokus.features.cashflow.presentation.review.dotType
import tech.dokus.features.cashflow.presentation.review.statusBadgeLocalized
import tech.dokus.domain.enums.DocumentType
import tech.dokus.features.cashflow.presentation.review.components.details.ClassifiedOnlyDetailsCard
import tech.dokus.features.cashflow.presentation.review.components.details.CounterpartyCard
import tech.dokus.features.cashflow.presentation.review.components.details.DocumentDetailsCard
import tech.dokus.features.cashflow.presentation.review.components.details.UnknownDocumentDetailsCard
import tech.dokus.features.cashflow.presentation.review.models.DocumentUiData
import tech.dokus.foundation.aura.components.DokusCardSurface
import tech.dokus.foundation.aura.components.PButton
import tech.dokus.foundation.aura.components.PButtonVariant
import tech.dokus.foundation.aura.components.icons.LockIcon
import tech.dokus.foundation.aura.components.status.StatusDot
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.style.textMuted
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper
import tech.dokus.features.cashflow.presentation.review.colorized as financialStatusColorized

@Composable
internal fun ReviewInspectorPane(
    state: DocumentReviewState,
    isAccountantReadOnly: Boolean,
    onIntent: (DocumentReviewIntent) -> Unit,
    onCorrectContact: () -> Unit,
    onCreateContact: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
    ) {
        InspectorHeader(
            state = state,
            isAccountantReadOnly = isAccountantReadOnly,
            onIntent = onIntent,
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = Constraints.Spacing.medium,
                    vertical = Constraints.Spacing.medium,
                ),
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Constraints.Spacing.medium),
            verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.small),
        ) {
            val uiData = state.uiData
            when (uiData) {
                is DocumentUiData.Invoice -> InspectorBody(state, uiData, isAccountantReadOnly, onIntent, onCorrectContact, onCreateContact)
                is DocumentUiData.CreditNote -> InspectorBody(state, uiData, isAccountantReadOnly, onIntent, onCorrectContact, onCreateContact)
                is DocumentUiData.Receipt -> InspectorBody(state, uiData, isAccountantReadOnly, onIntent, onCorrectContact, onCreateContact)
                is DocumentUiData.BankStatement -> InspectorBody(state, uiData, isAccountantReadOnly, onIntent, onCorrectContact, onCreateContact)
                // --- Classified-only document types ---
                is DocumentUiData.ProForma -> ClassifiedOnlyInspectorBody(uiData.documentType)
                is DocumentUiData.Quote -> ClassifiedOnlyInspectorBody(uiData.documentType)
                is DocumentUiData.OrderConfirmation -> ClassifiedOnlyInspectorBody(uiData.documentType)
                is DocumentUiData.DeliveryNote -> ClassifiedOnlyInspectorBody(uiData.documentType)
                is DocumentUiData.Reminder -> ClassifiedOnlyInspectorBody(uiData.documentType)
                is DocumentUiData.StatementOfAccount -> ClassifiedOnlyInspectorBody(uiData.documentType)
                is DocumentUiData.PurchaseOrder -> ClassifiedOnlyInspectorBody(uiData.documentType)
                is DocumentUiData.ExpenseClaim -> ClassifiedOnlyInspectorBody(uiData.documentType)
                is DocumentUiData.BankFee -> ClassifiedOnlyInspectorBody(uiData.documentType)
                is DocumentUiData.InterestStatement -> ClassifiedOnlyInspectorBody(uiData.documentType)
                is DocumentUiData.PaymentConfirmation -> ClassifiedOnlyInspectorBody(uiData.documentType)
                is DocumentUiData.VatReturn -> ClassifiedOnlyInspectorBody(uiData.documentType)
                is DocumentUiData.VatListing -> ClassifiedOnlyInspectorBody(uiData.documentType)
                is DocumentUiData.VatAssessment -> ClassifiedOnlyInspectorBody(uiData.documentType)
                is DocumentUiData.IcListing -> ClassifiedOnlyInspectorBody(uiData.documentType)
                is DocumentUiData.OssReturn -> ClassifiedOnlyInspectorBody(uiData.documentType)
                is DocumentUiData.CorporateTax -> ClassifiedOnlyInspectorBody(uiData.documentType)
                is DocumentUiData.CorporateTaxAdvance -> ClassifiedOnlyInspectorBody(uiData.documentType)
                is DocumentUiData.TaxAssessment -> ClassifiedOnlyInspectorBody(uiData.documentType)
                is DocumentUiData.PersonalTax -> ClassifiedOnlyInspectorBody(uiData.documentType)
                is DocumentUiData.WithholdingTax -> ClassifiedOnlyInspectorBody(uiData.documentType)
                is DocumentUiData.SocialContribution -> ClassifiedOnlyInspectorBody(uiData.documentType)
                is DocumentUiData.SocialFund -> ClassifiedOnlyInspectorBody(uiData.documentType)
                is DocumentUiData.SelfEmployedContribution -> ClassifiedOnlyInspectorBody(uiData.documentType)
                is DocumentUiData.Vapz -> ClassifiedOnlyInspectorBody(uiData.documentType)
                is DocumentUiData.SalarySlip -> ClassifiedOnlyInspectorBody(uiData.documentType)
                is DocumentUiData.PayrollSummary -> ClassifiedOnlyInspectorBody(uiData.documentType)
                is DocumentUiData.EmploymentContract -> ClassifiedOnlyInspectorBody(uiData.documentType)
                is DocumentUiData.Dimona -> ClassifiedOnlyInspectorBody(uiData.documentType)
                is DocumentUiData.C4 -> ClassifiedOnlyInspectorBody(uiData.documentType)
                is DocumentUiData.HolidayPay -> ClassifiedOnlyInspectorBody(uiData.documentType)
                is DocumentUiData.Contract -> ClassifiedOnlyInspectorBody(uiData.documentType)
                is DocumentUiData.Lease -> ClassifiedOnlyInspectorBody(uiData.documentType)
                is DocumentUiData.Loan -> ClassifiedOnlyInspectorBody(uiData.documentType)
                is DocumentUiData.Insurance -> ClassifiedOnlyInspectorBody(uiData.documentType)
                is DocumentUiData.Dividend -> ClassifiedOnlyInspectorBody(uiData.documentType)
                is DocumentUiData.ShareholderRegister -> ClassifiedOnlyInspectorBody(uiData.documentType)
                is DocumentUiData.CompanyExtract -> ClassifiedOnlyInspectorBody(uiData.documentType)
                is DocumentUiData.AnnualAccounts -> ClassifiedOnlyInspectorBody(uiData.documentType)
                is DocumentUiData.BoardMinutes -> ClassifiedOnlyInspectorBody(uiData.documentType)
                is DocumentUiData.Subsidy -> ClassifiedOnlyInspectorBody(uiData.documentType)
                is DocumentUiData.Fine -> ClassifiedOnlyInspectorBody(uiData.documentType)
                is DocumentUiData.Permit -> ClassifiedOnlyInspectorBody(uiData.documentType)
                is DocumentUiData.CustomsDeclaration -> ClassifiedOnlyInspectorBody(uiData.documentType)
                is DocumentUiData.Intrastat -> ClassifiedOnlyInspectorBody(uiData.documentType)
                is DocumentUiData.DepreciationSchedule -> ClassifiedOnlyInspectorBody(uiData.documentType)
                is DocumentUiData.Inventory -> ClassifiedOnlyInspectorBody(uiData.documentType)
                is DocumentUiData.Other -> ClassifiedOnlyInspectorBody(uiData.documentType)
                null -> InspectorBody(state, isAccountantReadOnly, onIntent, onCorrectContact, onCreateContact)
            }
            InspectorSourcesSection(
                state = state,
                isAccountantReadOnly = isAccountantReadOnly,
                onIntent = onIntent,
                showSourceList = false,
            )
        }

    }
}

@Composable
private fun InspectorFactGroupCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    DokusCardSurface(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Constraints.Spacing.medium),
            verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.small),
        ) {
            content()
        }
    }
}

// region InspectorBody overloads

@Composable
private fun InspectorBody(
    state: DocumentReviewState,
    data: DocumentUiData.Invoice,
    isAccountantReadOnly: Boolean,
    onIntent: (DocumentReviewIntent) -> Unit,
    onCorrectContact: () -> Unit,
    onCreateContact: () -> Unit,
) {
    FinancialDocumentBody(
        state = state,
        isAccountantReadOnly = isAccountantReadOnly,
        onIntent = onIntent,
        onCorrectContact = onCorrectContact,
        onCreateContact = onCreateContact,
    ) {
        DocumentDetailsCard(
            data = data,
            isReadOnly = isAccountantReadOnly || state.isDocumentConfirmed || state.isDocumentRejected,
            onDirectionSelected = { onIntent(DocumentReviewIntent.SelectDirection(it)) },
        )
    }
    InspectorAmountSection(
        total = data.totalAmount,
        subtotal = data.subtotalAmount,
        vat = data.vatAmount,
        currencySign = data.currencySign,
        financialStatus = state.financialStatus,
    )
    InspectorPaymentSection(state = state, isAccountantReadOnly = isAccountantReadOnly, onIntent = onIntent)
}

@Composable
private fun InspectorBody(
    state: DocumentReviewState,
    data: DocumentUiData.CreditNote,
    isAccountantReadOnly: Boolean,
    onIntent: (DocumentReviewIntent) -> Unit,
    onCorrectContact: () -> Unit,
    onCreateContact: () -> Unit,
) {
    FinancialDocumentBody(
        state = state,
        isAccountantReadOnly = isAccountantReadOnly,
        onIntent = onIntent,
        onCorrectContact = onCorrectContact,
        onCreateContact = onCreateContact,
    ) {
        DocumentDetailsCard(
            data = data,
            isReadOnly = isAccountantReadOnly || state.isDocumentConfirmed || state.isDocumentRejected,
            onDirectionSelected = { onIntent(DocumentReviewIntent.SelectDirection(it)) },
        )
    }
    InspectorAmountSection(
        total = data.totalAmount,
        subtotal = data.subtotalAmount,
        vat = data.vatAmount,
        currencySign = data.currencySign,
        financialStatus = state.financialStatus,
    )
    InspectorPaymentSection(state = state, isAccountantReadOnly = isAccountantReadOnly, onIntent = onIntent)
}

@Composable
private fun InspectorBody(
    state: DocumentReviewState,
    data: DocumentUiData.Receipt,
    isAccountantReadOnly: Boolean,
    onIntent: (DocumentReviewIntent) -> Unit,
    onCorrectContact: () -> Unit,
    onCreateContact: () -> Unit,
) {
    FinancialDocumentBody(
        state = state,
        isAccountantReadOnly = isAccountantReadOnly,
        onIntent = onIntent,
        onCorrectContact = onCorrectContact,
        onCreateContact = onCreateContact,
    ) {
        DocumentDetailsCard(data = data)
    }
    InspectorAmountSection(
        total = data.totalAmount,
        subtotal = null,
        vat = data.vatAmount,
        currencySign = data.currencySign,
        financialStatus = state.financialStatus,
    )
    InspectorPaymentSection(state = state, isAccountantReadOnly = isAccountantReadOnly, onIntent = onIntent)
}

@Composable
private fun InspectorBody(
    state: DocumentReviewState,
    data: DocumentUiData.BankStatement,
    isAccountantReadOnly: Boolean,
    onIntent: (DocumentReviewIntent) -> Unit,
    onCorrectContact: () -> Unit,
    onCreateContact: () -> Unit,
) {
    InspectorFactGroupCard {
        CounterpartyCard(
            state = state,
            onIntent = onIntent,
            onCorrectContact = onCorrectContact,
            onCreateContact = onCreateContact,
        )
    }
    InspectorFactGroupCard {
        DocumentDetailsCard(data = data)
    }
}

@Composable
private fun ClassifiedOnlyInspectorBody(documentType: DocumentType) {
    InspectorFactGroupCard {
        ClassifiedOnlyDetailsCard(documentType = documentType)
    }
}

@Composable
private fun InspectorBody(
    state: DocumentReviewState,
    isAccountantReadOnly: Boolean,
    onIntent: (DocumentReviewIntent) -> Unit,
    onCorrectContact: () -> Unit,
    onCreateContact: () -> Unit,
) {
    InspectorFactGroupCard {
        CounterpartyCard(
            state = state,
            onIntent = onIntent,
            onCorrectContact = onCorrectContact,
            onCreateContact = onCreateContact,
        )
    }
    InspectorFactGroupCard {
        UnknownDocumentDetailsCard(
            state = state,
            isAccountantReadOnly = isAccountantReadOnly,
            onIntent = onIntent,
        )
    }
}

@Composable
private fun FinancialDocumentBody(
    state: DocumentReviewState,
    isAccountantReadOnly: Boolean,
    onIntent: (DocumentReviewIntent) -> Unit,
    onCorrectContact: () -> Unit,
    onCreateContact: () -> Unit,
    detailsCard: @Composable () -> Unit,
) {
    InspectorFactGroupCard {
        CounterpartyCard(
            state = state,
            onIntent = onIntent,
            onCorrectContact = onCorrectContact,
            onCreateContact = onCreateContact,
        )
    }
    InspectorFactGroupCard {
        detailsCard()
    }
}

// endregion

@Composable
private fun InspectorHeader(
    state: DocumentReviewState,
    isAccountantReadOnly: Boolean,
    onIntent: (DocumentReviewIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.small),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CompressedStatusLine(state, isAccountantReadOnly, onIntent)
        }

        val supportsManualConfirm = when (state.uiData) {
            is DocumentUiData.Invoice,
            is DocumentUiData.CreditNote,
            is DocumentUiData.Receipt -> true
            else -> false
        }
        if (supportsManualConfirm && !isAccountantReadOnly &&
            !state.isDocumentConfirmed && !state.isDocumentRejected && !state.isDocumentUnsupported &&
            state.financialStatus == ReviewFinancialStatus.Review
        ) {
            PButton(
                text = "Confirm document",
                isEnabled = state.canConfirm,
                modifier = Modifier.fillMaxWidth(),
                onClick = { onIntent(DocumentReviewIntent.Confirm) },
            )
        } else if (supportsManualConfirm && !isAccountantReadOnly && state.canRecordPayment) {
            PButton(
                text = "Record payment",
                variant = PButtonVariant.Outline,
                modifier = Modifier.fillMaxWidth(),
                onClick = { onIntent(DocumentReviewIntent.OpenPaymentSheet) },
            )
        }
    }
}

@Composable
private fun CompressedStatusLine(
    state: DocumentReviewState,
    isAccountantReadOnly: Boolean = false,
    onIntent: (DocumentReviewIntent) -> Unit = {},
) {
    val statusColor = state.financialStatus.financialStatusColorized
    val detailText = state.compressedStatusDetailLocalized

    if (state.financialStatus == ReviewFinancialStatus.Review && !state.isDocumentConfirmed && !state.isDocumentUnsupported) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.xSmall),
        ) {
            StatusDot(type = state.financialStatus.dotType, size = 6.dp)
            Text(
                text = state.statusBadgeLocalized,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = statusColor,
            )
        }
        return
    }

    val canUnconfirm = !isAccountantReadOnly && state.isDocumentConfirmed && !state.isConfirming

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.xSmall),
        modifier = if (canUnconfirm) {
            Modifier.clickable { onIntent(DocumentReviewIntent.RequestUnconfirm) }
        } else {
            Modifier
        },
    ) {
        LockIcon(modifier = Modifier, tint = MaterialTheme.colorScheme.textMuted)
        Text(
            text = stringResource(Res.string.document_detail_confirmed),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = "\u00b7",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.textMuted,
        )
        StatusDot(type = state.financialStatus.dotType, size = 6.dp)
        Text(
            text = detailText,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = statusColor,
        )
    }
}

@Preview
@Composable
private fun ReviewInspectorPanePaidPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters,
) {
    TestWrapper(parameters) {
        ReviewInspectorPane(
            state = previewReviewContentState(entryStatus = CashflowEntryStatus.Paid),
            isAccountantReadOnly = false,
            onIntent = {},
            onCorrectContact = {},
            onCreateContact = {},
        )
    }
}

@Preview
@Composable
private fun ReviewInspectorPaneAutoPaidPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters,
) {
    TestWrapper(parameters) {
        ReviewInspectorPane(
            state = previewReviewContentState(
                entryStatus = CashflowEntryStatus.Paid,
                autoPaymentStatus = previewAutoPaymentStatus(canUndo = true),
            ),
            isAccountantReadOnly = false,
            onIntent = {},
            onCorrectContact = {},
            onCreateContact = {},
        )
    }
}

@Preview
@Composable
private fun ReviewInspectorPaneUnpaidPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters,
) {
    TestWrapper(parameters) {
        ReviewInspectorPane(
            state = previewReviewContentState(entryStatus = CashflowEntryStatus.Open),
            isAccountantReadOnly = false,
            onIntent = {},
            onCorrectContact = {},
            onCreateContact = {},
        )
    }
}

@Preview
@Composable
private fun ReviewInspectorPaneOverduePreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters,
) {
    TestWrapper(parameters) {
        ReviewInspectorPane(
            state = previewReviewContentState(entryStatus = CashflowEntryStatus.Overdue),
            isAccountantReadOnly = false,
            onIntent = {},
            onCorrectContact = {},
            onCreateContact = {},
        )
    }
}

@Preview
@Composable
private fun ReviewInspectorPaneReviewPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters,
) {
    TestWrapper(parameters) {
        ReviewInspectorPane(
            state = previewReviewContentState(entryStatus = null, documentStatus = DocumentStatus.NeedsReview),
            isAccountantReadOnly = false,
            onIntent = {},
            onCorrectContact = {},
            onCreateContact = {},
        )
    }
}
