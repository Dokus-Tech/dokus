package ai.dokus.app.cashflow.components

import ai.dokus.app.core.state.DokusState
import ai.dokus.app.resources.generated.Res
import ai.dokus.app.resources.generated.pending_documents_empty
import ai.dokus.app.resources.generated.pending_documents_need_confirmation
import ai.dokus.app.resources.generated.pending_documents_next
import ai.dokus.app.resources.generated.pending_documents_previous
import ai.dokus.app.resources.generated.pending_documents_title
import ai.dokus.foundation.design.components.common.DokusErrorContent
import ai.dokus.foundation.design.extensions.localizedUppercase
import ai.dokus.foundation.domain.enums.DocumentType
import ai.dokus.foundation.domain.model.DocumentProcessingDto
import ai.dokus.foundation.domain.model.common.PaginationState
import ai.dokus.foundation.domain.model.common.hasNextPage
import ai.dokus.foundation.domain.model.common.hasPreviousPage
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource

/**
 * A card component displaying pending documents that need confirmation.
 *
 * Based on the Figma design, this component shows:
 * - Title "Cash flow"
 * - List of pending documents with "Need confirmation" badge
 * - Pagination arrows
 * - Error state with retry button when loading fails
 *
 * @param state Full DokusState containing pagination data, loading, or error
 * @param onDocumentClick Callback when a document row is clicked
 * @param onPreviousClick Callback when the previous arrow button is clicked
 * @param onNextClick Callback when the next arrow button is clicked
 * @param modifier Optional modifier for the card
 */
@Composable
fun PendingDocumentsCard(
    state: DokusState<PaginationState<DocumentProcessingDto>>,
    onDocumentClick: (DocumentProcessingDto) -> Unit,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(24.dp)
        ) {
            // Title
            Text(
                text = stringResource(Res.string.pending_documents_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            when (state) {
                is DokusState.Loading, is DokusState.Idle -> {
                    // Loading/Idle state - expands to fill available space
                    PendingDocumentsLoadingContent(
                        modifier = Modifier.weight(1f)
                    )
                }

                is DokusState.Error -> {
                    // Error state - shows error message with retry button
                    PendingDocumentsErrorContent(
                        state = state,
                        modifier = Modifier.weight(1f)
                    )
                }

                is DokusState.Success -> {
                    val paginationState = state.data
                    if (paginationState.data.isEmpty()) {
                        // Empty state - expands to fill available space
                        PendingDocumentsEmptyContent(
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        // Document items list - expands to fill available space
                        PendingDocumentsListContent(
                            documents = paginationState.data,
                            onDocumentClick = onDocumentClick,
                            modifier = Modifier.weight(1f)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Navigation controls pinned at bottom
                        PaginationControls(
                            hasPreviousPage = paginationState.hasPreviousPage,
                            hasNextPage = paginationState.hasNextPage,
                            onPreviousClick = onPreviousClick,
                            onNextClick = onNextClick
                        )
                    }
                }
            }
        }
    }
}

/**
 * Loading state content with centered progress indicator.
 */
@Composable
private fun PendingDocumentsLoadingContent(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

/**
 * Empty state content with message.
 */
@Composable
private fun PendingDocumentsEmptyContent(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(Res.string.pending_documents_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Error state content with error message and retry button.
 */
@Composable
private fun PendingDocumentsErrorContent(
    state: DokusState.Error<*>,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        DokusErrorContent(
            exception = state.exception,
            retryHandler = state.retryHandler
        )
    }
}

/**
 * Content wrapper for the document list that can expand.
 */
@Composable
private fun PendingDocumentsListContent(
    documents: List<DocumentProcessingDto>,
    onDocumentClick: (DocumentProcessingDto) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        PendingDocumentsList(
            documents = documents,
            onDocumentClick = onDocumentClick
        )
    }
}

/**
 * List of pending document items with dividers.
 */
@Composable
private fun PendingDocumentsList(
    documents: List<DocumentProcessingDto>,
    onDocumentClick: (DocumentProcessingDto) -> Unit
) {
    documents.forEachIndexed { index, processing ->
        key(processing.id.toString()) {
            PendingDocumentItem(
                processing = processing,
                onClick = { onDocumentClick(processing) }
            )

            // Add divider between items (not after the last item)
            if (index < documents.size - 1) {
                Spacer(modifier = Modifier.height(12.dp))
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

/**
 * Pagination controls with previous and next buttons.
 */
@Composable
private fun PaginationControls(
    hasPreviousPage: Boolean,
    hasNextPage: Boolean,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Previous button
        FilledIconButton(
            onClick = onPreviousClick,
            enabled = hasPreviousPage,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                disabledContainerColor = MaterialTheme.colorScheme.surface,
                disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            ),
            modifier = Modifier
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(8.dp)
                )
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(Res.string.pending_documents_previous)
            )
        }

        // Next button
        FilledIconButton(
            onClick = onNextClick,
            enabled = hasNextPage,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                disabledContainerColor = MaterialTheme.colorScheme.surface,
                disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            ),
            modifier = Modifier
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(8.dp)
                )
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = stringResource(Res.string.pending_documents_next)
            )
        }
    }
}

/**
 * A single pending document item row displaying document name and "Need confirmation" badge.
 *
 * @param processing The document processing record to display
 * @param onClick Callback when the row is clicked
 * @param modifier Optional modifier for the row
 */
@Composable
private fun PendingDocumentItem(
    processing: DocumentProcessingDto,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val documentName = getDocumentDisplayName(processing)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Document name
        Text(
            text = documentName,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )

        // "Need confirmation" badge
        NeedConfirmationBadge()
    }
}

/**
 * Badge showing "Need confirmation" status.
 */
@Composable
private fun NeedConfirmationBadge(
    modifier: Modifier = Modifier
) {
    Text(
        text = stringResource(Res.string.pending_documents_need_confirmation),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.error,
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 12.dp, vertical = 4.dp)
    )
}

private const val MAX_DOCUMENT_NAME_LENGTH = 20

/**
 * Get display name for a pending document.
 * Uses extracted invoice/bill number if available, otherwise falls back to filename.
 */
@Composable
private fun getDocumentDisplayName(processing: DocumentProcessingDto): String {
    val filename = processing.document?.filename
    val extractedData = processing.extractedData

    // Try to get invoice/bill number from extracted data
    val documentNumber = extractedData?.invoice?.invoiceNumber
        ?: extractedData?.bill?.invoiceNumber

    // Get document type prefix (localizedUppercase is @Composable, call outside remember)
    val typePrefix = (processing.documentType ?: DocumentType.Unknown).localizedUppercase

    return remember(processing.id, typePrefix, documentNumber, filename) {
        when {
            !documentNumber.isNullOrBlank() -> {
                "$typePrefix $documentNumber"
            }
            !filename.isNullOrBlank() -> {
                val nameWithoutExtension = filename
                    .substringBeforeLast(".")
                    .uppercase()
                    .take(MAX_DOCUMENT_NAME_LENGTH)
                "$typePrefix $nameWithoutExtension"
            }
            else -> {
                // Fallback to document ID if no filename
                "$typePrefix ${processing.documentId.toString().take(8).uppercase()}"
            }
        }
    }
}
