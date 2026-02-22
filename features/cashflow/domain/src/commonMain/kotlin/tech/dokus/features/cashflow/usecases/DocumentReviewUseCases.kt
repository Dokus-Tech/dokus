package tech.dokus.features.cashflow.usecases

import tech.dokus.domain.enums.CounterpartyIntent
import tech.dokus.domain.ids.DocumentMatchReviewId
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.DocumentSourceId
import tech.dokus.domain.model.DocumentMatchResolutionDecision
import tech.dokus.domain.model.DocumentPagesResponse
import tech.dokus.domain.model.DocumentRecordDto
import tech.dokus.domain.model.RejectDocumentRequest
import tech.dokus.domain.model.UpdateDraftRequest
import tech.dokus.domain.model.ReprocessRequest
import tech.dokus.domain.model.ReprocessResponse
import tech.dokus.domain.model.UpdateDraftResponse

/**
 * Use case for loading a document record for review.
 */
interface GetDocumentRecordUseCase {
    suspend operator fun invoke(documentId: DocumentId): Result<DocumentRecordDto>
}

/**
 * Use case for updating a document draft's extracted data.
 */
interface UpdateDocumentDraftUseCase {
    suspend operator fun invoke(
        documentId: DocumentId,
        request: UpdateDraftRequest
    ): Result<UpdateDraftResponse>
}

/**
 * Use case for linking a draft document to a contact.
 */
interface UpdateDocumentDraftContactUseCase {
    suspend operator fun invoke(
        documentId: DocumentId,
        contactId: ContactId?,
        counterpartyIntent: CounterpartyIntent? = null
    ): Result<Unit>
}

/**
 * Use case for confirming a document.
 */
interface ConfirmDocumentUseCase {
    suspend operator fun invoke(
        documentId: DocumentId
    ): Result<DocumentRecordDto>
}

/**
 * Use case for rejecting a document.
 */
interface RejectDocumentUseCase {
    suspend operator fun invoke(
        documentId: DocumentId,
        request: RejectDocumentRequest
    ): Result<DocumentRecordDto>
}

/**
 * Use case for loading document preview pages.
 */
interface GetDocumentPagesUseCase {
    suspend operator fun invoke(
        documentId: DocumentId,
        dpi: Int = 150,
        maxPages: Int = 10
    ): Result<DocumentPagesResponse>
}

/**
 * Use case for loading PDF preview pages for a specific source.
 */
interface GetDocumentSourcePagesUseCase {
    suspend operator fun invoke(
        documentId: DocumentId,
        sourceId: DocumentSourceId,
        dpi: Int = 150,
        maxPages: Int = 10
    ): Result<DocumentPagesResponse>
}

/**
 * Use case for downloading raw content for a specific source.
 */
interface GetDocumentSourceContentUseCase {
    suspend operator fun invoke(
        documentId: DocumentId,
        sourceId: DocumentSourceId
    ): Result<ByteArray>
}

/**
 * Use case for re-triggering AI extraction on a document.
 * This is used when the initial extraction fails and the user wants to retry.
 */
interface ReprocessDocumentUseCase {
    suspend operator fun invoke(
        documentId: DocumentId,
        request: ReprocessRequest = ReprocessRequest()
    ): Result<ReprocessResponse>
}

/**
 * Use case for resolving a possible-match review item.
 */
interface ResolveDocumentMatchReviewUseCase {
    suspend operator fun invoke(
        reviewId: DocumentMatchReviewId,
        decision: DocumentMatchResolutionDecision
    ): Result<DocumentRecordDto>
}
