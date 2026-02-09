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

    val oldType = oldData?.let { resolveDraftType(it) }
    val newType = resolveDraftType(newData)
    if (oldType != newType) {
        corrections.add(
            TrackedCorrection(
                "documentType",
                oldType?.name,
                newType.name,
                now
            )
        )
    }

    val typeCorrections = when (newData) {
        is InvoiceDraftData -> buildCorrections(oldData as? InvoiceDraftData, newData, now)
        is BillDraftData -> buildCorrections(oldData as? BillDraftData, newData, now)
        is CreditNoteDraftData -> buildCorrections(oldData as? CreditNoteDraftData, newData, now)
        is ReceiptDraftData -> buildCorrections(oldData as? ReceiptDraftData, newData, now)
    }
    corrections.addAll(typeCorrections)

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

private fun resolveDraftType(data: DocumentDraftData): DocumentType = when (data) {
    is InvoiceDraftData -> DocumentType.Invoice
    is BillDraftData -> DocumentType.Bill
    is CreditNoteDraftData -> DocumentType.CreditNote
    is ReceiptDraftData -> DocumentType.Receipt
}

private fun buildCorrections(
    oldData: InvoiceDraftData?,
    newData: InvoiceDraftData,
    now: String
): List<TrackedCorrection> {
    val corrections = mutableListOf<TrackedCorrection>()
    if (oldData?.customerName != newData.customerName) {
        corrections.add(
            TrackedCorrection(
                "invoice.customerName",
                oldData?.customerName,
                newData.customerName,
                now
            )
        )
    }
    if (oldData?.invoiceNumber != newData.invoiceNumber) {
        corrections.add(
            TrackedCorrection(
                "invoice.invoiceNumber",
                oldData?.invoiceNumber,
                newData.invoiceNumber,
                now
            )
        )
    }
    if (oldData?.totalAmount != newData.totalAmount) {
        corrections.add(
            TrackedCorrection(
                "invoice.totalAmount",
                oldData?.totalAmount?.toString(),
                newData.totalAmount?.toString(),
                now
            )
        )
    }
    return corrections
}

private fun buildCorrections(
    oldData: BillDraftData?,
    newData: BillDraftData,
    now: String
): List<TrackedCorrection> {
    val corrections = mutableListOf<TrackedCorrection>()
    if (oldData?.supplierName != newData.supplierName) {
        corrections.add(
            TrackedCorrection(
                "bill.supplierName",
                oldData?.supplierName,
                newData.supplierName,
                now
            )
        )
    }
    if (oldData?.invoiceNumber != newData.invoiceNumber) {
        corrections.add(
            TrackedCorrection(
                "bill.invoiceNumber",
                oldData?.invoiceNumber,
                newData.invoiceNumber,
                now
            )
        )
    }
    if (oldData?.totalAmount != newData.totalAmount) {
        corrections.add(
            TrackedCorrection(
                "bill.totalAmount",
                oldData?.totalAmount?.toString(),
                newData.totalAmount?.toString(),
                now
            )
        )
    }
    return corrections
}

private fun buildCorrections(
    oldData: CreditNoteDraftData?,
    newData: CreditNoteDraftData,
    now: String
): List<TrackedCorrection> {
    val corrections = mutableListOf<TrackedCorrection>()
    if (oldData?.counterpartyName != newData.counterpartyName) {
        corrections.add(
            TrackedCorrection(
                "creditNote.counterpartyName",
                oldData?.counterpartyName,
                newData.counterpartyName,
                now
            )
        )
    }
    if (oldData?.creditNoteNumber != newData.creditNoteNumber) {
        corrections.add(
            TrackedCorrection(
                "creditNote.creditNoteNumber",
                oldData?.creditNoteNumber,
                newData.creditNoteNumber,
                now
            )
        )
    }
    if (oldData?.totalAmount != newData.totalAmount) {
        corrections.add(
            TrackedCorrection(
                "creditNote.totalAmount",
                oldData?.totalAmount?.toString(),
                newData.totalAmount?.toString(),
                now
            )
        )
    }
    return corrections
}

private fun buildCorrections(
    oldData: ReceiptDraftData?,
    newData: ReceiptDraftData,
    now: String
): List<TrackedCorrection> {
    val corrections = mutableListOf<TrackedCorrection>()
    if (oldData?.merchantName != newData.merchantName) {
        corrections.add(
            TrackedCorrection(
                "receipt.merchantName",
                oldData?.merchantName,
                newData.merchantName,
                now
            )
        )
    }
    if (oldData?.totalAmount != newData.totalAmount) {
        corrections.add(
            TrackedCorrection(
                "receipt.totalAmount",
                oldData?.totalAmount?.toString(),
                newData.totalAmount?.toString(),
                now
            )
        )
    }
    return corrections
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
