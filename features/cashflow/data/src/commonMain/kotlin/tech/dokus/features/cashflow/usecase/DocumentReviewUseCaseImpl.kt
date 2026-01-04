package tech.dokus.features.cashflow.usecase

import tech.dokus.domain.enums.CounterpartyIntent
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.model.ConfirmDocumentRequest
import tech.dokus.domain.model.DocumentPagesResponse
import tech.dokus.domain.model.DocumentRecordDto
import tech.dokus.domain.model.RejectDocumentRequest
import tech.dokus.domain.model.UpdateDraftRequest
import tech.dokus.domain.model.UpdateDraftResponse
import tech.dokus.features.cashflow.datasource.CashflowRemoteDataSource
import tech.dokus.features.cashflow.usecases.DocumentReviewUseCase

internal class DocumentReviewUseCaseImpl(
    private val cashflowRemoteDataSource: CashflowRemoteDataSource
) : DocumentReviewUseCase {
    override suspend fun getDocumentRecord(documentId: DocumentId): Result<DocumentRecordDto> {
        return cashflowRemoteDataSource.getDocumentRecord(documentId)
    }

    override suspend fun updateDocumentDraft(
        documentId: DocumentId,
        request: UpdateDraftRequest
    ): Result<UpdateDraftResponse> {
        return cashflowRemoteDataSource.updateDocumentDraft(documentId, request)
    }

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
        documentId: DocumentId,
        request: ConfirmDocumentRequest
    ): Result<DocumentRecordDto> {
        return cashflowRemoteDataSource.confirmDocument(documentId, request)
    }

    override suspend fun rejectDocument(
        documentId: DocumentId,
        request: RejectDocumentRequest
    ): Result<DocumentRecordDto> {
        return cashflowRemoteDataSource.rejectDocument(documentId, request)
    }

    override suspend fun getDocumentPages(
        documentId: DocumentId,
        dpi: Int,
        maxPages: Int
    ): Result<DocumentPagesResponse> {
        return cashflowRemoteDataSource.getDocumentPages(
            documentId = documentId,
            dpi = dpi,
            maxPages = maxPages
        )
    }
}
