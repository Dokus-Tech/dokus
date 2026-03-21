package tech.dokus.features.cashflow.usecase

import kotlinx.coroutines.flow.Flow
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.DocumentMatchReviewId
import tech.dokus.domain.ids.DocumentSourceId
import tech.dokus.domain.model.Dpi
import tech.dokus.domain.model.DocumentMatchResolutionDecision
import tech.dokus.domain.model.DocumentPagesResponse
import tech.dokus.domain.model.DocumentDetailDto
import tech.dokus.domain.model.DocumentRecordStreamEvent
import tech.dokus.domain.model.RejectDocumentRequest
import tech.dokus.domain.model.UpdateDraftRequest
import tech.dokus.domain.model.ReprocessRequest
import tech.dokus.domain.model.ReprocessResponse
import tech.dokus.domain.model.UpdateDraftResponse
import tech.dokus.features.cashflow.gateway.DocumentDetailGateway
import tech.dokus.features.cashflow.usecases.ConfirmDocumentUseCase
import tech.dokus.features.cashflow.usecases.UnconfirmDocumentUseCase
import tech.dokus.features.cashflow.usecases.GetDocumentPagesUseCase
import tech.dokus.features.cashflow.usecases.GetDocumentRecordUseCase
import tech.dokus.features.cashflow.usecases.GetDocumentSourceContentUseCase
import tech.dokus.features.cashflow.usecases.GetDocumentSourcePagesUseCase
import tech.dokus.features.cashflow.usecases.ObserveDocumentRecordEventsUseCase
import tech.dokus.features.cashflow.usecases.RejectDocumentUseCase
import tech.dokus.features.cashflow.usecases.ReprocessDocumentUseCase
import tech.dokus.features.cashflow.usecases.ResolveDocumentMatchReviewUseCase
import tech.dokus.features.cashflow.usecases.UpdateDocumentDraftContactUseCase
import tech.dokus.features.cashflow.usecases.UpdateDocumentDraftUseCase

internal class GetDocumentRecordUseCaseImpl(
    private val documentReviewGateway: DocumentDetailGateway
) : GetDocumentRecordUseCase {
    override suspend fun invoke(documentId: DocumentId): Result<DocumentDetailDto> {
        return documentReviewGateway.getDocumentRecord(documentId)
    }
}

internal class ObserveDocumentRecordEventsUseCaseImpl(
    private val documentReviewGateway: DocumentDetailGateway
) : ObserveDocumentRecordEventsUseCase {
    override fun invoke(documentId: DocumentId): Flow<DocumentRecordStreamEvent> {
        return documentReviewGateway.observeDocumentRecordEvents(documentId)
    }
}

internal class UpdateDocumentDraftUseCaseImpl(
    private val documentReviewGateway: DocumentDetailGateway
) : UpdateDocumentDraftUseCase {
    override suspend fun invoke(
        documentId: DocumentId,
        request: UpdateDraftRequest
    ): Result<UpdateDraftResponse> {
        return documentReviewGateway.updateDocumentDraft(documentId, request)
    }
}

internal class UpdateDocumentDraftContactUseCaseImpl(
    private val documentReviewGateway: DocumentDetailGateway
) : UpdateDocumentDraftContactUseCase {
    override suspend fun invoke(
        documentId: DocumentId,
        contactId: ContactId?,
        pendingCreation: Boolean
    ): Result<Unit> {
        return documentReviewGateway.updateDocumentDraftContact(
            documentId = documentId,
            contactId = contactId,
            pendingCreation = pendingCreation
        )
    }
}

internal class ConfirmDocumentUseCaseImpl(
    private val documentReviewGateway: DocumentDetailGateway
) : ConfirmDocumentUseCase {
    override suspend fun invoke(
        documentId: DocumentId
    ): Result<DocumentDetailDto> {
        return documentReviewGateway.confirmDocument(documentId)
    }
}

internal class UnconfirmDocumentUseCaseImpl(
    private val documentReviewGateway: DocumentDetailGateway
) : UnconfirmDocumentUseCase {
    override suspend fun invoke(
        documentId: DocumentId
    ): Result<DocumentDetailDto> {
        return documentReviewGateway.unconfirmDocument(documentId)
    }
}

internal class RejectDocumentUseCaseImpl(
    private val documentReviewGateway: DocumentDetailGateway
) : RejectDocumentUseCase {
    override suspend fun invoke(
        documentId: DocumentId,
        request: RejectDocumentRequest
    ): Result<DocumentDetailDto> {
        return documentReviewGateway.rejectDocument(documentId, request)
    }
}

internal class GetDocumentPagesUseCaseImpl(
    private val documentReviewGateway: DocumentDetailGateway
) : GetDocumentPagesUseCase {
    override suspend fun invoke(
        documentId: DocumentId,
        dpi: Dpi,
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
    private val documentReviewGateway: DocumentDetailGateway
) : GetDocumentSourcePagesUseCase {
    override suspend fun invoke(
        documentId: DocumentId,
        sourceId: DocumentSourceId,
        dpi: Dpi,
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
    private val documentReviewGateway: DocumentDetailGateway
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
    private val documentReviewGateway: DocumentDetailGateway
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
    private val documentReviewGateway: DocumentDetailGateway
) : ResolveDocumentMatchReviewUseCase {
    override suspend fun invoke(
        reviewId: DocumentMatchReviewId,
        decision: DocumentMatchResolutionDecision
    ): Result<DocumentDetailDto> {
        return documentReviewGateway.resolveDocumentMatchReview(
            reviewId = reviewId,
            decision = decision
        )
    }
}
