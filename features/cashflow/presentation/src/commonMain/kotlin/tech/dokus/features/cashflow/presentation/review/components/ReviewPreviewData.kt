@file:Suppress("LongMethod", "LongParameterList", "MagicNumber")

package tech.dokus.features.cashflow.presentation.review.components

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import tech.dokus.domain.Money
import tech.dokus.domain.enums.CashflowDirection
import tech.dokus.domain.enums.CashflowEntryStatus
import tech.dokus.domain.enums.CashflowSourceType
import tech.dokus.domain.enums.CounterpartyIntent
import tech.dokus.domain.enums.Currency
import tech.dokus.domain.enums.DocumentDirection
import tech.dokus.domain.enums.DocumentMatchReviewReasonType
import tech.dokus.domain.enums.DocumentMatchReviewStatus
import tech.dokus.domain.enums.DocumentMatchType
import tech.dokus.domain.enums.DocumentSource
import tech.dokus.domain.enums.DocumentSourceStatus
import tech.dokus.domain.enums.DocumentStatus
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.enums.ImportedBankTransactionStatus
import tech.dokus.domain.enums.PaymentCandidateTier
import tech.dokus.domain.ids.CashflowEntryId
import tech.dokus.domain.ids.DocumentBlobId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.DocumentMatchReviewId
import tech.dokus.domain.ids.DocumentSourceId
import tech.dokus.domain.ids.ImportedBankTransactionId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.CashflowEntry
import tech.dokus.domain.model.DocumentDraftDto
import tech.dokus.domain.model.DocumentDto
import tech.dokus.domain.model.ImportedBankTransactionDto
import tech.dokus.domain.model.DocumentPagePreviewDto
import tech.dokus.domain.model.DocumentRecordDto
import tech.dokus.domain.model.DocumentSourceDto
import tech.dokus.domain.model.DocumentMatchReviewSummaryDto
import tech.dokus.domain.model.FinancialLineItem
import tech.dokus.domain.model.InvoiceDraftData
import tech.dokus.domain.model.PartyDraft
import tech.dokus.domain.model.contact.CounterpartySnapshot
import tech.dokus.features.cashflow.presentation.review.DocumentPreviewState
import tech.dokus.features.cashflow.presentation.review.DocumentReviewState
import tech.dokus.features.cashflow.presentation.review.PaymentSheetState
import tech.dokus.features.cashflow.presentation.review.SourceEvidenceViewerState
import tech.dokus.foundation.app.state.DokusState

private val previewNow = LocalDateTime(2026, 2, 14, 9, 41, 0)
private val previewIssueDate = LocalDate(2026, 2, 14)
private val previewDueDate = LocalDate(2026, 2, 28)
private val previewTenantId = TenantId.parse("44e8ed5c-020a-4bbb-9439-ac85899c5589")
private val previewDocumentId = DocumentId.parse("e72f69a8-6913-4d8f-98e7-224db7f4133f")

