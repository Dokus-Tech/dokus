package tech.dokus.backend.routes.cashflow.documents

import kotlinx.serialization.json.JsonElement
import tech.dokus.database.repository.cashflow.CreditNoteRepository
import tech.dokus.database.repository.cashflow.DocumentMatchReviewSummary
import tech.dokus.database.repository.cashflow.DocumentRepository
import tech.dokus.database.repository.cashflow.DocumentSourceSummary
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
import tech.dokus.database.entity.CreditNoteEntity
import tech.dokus.database.entity.ExpenseEntity
import tech.dokus.database.entity.InvoiceEntity
import tech.dokus.domain.Quantity
import tech.dokus.domain.enums.CreditNoteType
import tech.dokus.domain.enums.DocumentDirection
import tech.dokus.domain.model.DocDto
import tech.dokus.domain.model.DocLineItem
import tech.dokus.domain.model.DocumentDraftDto
import tech.dokus.domain.model.DocumentDto
import tech.dokus.domain.model.DocumentIngestionDto
import tech.dokus.domain.model.DocumentMatchReviewSummaryDto
import tech.dokus.domain.model.DocumentProcessingStepDto
import tech.dokus.domain.model.DocumentSourceDto
import tech.dokus.domain.model.InvoicePeppolInfo
import tech.dokus.domain.model.PaymentLinkInfo
import tech.dokus.domain.model.InvoicePaymentInfo
import tech.dokus.domain.model.UpdateDraftRequest
import tech.dokus.domain.model.contact.ContactSuggestionDto
import tech.dokus.domain.model.contact.CounterpartyInfo
import tech.dokus.domain.model.contact.ResolvedContact
import tech.dokus.domain.utils.parseSafe
import tech.dokus.foundation.backend.storage.DocumentStorageService as MinioDocumentStorageService

/**
 * Add download URL to document DTO.
 * [storageKey] is resolved from the preferred source's blob, not from the document itself.
 */
@Suppress("TooGenericExceptionCaught")
internal suspend fun addDownloadUrl(
    document: DocumentDto,
    storageKey: String?,
    minioStorage: MinioDocumentStorageService,
    logger: org.slf4j.Logger
): DocumentDto {
    if (storageKey == null) return document
    val downloadUrl = try {
        minioStorage.getDownloadUrl(storageKey)
    } catch (e: RuntimeException) {
        logger.warn("Failed to get download URL for $storageKey: ${e.message}")
        null
    }
    return document.copy(downloadUrl = downloadUrl)
}

/**
 * Find a confirmed financial entity by document ID.
 * Returns the raw entity (InvoiceEntity, ExpenseEntity, or CreditNoteEntity).
 */
