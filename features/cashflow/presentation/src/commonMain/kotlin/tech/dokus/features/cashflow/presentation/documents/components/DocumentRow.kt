package tech.dokus.features.cashflow.presentation.documents.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import tech.dokus.domain.enums.DraftStatus
import tech.dokus.domain.enums.IngestionStatus
import tech.dokus.domain.model.DocumentRecordDto

/**
 * A row displaying a document in the documents list.
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

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = counterparty,
                    style = MaterialTheme.typography.titleMedium,
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

            Column(horizontalAlignment = Alignment.End) {
                if (amount != null) {
                    Text(
                        text = amount,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                DocumentStatusChip(status = status)
            }
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