internal fun previewReviewContentState(
    entryStatus: CashflowEntryStatus? = CashflowEntryStatus.Open,
    isDocumentConfirmed: Boolean = true,
    isEditMode: Boolean = false,
    previewState: DocumentPreviewState = DocumentPreviewState.Ready(
        pages = listOf(DocumentPagePreviewDto(page = 1, imageUrl = "/api/v1/documents/preview/pages/1.png")),
        totalPages = 1,
        renderedPages = 1,
        dpi = 180,
        hasMore = false,
    ),
    sourceViewerState: SourceEvidenceViewerState? = null,
    paymentSheetState: PaymentSheetState? = null,
    hasCrossMatchedSource: Boolean = true,
    showPendingMatchReview: Boolean = false,
): DocumentReviewState.Content {
    val tenantId = previewTenantId
    val documentId = previewDocumentId
    val draftData = InvoiceDraftData(
        direction = DocumentDirection.Inbound,
        invoiceNumber = "384421507",
        issueDate = previewIssueDate,
        dueDate = previewDueDate,
        currency = Currency.Eur,
        subtotalAmount = Money.from("239.67"),
        vatAmount = Money.from("49.33"),
        totalAmount = Money.from("289.00"),
        lineItems = listOf(
            FinancialLineItem(
                description = "Insurance premium - Q1 2026",
                quantity = 1,
                netAmount = 28900,
            )
        ),
        notes = "Insurance premium - Q1 2026",
        seller = PartyDraft(name = "KBC Bank NV"),
    )
    val draft = DocumentDraftDto(
        documentId = documentId,
        tenantId = tenantId,
        documentStatus = if (isDocumentConfirmed) DocumentStatus.Confirmed else DocumentStatus.NeedsReview,
        documentType = DocumentType.Invoice,
        direction = DocumentDirection.Inbound,
        extractedData = draftData,
        aiDraftData = draftData,
        aiDraftSourceRunId = null,
        draftVersion = 1,
        draftEditedAt = null,
        draftEditedBy = null,
        linkedContactId = null,
        counterpartySnapshot = CounterpartySnapshot(
            name = "KBC Bank NV",
            streetLine1 = "Havenlaan 2",
            postalCode = "1080",
            city = "Brussels"
        ),
        counterpartyIntent = CounterpartyIntent.None,
        lastSuccessfulRunId = null,
        createdAt = previewNow,
        updatedAt = previewNow,
    )

    val cashflowEntry = entryStatus?.let { status ->
        CashflowEntry(
            id = CashflowEntryId.generate(),
            tenantId = tenantId,
            sourceType = CashflowSourceType.Invoice,
            sourceId = "INV-384421507",
            documentId = documentId,
            direction = CashflowDirection.Out,
            eventDate = previewDueDate,
            amountGross = Money.from("289.00")!!,
            amountVat = Money.from("49.33")!!,
            remainingAmount = if (status == CashflowEntryStatus.Paid) {
                Money.from("0.00")!!
            } else {
                Money.from("289.00")!!
            },
            currency = Currency.Eur,
            status = status,
            paidAt = if (status == CashflowEntryStatus.Paid) {
                LocalDateTime(2026, 2, 15, 0, 0, 0)
            } else {
                null
            },
            contactId = null,
            contactName = "KBC Bank NV",
            description = "Insurance premium - Q1 2026",
            createdAt = previewNow,
            updatedAt = previewNow,
        )
    }

    val record = DocumentRecordDto(
        document = DocumentDto(
            id = documentId,
            tenantId = tenantId,
            filename = "KBC_384421507.pdf",
            contentType = "application/pdf",
            sizeBytes = 248_200,
            storageKey = "documents/$tenantId/KBC_384421507.pdf",
            source = DocumentSource.Upload,
            uploadedAt = previewNow,
        ),
        draft = draft,
        latestIngestion = null,
        confirmedEntity = null,
        cashflowEntryId = cashflowEntry?.id,
        pendingMatchReview = if (showPendingMatchReview) {
            DocumentMatchReviewSummaryDto(
                reviewId = DocumentMatchReviewId.generate(),
                reasonType = DocumentMatchReviewReasonType.MaterialConflict,
                status = DocumentMatchReviewStatus.Pending,
                createdAt = previewNow,
            )
        } else {
            null
        },
        sources = listOf(
            DocumentSourceDto(
                id = DocumentSourceId.generate(),
                tenantId = tenantId,
                documentId = documentId,
                blobId = DocumentBlobId.generate(),
                sourceChannel = DocumentSource.Upload,
                arrivalAt = previewNow,
                filename = "KBC_384421507.pdf",
                contentType = "application/pdf",
                sizeBytes = 248_200,
                status = DocumentSourceStatus.Linked,
                matchType = if (hasCrossMatchedSource) DocumentMatchType.SameContent else null,
            ),
            DocumentSourceDto(
                id = DocumentSourceId.generate(),
                tenantId = tenantId,
                documentId = documentId,
                blobId = DocumentBlobId.generate(),
                sourceChannel = DocumentSource.Peppol,
                arrivalAt = previewNow,
                filename = "UBL Invoice",
                contentType = "application/xml",
                sizeBytes = 4_800,
                status = DocumentSourceStatus.Linked,
                matchType = if (hasCrossMatchedSource) DocumentMatchType.SameDocument else null,
            ),
        ),
    )

    return DocumentReviewState.Content(
        documentId = documentId,
        document = record,
        draftData = draftData,
        originalData = draftData,
        previewState = previewState,
        hasUnsavedChanges = isEditMode,
        isEditMode = isEditMode,
        isDocumentConfirmed = isDocumentConfirmed,
        isDocumentRejected = false,
        confirmedCashflowEntryId = cashflowEntry?.id,
        cashflowEntryState = cashflowEntry?.let { DokusState.success(it) } ?: DokusState.idle(),
        sourceViewerState = sourceViewerState,
        paymentSheetState = paymentSheetState,
        counterpartyIntent = CounterpartyIntent.None,
    )
}

