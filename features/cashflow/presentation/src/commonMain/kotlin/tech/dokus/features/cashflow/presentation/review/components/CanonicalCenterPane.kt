package tech.dokus.features.cashflow.presentation.review.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.document_source_received_on
import tech.dokus.aura.resources.document_source_technical_details
import tech.dokus.domain.enums.DocumentSource
import tech.dokus.domain.enums.CashflowEntryStatus
import tech.dokus.features.cashflow.presentation.review.components.bankstatement.CanonicalBankStatementView
import tech.dokus.features.cashflow.presentation.review.components.comparison.DocumentComparisonPane
import tech.dokus.features.cashflow.presentation.review.models.DocumentUiData
import tech.dokus.features.cashflow.presentation.common.utils.formatShortDate
import tech.dokus.features.cashflow.presentation.review.DocumentPreviewState
import tech.dokus.features.cashflow.presentation.review.DocumentReviewIntent
import tech.dokus.features.cashflow.presentation.review.mvi.preview.DocumentPreviewIntent
import tech.dokus.features.cashflow.presentation.review.DocumentReviewState
import tech.dokus.features.cashflow.presentation.review.PdfPreviewPane
import tech.dokus.features.cashflow.presentation.review.SourceEvidenceViewerState
import tech.dokus.domain.model.contact.ResolvedContact
import tech.dokus.foundation.aura.components.DokusCardSurface
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.extensions.sourceViewerSubtitleLocalized
import tech.dokus.foundation.aura.style.textMuted
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

private val CanonicalPreviewWidth = Constraints.DocumentDetail.previewMaxWidth + 160.dp

