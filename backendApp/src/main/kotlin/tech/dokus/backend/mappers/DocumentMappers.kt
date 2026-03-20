@file:Suppress("LongParameterList")

package tech.dokus.backend.mappers

import kotlinx.serialization.json.JsonElement
import tech.dokus.backend.routes.cashflow.documents.ConfirmedBankStatement
import tech.dokus.database.entity.BankTransactionEntity
import tech.dokus.database.entity.CreditNoteEntity
import tech.dokus.database.entity.DocumentMatchReviewEntity
import tech.dokus.database.entity.DocumentSourceEntity
import tech.dokus.database.entity.DraftSummaryEntity
import tech.dokus.database.entity.ExpenseEntity
import tech.dokus.database.entity.IngestionRunSummaryEntity
import tech.dokus.database.entity.InvoiceEntity
import tech.dokus.domain.Quantity
import tech.dokus.domain.enums.CreditNoteType
import tech.dokus.domain.enums.DocumentDirection
import tech.dokus.domain.model.BankStatementTransactionDraftRowDto
import tech.dokus.domain.model.DocDto
import tech.dokus.domain.model.DocLineItem
import tech.dokus.domain.model.DocumentDraftDto
import tech.dokus.domain.model.DocumentIngestionDto
import tech.dokus.domain.model.DocumentMatchReviewSummaryDto
import tech.dokus.domain.model.DocumentProcessingStepDto
import tech.dokus.domain.model.DocumentSourceDto
import tech.dokus.domain.model.InvoicePaymentInfo
import tech.dokus.domain.model.InvoicePeppolInfo
import tech.dokus.domain.model.PartyDraftDto
import tech.dokus.domain.model.PaymentLinkInfo
import tech.dokus.domain.model.TransactionCommunicationDto
import tech.dokus.domain.model.contact.ContactSuggestionDto
import tech.dokus.domain.model.contact.CounterpartySnapshotDto
import tech.dokus.domain.model.contact.ResolvedContact
import tech.dokus.domain.utils.parseSafe

// =============================================================================
// Entity -> DocDto conversions
// =============================================================================

