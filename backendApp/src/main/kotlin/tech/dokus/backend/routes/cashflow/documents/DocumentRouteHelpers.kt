package tech.dokus.backend.routes.cashflow.documents

import tech.dokus.database.repository.cashflow.BillRepository
import tech.dokus.database.repository.cashflow.DocumentDraftRepository
import tech.dokus.database.repository.cashflow.DraftSummary
import tech.dokus.database.repository.cashflow.ExpenseRepository
import tech.dokus.database.repository.cashflow.IngestionRunSummary
import tech.dokus.database.repository.cashflow.InvoiceRepository
import tech.dokus.domain.enums.ContactLinkSource
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.DocumentDraftDto
import tech.dokus.domain.model.DocumentDto
import tech.dokus.domain.model.DocumentIngestionDto
import tech.dokus.domain.model.DocumentProcessingStepDto
import tech.dokus.domain.model.ExtractedDocumentData
import tech.dokus.domain.model.FinancialDocumentDto
import tech.dokus.domain.model.TrackedCorrection
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
 * Find confirmed financial entity by document ID.
 */
@Suppress("LongParameterList")
internal suspend fun findConfirmedEntity(
    documentId: DocumentId,
    documentType: DocumentType?,
    tenantId: TenantId,
    invoiceRepository: InvoiceRepository,
    billRepository: BillRepository,
    expenseRepository: ExpenseRepository
): FinancialDocumentDto? {
    return when (documentType) {
        DocumentType.Invoice -> invoiceRepository.findByDocumentId(tenantId, documentId)
        DocumentType.Bill -> billRepository.findByDocumentId(tenantId, documentId)
        else -> {
            // Try all types
            invoiceRepository.findByDocumentId(tenantId, documentId)
                ?: billRepository.findByDocumentId(tenantId, documentId)
                ?: expenseRepository.findByDocumentId(tenantId, documentId)
        }
    }
}

/**
 * Build list of tracked corrections between old and new extracted data.
 */
@Suppress("LongMethod", "CyclomaticComplexMethod")
internal fun buildCorrections(
    oldData: ExtractedDocumentData?,
    newData: ExtractedDocumentData,
    now: String
): List<TrackedCorrection> {
    val corrections = mutableListOf<TrackedCorrection>()

    if (oldData?.documentType != newData.documentType) {
        corrections.add(
            TrackedCorrection(
                "documentType",
                oldData?.documentType?.name,
                newData.documentType.name,
                now
            )
        )
    }

    // Invoice fields
    oldData?.invoice?.let { old ->
        newData.invoice?.let { new ->
            if (old.clientName != new.clientName) {
                corrections.add(
                    TrackedCorrection(
                        "invoice.clientName",
                        old.clientName,
                        new.clientName,
                        now
                    )
                )
            }
            if (old.invoiceNumber != new.invoiceNumber) {
                corrections.add(
                    TrackedCorrection(
                        "invoice.invoiceNumber",
                        old.invoiceNumber,
                        new.invoiceNumber,
                        now
                    )
                )
            }
            if (old.totalAmount != new.totalAmount) {
                corrections.add(
                    TrackedCorrection(
                        "invoice.totalAmount",
                        old.totalAmount?.toString(),
                        new.totalAmount?.toString(),
                        now
                    )
                )
            }
        }
    }

    // Bill fields
    oldData?.bill?.let { old ->
        newData.bill?.let { new ->
            if (old.supplierName != new.supplierName) {
                corrections.add(
                    TrackedCorrection(
                        "bill.supplierName",
                        old.supplierName,
                        new.supplierName,
                        now
                    )
                )
            }
            if (old.invoiceNumber != new.invoiceNumber) {
                corrections.add(
                    TrackedCorrection(
                        "bill.invoiceNumber",
                        old.invoiceNumber,
                        new.invoiceNumber,
                        now
                    )
                )
            }
            if (old.amount != new.amount) {
                corrections.add(
                    TrackedCorrection(
                        "bill.amount",
                        old.amount?.toString(),
                        new.amount?.toString(),
                        now
                    )
                )
            }
        }
    }

    // Expense fields
    oldData?.expense?.let { old ->
        newData.expense?.let { new ->
            if (old.merchant != new.merchant) {
                corrections.add(
                    TrackedCorrection(
                        "expense.merchant",
                        old.merchant,
                        new.merchant,
                        now
                    )
                )
            }
            if (old.amount != new.amount) {
                corrections.add(
                    TrackedCorrection(
                        "expense.amount",
                        old.amount?.toString(),
                        new.amount?.toString(),
                        now
                    )
                )
            }
            if (old.category != new.category) {
                corrections.add(
                    TrackedCorrection(
                        "expense.category",
                        old.category?.name,
                        new.category?.name,
                        now
                    )
                )
            }
        }
    }

    return corrections
}

/**
 * Convert DraftSummary to DocumentDraftDto.
 */
internal fun DraftSummary.toDto(): DocumentDraftDto = DocumentDraftDto(
    documentId = documentId,
    tenantId = tenantId,
    draftStatus = draftStatus,
    documentType = documentType,
    extractedData = extractedData,
    aiDraftData = aiDraftData,
    aiDescription = aiDescription,
    aiKeywords = aiKeywords,
    aiDraftSourceRunId = aiDraftSourceRunId,
    draftVersion = draftVersion,
    draftEditedAt = draftEditedAt,
    draftEditedBy = draftEditedBy,
    suggestedContactId = suggestedContactId,
    contactSuggestionConfidence = contactSuggestionConfidence,
    contactSuggestionReason = contactSuggestionReason,
    linkedContactId = linkedContactId,
    linkedContactSource = linkedContactSource,
    contactEvidence = contactEvidence,
    counterpartyIntent = counterpartyIntent,
    rejectReason = rejectReason,
    lastSuccessfulRunId = lastSuccessfulRunId,
    createdAt = createdAt,
    updatedAt = updatedAt
)

/**
 * Convert IngestionRunSummary to DocumentIngestionDto.
 */
internal fun IngestionRunSummary.toDto(
    includeRawExtraction: Boolean = false,
    includeTrace: Boolean = false
): DocumentIngestionDto {
    val rawExtraction = if (includeRawExtraction) {
        rawExtractionJson?.let { parseSafe<ExtractedDocumentData>(it).getOrNull() }
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
