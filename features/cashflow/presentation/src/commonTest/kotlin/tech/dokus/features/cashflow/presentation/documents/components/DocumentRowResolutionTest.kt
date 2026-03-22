package tech.dokus.features.cashflow.presentation.documents.components

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import tech.dokus.domain.Money
import tech.dokus.domain.enums.DocumentDirection
import tech.dokus.domain.enums.DocumentSource
import tech.dokus.domain.enums.DocumentStatus
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.enums.IngestionStatus
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.IngestionRunId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.DocumentDetailDto
import tech.dokus.domain.model.DocumentDraftDto
import tech.dokus.domain.model.DocumentDto
import tech.dokus.domain.model.DocumentIngestionDto
import tech.dokus.domain.model.DocumentListItemDto
import tech.dokus.domain.model.InvoiceDraftData
import tech.dokus.domain.model.contact.ResolvedContact
import tech.dokus.domain.model.DocDto
import tech.dokus.domain.model.from
import tech.dokus.foundation.aura.model.DocumentUiStatus
import kotlin.test.Test
import kotlin.test.assertEquals

class DocumentRowResolutionTest {

    // =========================================================================
    // Detail-context helpers (DocumentDetailDto)
    // =========================================================================

    @Test
    fun `counterparty column uses snapshot name only`() {
        val record = detailRecord(
            counterpartyName = "Apple Distribution International Ltd.",
            invoiceNumber = "1-12218196743"
        )

        assertEquals("Apple Distribution International Ltd.", resolveCounterparty(record))
    }

    @Test
    fun `description uses snapshot name and document number`() {
        val record = detailRecord(
            counterpartyName = "Apple Distribution International Ltd.",
            invoiceNumber = "1-12218196743"
        )

        assertEquals(
            "Apple Distribution International Ltd. \u2014 1-12218196743",
            resolveDescription(record, "Unknown")
        )
    }

    @Test
    fun `description falls back to filename and document number when snapshot is missing`() {
        val record = detailRecord(
            counterpartyName = null,
            invoiceNumber = "1-12218196743"
        )

        assertEquals(
            "apple-invoice.pdf \u2014 1-12218196743",
            resolveDescription(record, "Unknown")
        )
    }

    // =========================================================================
    // List-context helpers (DocumentListItemDto)
    // =========================================================================

    @Test
    fun `queued records use filename and preparing status in list row`() {
        val record = listRecord(
            counterpartyName = "Any Vendor",
            purposeRendered = "INV-1",
            draftStatus = DocumentStatus.NeedsReview,
            ingestionStatus = IngestionStatus.Queued
        )

        assertEquals("apple-invoice.pdf", resolveListVendorName(record, "Unknown"))
        assertEquals(DocumentUiStatus.Queued, resolveListInlineStatus(record))
        assertEquals(
            DocumentListReferenceValue.Status(DocumentUiStatus.Queued),
            resolveListReferenceValue(record)
        )
    }

    @Test
    fun `processing records use filename and reading status in list row`() {
        val record = listRecord(
            counterpartyName = "Any Vendor",
            purposeRendered = "INV-2",
            draftStatus = DocumentStatus.NeedsReview,
            ingestionStatus = IngestionStatus.Processing
        )

        assertEquals("apple-invoice.pdf", resolveListVendorName(record, "Unknown"))
        assertEquals(DocumentUiStatus.Processing, resolveListInlineStatus(record))
        assertEquals(
            DocumentListReferenceValue.Status(DocumentUiStatus.Processing),
            resolveListReferenceValue(record)
        )
    }

    @Test
    fun `failed records use filename and failed status in list row`() {
        val record = listRecord(
            counterpartyName = "Any Vendor",
            purposeRendered = "INV-3",
            draftStatus = DocumentStatus.NeedsReview,
            ingestionStatus = IngestionStatus.Failed
        )

        assertEquals("apple-invoice.pdf", resolveListVendorName(record, "Unknown"))
        assertEquals(DocumentUiStatus.Failed, resolveListInlineStatus(record))
        assertEquals(
            DocumentListReferenceValue.Status(DocumentUiStatus.Failed),
            resolveListReferenceValue(record)
        )
    }