@Composable
internal fun CanonicalCenterPane(
    state: DocumentReviewState,
    onIntent: (DocumentReviewIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.shouldUsePdfFallback) {
        PdfFallbackCenter(
            previewState = state.previewState,
            selectedFieldPath = state.selectedFieldPath,
            isProcessing = state.isProcessing,
            onLoadMore = { maxPages -> onIntent(DocumentReviewIntent.Preview(DocumentPreviewIntent.LoadMorePages(maxPages))) },
            modifier = modifier,
        )
        return
    }

    state.sourceViewerState?.let { viewerState ->
        SourceViewerCenter(
            contentState = state,
            viewerState = viewerState,
            onToggleTechnicalDetails = { onIntent(DocumentReviewIntent.Preview(DocumentPreviewIntent.ToggleSourceTechnicalDetails)) },
            onRetry = { onIntent(DocumentReviewIntent.Preview(DocumentPreviewIntent.OpenSourceModal(viewerState.sourceId))) },
            modifier = modifier,
        )
        return
    }

    val contact = state.effectiveContact
    val contactName = when (contact) {
        is ResolvedContact.Linked -> contact.name
        is ResolvedContact.Suggested -> contact.name
        is ResolvedContact.Detected -> contact.name
        is ResolvedContact.Unknown -> null
    }
    val contactAddress = (contact as? ResolvedContact.Detected)?.address
    val uiData = state.uiData

    // Side-by-side PDF comparison when pending match review exists
    val pendingReview = state.documentRecord?.pendingMatchReview
    if (state.shouldShowPendingMatchComparison && pendingReview != null) {
        DocumentComparisonPane(
            existingPreviewState = state.previewState,
            incomingPreviewState = state.incomingPreviewState,
            reasonType = pendingReview.reasonType,
            onSameDocument = { onIntent(DocumentReviewIntent.ResolvePossibleMatchSame) },
            onDifferentDocument = { onIntent(DocumentReviewIntent.ResolvePossibleMatchDifferent) },
            isResolving = state.isResolvingMatchReview,
            onLoadMore = { maxPages -> onIntent(DocumentReviewIntent.Preview(DocumentPreviewIntent.LoadMorePages(maxPages))) },
            modifier = modifier,
        )
        return
    }

    if (uiData is DocumentUiData.Invoice) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            CanonicalInvoiceDocumentCard(
                data = uiData,
                counterpartyName = contactName ?: state.documentRecord?.document?.filename ?: "",
                counterpartyAddress = contactAddress,
                modifier = Modifier
                    .width(CanonicalPreviewWidth)
                    .fillMaxHeight()
            )
        }
        return
    }

    if (uiData is DocumentUiData.BankStatement) {
        CanonicalBankStatementView(
            data = uiData,
            onToggleTransaction = { index ->
                onIntent(DocumentReviewIntent.ToggleBankStatementTransaction(index))
            },
            onReject = { onIntent(DocumentReviewIntent.ShowRejectDialog) },
            onConfirm = { onIntent(DocumentReviewIntent.Confirm) },
            isConfirming = state.isConfirming,
            isReadOnly = state.documentStatus == tech.dokus.domain.enums.DocumentStatus.Confirmed,
            modifier = modifier,
        )
        return
    }

    val creditNote = uiData as? DocumentUiData.CreditNote ?: return
    val currencySign = creditNote.currencySign
    val documentNumber = creditNote.creditNoteNumber ?: "\u2014"
    val totalAmount = creditNote.totalAmount?.toDisplayString() ?: "\u2014"

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        DokusCardSurface(
            modifier = Modifier
                .width(CanonicalPreviewWidth)
                .fillMaxHeight(),
            shape = MaterialTheme.shapes.small,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(
                        horizontal = Constraints.Spacing.xxxLarge,
                        vertical = Constraints.Spacing.xxxLarge
                    ),
                verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.medium),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = Constraints.Spacing.small),
                        verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.xSmall),
                    ) {
                        Text(
                            text = contactName ?: state.documentRecord?.document?.filename ?: "",
                            style = MaterialTheme.typography.displaySmall.copy(fontSize = 20.sp),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        contactAddress?.let { address ->
                            Text(
                                text = address,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.textMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    Text(
                        text = "CREDIT NOTE",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "$currencySign$totalAmount",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    val description = creditNote.reason ?: creditNote.notes
                    if (!description.isNullOrBlank()) {
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.textMuted,
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.large),
                ) {
                    CanonicalMetaCell("Issue", creditNote.issueDate ?: "\u2014")
                    CanonicalMetaCell("Credit note", documentNumber)
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                CanonicalLineItems(
                    lineItems = creditNote.lineItems,
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                CanonicalTotals(
                    currencySign = currencySign,
                    subtotal = creditNote.subtotalAmount?.toDisplayString(),
                    vat = creditNote.vatAmount?.toDisplayString(),
                    total = totalAmount,
                )

                val notes = creditNote.notes
                if (!notes.isNullOrBlank()) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Notes",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.textMuted,
                        )
                        Text(
                            text = notes,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PdfFallbackCenter(
    previewState: DocumentPreviewState,
    selectedFieldPath: String?,
    isProcessing: Boolean,
    onLoadMore: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    DokusCardSurface(
        modifier = modifier,
    ) {
        PdfPreviewPane(
            state = previewState,
            selectedFieldPath = selectedFieldPath,
            onLoadMore = onLoadMore,
            modifier = Modifier.fillMaxSize(),
            showScanAnimation = isProcessing
        )
    }
}

@Composable
private fun SourceViewerCenter(
    contentState: DocumentReviewState,
    viewerState: SourceEvidenceViewerState,
    onToggleTechnicalDetails: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pdfPreviewState = viewerState.previewState as? DocumentPreviewState.Ready
    if (pdfPreviewState != null && pdfPreviewState.pages.isNotEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter,
        ) {
            PdfPreviewPane(
                state = pdfPreviewState,
                selectedFieldPath = null,
                onLoadMore = {},
                modifier = Modifier
                    .width(CanonicalPreviewWidth)
                    .fillMaxHeight(),
                showScanAnimation = false,
            )
        }
        return
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        DokusCardSurface(
            modifier = Modifier
                .width(CanonicalPreviewWidth)
                .fillMaxHeight(),
            shape = MaterialTheme.shapes.small,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(Constraints.Spacing.medium),
                verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.small),
            ) {
                Text(
                    text = viewerState.sourceName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = viewerState.sourceType.sourceViewerSubtitleLocalized,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.textMuted,
                )
                viewerState.sourceReceivedAt?.let { receivedAt ->
                    Text(
                        text = stringResource(
                            Res.string.document_source_received_on,
                            formatShortDate(receivedAt.date),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.textMuted,
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                SourceEvidenceBody(
                    contentState = contentState,
                    viewerState = viewerState,
                    onRetry = onRetry,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                )

                if (viewerState.sourceType == DocumentSource.Peppol) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onToggleTechnicalDetails)
                            .padding(vertical = Constraints.Spacing.xSmall),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.xSmall),
                    ) {
                        Text(
                            text = if (viewerState.isTechnicalDetailsExpanded) "\u2304" else "\u203A",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.textMuted,
                        )
                        Text(
                            text = stringResource(Res.string.document_source_technical_details),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.textMuted,
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun CanonicalCenterPanePreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        CanonicalCenterPane(
            state = previewReviewContentState(entryStatus = CashflowEntryStatus.Open),
            onIntent = {},
            modifier = Modifier.fillMaxSize(),
        )
    }
}
