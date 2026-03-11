package tech.dokus.backend.services.documents

import tech.dokus.backend.routes.cashflow.documents.addDownloadUrl
import tech.dokus.backend.routes.cashflow.documents.findConfirmedEntity
import tech.dokus.backend.routes.cashflow.documents.toDto
import tech.dokus.backend.routes.cashflow.documents.toSummaryDto
import tech.dokus.database.repository.cashflow.CashflowEntriesRepository
import tech.dokus.database.repository.cashflow.CreditNoteRepository
import tech.dokus.database.repository.cashflow.DocumentIngestionRunRepository
import tech.dokus.database.repository.cashflow.DocumentRepository
import tech.dokus.database.repository.cashflow.ExpenseRepository
import tech.dokus.database.repository.cashflow.InvoiceRepository
import tech.dokus.database.repository.cashflow.selectPreferredSource
import tech.dokus.domain.enums.DocumentStatus
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.DocumentRecordDto
import tech.dokus.foundation.backend.storage.DocumentStorageService
import tech.dokus.foundation.backend.utils.loggerFor

@Suppress("LongParameterList")
internal class DocumentRecordLoader(
    private val documentRepository: DocumentRepository,
    private val ingestionRepository: DocumentIngestionRunRepository,
    private val invoiceRepository: InvoiceRepository,
    private val expenseRepository: ExpenseRepository,
    private val creditNoteRepository: CreditNoteRepository,
    private val cashflowEntriesRepository: CashflowEntriesRepository,
    private val truthService: DocumentTruthService,
    private val documentStorageService: DocumentStorageService,
) {
    private val logger = loggerFor<DocumentRecordLoader>()

    suspend fun load(
        tenantId: TenantId,
        documentId: DocumentId,
    ): DocumentRecordDto? {
        val document = documentRepository.getById(tenantId, documentId) ?: return null
        val sources = truthService.listSources(tenantId, documentId)
        val preferredSource = selectPreferredSource(sources)
        val effectiveDocument = if (preferredSource != null) {
            document.copy(
                filename = preferredSource.filename ?: document.filename,
                contentType = preferredSource.contentType,
                sizeBytes = preferredSource.sizeBytes,
                storageKey = preferredSource.storageKey,
                effectiveOrigin = preferredSource.sourceChannel,
                uploadedAt = preferredSource.arrivalAt,
            )
        } else {
            document
        }

        val documentWithUrl = addDownloadUrl(effectiveDocument, documentStorageService, logger)
        val draft = documentRepository.getDraftByDocumentId(documentId, tenantId)
        val latestIngestion = ingestionRepository.getLatestForDocument(documentId, tenantId)
        val pendingReview = truthService.getPendingReviewByDocument(tenantId, documentId)
        val confirmedEntity = if (draft?.documentStatus == DocumentStatus.Confirmed) {
            findConfirmedEntity(
                documentId = documentId,
                documentType = draft.documentType,
                tenantId = tenantId,
                invoiceRepository = invoiceRepository,
                expenseRepository = expenseRepository,
                creditNoteRepository = creditNoteRepository,
            )
        } else {
            null
        }
        val cashflowEntryId = if (draft?.documentStatus == DocumentStatus.Confirmed) {
            cashflowEntriesRepository.getByDocumentId(tenantId, documentId).getOrNull()?.id
        } else {
            null
        }

        return DocumentRecordDto(
            document = documentWithUrl,
            draft = draft?.toDto(),
            latestIngestion = latestIngestion?.toDto(
                includeRawExtraction = true,
                includeTrace = true,
            ),
            confirmedEntity = confirmedEntity,
            cashflowEntryId = cashflowEntryId,
            pendingMatchReview = pendingReview?.toSummaryDto(),
            sources = sources.map { it.toDto() },
        )
    }
}
