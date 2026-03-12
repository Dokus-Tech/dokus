package tech.dokus.features.cashflow.presentation.review.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.cashflow_no_preview
import tech.dokus.aura.resources.document_source_technical_details
import tech.dokus.aura.resources.upload_action_retry
import tech.dokus.domain.Money
import tech.dokus.domain.enums.DocumentSource
import tech.dokus.domain.model.AnnualAccountsDraftData
import tech.dokus.domain.model.BankFeeDraftData
import tech.dokus.domain.model.BankStatementDraftData
import tech.dokus.domain.model.BoardMinutesDraftData
import tech.dokus.domain.model.C4DraftData
import tech.dokus.domain.model.CompanyExtractDraftData
import tech.dokus.domain.model.ContractDraftData
import tech.dokus.domain.model.CorporateTaxAdvanceDraftData
import tech.dokus.domain.model.CorporateTaxDraftData
import tech.dokus.domain.model.CreditNoteDraftData
import tech.dokus.domain.model.CustomsDeclarationDraftData
import tech.dokus.domain.model.DeliveryNoteDraftData
import tech.dokus.domain.model.DepreciationScheduleDraftData
import tech.dokus.domain.model.DimonaDraftData
import tech.dokus.domain.model.DividendDraftData
import tech.dokus.domain.model.EmploymentContractDraftData
import tech.dokus.domain.model.ExpenseClaimDraftData
import tech.dokus.domain.model.FineDraftData
import tech.dokus.domain.model.HolidayPayDraftData
import tech.dokus.domain.model.IcListingDraftData
import tech.dokus.domain.model.InsuranceDraftData
import tech.dokus.domain.model.InterestStatementDraftData
import tech.dokus.domain.model.IntrastatDraftData
import tech.dokus.domain.model.InventoryDraftData
import tech.dokus.domain.model.InvoiceDraftData
import tech.dokus.domain.model.LeaseDraftData
import tech.dokus.domain.model.LoanDraftData
import tech.dokus.domain.model.OrderConfirmationDraftData
import tech.dokus.domain.model.OssReturnDraftData
import tech.dokus.domain.model.OtherDraftData
import tech.dokus.domain.model.PaymentConfirmationDraftData
import tech.dokus.domain.model.PayrollSummaryDraftData
import tech.dokus.domain.model.PermitDraftData
import tech.dokus.domain.model.PersonalTaxDraftData
import tech.dokus.domain.model.ProFormaDraftData
import tech.dokus.domain.model.PurchaseOrderDraftData
import tech.dokus.domain.model.QuoteDraftData
import tech.dokus.domain.model.ReceiptDraftData
import tech.dokus.domain.model.ReminderDraftData
import tech.dokus.domain.model.SalarySlipDraftData
import tech.dokus.domain.model.SelfEmployedContributionDraftData
import tech.dokus.domain.model.ShareholderRegisterDraftData
import tech.dokus.domain.model.SocialContributionDraftData
import tech.dokus.domain.model.SocialFundDraftData
import tech.dokus.domain.model.StatementOfAccountDraftData
import tech.dokus.domain.model.SubsidyDraftData
import tech.dokus.domain.model.TaxAssessmentDraftData
import tech.dokus.domain.model.VapzDraftData
import tech.dokus.domain.model.VatAssessmentDraftData
import tech.dokus.domain.model.VatListingDraftData
import tech.dokus.domain.model.VatReturnDraftData
import tech.dokus.domain.model.WithholdingTaxDraftData
import tech.dokus.domain.model.resolvedCounterpartyName
import tech.dokus.features.cashflow.presentation.common.utils.formatShortDate
import tech.dokus.features.cashflow.presentation.review.DocumentPreviewState
import tech.dokus.features.cashflow.presentation.review.DocumentReviewState
import tech.dokus.features.cashflow.presentation.review.PdfPreviewPane
import tech.dokus.features.cashflow.presentation.review.SourceEvidenceViewerState
import tech.dokus.foundation.aura.components.PButton
import tech.dokus.foundation.aura.components.common.DokusLoader
import tech.dokus.foundation.aura.extensions.localized
import tech.dokus.foundation.aura.extensions.sourceViewerSubtitleLocalized
import tech.dokus.foundation.aura.style.textMuted

@Composable
internal fun SourceEvidenceBody(
    contentState: DocumentReviewState,
    viewerState: SourceEvidenceViewerState,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        viewerState.isTechnicalDetailsExpanded -> SourceTechnicalDetails(
            viewerState = viewerState,
            onRetry = onRetry,
            modifier = modifier,
        )

        else -> SourceCanonicalOrPdfPreview(
            contentState = contentState,
            viewerState = viewerState,
            modifier = modifier,
        )
    }
}

