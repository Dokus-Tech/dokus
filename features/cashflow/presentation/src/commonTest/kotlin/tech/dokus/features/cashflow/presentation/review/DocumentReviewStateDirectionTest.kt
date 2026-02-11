package tech.dokus.features.cashflow.presentation.review

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import tech.dokus.domain.Money
import tech.dokus.domain.enums.CounterpartyIntent
import tech.dokus.domain.enums.DocumentDirection
import tech.dokus.domain.enums.DocumentSource
import tech.dokus.domain.enums.DocumentStatus
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.DocumentDraftDto
import tech.dokus.domain.model.DocumentDto
import tech.dokus.domain.model.DocumentRecordDto
import tech.dokus.domain.model.InvoiceDraftData
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DocumentReviewStateDirectionTest {

    @Test
    fun `confirm is blocked for invoice when direction is unknown`() {
        val state = contentState(direction = DocumentDirection.Unknown)

        assertFalse(state.canConfirm)
        assertTrue(state.confirmBlockedReason != null)
    }

    @Test
    fun `confirm is allowed after invoice direction is selected`() {
        val state = contentState(direction = DocumentDirection.Inbound)

        assertTrue(state.canConfirm)
    }

    private fun contentState(direction: DocumentDirection): DocumentReviewState.Content {
        val tenantId = TenantId.parse("44e8ed5c-020a-4bbb-9439-ac85899c5589")
        val documentId = DocumentId.parse("e72f69a8-6913-4d8f-98e7-224db7f4133f")
        val contactId = ContactId.parse("ee14421b-c659-45e3-9a27-e4d3ef6b32eb")
        val now = LocalDateTime(2026, 2, 11, 0, 0, 0)

        val draftData = InvoiceDraftData(
            direction = direction,
            issueDate = LocalDate(2026, 2, 10),
            subtotalAmount = Money.from("100.00")
        )

        val draft = DocumentDraftDto(
            documentId = documentId,
            tenantId = tenantId,
            documentStatus = DocumentStatus.NeedsReview,
            documentType = DocumentType.Invoice,
            extractedData = draftData,
            aiDraftData = draftData,
            aiDraftSourceRunId = null,
            draftVersion = 0,
            draftEditedAt = null,
            draftEditedBy = null,
            linkedContactId = contactId,
            counterpartyIntent = CounterpartyIntent.None,
            lastSuccessfulRunId = null,
            createdAt = now,
            updatedAt = now
        )

        val document = DocumentRecordDto(
            document = DocumentDto(
                id = documentId,
                tenantId = tenantId,
                filename = "invoice.pdf",
                contentType = "application/pdf",
                sizeBytes = 1200L,
                storageKey = "documents/$tenantId/invoice.pdf",
                source = DocumentSource.Upload,
                uploadedAt = now
            ),
            draft = draft,
            latestIngestion = null,
            confirmedEntity = null
        )

        return DocumentReviewState.Content(
            documentId = documentId,
            document = document,
            draftData = draftData,
            originalData = draftData,
            isContactRequired = true,
            selectedContactId = contactId,
            counterpartyIntent = CounterpartyIntent.None
        )
    }
}
