package tech.dokus.features.cashflow.presentation.model

import tech.dokus.domain.enums.IngestionStatus
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.enums.DraftStatus
import tech.dokus.domain.model.DocumentRecordDto
import tech.dokus.domain.processing.DocumentProcessingConstants.AUTO_CONFIRM_CONFIDENCE_THRESHOLD
import tech.dokus.foundation.aura.model.DocumentUiStatus

/**
 * Maps a [DocumentRecordDto] to its UI-displayable status.
 *
 * Priority order (first matching wins):
 * 1. Failed: ingestion failed or has error message
 * 2. Queued: no ingestion or ingestion status is Queued
 * 3. Processing: ingestion status is Processing
 * 4. Ready: succeeded and draft is ready to confirm (invoice additionally requires linked contact)
 * 5. Review: succeeded but needs user attention (needs input/review or low-confidence)
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
 * - `DraftStatus.Ready` means the extracted data is complete enough to confirm.
 * - Invoices additionally require a linked contact to be truly confirmable.
 *
 * Confidence is used as a UI guardrail: low-confidence results still show as Review.
 */
private fun DocumentRecordDto.determineSucceededStatus(): DocumentUiStatus {
    val draft = draft ?: return DocumentUiStatus.Review
    val documentType = draft.documentType ?: DocumentType.Unknown

    // Coerce to valid range [0.0, 1.0] to handle any malformed data
    val confidence = (latestIngestion?.confidence ?: 0.0).coerceIn(0.0, 1.0)
    val hasHighConfidence = confidence >= AUTO_CONFIRM_CONFIDENCE_THRESHOLD

    val isDraftReady = when (draft.draftStatus) {
        DraftStatus.Ready, DraftStatus.Confirmed -> true
        DraftStatus.NeedsInput, DraftStatus.NeedsReview, DraftStatus.Rejected -> false
    }

    if (!isDraftReady) return DocumentUiStatus.Review
    if (!hasHighConfidence) return DocumentUiStatus.Review

    // Invoice confirmation requires a linked contact; bills/expenses don't.
    if (documentType == DocumentType.Invoice && draft.linkedContactId == null) {
        return DocumentUiStatus.Review
    }

    return DocumentUiStatus.Ready
}
