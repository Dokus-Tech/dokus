package tech.dokus.features.cashflow.gateway

import kotlinx.coroutines.flow.Flow
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.DocumentMatchReviewId
import tech.dokus.domain.ids.DocumentSourceId
import tech.dokus.domain.model.Dpi
import tech.dokus.domain.model.DocumentDetailDto
import tech.dokus.domain.model.DocumentMatchResolutionDecision
import tech.dokus.domain.model.DocumentPagesResponse
import tech.dokus.domain.model.DocumentRecordStreamEvent
import tech.dokus.domain.model.RejectDocumentRequest
import tech.dokus.domain.model.ReprocessRequest
import tech.dokus.domain.model.ReprocessResponse
import tech.dokus.domain.model.UpdateDraftRequest
import tech.dokus.domain.model.UpdateDraftResponse

/**
 * Gateway for document review operations.
 */
interface DocumentDetailGateway {
    suspend fun getDocumentRecord(documentId: DocumentId): Result<DocumentDetailDto>
    fun observeDocumentRecordEvents(documentId: DocumentId): Flow<DocumentRecordStreamEvent>

    suspend fun updateDocumentDraft(
        documentId: DocumentId,
        request: UpdateDraftRequest
    ): Result<UpdateDraftResponse>

    suspend fun updateDocumentDraftContact(
        documentId: DocumentId,
        contactId: ContactId?,
        pendingCreation: Boolean = false
    ): Result<Unit>

    suspend fun confirmDocument(
        documentId: DocumentId
    ): Result<DocumentDetailDto>

    suspend fun unconfirmDocument(
        documentId: DocumentId
    ): Result<DocumentDetailDto>

    suspend fun rejectDocument(
        documentId: DocumentId,
        request: RejectDocumentRequest
    ): Result<DocumentDetailDto>

    suspend fun getDocumentPages(
        documentId: DocumentId,
        dpi: Dpi,
        maxPages: Int = 10
    ): Result<DocumentPagesResponse>

    suspend fun getDocumentSourcePages(
        documentId: DocumentId,
        sourceId: DocumentSourceId,
        dpi: Dpi,
        maxPages: Int = 10
    ): Result<DocumentPagesResponse>

    suspend fun getDocumentSourceContent(
        documentId: DocumentId,
        sourceId: DocumentSourceId
    ): Result<ByteArray>

    suspend fun reprocessDocument(
        documentId: DocumentId,
        request: ReprocessRequest = ReprocessRequest()
    ): Result<ReprocessResponse>

    suspend fun resolveDocumentMatchReview(
        reviewId: DocumentMatchReviewId,
        decision: DocumentMatchResolutionDecision
    ): Result<DocumentDetailDto>
}
