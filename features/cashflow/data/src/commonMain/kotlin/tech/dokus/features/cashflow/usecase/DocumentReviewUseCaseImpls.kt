package tech.dokus.features.cashflow.usecase

import tech.dokus.domain.enums.CounterpartyIntent
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.DocumentMatchReviewId
import tech.dokus.domain.ids.DocumentSourceId
import tech.dokus.domain.model.DocumentMatchResolutionDecision
import tech.dokus.domain.model.DocumentPagesResponse
import tech.dokus.domain.model.DocumentRecordDto
import tech.dokus.domain.model.RejectDocumentRequest
import tech.dokus.domain.model.UpdateDraftRequest
import tech.dokus.domain.model.ReprocessRequest
import tech.dokus.domain.model.ReprocessResponse
import tech.dokus.domain.model.UpdateDraftResponse
import tech.dokus.features.cashflow.gateway.DocumentReviewGateway
import tech.dokus.features.cashflow.usecases.ConfirmDocumentUseCase
import tech.dokus.features.cashflow.usecases.GetDocumentPagesUseCase
import tech.dokus.features.cashflow.usecases.GetDocumentRecordUseCase
import tech.dokus.features.cashflow.usecases.GetDocumentSourceContentUseCase
import tech.dokus.features.cashflow.usecases.GetDocumentSourcePagesUseCase
import tech.dokus.features.cashflow.usecases.RejectDocumentUseCase
import tech.dokus.features.cashflow.usecases.ReprocessDocumentUseCase
import tech.dokus.features.cashflow.usecases.ResolveDocumentMatchReviewUseCase
import tech.dokus.features.cashflow.usecases.UpdateDocumentDraftContactUseCase
import tech.dokus.features.cashflow.usecases.UpdateDocumentDraftUseCase

internal class GetDocumentRecordUseCaseImpl(
    private val documentReviewGateway: DocumentReviewGateway
) : GetDocumentRecordUseCase {
    override suspend fun invoke(documentId: DocumentId): Result<DocumentRecordDto> {
        return documentReviewGateway.getDocumentRecord(documentId)
    }
}

internal class UpdateDocumentDraftUseCaseImpl(
    private val documentReviewGateway: DocumentReviewGateway
) : UpdateDocumentDraftUseCase {
    override suspend fun invoke(
        documentId: DocumentId,
        request: UpdateDraftRequest
    ): Result<UpdateDraftResponse> {
        return documentReviewGateway.updateDocumentDraft(documentId, request)
    }
}

internal class UpdateDocumentDraftContactUseCaseImpl(
    private val documentReviewGateway: DocumentReviewGateway
) : UpdateDocumentDraftContactUseCase {
    override suspend fun invoke(
        documentId: DocumentId,
        contactId: ContactId?,
        counterpartyIntent: CounterpartyIntent?
    ): Result<Unit> {
        return documentReviewGateway.updateDocumentDraftContact(
            documentId = documentId,
            contactId = contactId,
            counterpartyIntent = counterpartyIntent
        )
    }
}

internal class ConfirmDocumentUseCaseImpl(
    private val documentReviewGateway: DocumentReviewGateway
) : ConfirmDocumentUseCase {
    override suspend fun invoke(
        documentId: DocumentId
    ): Result<DocumentRecordDto> {
        return documentReviewGateway.confirmDocument(documentId)
    }
}

internal class RejectDocumentUseCaseImpl(
    private val documentReviewGateway: DocumentReviewGateway
) : RejectDocumentUseCase {
    override suspend fun invoke(
        documentId: DocumentId,
        request: RejectDocumentRequest
    ): Result<DocumentRecordDto> {
        return documentReviewGateway.rejectDocument(documentId, request)
    }
}

internal class GetDocumentPagesUseCaseImpl(
    private val documentReviewGateway: DocumentReviewGateway
) : GetDocumentPagesUseCase {
    override suspend fun invoke(
        documentId: DocumentId,
        dpi: Int,
        maxPages: Int
    ): Result<DocumentPagesResponse> {
        return documentReviewGateway.getDocumentPages(
            documentId = documentId,
            dpi = dpi,
            maxPages = maxPages
        )
    }
}

internal class GetDocumentSourcePagesUseCaseImpl(
    private val documentReviewGateway: DocumentReviewGateway
) : GetDocumentSourcePagesUseCase {
    override suspend fun invoke(
        documentId: DocumentId,
        sourceId: DocumentSourceId,
        dpi: Int,
        maxPages: Int
    ): Result<DocumentPagesResponse> {
        return documentReviewGateway.getDocumentSourcePages(
            documentId = documentId,
            sourceId = sourceId,
            dpi = dpi,
            maxPages = maxPages
        )
    }
}

internal class GetDocumentSourceContentUseCaseImpl(
    private val documentReviewGateway: DocumentReviewGateway
) : GetDocumentSourceContentUseCase {
    override suspend fun invoke(
        documentId: DocumentId,
        sourceId: DocumentSourceId
    ): Result<ByteArray> {
        return documentReviewGateway.getDocumentSourceContent(
            documentId = documentId,
            sourceId = sourceId
        )
    }
}

internal class ReprocessDocumentUseCaseImpl(
    private val documentReviewGateway: DocumentReviewGateway
) : ReprocessDocumentUseCase {
    override suspend fun invoke(
        documentId: DocumentId,
        request: ReprocessRequest
    ): Result<ReprocessResponse> {
        return documentReviewGateway.reprocessDocument(
            documentId = documentId,
            request = request
        )
    }
}

internal class ResolveDocumentMatchReviewUseCaseImpl(
    private val documentReviewGateway: DocumentReviewGateway
) : ResolveDocumentMatchReviewUseCase {
    override suspend fun invoke(
        reviewId: DocumentMatchReviewId,
        decision: DocumentMatchResolutionDecision
    ): Result<DocumentRecordDto> {
        return documentReviewGateway.resolveDocumentMatchReview(
            reviewId = reviewId,
            decision = decision
        )
    }
}
