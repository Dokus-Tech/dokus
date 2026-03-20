package tech.dokus.backend.services.documents.confirmation

import tech.dokus.database.repository.cashflow.DocumentRepository
import tech.dokus.database.repository.cashflow.DraftSummaryEntity
import tech.dokus.domain.enums.DocumentStatus
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.TenantId

/**
 * Validates that a draft is in a confirmable state (NeedsReview or Confirmed).
 * Uses the repository — no direct table access.
 */
internal suspend fun requireConfirmableDraft(
    documentRepository: DocumentRepository,
    tenantId: TenantId,
    documentId: DocumentId
): DraftSummaryEntity {
    val draft = documentRepository.getDraftByDocumentId(documentId, tenantId)
        ?: throw DokusException.NotFound("Draft not found for document")

    val status = draft.documentStatus
    if (status != DocumentStatus.NeedsReview && status != DocumentStatus.Confirmed) {
        throw DokusException.BadRequest("Draft is not ready for confirmation: $status")
    }

    return draft
}
