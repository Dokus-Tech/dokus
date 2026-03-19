@file:Suppress("LongMethod", "LongParameterList", "MagicNumber")

package tech.dokus.features.cashflow.presentation.review.components

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import tech.dokus.domain.Money
import tech.dokus.domain.enums.CashflowDirection
import tech.dokus.domain.enums.CashflowEntryStatus
import tech.dokus.domain.enums.CashflowSourceType
import tech.dokus.domain.enums.Currency
import tech.dokus.domain.enums.AutoMatchStatus
import tech.dokus.domain.enums.DocumentDirection
import tech.dokus.domain.enums.ReviewReason
import tech.dokus.domain.enums.DocumentMatchReviewStatus
import tech.dokus.domain.enums.SourceMatchKind
import tech.dokus.domain.enums.DocumentSource
import tech.dokus.domain.enums.DocumentSourceStatus
import tech.dokus.domain.enums.DocumentStatus
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.enums.BankTransactionStatus
import tech.dokus.domain.ids.CashflowEntryId
import tech.dokus.domain.ids.Iban
import tech.dokus.domain.ids.DocumentBlobId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.DocumentMatchReviewId
import tech.dokus.domain.ids.DocumentSourceId
import tech.dokus.domain.ids.BankTransactionId
import tech.dokus.domain.ids.PaymentId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.model.AutoPaymentStatus
import tech.dokus.domain.model.CashflowContactRef
import tech.dokus.domain.model.CashflowEntry
import tech.dokus.domain.model.DocumentDraftDto
import tech.dokus.domain.model.DocumentDto
import tech.dokus.domain.model.BankTransactionDto
import tech.dokus.domain.model.TransactionCommunication
import tech.dokus.domain.ids.StructuredCommunication
import tech.dokus.domain.model.Dpi
import tech.dokus.domain.model.DocumentPagePreviewDto
import tech.dokus.domain.model.DocumentDetailDto
import tech.dokus.domain.model.DocumentSourceDto
import tech.dokus.domain.model.DocumentMatchReviewSummaryDto
import tech.dokus.domain.model.FinancialLineItem
import tech.dokus.domain.model.BankStatementDraftData
import tech.dokus.domain.model.CreditNoteDraftData
import tech.dokus.domain.model.DocumentDraftData
import tech.dokus.domain.model.InvoiceDraftData
import tech.dokus.domain.model.PartyDraft
import tech.dokus.domain.model.ReceiptDraftData
import tech.dokus.domain.model.toDocDto
import tech.dokus.domain.model.toDocumentType
import tech.dokus.domain.model.toEmptyDraftData
import tech.dokus.domain.model.contact.CounterpartySnapshot
import tech.dokus.domain.model.contact.ResolvedContact
import tech.dokus.features.cashflow.presentation.review.DocumentPreviewState
import tech.dokus.features.cashflow.presentation.review.DocumentReviewState
import tech.dokus.features.cashflow.presentation.review.PaymentSheetState
import tech.dokus.features.cashflow.presentation.review.ReviewDocumentData
import tech.dokus.features.cashflow.presentation.review.SourceEvidenceViewerState
import tech.dokus.foundation.app.state.DokusState

private val previewNow = LocalDateTime(2026, 2, 14, 9, 41, 0)
private val previewIssueDate = LocalDate(2026, 2, 14)
private val previewDueDate = LocalDate(2026, 2, 28)
private val previewTenantId = TenantId.parse("44e8ed5c-020a-4bbb-9439-ac85899c5589")
private val previewDocumentId = DocumentId.parse("e72f69a8-6913-4d8f-98e7-224db7f4133f")

