package tech.dokus.features.cashflow.presentation.review

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import tech.dokus.domain.Money
import tech.dokus.domain.enums.CashflowDirection
import tech.dokus.domain.enums.CashflowEntryStatus
import tech.dokus.domain.enums.CashflowSourceType
import tech.dokus.domain.enums.DocumentDirection
import tech.dokus.domain.enums.DocumentMatchReviewReasonType
import tech.dokus.domain.enums.DocumentMatchReviewStatus
import tech.dokus.domain.enums.DocumentMatchType
import tech.dokus.domain.enums.DocumentSource
import tech.dokus.domain.enums.DocumentSourceStatus
import tech.dokus.domain.enums.DocumentStatus
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.enums.IngestionStatus
import tech.dokus.domain.ids.CashflowEntryId
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.DocumentBlobId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.DocumentMatchReviewId
import tech.dokus.domain.ids.DocumentSourceId
import tech.dokus.domain.ids.IngestionRunId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.CashflowEntry
import tech.dokus.domain.model.CreditNoteDraftData
import tech.dokus.domain.model.DocumentDraftData
import tech.dokus.domain.model.DocumentDraftDto
import tech.dokus.domain.model.DocumentDto
import tech.dokus.domain.model.DocumentIngestionDto
import tech.dokus.domain.model.DocumentMatchReviewSummaryDto
import tech.dokus.domain.model.DocumentRecordDto
import tech.dokus.domain.model.DocumentSourceDto
import tech.dokus.domain.model.InvoiceDraftData
import tech.dokus.domain.model.ReceiptDraftData
import tech.dokus.foundation.app.state.DokusState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DocumentReviewCanonicalStateTest {

    @Test
    fun `invoice uses canonical center`() {
        val state = contentState(invoiceDraft())

        assertTrue(state.canRenderCanonical)
        assertFalse(state.shouldUsePdfFallback)
    }

    @Test
    fun `credit note uses canonical center`() {
        val state = contentState(creditNoteDraft())

        assertTrue(state.canRenderCanonical)
        assertFalse(state.shouldUsePdfFallback)
    }

    @Test
    fun `receipt uses pdf fallback`() {
        val state = contentState(receiptDraft())

        assertFalse(state.canRenderCanonical)
        assertTrue(state.shouldUsePdfFallback)
    }

    @Test
    fun `confirmed document without cashflow entry is unpaid`() {
        val state = contentState(
            draftData = invoiceDraft(),
            isDocumentConfirmed = true,
            cashflowEntryState = DokusState.idle(),
        )

        assertEquals(ReviewFinancialStatus.Unpaid, state.financialStatus)
    }

    @Test
    fun `paid cashflow entry maps to paid financial status`() {
        val state = contentState(
            draftData = invoiceDraft(),
            isDocumentConfirmed = true,
            cashflowEntryState = DokusState.success(cashflowEntry(CashflowEntryStatus.Paid)),
        )

        assertEquals(ReviewFinancialStatus.Paid, state.financialStatus)
    }

    @Test
    fun `processing state keeps review status even with paid entry`() {
        val state = contentState(
            draftData = invoiceDraft(),
            ingestionStatus = IngestionStatus.Processing,
            isDocumentConfirmed = true,
            cashflowEntryState = DokusState.success(cashflowEntry(CashflowEntryStatus.Paid)),
        )

        assertEquals(ReviewFinancialStatus.Review, state.financialStatus)
    }

    @Test
    fun `overdue days are derived from due date`() {
        val state = contentState(
            draftData = invoiceDraft(dueDate = LocalDate(2000, 1, 1)),
            isDocumentConfirmed = true,
            cashflowEntryState = DokusState.idle(),
        )

        assertEquals(ReviewFinancialStatus.Overdue, state.financialStatus)
        assertTrue((state.overdueDays ?: 0) > 0)
    }

    @Test
    fun `cross-match is visible when linked source has trusted match type and no pending review`() {
        val state = contentState(
            draftData = invoiceDraft(),
            sources = listOf(source(matchType = DocumentMatchType.ExactFile)),
            pendingMatchReview = null,
        )

        assertTrue(state.hasCrossMatchedSources)
    }

    @Test
    fun `cross-match is hidden when pending review exists`() {
        val state = contentState(
            draftData = invoiceDraft(),
            sources = listOf(source(matchType = DocumentMatchType.SameDocument)),
            pendingMatchReview = pendingReview(),
        )

        assertFalse(state.hasCrossMatchedSources)
    }

    @Test
    fun `cross-match is hidden when source is not linked`() {
        val state = contentState(
            draftData = invoiceDraft(),
            sources = listOf(
                source(
                    matchType = DocumentMatchType.SameContent,
                    status = DocumentSourceStatus.Detached,
                )
            ),
        )

        assertFalse(state.hasCrossMatchedSources)
    }

    private fun contentState(
        draftData: DocumentDraftData,
        ingestionStatus: IngestionStatus? = null,
        isDocumentConfirmed: Boolean = false,
        cashflowEntryState: DokusState<CashflowEntry> = DokusState.idle(),
        sources: List<DocumentSourceDto> = emptyList(),
        pendingMatchReview: DocumentMatchReviewSummaryDto? = null,
    ): DocumentReviewState.Content {
        val tenantId = TenantId.parse("44e8ed5c-020a-4bbb-9439-ac85899c5589")
        val documentId = DocumentId.parse("e72f69a8-6913-4d8f-98e7-224db7f4133f")
        val now = LocalDateTime(2026, 2, 11, 0, 0, 0)

        val draft = DocumentDraftDto(
            documentId = documentId,
            tenantId = tenantId,
            documentStatus = if (isDocumentConfirmed) DocumentStatus.Confirmed else DocumentStatus.NeedsReview,
            documentType = when (draftData) {
                is InvoiceDraftData -> DocumentType.Invoice
                is CreditNoteDraftData -> DocumentType.CreditNote
                is ReceiptDraftData -> DocumentType.Receipt
            },
            extractedData = draftData,
            aiDraftData = draftData,
            aiDraftSourceRunId = null,
            draftVersion = 1,
            draftEditedAt = null,
            draftEditedBy = null,
            linkedContactId = null,
            lastSuccessfulRunId = null,
            createdAt = now,
            updatedAt = now,
        )

        val ingestion = ingestionStatus?.let { status ->
            DocumentIngestionDto(
                id = IngestionRunId.generate(),
                documentId = documentId,
                tenantId = tenantId,
                status = status,
                provider = "openai",
                queuedAt = now,
                startedAt = if (status != IngestionStatus.Queued) now else null,
                finishedAt = if (status == IngestionStatus.Succeeded || status == IngestionStatus.Failed) now else null,
                errorMessage = null,
                confidence = 0.9,
            )
        }

        val record = DocumentRecordDto(
            document = DocumentDto(
                id = documentId,
                tenantId = tenantId,
                filename = "invoice.pdf",
                contentType = "application/pdf",
                sizeBytes = 1200L,
                storageKey = "documents/$tenantId/invoice.pdf",
                source = DocumentSource.Upload,
                uploadedAt = now,
            ),
            draft = draft,
            latestIngestion = ingestion,
            confirmedEntity = null,
            pendingMatchReview = pendingMatchReview,
            sources = sources,
        )

        return DocumentReviewState.Content(
            documentId = documentId,
            document = record,
            draftData = draftData,
            originalData = draftData,
            selectedContactId = ContactId.generate(),
            contactSelectionState = ContactSelectionState.Selected,
            isContactRequired = true,
            isDocumentConfirmed = isDocumentConfirmed,
            cashflowEntryState = cashflowEntryState,
        )
    }

    private fun invoiceDraft(
        issueDate: LocalDate = LocalDate(2026, 2, 10),
        dueDate: LocalDate = LocalDate(2026, 2, 28),
    ) = InvoiceDraftData(
        direction = DocumentDirection.Inbound,
        issueDate = issueDate,
        dueDate = dueDate,
        subtotalAmount = Money.from("100.00"),
        vatAmount = Money.from("21.00"),
        totalAmount = Money.from("121.00"),
    )

    private fun creditNoteDraft() = CreditNoteDraftData(
        direction = DocumentDirection.Inbound,
        issueDate = LocalDate(2026, 2, 10),
        subtotalAmount = Money.from("50.00"),
        vatAmount = Money.from("10.50"),
        totalAmount = Money.from("60.50"),
    )

    private fun receiptDraft() = ReceiptDraftData(
        direction = DocumentDirection.Inbound,
        date = LocalDate(2026, 2, 10),
        totalAmount = Money.from("21.00"),
        vatAmount = Money.from("3.64"),
    )

    private fun cashflowEntry(status: CashflowEntryStatus) = CashflowEntry(
        id = CashflowEntryId.generate(),
        tenantId = TenantId.generate(),
        sourceType = CashflowSourceType.Invoice,
        sourceId = "source-id",
        documentId = DocumentId.generate(),
        direction = CashflowDirection.In,
        eventDate = LocalDate(2026, 2, 11),
        amountGross = Money.from("121.00")!!,
        amountVat = Money.from("21.00")!!,
        remainingAmount = if (status == CashflowEntryStatus.Paid) Money.ZERO else Money.from("121.00")!!,
        currency = tech.dokus.domain.enums.Currency.Eur,
        status = status,
        paidAt = if (status == CashflowEntryStatus.Paid) LocalDateTime(2026, 2, 15, 0, 0, 0) else null,
        contactId = null,
        createdAt = LocalDateTime(2026, 2, 11, 0, 0, 0),
        updatedAt = LocalDateTime(2026, 2, 11, 0, 0, 0),
    )

    private fun source(
        matchType: DocumentMatchType?,
        status: DocumentSourceStatus = DocumentSourceStatus.Linked,
    ) = DocumentSourceDto(
        id = DocumentSourceId.generate(),
        tenantId = TenantId.generate(),
        documentId = DocumentId.generate(),
        blobId = DocumentBlobId.generate(),
        sourceChannel = DocumentSource.Upload,
        arrivalAt = LocalDateTime(2026, 2, 10, 0, 0, 0),
        status = status,
        matchType = matchType,
        filename = "source.pdf",
    )

    private fun pendingReview() = DocumentMatchReviewSummaryDto(
        reviewId = DocumentMatchReviewId.generate(),
        reasonType = DocumentMatchReviewReasonType.MaterialConflict,
        status = DocumentMatchReviewStatus.Pending,
        createdAt = LocalDateTime(2026, 2, 10, 0, 0, 0),
    )
}
