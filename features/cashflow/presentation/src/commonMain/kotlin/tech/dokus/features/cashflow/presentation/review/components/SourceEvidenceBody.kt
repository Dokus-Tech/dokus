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
import androidx.compose.material3.Button
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
import tech.dokus.domain.model.CreditNoteDraftData
import tech.dokus.domain.model.InvoiceDraftData
import tech.dokus.features.cashflow.presentation.common.utils.formatShortDate
import tech.dokus.features.cashflow.presentation.review.DocumentPreviewState
import tech.dokus.features.cashflow.presentation.review.DocumentReviewState
import tech.dokus.features.cashflow.presentation.review.PdfPreviewPane
import tech.dokus.features.cashflow.presentation.review.SourceEvidenceViewerState
import tech.dokus.foundation.aura.components.common.DokusLoader
import tech.dokus.foundation.aura.extensions.localized
import tech.dokus.foundation.aura.extensions.sourceViewerSubtitleLocalized
import tech.dokus.foundation.aura.style.textMuted

@Composable
internal fun SourceEvidenceBody(
    contentState: DocumentReviewState.Content,
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
    contentState: DocumentReviewState.Content,
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
    contentState: DocumentReviewState.Content,
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
                StructuredValue("Vendor", contentState.document.draft?.counterpartySnapshot?.name ?: "\u2014")
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
                StructuredValue("Counterparty", draft.counterpartyName ?: "\u2014")
                StructuredValue("Credit note", draft.creditNoteNumber ?: "\u2014")
                StructuredValue("Date", draft.issueDate?.let { formatShortDate(it) } ?: "\u2014")
                StructuredValue("Total", draft.totalAmount?.toDisplayString() ?: "\u2014", emphasized = true)
            }

            else -> {
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
                Button(onClick = onRetry, modifier = Modifier.padding(top = 8.dp)) {
                    Text(stringResource(Res.string.upload_action_retry))
                }
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