internal fun previewReviewContentState(
    entryStatus: CashflowEntryStatus? = CashflowEntryStatus.Open,
    documentStatus: DocumentStatus = DocumentStatus.Confirmed,
    hasUnsyncedChanges: Boolean = false,
    previewState: DocumentPreviewState = DocumentPreviewState.Ready(
        pages = listOf(DocumentPagePreviewDto(page = 1, imageUrl = "/api/v1/documents/preview/pages/1.png")),
        totalPages = 1,
        renderedPages = 1,
        dpi = Dpi.create(180),
        hasMore = false,
    ),
    sourceViewerState: SourceEvidenceViewerState? = null,
    paymentSheetState: PaymentSheetState? = null,
    autoPaymentStatus: DokusState<AutoPaymentStatus> = DokusState.idle(),
    isUndoingAutoPayment: Boolean = false,
    hasCrossMatchedSource: Boolean = true,
    showPendingMatchReview: Boolean = false,
): DocumentReviewState {
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
        documentStatus = documentStatus,
        documentType = DocumentType.Invoice,
        direction = DocumentDirection.Inbound,
        content = draftData.toDocDto(),
        aiDraftSourceRunId = null,
        draftVersion = 1,
        draftEditedAt = null,
        draftEditedBy = null,
        resolvedContact = ResolvedContact.Detected(
            name = "KBC Bank NV",
            vatNumber = null,
            iban = null,
            address = "Havenlaan 2, 1080 Brussels",
        ),
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
            contact = CashflowContactRef(id = ContactId.parse("00000000-0000-0000-0000-000000000001"), name = "KBC Bank NV"),
            description = "Insurance premium - Q1 2026",
            createdAt = previewNow,
            updatedAt = previewNow,
        )
    }

    val record = DocumentDetailDto(
        document = DocumentDto(
            id = documentId,
            tenantId = tenantId,
            filename = "KBC_384421507.pdf",
            uploadedAt = previewNow,
            sortDate = previewNow.date,
        ),
        draft = draft,
        latestIngestion = null,
        cashflowEntryId = cashflowEntry?.id,
        pendingMatchReview = if (showPendingMatchReview) {
            DocumentMatchReviewSummaryDto(
                reviewId = DocumentMatchReviewId.generate(),
                reasonType = ReviewReason.MaterialConflict,
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
                matchType = if (hasCrossMatchedSource) SourceMatchKind.SameContent else null,
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
                matchType = if (hasCrossMatchedSource) SourceMatchKind.SameDocument else null,
            ),
        ),
    )

    val docDtoContent = draftData.toDocDto()
    return DocumentReviewState(
        document = DokusState.success(
            ReviewDocumentData(
                documentId = documentId,
                documentRecord = record,
                draftData = docDtoContent,
                originalData = docDtoContent,
                previewUrl = null,
                contactSuggestions = emptyList(),
            )
        ),
        previewState = previewState,
        hasUnsavedChanges = hasUnsyncedChanges,
        documentStatus = documentStatus,
        confirmedCashflowEntryId = cashflowEntry?.id,
        cashflowEntryState = cashflowEntry?.let { DokusState.success(it) } ?: DokusState.idle(),
        autoPaymentStatus = autoPaymentStatus,
        isUndoingAutoPayment = isUndoingAutoPayment,
        sourceViewerState = sourceViewerState,
        paymentSheetState = paymentSheetState,
        today = LocalDate(2026, 3, 1),
    )
}

internal fun previewAutoPaymentStatus(
    canUndo: Boolean = true,
    confidenceScore: Double = 0.97,
): DokusState<AutoPaymentStatus> = DokusState.success(
    AutoPaymentStatus.AutoPaid(
        paymentId = PaymentId.parse("6cc26605-d49d-480a-ad2e-93fca770de95"),
        bankTransactionId = BankTransactionId.parse("b038fd5b-c2b7-45b4-a0f2-f3a17d673aa3"),
        confidenceScore = confidenceScore,
        reasons = listOf("structured_reference_match", "exact_amount", "date_proximity"),
        autoPaidAt = LocalDateTime(2026, 2, 15, 7, 33, 0),
        canUndo = canUndo,
    )
)

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

