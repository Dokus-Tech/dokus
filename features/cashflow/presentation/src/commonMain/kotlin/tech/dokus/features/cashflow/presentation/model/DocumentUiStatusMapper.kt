package tech.dokus.features.cashflow.presentation.model

import tech.dokus.domain.enums.IngestionStatus
import tech.dokus.domain.model.DocumentRecordDto
import tech.dokus.foundation.aura.model.DocumentProcessingConstants.AUTO_CONFIRM_CONFIDENCE_THRESHOLD
import tech.dokus.foundation.aura.model.DocumentUiStatus

/**
 * Maps a [DocumentRecordDto] to its UI-displayable status.
 *
 * Priority order (first matching wins):
 * 1. Failed: ingestion failed or has error message
 * 2. Queued: no ingestion or ingestion status is Queued
 * 3. Processing: ingestion status is Processing
 * 4. Ready: succeeded with linked contact and high confidence
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
 * Ready requires: linked contact AND confidence >= threshold
 */
private fun DocumentRecordDto.determineSucceededStatus(): DocumentUiStatus {
    val linkedContact = draft?.linkedContactId
    val confidence = latestIngestion?.confidence ?: 0.0

    // Ready requires both conditions
    val hasLinkedContact = linkedContact != null
    val hasHighConfidence = confidence >= AUTO_CONFIRM_CONFIDENCE_THRESHOLD

    return if (hasLinkedContact && hasHighConfidence) {
        DocumentUiStatus.Ready
    } else {
        DocumentUiStatus.Review
    }
}
