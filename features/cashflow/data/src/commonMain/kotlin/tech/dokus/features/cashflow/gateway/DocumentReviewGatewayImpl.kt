package tech.dokus.features.cashflow.gateway

import tech.dokus.domain.enums.CounterpartyIntent
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.DocumentMatchReviewId
import tech.dokus.domain.model.DocumentMatchResolutionDecision
import tech.dokus.domain.model.RejectDocumentRequest
import tech.dokus.domain.model.ReprocessRequest
import tech.dokus.domain.model.ResolveDocumentMatchReviewRequest
import tech.dokus.domain.model.UpdateDraftRequest
import tech.dokus.features.cashflow.datasource.CashflowRemoteDataSource

internal class DocumentReviewGatewayImpl(
    private val cashflowRemoteDataSource: CashflowRemoteDataSource
) : DocumentReviewGateway {
    override suspend fun getDocumentRecord(documentId: DocumentId) =
        cashflowRemoteDataSource.getDocumentRecord(documentId)

    override suspend fun updateDocumentDraft(
        documentId: DocumentId,
        request: UpdateDraftRequest
    ) = cashflowRemoteDataSource.updateDocumentDraft(documentId, request)

    override suspend fun updateDocumentDraftContact(
        documentId: DocumentId,
        contactId: ContactId?,
        counterpartyIntent: CounterpartyIntent?
    ): Result<Unit> {
        return cashflowRemoteDataSource.updateDocumentDraftContact(
            documentId = documentId,
            contactId = contactId,
            counterpartyIntent = counterpartyIntent
        )
    }

    override suspend fun confirmDocument(
        documentId: DocumentId
    ) = cashflowRemoteDataSource.confirmDocument(documentId)

    override suspend fun rejectDocument(
        documentId: DocumentId,
        request: RejectDocumentRequest
    ) = cashflowRemoteDataSource.rejectDocument(documentId, request)

    override suspend fun getDocumentPages(
        documentId: DocumentId,
        dpi: Int,
        maxPages: Int
    ) = cashflowRemoteDataSource.getDocumentPages(
        documentId = documentId,
        dpi = dpi,
        maxPages = maxPages
    )

    override suspend fun reprocessDocument(
        documentId: DocumentId,
        request: ReprocessRequest
    ) = cashflowRemoteDataSource.reprocessDocument(
        documentId = documentId,
        request = request
    )

    override suspend fun resolveDocumentMatchReview(
        reviewId: DocumentMatchReviewId,
        decision: DocumentMatchResolutionDecision
    ) = cashflowRemoteDataSource.resolveDocumentMatchReview(
        reviewId = reviewId,
        request = ResolveDocumentMatchReviewRequest(decision = decision)
    )
}
