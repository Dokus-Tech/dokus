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
import tech.dokus.domain.ids.CashflowEntryId
import tech.dokus.domain.ids.DocumentBlobId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.DocumentMatchReviewId
import tech.dokus.domain.ids.DocumentSourceId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.CashflowEntry
import tech.dokus.domain.model.DocumentDraftDto
import tech.dokus.domain.model.DocumentDto
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
    val tenantId = TenantId.parse("44e8ed5c-020a-4bbb-9439-ac85899c5589")
    val documentId = DocumentId.parse("e72f69a8-6913-4d8f-98e7-224db7f4133f")
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
): PaymentSheetState = PaymentSheetState(
    amountText = "289.00",
    amount = Money.from("289.00"),
    paidAt = LocalDate(2026, 2, 15),
    note = "Bank transfer",
    isSubmitting = false,
    amountError = if (withError) {
        tech.dokus.domain.exceptions.DokusException.Validation.PaymentAmountMustBePositive
    } else {
        null
    },
)