    @Test
    fun `review records keep normal counterparty and reference text`() {
        val record = listRecord(
            counterpartyName = "Tesla Belgium BVBA",
            purposeRendered = "INV-10",
            draftStatus = DocumentStatus.NeedsReview,
            ingestionStatus = IngestionStatus.Succeeded
        )

        assertEquals("Tesla Belgium BVBA", resolveListVendorName(record, "Unknown"))
        assertEquals(null, resolveListInlineStatus(record))
        assertEquals(
            DocumentListReferenceValue.Reference("INV-10"),
            resolveListReferenceValue(record)
        )
    }

    @Test
    fun `confirmed records keep normal counterparty and reference text`() {
        val record = listRecord(
            counterpartyName = "Tesla Belgium BVBA",
            purposeRendered = "INV-11",
            draftStatus = DocumentStatus.Confirmed,
            ingestionStatus = IngestionStatus.Succeeded
        )

        assertEquals("Tesla Belgium BVBA", resolveListVendorName(record, "Unknown"))
        assertEquals(null, resolveListInlineStatus(record))
        assertEquals(
            DocumentListReferenceValue.Reference("INV-11"),
            resolveListReferenceValue(record)
        )
    }

    // =========================================================================
    // Factories
    // =========================================================================

    private fun detailRecord(
        counterpartyName: String?,
        invoiceNumber: String?,
        draftStatus: DocumentStatus = DocumentStatus.NeedsReview,
        ingestionStatus: IngestionStatus? = null,
    ): DocumentDetailDto {
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
            documentStatus = draftStatus,
            documentType = DocumentType.Invoice,
            content = DocDto.from(extractedData),
            aiDraftSourceRunId = null,
            draftVersion = 0,
            draftEditedAt = null,
            draftEditedBy = null,
            resolvedContact = counterpartyName?.let {
                ResolvedContact.Detected(name = it, vatNumber = null, iban = null, address = null)
            } ?: ResolvedContact.Unknown,
            lastSuccessfulRunId = null,
            createdAt = now,
            updatedAt = now
        )

        return DocumentDetailDto(
            document = DocumentDto(
                id = documentId,
                tenantId = tenantId,
                filename = "apple-invoice.pdf",
                uploadedAt = now,
                sortDate = LocalDate(2026, 2, 11),
            ),
            draft = draft,
            latestIngestion = ingestionStatus?.let {
                DocumentIngestionDto(
                    id = IngestionRunId.generate(),
                    documentId = documentId,
                    tenantId = tenantId,
                    status = it,
                    provider = "openai",
                    queuedAt = now,
                    startedAt = now,
                    finishedAt = now,
                    errorMessage = null,
                    confidence = null
                )
            },
        )
    }

    private fun listRecord(
        counterpartyName: String?,
        purposeRendered: String?,
        draftStatus: DocumentStatus = DocumentStatus.NeedsReview,
        ingestionStatus: IngestionStatus? = null,
    ): DocumentListItemDto {
        val tenantId = TenantId.parse("44e8ed5c-020a-4bbb-9439-ac85899c5589")
        val documentId = DocumentId.parse("e72f69a8-6913-4d8f-98e7-224db7f4133f")
        val now = LocalDateTime(2026, 2, 11, 0, 0, 0)

        return DocumentListItemDto(
            documentId = documentId,
            tenantId = tenantId,
            filename = "apple-invoice.pdf",
            documentType = DocumentType.Invoice,
            direction = DocumentDirection.Inbound,
            documentStatus = draftStatus,
            ingestionStatus = ingestionStatus,
            effectiveOrigin = DocumentSource.Upload,
            uploadedAt = now,
            counterpartyDisplayName = counterpartyName,
            purposeRendered = purposeRendered,
            totalAmount = null,
            currency = null,
            sortDate = LocalDate(2026, 2, 11),
        )
    }
}