@Suppress("LongParameterList")
internal suspend fun findConfirmedEntity(
    documentId: DocumentId,
    documentType: DocumentType?,
    tenantId: TenantId,
    invoiceRepository: InvoiceRepository,
    expenseRepository: ExpenseRepository,
    creditNoteRepository: CreditNoteRepository
): Any? {
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
 * Convert a confirmed entity (InvoiceEntity, ExpenseEntity, CreditNoteEntity) to [DocDto].
 */
internal fun confirmedEntityToDocDto(entity: Any): DocDto = when (entity) {
    is InvoiceEntity -> entity.toDocDto()
    is ExpenseEntity -> entity.toDocDto()
    is CreditNoteEntity -> entity.toDocDto()
    else -> error("Unsupported entity type for DocDto conversion: ${entity::class.simpleName}")
}

// =============================================================================
// Entity → DocDto conversions
// =============================================================================

internal fun InvoiceEntity.toDocDto(): DocDto.Invoice.Confirmed = DocDto.Invoice.Confirmed(
    id = id,
    tenantId = tenantId,
    contactId = contactId,
    direction = direction,
    invoiceNumber = invoiceNumber.value,
    issueDate = issueDate,
    dueDate = dueDate,
    currency = currency,
    subtotalAmount = subtotalAmount,
    vatAmount = vatAmount,
    totalAmount = totalAmount,
    paidAmount = paidAmount,
    lineItems = items.map { item ->
        DocLineItem(
            description = item.description,
            quantity = Quantity(item.quantity),
            unitPrice = item.unitPrice,
            vatRate = item.vatRate,
            netAmount = item.lineTotal,
            vatAmount = item.vatAmount,
            sortOrder = item.sortOrder,
        )
    },
    iban = senderIban,
    notes = notes,
    status = status,
    structuredCommunication = structuredCommunication,
    peppol = if (peppolId != null && peppolSentAt != null) InvoicePeppolInfo(
        peppolId = peppolId!!,
        sentAt = peppolSentAt!!,
        status = peppolStatus ?: tech.dokus.domain.enums.PeppolStatus.Pending
    ) else null,
    paymentLinkInfo = if (paymentLink != null) PaymentLinkInfo(
        url = paymentLink!!,
        expiresAt = paymentLinkExpiresAt
    ) else null,
    paymentInfo = if (paidAt != null) InvoicePaymentInfo(
        paidAt = paidAt!!,
        paymentMethod = paymentMethod ?: tech.dokus.domain.enums.PaymentMethod.BankTransfer
    ) else null,
    documentId = documentId,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

internal fun ExpenseEntity.toDocDto(): DocDto.Receipt.Confirmed = DocDto.Receipt.Confirmed(
    id = id,
    tenantId = tenantId,
    direction = DocumentDirection.Inbound,
    merchantName = merchant,
    merchantVat = null,
    date = date,
    currency = tech.dokus.domain.enums.Currency.Eur,
    totalAmount = amount,
    vatAmount = vatAmount,
    lineItems = emptyList(),
    receiptNumber = null,
    notes = notes,
    vatRate = vatRate,
    category = category,
    isDeductible = isDeductible,
    deductiblePercentage = deductiblePercentage,
    paymentMethod = paymentMethod,
    contactId = contactId,
    documentId = documentId,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

internal fun CreditNoteEntity.toDocDto(): DocDto.CreditNote.Confirmed = DocDto.CreditNote.Confirmed(
    id = id,
    tenantId = tenantId,
    contactId = contactId,
    creditNoteType = creditNoteType,
    direction = when (creditNoteType) {
        CreditNoteType.Sales -> DocumentDirection.Outbound
        CreditNoteType.Purchase -> DocumentDirection.Inbound
    },
    creditNoteNumber = creditNoteNumber,
    issueDate = issueDate,
    currency = currency,
    subtotalAmount = subtotalAmount,
    vatAmount = vatAmount,
    totalAmount = totalAmount,
    lineItems = emptyList(),
    status = status,
    settlementIntent = settlementIntent,
    reason = reason,
    notes = notes,
    documentId = documentId,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

/**
 * Convert DraftSummary to DocumentDraftDto.
 */
internal fun DraftSummary.toDto(
    resolvedContact: ResolvedContact = ResolvedContact.Unknown,
    contactSuggestions: List<ContactSuggestionDto> = emptyList(),
    content: DocDto? = null,
): DocumentDraftDto = DocumentDraftDto(
    documentId = documentId,
    tenantId = tenantId,
    documentStatus = documentStatus,
    documentType = documentType,
    direction = direction,
    content = content,
    aiKeywords = aiKeywords,
    purposeBase = purposeBase,
    purposePeriodYear = purposePeriodYear,
    purposePeriodMonth = purposePeriodMonth,
    purposeRendered = purposeRendered,
    purposeSource = purposeSource,
    purposeLocked = purposeLocked,
    purposePeriodMode = purposePeriodMode,
    aiDraftSourceRunId = aiDraftSourceRunId,
    draftVersion = draftVersion,
    draftEditedAt = draftEditedAt,
    draftEditedBy = draftEditedBy,
    resolvedContact = resolvedContact,
    contactSuggestions = contactSuggestions,
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
        rawExtractionJson?.let { parseSafe<JsonElement>(it).getOrNull() }
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

internal fun DocumentSourceSummary.toDto(): DocumentSourceDto = DocumentSourceDto(
    id = id,
    tenantId = tenantId,
    documentId = documentId,
    blobId = blobId,
    peppolRawUblBlobId = peppolRawUblBlobId,
    sourceChannel = sourceChannel,
    arrivalAt = arrivalAt,
    contentHash = contentHash,
    identityKeyHash = identityKeyHash,
    status = status,
    isCorrective = isCorrective,
    extractedSnapshotJson = extractedSnapshotJson,
    peppolStructuredSnapshotJson = peppolStructuredSnapshotJson,
    peppolSnapshotVersion = peppolSnapshotVersion,
    detachedAt = detachedAt,
    filename = filename,
    contentType = contentType,
    sizeBytes = sizeBytes,
    matchType = matchType
)

internal fun DocumentMatchReviewSummary.toSummaryDto(): DocumentMatchReviewSummaryDto =
    DocumentMatchReviewSummaryDto(
        reviewId = id,
        reasonType = reasonType,
        status = status,
        createdAt = createdAt
    )

/**
 * Update draft counterparty (contact ID and intent).
 */
internal suspend fun updateDraftCounterparty(
    documentRepository: DocumentRepository,
    documentId: DocumentId,
    tenantId: TenantId,
    request: UpdateDraftRequest
) {
    val contactId = request.contactId?.let { ContactId.parse(it) }
    val counterparty: CounterpartyInfo = if (contactId != null) {
        CounterpartyInfo.Linked(
            contactId = contactId,
            source = ContactLinkSource.User,
        )
    } else {
        CounterpartyInfo.Unresolved(
            pendingCreation = request.pendingCreation == true,
        )
    }
    val updated = documentRepository.updateCounterparty(
        documentId = documentId,
        tenantId = tenantId,
        counterparty = counterparty,
    )
    if (!updated) {
        throw DokusException.InternalError("Failed to update document counterparty")
    }
}
