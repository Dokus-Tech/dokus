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
import androidx.compose.ui.text.style.TextAlign
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
import tech.dokus.features.cashflow.presentation.review.models.DocumentUiData
import tech.dokus.features.cashflow.presentation.review.models.LineItemUiData
import tech.dokus.features.cashflow.presentation.common.utils.formatShortDate
import tech.dokus.features.cashflow.presentation.review.DocumentPreviewState
import tech.dokus.features.cashflow.presentation.review.DocumentReviewIntent
import tech.dokus.features.cashflow.presentation.review.DocumentReviewState
import tech.dokus.features.cashflow.presentation.review.PdfPreviewPane
import tech.dokus.features.cashflow.presentation.review.SourceEvidenceViewerState
import tech.dokus.features.cashflow.presentation.review.models.counterpartyInfo
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
            onLoadMore = { maxPages -> onIntent(DocumentReviewIntent.LoadMorePages(maxPages)) },
            modifier = modifier,
        )
        return
    }

    state.sourceViewerState?.let { viewerState ->
        SourceViewerCenter(
            contentState = state,
            viewerState = viewerState,
            onToggleTechnicalDetails = { onIntent(DocumentReviewIntent.ToggleSourceTechnicalDetails) },
            onRetry = { onIntent(DocumentReviewIntent.OpenSourceModal(viewerState.sourceId)) },
            modifier = modifier,
        )
        return
    }

    val counterparty = counterpartyInfo(state)
    val uiData = state.uiData

    if (uiData is DocumentUiData.Invoice) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            CanonicalInvoiceDocumentCard(
                data = uiData,
                counterpartyName = counterparty.name ?: state.documentRecord?.document?.filename ?: "",
                counterpartyAddress = counterparty.address?.formatted,
                modifier = Modifier
                    .width(CanonicalPreviewWidth)
                    .fillMaxHeight()
            )
        }
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
                            text = counterparty.name ?: state.documentRecord?.document?.filename ?: "",
                            style = MaterialTheme.typography.displaySmall.copy(fontSize = 20.sp),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        counterparty.address?.formatted?.let { address ->
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

@Composable
private fun CanonicalMetaCell(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.textMuted,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun CanonicalLineItems(
    lineItems: List<LineItemUiData>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.small),
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Description",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.textMuted,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "Amount",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.textMuted,
                textAlign = TextAlign.End,
                modifier = Modifier.width(96.dp),
            )
        }

        if (lineItems.isEmpty()) {
            Text(
                text = "No line items",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.textMuted,
            )
        } else {
            lineItems.forEach { item ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = item.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = item.displayAmount,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.End,
                        modifier = Modifier.width(96.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun CanonicalTotals(
    currencySign: String,
    subtotal: String?,
    vat: String?,
    total: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        CanonicalTotalRow("Subtotal", subtotal, currencySign)
        CanonicalTotalRow("VAT", vat, currencySign)
        HorizontalDivider(
            modifier = Modifier.width(220.dp),
            color = MaterialTheme.colorScheme.onSurface
        )
        Row(
            modifier = Modifier.width(220.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Total",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "$currencySign$total",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun CanonicalTotalRow(
    label: String,
    value: String?,
    currencySign: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.width(220.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.textMuted,
        )
        Text(
            text = if (value != null) "$currencySign$value" else "\u2014",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
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