internal fun previewStateForDocumentType(
    documentType: DocumentType,
): DocumentReviewState {
    val tenantId = previewTenantId
    val documentId = previewDocumentId
    val draftData = previewDraftData(documentType)
    val resolvedType = draftData.toDocumentType()

    val hasFinancialEntry = resolvedType == DocumentType.Invoice ||
        resolvedType == DocumentType.CreditNote ||
        resolvedType == DocumentType.Receipt

    val docDtoContent = draftData.toDocDto()
    val draft = DocumentDraftDto(
        documentId = documentId,
        tenantId = tenantId,
        documentStatus = if (resolvedType.supported) DocumentStatus.Confirmed else DocumentStatus.Confirmed,
        documentType = resolvedType,
        direction = docDtoContent.direction,
        content = docDtoContent,
        aiDraftSourceRunId = null,
        draftVersion = 1,
        draftEditedAt = null,
        draftEditedBy = null,
        resolvedContact = ResolvedContact.Detected(
            name = "KBC Bank NV",
            vatNumber = null,
            iban = null,
            address = null,
        ),
        lastSuccessfulRunId = null,
        createdAt = previewNow,
        updatedAt = previewNow,
    )

    val cashflowEntry = if (hasFinancialEntry) {
        CashflowEntry(
            id = CashflowEntryId.generate(),
            tenantId = tenantId,
            sourceType = CashflowSourceType.Invoice,
            sourceId = "DOC-${resolvedType.dbValue}",
            documentId = documentId,
            direction = CashflowDirection.Out,
            eventDate = previewDueDate,
            amountGross = Money.from("289.00")!!,
            amountVat = Money.from("49.33")!!,
            remainingAmount = Money.from("289.00")!!,
            currency = Currency.Eur,
            status = CashflowEntryStatus.Open,
            paidAt = null,
            contact = CashflowContactRef(id = ContactId.parse("00000000-0000-0000-0000-000000000001"), name = "KBC Bank NV"),
            description = "Preview - ${resolvedType.name}",
            createdAt = previewNow,
            updatedAt = previewNow,
        )
    } else {
        null
    }

    val record = DocumentDetailDto(
        document = DocumentDto(
            id = documentId,
            tenantId = tenantId,
            filename = "${resolvedType.dbValue.lowercase()}_sample.pdf",
            uploadedAt = previewNow,
            sortDate = previewNow.date,
        ),
        draft = draft,
        latestIngestion = null,
        cashflowEntryId = cashflowEntry?.id,
        pendingMatchReview = null,
        sources = listOf(
            DocumentSourceDto(
                id = DocumentSourceId.generate(),
                tenantId = tenantId,
                documentId = documentId,
                blobId = DocumentBlobId.generate(),
                sourceChannel = DocumentSource.Upload,
                arrivalAt = previewNow,
                filename = "${resolvedType.dbValue.lowercase()}_sample.pdf",
                contentType = "application/pdf",
                sizeBytes = 248_200,
                status = DocumentSourceStatus.Linked,
                matchType = null,
            ),
        ),
    )

    return DocumentReviewState(
        document = DokusState.success(
            ReviewDocumentData(
                documentId = documentId,
                documentRecord = record,
                draftData = docDtoContent,
                originalData = docDtoContent,
                previewUrl = null,
                contactSuggestions = emptyList(),
            )
        ),
        previewState = DocumentPreviewState.Ready(
            pages = listOf(DocumentPagePreviewDto(page = 1, imageUrl = "/api/v1/documents/preview/pages/1.png")),
            totalPages = 1,
            renderedPages = 1,
            dpi = Dpi.create(180),
            hasMore = false,
        ),
        hasUnsavedChanges = false,
        documentStatus = DocumentStatus.Confirmed,
        confirmedCashflowEntryId = cashflowEntry?.id,
        cashflowEntryState = cashflowEntry?.let { DokusState.success(it) } ?: DokusState.idle(),
        autoPaymentStatus = DokusState.idle(),
        isUndoingAutoPayment = false,
        sourceViewerState = null,
        paymentSheetState = null,
        today = LocalDate(2026, 3, 1),
    )
}