@Composable
private fun SourceCanonicalOrPdfPreview(
    contentState: DocumentReviewState,
    viewerState: SourceEvidenceViewerState,
    modifier: Modifier = Modifier,
) {
    when (val previewState = viewerState.previewState) {
        is DocumentPreviewState.Ready -> {
            if (previewState.pages.isEmpty()) {
                SourceEmptyPreview(modifier = modifier)
            } else {
                PdfPreviewPane(
                    state = previewState,
                    selectedFieldPath = null,
                    onLoadMore = {},
                    modifier = modifier.fillMaxSize(),
                    showScanAnimation = false,
                )
            }
        }

        is DocumentPreviewState.Error -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = previewState.exception.localized,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }

        DocumentPreviewState.Loading -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                DokusLoader()
            }
        }

        DocumentPreviewState.NoPreview,
        DocumentPreviewState.NotPdf -> {
            SourceStructuredEvidence(
                contentState = contentState,
                sourceType = viewerState.sourceType,
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun SourceEmptyPreview(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = stringResource(Res.string.cashflow_no_preview),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.textMuted,
        )
    }
}

@Composable
private fun SourceStructuredEvidence(
    contentState: DocumentReviewState,
    sourceType: DocumentSource,
    modifier: Modifier = Modifier,
) {
    val draft = contentState.draftData
    val scroll = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scroll),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = sourceType.sourceViewerSubtitleLocalized.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.textMuted,
        )

        when (draft) {
            is InvoiceDraftData -> {
                StructuredValue("Vendor", contentState.documentRecord?.draft?.counterpartyDisplayName ?: "\u2014")
                StructuredValue("Invoice", draft.invoiceNumber ?: "\u2014")
                StructuredValue("Date", draft.issueDate?.let { formatShortDate(it) } ?: "\u2014")
                StructuredValue("Due", draft.dueDate?.let { formatShortDate(it) } ?: "\u2014")
                StructuredValue("Total", draft.totalAmount?.toDisplayString() ?: "\u2014", emphasized = true)

                if (draft.lineItems.isNotEmpty()) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Text(
                        text = "LINE ITEMS",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.textMuted,
                    )
                    draft.lineItems.forEach { item ->
                        StructuredValue(
                            label = item.description.ifBlank { "\u2014" },
                            value = item.netAmount?.let { Money(it).toDisplayString() } ?: "\u2014",
                        )
                    }
                }
            }

            is CreditNoteDraftData -> {
                StructuredValue("Counterparty", draft.resolvedCounterpartyName ?: "\u2014")
                StructuredValue("Credit note", draft.creditNoteNumber ?: "\u2014")
                StructuredValue("Date", draft.issueDate?.let { formatShortDate(it) } ?: "\u2014")
                StructuredValue("Total", draft.totalAmount?.toDisplayString() ?: "\u2014", emphasized = true)
            }

            is ReceiptDraftData,
            is BankStatementDraftData,
            is ProFormaDraftData,
            is QuoteDraftData,
            is OrderConfirmationDraftData,
            is DeliveryNoteDraftData,
            is ReminderDraftData,
            is StatementOfAccountDraftData,
            is PurchaseOrderDraftData,
            is ExpenseClaimDraftData,
            is BankFeeDraftData,
            is InterestStatementDraftData,
            is PaymentConfirmationDraftData,
            is VatReturnDraftData,
            is VatListingDraftData,
            is VatAssessmentDraftData,
            is IcListingDraftData,
            is OssReturnDraftData,
            is CorporateTaxDraftData,
            is CorporateTaxAdvanceDraftData,
            is TaxAssessmentDraftData,
            is PersonalTaxDraftData,
            is WithholdingTaxDraftData,
            is SocialContributionDraftData,
            is SocialFundDraftData,
            is SelfEmployedContributionDraftData,
            is VapzDraftData,
            is SalarySlipDraftData,
            is PayrollSummaryDraftData,
            is EmploymentContractDraftData,
            is DimonaDraftData,
            is C4DraftData,
            is HolidayPayDraftData,
            is ContractDraftData,
            is LeaseDraftData,
            is LoanDraftData,
            is InsuranceDraftData,
            is DividendDraftData,
            is ShareholderRegisterDraftData,
            is CompanyExtractDraftData,
            is AnnualAccountsDraftData,
            is BoardMinutesDraftData,
            is SubsidyDraftData,
            is FineDraftData,
            is PermitDraftData,
            is CustomsDeclarationDraftData,
            is IntrastatDraftData,
            is DepreciationScheduleDraftData,
            is InventoryDraftData,
            is OtherDraftData,
            null -> {
                Text(
                    text = stringResource(Res.string.cashflow_no_preview),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.textMuted,
                )
            }
        }
    }
}

@Composable
private fun StructuredValue(
    label: String,
    value: String,
    emphasized: Boolean = false,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.textMuted,
        )
        Text(
            text = value,
            style = if (emphasized) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodySmall,
            fontWeight = if (emphasized) FontWeight.SemiBold else FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun SourceTechnicalDetails(
    viewerState: SourceEvidenceViewerState,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        viewerState.isLoadingRawContent -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                DokusLoader()
            }
        }

        viewerState.rawContentError != null -> {
            Column(
                modifier = modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = viewerState.rawContentError.localized,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
                PButton(
                    text = stringResource(Res.string.upload_action_retry),
                    modifier = Modifier.padding(top = 8.dp),
                    onClick = onRetry,
                )
            }
        }

        else -> {
            Text(
                text = viewerState.rawContent ?: stringResource(Res.string.document_source_technical_details),
                modifier = modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
