package tech.dokus.features.cashflow.presentation.review.route

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import tech.dokus.domain.Money
import tech.dokus.domain.enums.DocumentDirection
import tech.dokus.domain.enums.DocumentSource
import tech.dokus.domain.enums.DocumentStatus
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.enums.IngestionStatus
import tech.dokus.domain.enums.InvoiceStatus
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.IngestionRunId
import tech.dokus.domain.ids.InvoiceId
import tech.dokus.domain.ids.InvoiceNumber
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.DocumentDraftDto
import tech.dokus.domain.model.DocumentDto
import tech.dokus.domain.model.DocumentIngestionDto
import tech.dokus.domain.model.DocumentRecordDto
import tech.dokus.domain.model.FinancialDocumentDto
import tech.dokus.domain.model.InvoiceDraftData
import tech.dokus.foundation.app.shell.DocQueueStatus
import tech.dokus.foundation.app.shell.DocQueueStatusDetail
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class DocumentQueueMapperTest {

    @Test
    fun `paid invoice maps to paid queue status`() {
        val record = record(
            draftStatus = DocumentStatus.Confirmed,
            ingestionStatus = IngestionStatus.Succeeded,
            dueDate = LocalDate(2999, 1, 1),
            paid = true,
        )

        val item = record.toDocQueueItem()

        assertEquals(DocQueueStatus.Paid, item.status)
        assertNull(item.statusDetail)
    }

    @Test
    fun `confirmed unpaid invoice maps to unpaid queue status`() {
        val record = record(
            draftStatus = DocumentStatus.Confirmed,
            ingestionStatus = IngestionStatus.Succeeded,
            dueDate = LocalDate(2999, 1, 1),
            paid = false,
        )

        val item = record.toDocQueueItem()

        assertEquals(DocQueueStatus.Unpaid, item.status)
        assertNull(item.statusDetail)
    }

    @Test
    fun `confirmed past-due invoice maps to overdue queue status with day suffix`() {
        val record = record(
            draftStatus = DocumentStatus.Confirmed,
            ingestionStatus = IngestionStatus.Succeeded,
            dueDate = LocalDate(2000, 1, 1),
            paid = false,
        )

        val item = record.toDocQueueItem()

        assertEquals(DocQueueStatus.Overdue, item.status)
        val statusDetail = assertNotNull(item.statusDetail)
        require(statusDetail is DocQueueStatusDetail.OverdueDays)
        assertEquals(true, statusDetail.days > 0)
    }

    @Test
    fun `needs-review document with processing ingestion maps to review processing detail`() {
        val record = record(
            draftStatus = DocumentStatus.NeedsReview,
            ingestionStatus = IngestionStatus.Processing,
            dueDate = LocalDate(2999, 1, 1),
            paid = false,
            includeConfirmedEntity = false,
        )

        val item = record.toDocQueueItem()

        assertEquals(DocQueueStatus.Review, item.status)
        assertEquals(DocQueueStatusDetail.Processing, item.statusDetail)
    }

    @Test
    fun `needs-review document with succeeded ingestion maps to review detail`() {
        val record = record(
            draftStatus = DocumentStatus.NeedsReview,
            ingestionStatus = IngestionStatus.Succeeded,
            dueDate = LocalDate(2999, 1, 1),
            paid = false,
            includeConfirmedEntity = false,
        )

        val item = record.toDocQueueItem()

        assertEquals(DocQueueStatus.Review, item.status)
        assertNull(item.statusDetail)
    }

    private fun record(
        draftStatus: DocumentStatus,
        ingestionStatus: IngestionStatus,
        dueDate: LocalDate,
        paid: Boolean,
        includeConfirmedEntity: Boolean = true,
    ): DocumentRecordDto {
        val tenantId = TenantId.parse("44e8ed5c-020a-4bbb-9439-ac85899c5589")
        val documentId = DocumentId.parse("e72f69a8-6913-4d8f-98e7-224db7f4133f")
        val now = LocalDateTime(2026, 2, 11, 0, 0, 0)
        val total = Money.from("121.00")!!

        val draftData = InvoiceDraftData(
            direction = DocumentDirection.Outbound,
            invoiceNumber = "INV-2026-001",
            issueDate = LocalDate(2026, 1, 1),
            dueDate = dueDate,
            subtotalAmount = Money.from("100.00")!!,
            vatAmount = Money.from("21.00")!!,
            totalAmount = total,
        )

        val draft = DocumentDraftDto(
            documentId = documentId,
            tenantId = tenantId,
            documentStatus = draftStatus,
            documentType = DocumentType.Invoice,
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

        val ingestion = DocumentIngestionDto(
            id = IngestionRunId.generate(),
            documentId = documentId,
            tenantId = tenantId,
            status = ingestionStatus,
            provider = "openai",
            queuedAt = now,
            startedAt = if (ingestionStatus != IngestionStatus.Queued) now else null,
            finishedAt = if (ingestionStatus == IngestionStatus.Succeeded || ingestionStatus == IngestionStatus.Failed) now else null,
            errorMessage = null,
            confidence = 0.92,
        )

        val confirmedEntity = if (includeConfirmedEntity) {
            FinancialDocumentDto.InvoiceDto(
                id = InvoiceId.generate(),
                tenantId = tenantId,
                direction = DocumentDirection.Outbound,
                contactId = ContactId.generate(),
                invoiceNumber = InvoiceNumber("INV-2026-001"),
                issueDate = LocalDate(2026, 1, 1),
                dueDate = dueDate,
                subtotalAmount = Money.from("100.00")!!,
                vatAmount = Money.from("21.00")!!,
                totalAmount = total,
                paidAmount = if (paid) total else Money.ZERO,
                status = when {
                    paid -> InvoiceStatus.Paid
                    dueDate < LocalDate(2026, 2, 11) -> InvoiceStatus.Overdue
                    else -> InvoiceStatus.Sent
                },
                documentId = documentId,
                paidAt = if (paid) LocalDateTime(2026, 1, 2, 0, 0, 0) else null,
                createdAt = now,
                updatedAt = now,
            )
        } else {
            null
        }

        return DocumentRecordDto(
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
            confirmedEntity = confirmedEntity,
        )
    }
}
