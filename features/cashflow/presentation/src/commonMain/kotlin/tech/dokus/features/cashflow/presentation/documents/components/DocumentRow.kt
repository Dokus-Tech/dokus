package tech.dokus.features.cashflow.presentation.documents.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.documents_view_details
import tech.dokus.domain.enums.DraftStatus
import tech.dokus.domain.enums.IngestionStatus
import tech.dokus.domain.model.DocumentRecordDto

/**
 * A table-style row displaying a document in the documents list.
 * Uses flat design with subtle hover states, matching Firstbase styling.
 */
@Composable
internal fun DocumentRow(
    document: DocumentRecordDto,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val counterparty = extractCounterparty(document)
    val documentType = document.draft?.documentType?.name ?: "Unclassified"
    val amount = extractAmount(document)
    val status = computeDisplayStatus(document)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Counterparty + Document type
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = counterparty,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = documentType,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Right: Amount + Status + Chevron
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Amount column
            Column(horizontalAlignment = Alignment.End) {
                if (amount != null) {
                    Text(
                        text = amount,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                DocumentStatusChip(status = status)
            }

            // Chevron
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = stringResource(Res.string.documents_view_details),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Extracts the counterparty name from the document.
 */
private fun extractCounterparty(document: DocumentRecordDto): String {
    val draft = document.draft
    val extractedData = draft?.extractedData

    return extractedData?.invoice?.clientName
        ?: extractedData?.bill?.supplierName
        ?: extractedData?.expense?.merchant
        ?: document.document.filename
        ?: "Unknown"
}

/**
 * Extracts the amount from the document.
 */
private fun extractAmount(document: DocumentRecordDto): String? {
    val extractedData = document.draft?.extractedData

    val amount = extractedData?.invoice?.totalAmount
        ?: extractedData?.bill?.amount
        ?: extractedData?.expense?.amount

    return amount?.let { "\u20AC${it.toDouble()}" }
}

/**
 * Display status derived from ingestion and draft status.
 */
enum class DocumentDisplayStatus {
    Processing,
    NeedsReview,
    Ready,
    Confirmed,
    Failed,
    Rejected
}

/**
 * Computes the display status from the document state.
 */
internal fun computeDisplayStatus(document: DocumentRecordDto): DocumentDisplayStatus {
    val ingestionStatus = document.latestIngestion?.status
    val draftStatus = document.draft?.draftStatus

    return when {
        ingestionStatus == IngestionStatus.Processing ||
            ingestionStatus == IngestionStatus.Queued -> DocumentDisplayStatus.Processing
        ingestionStatus == IngestionStatus.Failed -> DocumentDisplayStatus.Failed
        draftStatus == DraftStatus.Confirmed -> DocumentDisplayStatus.Confirmed
        draftStatus == DraftStatus.Rejected -> DocumentDisplayStatus.Rejected
        draftStatus == DraftStatus.Ready -> DocumentDisplayStatus.Ready
        draftStatus == DraftStatus.NeedsReview ||
            draftStatus == DraftStatus.NeedsInput -> DocumentDisplayStatus.NeedsReview
        else -> DocumentDisplayStatus.Processing
    }
}
