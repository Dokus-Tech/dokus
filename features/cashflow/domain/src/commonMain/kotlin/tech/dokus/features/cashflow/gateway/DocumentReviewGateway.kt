package tech.dokus.features.cashflow.gateway

import tech.dokus.domain.enums.CounterpartyIntent
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.model.ConfirmDocumentRequest
import tech.dokus.domain.model.DocumentPagesResponse
import tech.dokus.domain.model.DocumentRecordDto
import tech.dokus.domain.model.RejectDocumentRequest
import tech.dokus.domain.model.UpdateDraftRequest
import tech.dokus.domain.model.ReprocessRequest
import tech.dokus.domain.model.ReprocessResponse
import tech.dokus.domain.model.UpdateDraftResponse

/**
 * Gateway for document review operations.
 */
interface DocumentReviewGateway {
    suspend fun getDocumentRecord(documentId: DocumentId): Result<DocumentRecordDto>

    suspend fun updateDocumentDraft(
        documentId: DocumentId,
        request: UpdateDraftRequest
    ): Result<UpdateDraftResponse>

    suspend fun updateDocumentDraftContact(
        documentId: DocumentId,
        contactId: ContactId?,
        counterpartyIntent: CounterpartyIntent? = null
    ): Result<Unit>

    suspend fun confirmDocument(
        documentId: DocumentId,
        request: ConfirmDocumentRequest
    ): Result<DocumentRecordDto>

    suspend fun rejectDocument(
        documentId: DocumentId,
        request: RejectDocumentRequest
    ): Result<DocumentRecordDto>

    suspend fun getDocumentPages(
        documentId: DocumentId,
        dpi: Int = 150,
        maxPages: Int = 10
    ): Result<DocumentPagesResponse>

    suspend fun reprocessDocument(
        documentId: DocumentId,
        request: ReprocessRequest = ReprocessRequest()
    ): Result<ReprocessResponse>
}