internal fun previewSourceEvidenceViewerState(
    sourceType: DocumentSource = DocumentSource.Peppol,
    previewState: DocumentPreviewState = DocumentPreviewState.NotPdf,
    isTechnicalDetailsExpanded: Boolean = false,
    rawContent: String? = "<Invoice>...</Invoice>",
): SourceEvidenceViewerState = SourceEvidenceViewerState(
    sourceId = DocumentSourceId.generate(),
    sourceName = if (sourceType == DocumentSource.Peppol) "UBL Invoice" else "Original document",
    sourceType = sourceType,
    sourceReceivedAt = previewNow,
    previewState = previewState,
    isTechnicalDetailsExpanded = isTechnicalDetailsExpanded,
    rawContent = rawContent,
)

internal fun previewPaymentSheetState(
    withError: Boolean = false,
    withSuggestedTransaction: Boolean = false,
    withTransactionPicker: Boolean = false,
): PaymentSheetState {
    val transactions = if (withSuggestedTransaction || withTransactionPicker) {
        previewImportedTransactions()
    } else {
        kotlin.collections.emptyList()
    }
    val selected = transactions.firstOrNull().takeIf { withSuggestedTransaction }
    val selectedAmount = when {
        selected == null -> null
        selected.signedAmount.isNegative -> -selected.signedAmount
        selected.signedAmount.isPositive -> selected.signedAmount
        else -> null
    }

    return PaymentSheetState(
        amountText = selectedAmount?.toDisplayString() ?: "289.00",
        amount = selectedAmount ?: Money.from("289.00"),
        paidAt = selected?.transactionDate ?: LocalDate(2026, 2, 15),
        note = "Bank transfer",
        suggestedTransaction = selected,
        selectedTransaction = selected,
        selectableTransactions = transactions,
        showTransactionPicker = withTransactionPicker,
        isLoadingTransactions = false,
        transactionsError = null,
        isSubmitting = false,
        amountError = if (withError) {
            tech.dokus.domain.exceptions.DokusException.Validation.PaymentAmountMustBePositive
        } else {
            null
        },
    )
}

internal fun previewImportedTransactions(): List<ImportedBankTransactionDto> = listOf(
    ImportedBankTransactionDto(
        id = ImportedBankTransactionId.parse("b038fd5b-c2b7-45b4-a0f2-f3a17d673aa3"),
        tenantId = previewTenantId,
        documentId = previewDocumentId,
        transactionDate = LocalDate(2026, 2, 15),
        signedAmount = Money.from("-289.00")!!,
        counterpartyName = "KBC Bank NV",
        counterpartyIban = "BE68539007547034",
        structuredCommunicationRaw = "+++123/4567/89123+++",
        descriptionRaw = "SEPA transfer premium Q1",
        rowConfidence = 0.97,
        largeAmountFlag = false,
        status = ImportedBankTransactionStatus.Suggested,
        linkedCashflowEntryId = null,
        suggestedCashflowEntryId = null,
        score = 0.93,
        tier = PaymentCandidateTier.Strong,
        createdAt = previewNow,
        updatedAt = previewNow,
    ),
    ImportedBankTransactionDto(
        id = ImportedBankTransactionId.parse("cbf4ded5-7e9d-4f66-b8f4-9751f98e3b0b"),
        tenantId = previewTenantId,
        documentId = previewDocumentId,
        transactionDate = LocalDate(2026, 2, 12),
        signedAmount = Money.from("-289.00")!!,
        counterpartyName = "KBC Bank NV",
        counterpartyIban = null,
        structuredCommunicationRaw = null,
        descriptionRaw = "Transfer KBC",
        rowConfidence = 0.91,
        largeAmountFlag = false,
        status = ImportedBankTransactionStatus.Unmatched,
        linkedCashflowEntryId = null,
        suggestedCashflowEntryId = null,
        score = 0.74,
        tier = PaymentCandidateTier.Possible,
        createdAt = previewNow,
        updatedAt = previewNow,
    ),
    ImportedBankTransactionDto(
        id = ImportedBankTransactionId.parse("f1496fba-d577-4f95-84f5-c75ef229f6cb"),
        tenantId = previewTenantId,
        documentId = previewDocumentId,
        transactionDate = LocalDate(2026, 2, 10),
        signedAmount = Money.from("-300.00")!!,
        counterpartyName = "AXA Belgium",
        counterpartyIban = null,
        structuredCommunicationRaw = null,
        descriptionRaw = "AXA insurance transfer",
        rowConfidence = 0.95,
        largeAmountFlag = false,
        status = ImportedBankTransactionStatus.Unmatched,
        linkedCashflowEntryId = null,
        suggestedCashflowEntryId = null,
        score = 0.70,
        tier = PaymentCandidateTier.Possible,
        createdAt = previewNow,
        updatedAt = previewNow,
    ),
)
