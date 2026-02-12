package tech.dokus.features.cashflow.presentation.documents.components

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import tech.dokus.domain.Money
import tech.dokus.domain.enums.DocumentDirection
import tech.dokus.domain.enums.DocumentSource
import tech.dokus.domain.enums.DocumentStatus
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.DocumentDraftDto
import tech.dokus.domain.model.DocumentDto
import tech.dokus.domain.model.DocumentRecordDto
import tech.dokus.domain.model.InvoiceDraftData
import tech.dokus.domain.model.contact.CounterpartySnapshot
import kotlin.test.Test
import kotlin.test.assertEquals

class DocumentRowResolutionTest {

    @Test
    fun `counterparty column uses snapshot name only`() {
        val record = record(
            counterpartyName = "Apple Distribution International Ltd.",
            invoiceNumber = "1-12218196743"
        )

        assertEquals("Apple Distribution International Ltd.", resolveCounterparty(record))
    }

    @Test
    fun `description uses snapshot name and document number`() {
        val record = record(
            counterpartyName = "Apple Distribution International Ltd.",
            invoiceNumber = "1-12218196743"
        )

        assertEquals(
            "Apple Distribution International Ltd. — 1-12218196743",
            resolveDescription(record, "Unknown")
        )
    }

    @Test
    fun `description falls back to filename and document number when snapshot is missing`() {
        val record = record(
            counterpartyName = null,
            invoiceNumber = "1-12218196743"
        )

        assertEquals(
            "apple-invoice.pdf — 1-12218196743",
            resolveDescription(record, "Unknown")
        )
    }

    private fun record(counterpartyName: String?, invoiceNumber: String?): DocumentRecordDto {
        val tenantId = TenantId.parse("44e8ed5c-020a-4bbb-9439-ac85899c5589")
        val documentId = DocumentId.parse("e72f69a8-6913-4d8f-98e7-224db7f4133f")
        val now = LocalDateTime(2026, 2, 11, 0, 0, 0)

        val extractedData = InvoiceDraftData(
            direction = DocumentDirection.Inbound,
            invoiceNumber = invoiceNumber,
            issueDate = LocalDate(2026, 2, 10),
            subtotalAmount = Money.from("100.00")
        )

        val draft = DocumentDraftDto(
            documentId = documentId,
            tenantId = tenantId,
            documentStatus = DocumentStatus.NeedsReview,
            documentType = DocumentType.Invoice,
            extractedData = extractedData,
            aiDraftData = extractedData,
            aiDraftSourceRunId = null,
            draftVersion = 0,
            draftEditedAt = null,
            draftEditedBy = null,
            linkedContactId = null,
            counterpartySnapshot = counterpartyName?.let { CounterpartySnapshot(name = it) },
            lastSuccessfulRunId = null,
            createdAt = now,
            updatedAt = now
        )

        return DocumentRecordDto(
            document = DocumentDto(
                id = documentId,
                tenantId = tenantId,
                filename = "apple-invoice.pdf",
                contentType = "application/pdf",
                sizeBytes = 1200L,
                storageKey = "documents/$tenantId/apple-invoice.pdf",
                source = DocumentSource.Upload,
                uploadedAt = now
            ),
            draft = draft,
            latestIngestion = null,
            confirmedEntity = null
        )
    }
}
