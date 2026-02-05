package tech.dokus.features.cashflow.presentation.model

import tech.dokus.domain.enums.DocumentStatus
import tech.dokus.domain.enums.IngestionStatus
import tech.dokus.domain.model.DocumentRecordDto
import tech.dokus.foundation.aura.model.DocumentUiStatus

/**
 * Maps a [DocumentRecordDto] to its UI-displayable status.
 *
 * Priority order (first matching wins):
 * 1. Failed: ingestion failed or has error message
 * 2. Queued: no ingestion or ingestion status is Queued
 * 3. Processing: ingestion status is Processing
 * 4. Ready: succeeded and draft is confirmed or otherwise ready
 * 5. Review: succeeded but needs user attention
 */
fun DocumentRecordDto.toUiStatus(): DocumentUiStatus {
    val ingestion = latestIngestion

    // No ingestion record yet - treat as queued
    if (ingestion == null) {
        return DocumentUiStatus.Queued
    }

    // Check for failure first (highest priority)
    if (ingestion.status == IngestionStatus.Failed || !ingestion.errorMessage.isNullOrBlank()) {
        return DocumentUiStatus.Failed
    }

    // Check processing states
    return when (ingestion.status) {
        IngestionStatus.Queued -> DocumentUiStatus.Queued
        IngestionStatus.Processing -> DocumentUiStatus.Processing
        IngestionStatus.Succeeded -> determineSucceededStatus()
        IngestionStatus.Failed -> DocumentUiStatus.Failed // Already handled above, but exhaustive
    }
}

/**
 * Determines UI status for successfully processed documents.
 *
 * The ingestion status is already "Succeeded", so we primarily rely on the draft status:
 * - `DocumentStatus.Confirmed` or `DocumentStatus.Ready` means the document is ready.
 * - `DocumentStatus.NeedsReview` and `DocumentStatus.Rejected` mean user attention is required.
 */
private fun DocumentRecordDto.determineSucceededStatus(): DocumentUiStatus {
    val draft = draft ?: return DocumentUiStatus.Review
    return when (draft.documentStatus) {
        DocumentStatus.Confirmed, DocumentStatus.Ready -> DocumentUiStatus.Ready
        DocumentStatus.NeedsReview, DocumentStatus.Rejected -> DocumentUiStatus.Review
    }
}
