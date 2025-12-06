package ai.dokus.app.cashflow.components

import ai.dokus.app.resources.generated.Res
import ai.dokus.app.resources.generated.pending_documents_empty
import ai.dokus.app.resources.generated.pending_documents_need_confirmation
import ai.dokus.app.resources.generated.pending_documents_next
import ai.dokus.app.resources.generated.pending_documents_previous
import ai.dokus.app.resources.generated.pending_documents_title
import ai.dokus.foundation.design.extensions.localizedUppercase
import ai.dokus.foundation.domain.enums.MediaDocumentType
import ai.dokus.foundation.domain.model.MediaDto
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
 *
 * @param documents List of pending media documents to display on current page
 * @param isLoading Whether the documents are being loaded
 * @param hasPreviousPage Whether there's a previous page
 * @param hasNextPage Whether there's a next page
 * @param onDocumentClick Callback when a document row is clicked
 * @param onPreviousClick Callback when the previous arrow button is clicked
 * @param onNextClick Callback when the next arrow button is clicked
 * @param modifier Optional modifier for the card
 */
@Composable
fun PendingDocumentsCard(
    documents: List<MediaDto>,
    isLoading: Boolean,
    hasPreviousPage: Boolean,
    hasNextPage: Boolean,
    onDocumentClick: (MediaDto) -> Unit,
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
                .fillMaxWidth()
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
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                // Loading state
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (documents.isEmpty()) {
                // Empty state
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(Res.string.pending_documents_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                // Document items list with stable keys for efficient recomposition
                @OptIn(kotlin.uuid.ExperimentalUuidApi::class)
                documents.forEachIndexed { index, document ->
                    key(document.id.value) {
                        PendingDocumentItem(
                            document = document,
                            onClick = { onDocumentClick(document) }
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

                Spacer(modifier = Modifier.height(16.dp))

                // Navigation controls
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
        }
    }
}

/**
 * A single pending document item row displaying document name and "Need confirmation" badge.
 *
 * @param document The media document to display
 * @param onClick Callback when the row is clicked
 * @param modifier Optional modifier for the row
 */
@Composable
private fun PendingDocumentItem(
    document: MediaDto,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Determine document display name, cached per document
    val documentName = getDocumentDisplayName(document)

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
 * Uses MaterialTheme error colors for visual consistency.
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

/**
 * Maximum length for document name display to prevent layout issues.
 */
private const val MAX_DOCUMENT_NAME_LENGTH = 20

/**
 * Get display name for a pending document.
 * Uses extracted invoice number if available, otherwise falls back to filename.
 * Result is remembered to avoid recomputing on every recomposition.
 */
@OptIn(kotlin.uuid.ExperimentalUuidApi::class)
@Composable
private fun getDocumentDisplayName(document: MediaDto): String {
    val extraction = document.extraction

    // Try to get invoice number from extraction
    val invoiceNumber = extraction?.invoice?.invoiceNumber
        ?: extraction?.bill?.invoiceNumber

    // Get localized type prefix
    val typePrefix = remember(extraction?.documentType) {
        extraction?.documentType ?: if (invoiceNumber != null) {
            MediaDocumentType.Invoice
        } else {
            MediaDocumentType.Unknown
        }
    }.localizedUppercase

    return remember(document.id, typePrefix) {
        if (!invoiceNumber.isNullOrBlank()) {
            "$typePrefix $invoiceNumber"
        } else {
            // Fall back to filename without extension
            val nameWithoutExtension = document.filename
                .substringBeforeLast(".")
                .uppercase()
                .take(MAX_DOCUMENT_NAME_LENGTH)
            "$typePrefix $nameWithoutExtension"
        }
    }
}