@Suppress("CyclomaticComplexMethod")
private fun previewDraftData(type: DocumentType): DocumentDraftData = when (type) {
    DocumentType.Invoice -> InvoiceDraftData(
        direction = DocumentDirection.Inbound,
        invoiceNumber = "384421507",
        issueDate = previewIssueDate,
        dueDate = previewDueDate,
        currency = Currency.Eur,
        subtotalAmount = Money.from("239.67"),
        vatAmount = Money.from("49.33"),
        totalAmount = Money.from("289.00"),
        lineItems = listOf(
            FinancialLineItem(description = "Insurance premium - Q1 2026", quantity = 1, netAmount = 28900),
        ),
        notes = "Insurance premium - Q1 2026",
        seller = PartyDraft(name = "KBC Bank NV"),
    )
    DocumentType.CreditNote -> CreditNoteDraftData(
        direction = DocumentDirection.Inbound,
        creditNoteNumber = "CN-2026-0042",
        issueDate = previewIssueDate,
        originalInvoiceNumber = "384421507",
        currency = Currency.Eur,
        subtotalAmount = Money.from("82.64"),
        vatAmount = Money.from("17.36"),
        totalAmount = Money.from("100.00"),
        lineItems = listOf(
            FinancialLineItem(description = "Pricing correction", quantity = 1, netAmount = 10000),
        ),
        reason = "Pricing correction",
        seller = PartyDraft(name = "KBC Bank NV"),
    )
    DocumentType.Receipt -> ReceiptDraftData(
        receiptNumber = "R-2026-0199",
        date = previewIssueDate,
        totalAmount = Money.from("45.50"),
        vatAmount = Money.from("7.89"),
        currency = Currency.Eur,
        notes = "Office supplies",
    )
    DocumentType.BankStatement -> BankStatementDraftData(
        accountIban = Iban("BE68539007547034"),
        periodStart = previewIssueDate,
        periodEnd = previewDueDate,
    )
    DocumentType.Unknown -> InvoiceDraftData(
        direction = DocumentDirection.Unknown,
        currency = Currency.Eur,
    )
    else -> type.toEmptyDraftData()
}

internal fun previewImportedTransactions(): List<BankTransactionDto> = listOf(
    BankTransactionDto(
        id = BankTransactionId.parse("b038fd5b-c2b7-45b4-a0f2-f3a17d673aa3"),
        tenantId = previewTenantId,
        documentId = previewDocumentId,
        transactionDate = LocalDate(2026, 2, 15),
        signedAmount = Money.from("-289.00")!!,
        counterparty = CounterpartySnapshot(
            name = "KBC Bank NV",
            iban = Iban("BE68539007547034"),
        ),
        communication = TransactionCommunication.Structured(
            raw = "+++123/4567/89123+++",
            normalized = StructuredCommunication("+++123/4567/89123+++"),
        ),
        descriptionRaw = "SEPA transfer premium Q1",
        status = BankTransactionStatus.NeedsReview,
        createdAt = previewNow,
        updatedAt = previewNow,
    ),
    BankTransactionDto(
        id = BankTransactionId.parse("cbf4ded5-7e9d-4f66-b8f4-9751f98e3b0b"),
        tenantId = previewTenantId,
        documentId = previewDocumentId,
        transactionDate = LocalDate(2026, 2, 12),
        signedAmount = Money.from("-289.00")!!,
        counterparty = CounterpartySnapshot(name = "KBC Bank NV"),
        descriptionRaw = "Transfer KBC",
        status = BankTransactionStatus.Unmatched,
        createdAt = previewNow,
        updatedAt = previewNow,
    ),
    BankTransactionDto(
        id = BankTransactionId.parse("f1496fba-d577-4f95-84f5-c75ef229f6cb"),
        tenantId = previewTenantId,
        documentId = previewDocumentId,
        transactionDate = LocalDate(2026, 2, 10),
        signedAmount = Money.from("-300.00")!!,
        counterparty = CounterpartySnapshot(name = "AXA Belgium"),
        descriptionRaw = "AXA insurance transfer",
        status = BankTransactionStatus.Unmatched,
        createdAt = previewNow,
        updatedAt = previewNow,
    ),
)
