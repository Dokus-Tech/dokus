package tech.dokus.features.cashflow.presentation.review.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import tech.dokus.domain.enums.DocumentSource
import tech.dokus.domain.model.CreditNoteDraftData
import tech.dokus.domain.model.InvoiceDraftData
import tech.dokus.features.cashflow.presentation.review.DocumentPreviewState
import tech.dokus.features.cashflow.presentation.review.DocumentReviewState
import tech.dokus.features.cashflow.presentation.review.SourceEvidenceModalState
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.extensions.colorized
import tech.dokus.foundation.aura.extensions.localized
import tech.dokus.foundation.aura.extensions.localizedUppercase
import tech.dokus.foundation.aura.style.textMuted
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

@Composable
internal fun SourceEvidenceDialog(
    contentState: DocumentReviewState.Content,
    modalState: SourceEvidenceModalState,
    onClose: () -> Unit,
    onToggleRawView: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Dialog(onDismissRequest = onClose) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .heightIn(min = Constraints.DialogSize.maxWidth, max = Constraints.DialogSize.maxWidth * 1.35f),
            shape = MaterialTheme.shapes.medium,
            color = AlertDialogDefaults.containerColor,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(Constraints.Spacing.medium),
                verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.medium),
            ) {
                Header(
                    modalState = modalState,
                    onClose = onClose,
                    onToggleRawView = onToggleRawView,
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    if (modalState.showRawContent) {
                        RawContentPane(modalState = modalState, onRetry = onRetry)
                    } else {
                        SourcePreviewPane(
                            contentState = contentState,
                            modalState = modalState,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Header(
    modalState: SourceEvidenceModalState,
    onClose: () -> Unit,
    onToggleRawView: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = modalState.sourceType.localizedUppercase,
                style = MaterialTheme.typography.labelSmall,
                color = modalState.sourceType.colorized,
            )
            Text(
                text = modalState.sourceName,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (modalState.sourceType == DocumentSource.Peppol) {
                OutlinedButton(onClick = onToggleRawView) {
                    Text(if (modalState.showRawContent) "Show structured" else "Show raw XML")
                }
            }
            OutlinedButton(onClick = onClose) {
                Text("Close")
            }
        }
    }
}

@Composable
private fun SourcePreviewPane(
    contentState: DocumentReviewState.Content,
    modalState: SourceEvidenceModalState,
    modifier: Modifier = Modifier,
) {
    when (val previewState = modalState.previewState) {
        is DocumentPreviewState.Ready -> {
            // Reuse PDF pane for source-specific image URLs.
            tech.dokus.features.cashflow.presentation.review.PdfPreviewPane(
                state = previewState,
                selectedFieldPath = null,
                onLoadMore = {},
                modifier = modifier.fillMaxSize(),
                showScanAnimation = false,
            )
        }
        is DocumentPreviewState.Error -> {
            Column(
                modifier = modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = previewState.exception.localized,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
        DocumentPreviewState.Loading -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Loading preview\u2026",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.textMuted,
                )
            }
        }
        DocumentPreviewState.NoPreview,
        DocumentPreviewState.NotPdf -> {
            StructuredEvidencePane(contentState = contentState, modifier = modifier)
        }
    }
}

@Composable
private fun StructuredEvidencePane(
    contentState: DocumentReviewState.Content,
    modifier: Modifier = Modifier,
) {
    val draft = contentState.draftData
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.small),
    ) {
        Text(
            text = "Structured source data",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.textMuted,
        )
        when (draft) {
            is InvoiceDraftData -> {
                Value("Vendor", contentState.document.draft?.counterpartySnapshot?.name ?: "\u2014")
                Value("Invoice", draft.invoiceNumber ?: "\u2014")
                Value("Issue date", draft.issueDate?.toString() ?: "\u2014")
                Value("Due date", draft.dueDate?.toString() ?: "\u2014")
                Value("Subtotal", draft.subtotalAmount?.toDisplayString() ?: "\u2014")
                Value("VAT", draft.vatAmount?.toDisplayString() ?: "\u2014")
                Value("Total", draft.totalAmount?.toDisplayString() ?: "\u2014")
            }
            is CreditNoteDraftData -> {
                Value("Counterparty", draft.counterpartyName ?: "\u2014")
                Value("Credit note", draft.creditNoteNumber ?: "\u2014")
                Value("Issue date", draft.issueDate?.toString() ?: "\u2014")
                Value("Subtotal", draft.subtotalAmount?.toDisplayString() ?: "\u2014")
                Value("VAT", draft.vatAmount?.toDisplayString() ?: "\u2014")
                Value("Total", draft.totalAmount?.toDisplayString() ?: "\u2014")
            }
            else -> {
                Text(
                    text = "No structured data available",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.textMuted,
                )
            }
        }
    }
}

@Composable
private fun RawContentPane(
    modalState: SourceEvidenceModalState,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        modalState.isLoadingRawContent -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Loading raw XML\u2026",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.textMuted,
                )
            }
        }
        modalState.rawContentError != null -> {
            Column(
                modifier = modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = modalState.rawContentError.localized,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
                Button(onClick = onRetry, modifier = Modifier.padding(top = Constraints.Spacing.small)) {
                    Text("Retry")
                }
            }
        }
        else -> {
            Text(
                text = modalState.rawContent ?: "No raw content",
                modifier = modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun Value(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.textMuted,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Preview
@Composable
private fun SourceEvidenceDialogStructuredPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        SourceEvidenceDialog(
            contentState = previewReviewContentState(),
            modalState = previewSourceEvidenceModalState(
                sourceType = DocumentSource.Peppol,
                previewState = DocumentPreviewState.NotPdf,
            ),
            onClose = {},
            onToggleRawView = {},
            onRetry = {},
        )
    }
}

@Preview
@Composable
private fun SourceEvidenceDialogRawPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        SourceEvidenceDialog(
            contentState = previewReviewContentState(),
            modalState = previewSourceEvidenceModalState(
                sourceType = DocumentSource.Peppol,
                showRawContent = true,
                rawContent = "<Invoice>\n  <ID>INV-8847291</ID>\n</Invoice>",
            ),
            onClose = {},
            onToggleRawView = {},
            onRetry = {},
        )
    }
}
