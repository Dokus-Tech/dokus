package tech.dokus.backend.routes.cashflow.documents

import tech.dokus.database.repository.cashflow.BillRepository
import tech.dokus.database.repository.cashflow.DocumentDraftRepository
import tech.dokus.database.repository.cashflow.DraftSummary
import tech.dokus.database.repository.cashflow.ExpenseRepository
import tech.dokus.database.repository.cashflow.IngestionRunSummary
import tech.dokus.database.repository.cashflow.InvoiceRepository
import tech.dokus.domain.enums.ContactLinkSource
import tech.dokus.domain.enums.DocumentKind
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.DocumentDraftDto
import tech.dokus.domain.model.DocumentDto
import tech.dokus.domain.model.DocumentIngestionDto
import tech.dokus.domain.model.DocumentProcessingStepDto
import tech.dokus.domain.model.DocumentDraftData
import tech.dokus.domain.model.BillDraftData
import tech.dokus.domain.model.CreditNoteDraftData
import tech.dokus.domain.model.InvoiceDraftData
import tech.dokus.domain.model.ReceiptDraftData
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
    oldData: DocumentDraftData?,
    newData: DocumentDraftData,
    now: String
): List<TrackedCorrection> {
    val corrections = mutableListOf<TrackedCorrection>()

    val oldKind = oldData?.let { resolveDraftKind(it) }
    val newKind = resolveDraftKind(newData)
    if (oldKind != newKind) {
        corrections.add(
            TrackedCorrection(
                "documentKind",
                oldKind?.name,
                newKind.name,
                now
            )
        )
    }

    when (newData) {
        is InvoiceDraftData -> {
            val old = oldData as? InvoiceDraftData
            if (old?.customerName != newData.customerName) {
                corrections.add(
                    TrackedCorrection(
                        "invoice.customerName",
                        old?.customerName,
                        newData.customerName,
                        now
                    )
                )
            }
            if (old?.invoiceNumber != newData.invoiceNumber) {
                corrections.add(
                    TrackedCorrection(
                        "invoice.invoiceNumber",
                        old?.invoiceNumber,
                        newData.invoiceNumber,
                        now
                    )
                )
            }
            if (old?.totalAmount != newData.totalAmount) {
                corrections.add(
                    TrackedCorrection(
                        "invoice.totalAmount",
                        old?.totalAmount?.toString(),
                        newData.totalAmount?.toString(),
                        now
                    )
                )
            }
        }
        is BillDraftData -> {
            val old = oldData as? BillDraftData
            if (old?.supplierName != newData.supplierName) {
                corrections.add(
                    TrackedCorrection(
                        "bill.supplierName",
                        old?.supplierName,
                        newData.supplierName,
                        now
                    )
                )
            }
            if (old?.invoiceNumber != newData.invoiceNumber) {
                corrections.add(
                    TrackedCorrection(
                        "bill.invoiceNumber",
                        old?.invoiceNumber,
                        newData.invoiceNumber,
                        now
                    )
                )
            }
            if (old?.totalAmount != newData.totalAmount) {
                corrections.add(
                    TrackedCorrection(
                        "bill.totalAmount",
                        old?.totalAmount?.toString(),
                        newData.totalAmount?.toString(),
                        now
                    )
                )
            }
        }
        is CreditNoteDraftData -> {
            val old = oldData as? CreditNoteDraftData
            if (old?.counterpartyName != newData.counterpartyName) {
                corrections.add(
                    TrackedCorrection(
                        "creditNote.counterpartyName",
                        old?.counterpartyName,
                        newData.counterpartyName,
                        now
                    )
                )
            }
            if (old?.creditNoteNumber != newData.creditNoteNumber) {
                corrections.add(
                    TrackedCorrection(
                        "creditNote.creditNoteNumber",
                        old?.creditNoteNumber,
                        newData.creditNoteNumber,
                        now
                    )
                )
            }
            if (old?.totalAmount != newData.totalAmount) {
                corrections.add(
                    TrackedCorrection(
                        "creditNote.totalAmount",
                        old?.totalAmount?.toString(),
                        newData.totalAmount?.toString(),
                        now
                    )
                )
            }
        }
        is ReceiptDraftData -> {
            val old = oldData as? ReceiptDraftData
            if (old?.merchantName != newData.merchantName) {
                corrections.add(
                    TrackedCorrection(
                        "receipt.merchantName",
                        old?.merchantName,
                        newData.merchantName,
                        now
                    )
                )
            }
            if (old?.totalAmount != newData.totalAmount) {
                corrections.add(
                    TrackedCorrection(
                        "receipt.totalAmount",
                        old?.totalAmount?.toString(),
                        newData.totalAmount?.toString(),
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
    documentStatus = documentStatus,
    documentType = documentType,
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

private fun resolveDraftKind(data: DocumentDraftData): DocumentKind = when (data) {
    is InvoiceDraftData -> DocumentKind.Invoice
    is BillDraftData -> DocumentKind.Bill
    is CreditNoteDraftData -> DocumentKind.CreditNote
    is ReceiptDraftData -> DocumentKind.Receipt
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