fun DocDto.Invoice.Confirmed.Companion.from(entity: InvoiceEntity): DocDto.Invoice.Confirmed =
    DocDto.Invoice.Confirmed(
        id = entity.id,
        tenantId = entity.tenantId,
        contactId = entity.contactId,
        direction = entity.direction,
        invoiceNumber = entity.invoiceNumber.value,
        issueDate = entity.issueDate,
        dueDate = entity.dueDate,
        currency = entity.currency,
        subtotalAmount = entity.subtotalAmount,
        vatAmount = entity.vatAmount,
        totalAmount = entity.totalAmount,
        paidAmount = entity.paidAmount,
        lineItems = entity.items.map { item ->
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
        iban = entity.senderIban,
        notes = entity.notes,
        status = entity.status,
        structuredCommunication = entity.structuredCommunication,
        peppol = if (entity.peppolId != null && entity.peppolSentAt != null) InvoicePeppolInfo(
            peppolId = entity.peppolId!!,
            sentAt = entity.peppolSentAt!!,
            status = entity.peppolStatus ?: tech.dokus.domain.enums.PeppolStatus.Pending
        ) else null,
        paymentLinkInfo = if (entity.paymentLink != null) PaymentLinkInfo(
            url = entity.paymentLink!!,
            expiresAt = entity.paymentLinkExpiresAt
        ) else null,
        paymentInfo = if (entity.paidAt != null) InvoicePaymentInfo(
            paidAt = entity.paidAt!!,
            paymentMethod = entity.paymentMethod ?: tech.dokus.domain.enums.PaymentMethod.BankTransfer
        ) else null,
        documentId = entity.documentId,
        confirmedAt = entity.confirmedAt,
        confirmedBy = entity.confirmedBy,
        createdAt = entity.createdAt,
        updatedAt = entity.updatedAt,
    )

fun DocDto.Receipt.Confirmed.Companion.from(entity: ExpenseEntity): DocDto.Receipt.Confirmed =
    DocDto.Receipt.Confirmed(
        id = entity.id,
        tenantId = entity.tenantId,
        direction = DocumentDirection.Inbound,
        merchantName = entity.merchant,
        merchantVat = null,
        date = entity.date,
        currency = tech.dokus.domain.enums.Currency.Eur,
        totalAmount = entity.amount,
        vatAmount = entity.vatAmount,
        lineItems = emptyList(),
        receiptNumber = null,
        notes = entity.notes,
        vatRate = entity.vatRate,
        category = entity.category,
        isDeductible = entity.isDeductible,
        deductiblePercentage = entity.deductiblePercentage,
        paymentMethod = entity.paymentMethod,
        contactId = entity.contactId,
        documentId = entity.documentId,
        confirmedAt = entity.confirmedAt,
        confirmedBy = entity.confirmedBy,
        createdAt = entity.createdAt,
        updatedAt = entity.updatedAt,
    )

fun DocDto.CreditNote.Confirmed.Companion.from(entity: CreditNoteEntity): DocDto.CreditNote.Confirmed =
    DocDto.CreditNote.Confirmed(
        id = entity.id,
        tenantId = entity.tenantId,
        contactId = entity.contactId,
        creditNoteType = entity.creditNoteType,
        direction = when (entity.creditNoteType) {
            CreditNoteType.Sales -> DocumentDirection.Outbound
            CreditNoteType.Purchase -> DocumentDirection.Inbound
        },
        creditNoteNumber = entity.creditNoteNumber,
        issueDate = entity.issueDate,
        currency = entity.currency,
        subtotalAmount = entity.subtotalAmount,
        vatAmount = entity.vatAmount,
        totalAmount = entity.totalAmount,
        lineItems = emptyList(),
        status = entity.status,
        settlementIntent = entity.settlementIntent,
        reason = entity.reason,
        notes = entity.notes,
        documentId = entity.documentId,
        confirmedAt = entity.confirmedAt,
        confirmedBy = entity.confirmedBy,
        createdAt = entity.createdAt,
        updatedAt = entity.updatedAt,
    )

internal fun DocDto.BankStatement.Draft.Companion.from(statement: ConfirmedBankStatement): DocDto.BankStatement.Draft =
    DocDto.BankStatement.Draft(
        direction = DocumentDirection.Neutral,
        accountIban = statement.statement.accountIban,
        openingBalance = statement.statement.openingBalance,
        closingBalance = statement.statement.closingBalance,
        periodStart = statement.statement.periodStart,
        periodEnd = statement.statement.periodEnd,
        notes = null,
        institution = PartyDraftDto(),
        transactions = statement.transactions.map { BankStatementTransactionDraftRowDto.from(it) },
    )

fun BankStatementTransactionDraftRowDto.Companion.from(entity: BankTransactionEntity): BankStatementTransactionDraftRowDto =
    BankStatementTransactionDraftRowDto(
        transactionDate = entity.transactionDate,
        signedAmount = entity.signedAmount,
        counterparty = CounterpartySnapshotDto(
            name = entity.counterpartyName,
            iban = entity.counterpartyIban,
            bic = entity.counterpartyBic,
        ),
        communication = TransactionCommunicationDto.from(
            structuredCommunicationRaw = entity.structuredCommunicationRaw,
            freeCommunication = entity.freeCommunication,
        ),
        descriptionRaw = entity.descriptionRaw,
        rowConfidence = 1.0,
        largeAmountFlag = false,
        excluded = false,
        potentialDuplicate = false,
    )

// =============================================================================
// Entity -> DTO conversions
// =============================================================================

fun DocumentDraftDto.Companion.from(
    entity: DraftSummaryEntity,
    resolvedContact: ResolvedContact = ResolvedContact.Unknown,
    contactSuggestions: List<ContactSuggestionDto> = emptyList(),
    content: DocDto? = null,
): DocumentDraftDto = DocumentDraftDto(
    documentId = entity.documentId,
    tenantId = entity.tenantId,
    documentStatus = entity.documentStatus,
    documentType = entity.documentType,
    direction = entity.direction,
    content = content,
    aiKeywords = entity.aiKeywords,
    purposeBase = entity.purposeBase,
    purposePeriodYear = entity.purposePeriodYear,
    purposePeriodMonth = entity.purposePeriodMonth,
    purposeRendered = entity.purposeRendered,
    purposeSource = entity.purposeSource,
    purposeLocked = entity.purposeLocked,
    purposePeriodMode = entity.purposePeriodMode,
    aiDraftSourceRunId = entity.aiDraftSourceRunId,
    draftVersion = entity.draftVersion,
    draftEditedAt = entity.draftEditedAt,
    draftEditedBy = entity.draftEditedBy,
    resolvedContact = resolvedContact,
    contactSuggestions = contactSuggestions,
    rejectReason = entity.rejectReason,
    lastSuccessfulRunId = entity.lastSuccessfulRunId,
    createdAt = entity.createdAt,
    updatedAt = entity.updatedAt
)

fun DocumentIngestionDto.Companion.from(
    entity: IngestionRunSummaryEntity,
    includeRawExtraction: Boolean = false,
    includeTrace: Boolean = false,
): DocumentIngestionDto {
    val rawExtraction = if (includeRawExtraction) {
        entity.rawExtractionJson?.let { parseSafe<JsonElement>(it).getOrNull() }
    } else {
        null
    }

    val processingTrace = if (includeTrace) {
        entity.processingTrace?.let { parseSafe<List<DocumentProcessingStepDto>>(it).getOrNull() }
    } else {
        null
    }

    return DocumentIngestionDto(
        id = entity.id,
        documentId = entity.documentId,
        tenantId = entity.tenantId,
        status = entity.status,
        provider = entity.provider,
        queuedAt = entity.queuedAt,
        startedAt = entity.startedAt,
        finishedAt = entity.finishedAt,
        errorMessage = entity.errorMessage,
        confidence = entity.confidence,
        processingOutcome = entity.processingOutcome,
        rawExtraction = rawExtraction,
        processingTrace = processingTrace
    )
}

fun DocumentSourceDto.Companion.from(entity: DocumentSourceEntity): DocumentSourceDto = DocumentSourceDto(
    id = entity.id,
    tenantId = entity.tenantId,
    documentId = entity.documentId,
    blobId = entity.blobId,
    peppolRawUblBlobId = entity.peppolRawUblBlobId,
    sourceChannel = entity.sourceChannel,
    arrivalAt = entity.arrivalAt,
    contentHash = entity.contentHash,
    identityKeyHash = entity.identityKeyHash,
    status = entity.status,
    isCorrective = entity.isCorrective,
    extractedSnapshotJson = entity.extractedSnapshotJson,
    peppolStructuredSnapshotJson = entity.peppolStructuredSnapshotJson,
    peppolSnapshotVersion = entity.peppolSnapshotVersion,
    detachedAt = entity.detachedAt,
    filename = entity.filename,
    contentType = entity.contentType,
    sizeBytes = entity.sizeBytes,
    matchType = entity.matchType
)

fun DocumentMatchReviewSummaryDto.Companion.from(entity: DocumentMatchReviewEntity): DocumentMatchReviewSummaryDto =
    DocumentMatchReviewSummaryDto(
        reviewId = entity.id,
        incomingSourceId = entity.incomingSourceId,
        reasonType = entity.reasonType,
        status = entity.status,
        createdAt = entity.createdAt
    )
