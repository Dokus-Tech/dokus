package tech.dokus.features.cashflow.presentation.model

import tech.dokus.domain.enums.DocumentStatus
import tech.dokus.domain.enums.IngestionStatus
import tech.dokus.domain.model.DocumentDetailDto
import tech.dokus.domain.model.DocumentListItemDto
import tech.dokus.foundation.aura.model.DocumentUiStatus

/**
 * Maps a [DocumentDetailDto] to its UI-displayable status.
 *
 * Priority order (first matching wins):
 * 1. Failed: ingestion failed or has error message
 * 2. Queued: no ingestion or ingestion status is Queued
 * 3. Processing: ingestion status is Processing
 * 4. Ready: succeeded and draft is confirmed
 * 5. Review: succeeded but needs user attention
 */
fun DocumentDetailDto.toUiStatus(): DocumentUiStatus {
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
 * - `DocumentStatus.Confirmed` means the document is ready.
 * - `DocumentStatus.NeedsReview` and `DocumentStatus.Rejected` mean user attention is required.
 */
private fun DocumentDetailDto.determineSucceededStatus(): DocumentUiStatus {
    val draft = draft ?: return DocumentUiStatus.Review
    return when (draft.documentStatus) {
        DocumentStatus.Confirmed -> DocumentUiStatus.Ready
        DocumentStatus.NeedsReview, DocumentStatus.Rejected -> DocumentUiStatus.Review
    }
}

/**
 * Maps a [DocumentListItemDto] to its UI-displayable status.
 *
 * Same logic as [DocumentDetailDto.toUiStatus] but using flat fields.
 */
fun DocumentListItemDto.toUiStatus(): DocumentUiStatus {
    val ingestion = ingestionStatus

    // No ingestion record yet - treat as queued
    if (ingestion == null) {
        return DocumentUiStatus.Queued
    }

    // Check for failure first (highest priority)
    if (ingestion == IngestionStatus.Failed) {
        return DocumentUiStatus.Failed
    }

    // Check processing states
    return when (ingestion) {
        IngestionStatus.Queued -> DocumentUiStatus.Queued
        IngestionStatus.Processing -> DocumentUiStatus.Processing
        IngestionStatus.Succeeded -> determineListItemSucceededStatus()
        IngestionStatus.Failed -> DocumentUiStatus.Failed
    }
}

private fun DocumentListItemDto.determineListItemSucceededStatus(): DocumentUiStatus {
    val status = documentStatus ?: return DocumentUiStatus.Review
    return when (status) {
        DocumentStatus.Confirmed -> DocumentUiStatus.Ready
        DocumentStatus.NeedsReview, DocumentStatus.Rejected -> DocumentUiStatus.Review
    }
}
