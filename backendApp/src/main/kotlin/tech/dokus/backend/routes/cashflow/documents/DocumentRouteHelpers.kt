package tech.dokus.backend.routes.cashflow.documents

import tech.dokus.database.repository.cashflow.CreditNoteRepository
import tech.dokus.database.repository.cashflow.DocumentDraftRepository
import tech.dokus.database.repository.cashflow.DraftSummary
import tech.dokus.database.repository.cashflow.ExpenseRepository
import tech.dokus.database.repository.cashflow.IngestionRunSummary
import tech.dokus.database.repository.cashflow.InvoiceRepository
import tech.dokus.domain.enums.ContactLinkSource
import tech.dokus.domain.enums.DocumentDirection
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.CreditNoteDraftData
import tech.dokus.domain.model.DocumentDraftData
import tech.dokus.domain.model.DocumentDraftDto
import tech.dokus.domain.model.DocumentDto
import tech.dokus.domain.model.DocumentIngestionDto
import tech.dokus.domain.model.DocumentProcessingStepDto
import tech.dokus.domain.model.FinancialDocumentDto
import tech.dokus.domain.model.InvoiceDraftData
import tech.dokus.domain.model.ReceiptDraftData
import tech.dokus.domain.model.UpdateDraftRequest
import tech.dokus.domain.utils.parseSafe
import tech.dokus.foundation.backend.storage.DocumentStorageService as MinioDocumentStorageService

/**
 * Add download URL to document DTO.
 */
@Suppress("TooGenericExceptionCaught")
internal suspend fun addDownloadUrl(
    document: DocumentDto,
    minioStorage: MinioDocumentStorageService,
    logger: org.slf4j.Logger
): DocumentDto {
    val downloadUrl = try {
        minioStorage.getDownloadUrl(document.storageKey)
    } catch (e: RuntimeException) {
        logger.warn("Failed to get download URL for ${document.storageKey}: ${e.message}")
        null
    }
    return document.copy(downloadUrl = downloadUrl)
}

/**
 * Find a confirmed financial entity by document ID.
 */
@Suppress("LongParameterList")
internal suspend fun findConfirmedEntity(
    documentId: DocumentId,
    documentType: DocumentType?,
    tenantId: TenantId,
    invoiceRepository: InvoiceRepository,
    expenseRepository: ExpenseRepository,
    creditNoteRepository: CreditNoteRepository
): FinancialDocumentDto? {
    return when (documentType) {
        DocumentType.Invoice -> invoiceRepository.findByDocumentId(tenantId, documentId)
        DocumentType.CreditNote -> creditNoteRepository.findByDocumentId(tenantId, documentId)
        else -> {
            // Try all types
            invoiceRepository.findByDocumentId(tenantId, documentId)
                ?: expenseRepository.findByDocumentId(tenantId, documentId)
                ?: creditNoteRepository.findByDocumentId(tenantId, documentId)
        }
    }
}


/**
 * Convert DraftSummary to DocumentDraftDto.
 */
internal fun DraftSummary.toDto(): DocumentDraftDto = DocumentDraftDto(
    documentId = documentId,
    tenantId = tenantId,
    documentStatus = documentStatus,
    documentType = documentType,
    direction = extractedData.directionOrUnknown(),
    extractedData = extractedData,
    aiDraftData = aiDraftData,
    aiDescription = aiDescription,
    aiKeywords = aiKeywords,
    aiDraftSourceRunId = aiDraftSourceRunId,
    draftVersion = draftVersion,
    draftEditedAt = draftEditedAt,
    draftEditedBy = draftEditedBy,
    contactSuggestions = contactSuggestions,
    counterpartySnapshot = counterpartySnapshot,
    matchEvidence = matchEvidence,
    linkedContactId = linkedContactId,
    linkedContactSource = linkedContactSource,
    counterpartyIntent = counterpartyIntent,
    rejectReason = rejectReason,
    lastSuccessfulRunId = lastSuccessfulRunId,
    createdAt = createdAt,
    updatedAt = updatedAt
)

private fun DocumentDraftData?.directionOrUnknown(): DocumentDirection = when (this) {
    is InvoiceDraftData -> this.direction
    is ReceiptDraftData -> this.direction
    is CreditNoteDraftData -> when (this.direction) {
        tech.dokus.domain.enums.CreditNoteDirection.Sales -> DocumentDirection.Outbound
        tech.dokus.domain.enums.CreditNoteDirection.Purchase -> DocumentDirection.Inbound
        tech.dokus.domain.enums.CreditNoteDirection.Unknown -> DocumentDirection.Unknown
    }
    null -> DocumentDirection.Unknown
}


/**
 * Convert IngestionRunSummary to DocumentIngestionDto.
 */
internal fun IngestionRunSummary.toDto(
    includeRawExtraction: Boolean = false,
    includeTrace: Boolean = false
): DocumentIngestionDto {
    val rawExtraction = if (includeRawExtraction) {
        rawExtractionJson?.let { parseSafe<kotlinx.serialization.json.JsonElement>(it).getOrNull() }
    } else {
        null
    }

    val processingTrace = if (includeTrace) {
        processingTrace?.let { parseSafe<List<DocumentProcessingStepDto>>(it).getOrNull() }
    } else {
        null
    }

    return DocumentIngestionDto(
        id = id,
        documentId = documentId,
        tenantId = tenantId,
        status = status,
        provider = provider,
        queuedAt = queuedAt,
        startedAt = startedAt,
        finishedAt = finishedAt,
        errorMessage = errorMessage,
        confidence = confidence,
        processingOutcome = processingOutcome,
        rawExtraction = rawExtraction,
        processingTrace = processingTrace
    )
}

/**
 * Update draft counterparty (contact ID and intent).
 */
internal suspend fun updateDraftCounterparty(
    draftRepository: DocumentDraftRepository,
    documentId: DocumentId,
    tenantId: TenantId,
    request: UpdateDraftRequest
) {
    val contactId = request.contactId?.let { ContactId.parse(it) }
    val updated = draftRepository.updateCounterparty(
        documentId = documentId,
        tenantId = tenantId,
        contactId = contactId,
        intent = request.counterpartyIntent,
        source = if (contactId != null) ContactLinkSource.User else null
    )
    if (!updated) {
        throw DokusException.InternalError("Failed to update document counterparty")
    }
}
